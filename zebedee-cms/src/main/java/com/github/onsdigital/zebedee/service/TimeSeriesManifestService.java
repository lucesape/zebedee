package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Service provides functionality to delete time series generated files from a collection.
 */
public class TimeSeriesManifestService {

    private static final String LOG_MSG = "Deleting generated times series files from collection.";
    private static final String NO_DELETES_LOG_MSG = "TimeSeries Manifest contains no dataSet entries. No files will be deleted";
    private static final String ERROR_LOG_MSG = "Error while deleting old time series from collection";
    private static final String ZIPS_REMOVED_LOG_MSG = "Collection unlocked. Deleting generated time series zip files.";
    private static final String FILENAME = "timeseries-manifest.json";

    private static ExecutorService executorService = Executors.newFixedThreadPool(20);
    private static final TimeSeriesManifestService instance = new TimeSeriesManifestService();

    /**
     * {@link BiFunction} creates {@link Callable} jobs to delete {@link TimeSeriesManifest} entries.
     */
    private BiFunction<Collection, Path, Callable<Boolean>> callableFactoryFunction = (collection, uri) -> () -> {
        if (collection.isInCollection(uri.toString())) {
            Path deletePath = collection.find(uri.toString());
            File parent = deletePath.getParent().toFile();
            FileUtils.deleteQuietly(deletePath.toFile());

            if (parent != null && parent.isDirectory() && parent.list().length == 0) {
                FileUtils.deleteQuietly(parent);
            }
        }
        return true;
    };

    /**
     * {@link BiFunction} saves a {@link TimeSeriesManifest} as a json file in the root of the collection.
     */
    private BiFunction<Path, TimeSeriesManifest, Boolean> saveCollectionManifestFunction = (manifestPath, manifest) -> {
        try (OutputStream out = Files.newOutputStream(manifestPath)) {
            IOUtils.write(new Gson().toJson(manifest).getBytes(), out);
        } catch (IOException e) {
            logError(e, "Unexpected error while attempting to save TimeSeriesManifest")
                    .path(manifestPath.toString()).throwUnchecked(e);
        }
        return true;
    };

    private BiFunction<Path, DataIndex, TimeSeriesManifest> getTimeSeriesManifestFunction = ((manifestPath, dataIndex) -> {
        try {
            if (Files.exists(manifestPath)) {
                try (InputStream in = Files.newInputStream(manifestPath)) {
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(in, writer);
                    TimeSeriesManifest manifest = new Gson().fromJson(writer.toString(), TimeSeriesManifest.class);
                    manifest.setDataIndex(dataIndex);
                    return manifest;
                }
            }
            return new TimeSeriesManifest(dataIndex);
        } catch (Exception ex) {
            throw logError(ex, "Unexpected error while attempting to read timeseries-manifest.json")
                    .path(manifestPath.toString()).uncheckedException(ex);
        }
    });

    /**
     * @return singleton instance of the service - use this instead of creating one.
     */
    public static TimeSeriesManifestService get() {
        return instance;
    }

    /**
     * Create a new TimeSeriesManifestService.
     *
     * @param executorService an impl of {@link ExecutorService} to manage and execute each of the jobs.
     * @param callableFactoryFunction a functional interface to create the delete jobs.
     * @param saveCollectionManifestFunction a functional interface to save the collection manifest to disk;
     * @param getTimeSeriesManifestFunction a functional interface to get the {@link TimeSeriesManifest} from disk;
     */
    TimeSeriesManifestService(final ExecutorService executorService,
                              final BiFunction<Collection, Path, Callable<Boolean>> callableFactoryFunction,
                              final BiFunction<Path, TimeSeriesManifest, Boolean> saveCollectionManifestFunction,
                              final BiFunction<Path, DataIndex, TimeSeriesManifest> getTimeSeriesManifestFunction) {
        this.executorService = executorService;
        this.callableFactoryFunction = callableFactoryFunction;
        this.saveCollectionManifestFunction = saveCollectionManifestFunction;
        this.getTimeSeriesManifestFunction = getTimeSeriesManifestFunction;
    }

    private TimeSeriesManifestService() {
        // Hide constructor.
    }

    /**
     * Deletes timeseries files generated by {@link com.github.onsdigital.zebedee.model.approval.ApproveTask}. Typical
     * usage unlocking an approved collection and changing the uploaded .csdb file.
     */
    public Boolean deleteGeneratedTimeSeriesFilesByDataId(String datasetId, DataIndex dataIndex, Collection collection,
                                                          Session session) throws ZebedeeException, IOException {
        boolean result = false;
        TimeSeriesManifest manifest = getTimeSeriesManifestFunction.apply(manifestPath(collection), dataIndex);

        if (!manifest.isEmpty()) {
            Optional<Set<Path>> deletes = manifest.getByDatasetId(datasetId);
            if (deletes.isPresent()) {
                try {
                    executorService.invokeAll(
                            deletes.get()
                                    .stream()
                                    .map(targetUri -> callableFactoryFunction.apply(collection, targetUri))
                                    .collect(Collectors.toList()));

                    manifest.removeDataset(datasetId);
                    saveCollectionManifestFunction.apply(manifestPath(collection), manifest);
                    result = true;
                } catch (InterruptedException ex) {
                    throw logError(ex, ERROR_LOG_MSG).collectionName(collection).user(session.email)
                            .logAndThrowEX(BadRequestException.class);
                }
            }
            logDebug(result ? LOG_MSG : NO_DELETES_LOG_MSG).collectionName(collection).user(session.email)
                    .dataSetId(datasetId).log();
        }
        return result;
    }

    public boolean deleteGeneratedTimeSeriesZips(Collection collection, Session session, DataIndex dataIndex)
            throws ZebedeeException, IOException {
        TimeSeriesManifest manifest = getCollectionManifest(collection, dataIndex);
        boolean result = false;

        if (!manifest.getTimeseriesZips().isEmpty()) {
            Iterator<String> iterator = manifest.getTimeseriesZips().iterator();
            while (iterator.hasNext()) {
                String zipPath = iterator.next();
                collection.deleteFile(zipPath);
                iterator.remove();
            }
            logDebug(ZIPS_REMOVED_LOG_MSG).collectionName(collection).user(session.email).log();
            saveCollectionManifestFunction.apply(manifestPath(collection), manifest);
            result = true;
        }
        return result;
    }

    public boolean saveCollectionManifest(Collection collection, TimeSeriesManifest manifest) {
        return saveCollectionManifestFunction.apply(manifestPath(collection), manifest);
    }

    public TimeSeriesManifest getCollectionManifest(Collection collection, DataIndex dataIndex) throws ZebedeeException {
        return getTimeSeriesManifestFunction.apply(manifestPath(collection), dataIndex);
    }

    private Path manifestPath(Collection collection) {
        return collection.getPath().resolve(FILENAME);
    }
}
