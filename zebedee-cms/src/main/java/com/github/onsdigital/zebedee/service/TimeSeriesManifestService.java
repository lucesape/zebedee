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
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Service provides functionality to delete time series generated files from a collection.
 */
public class TimeSeriesManifestService {

    private static final String LOG_MSG = "Deleting generated times series files from collection.";
    private static final String NO_DELETES_LOG_MSG = "TimeSeries Manifest contains no dataSet entries. No files will be deleted";
    private static final String ERROR_LOG_MSG = "Error while deleting old time series from collection";
    private static final String ZIPS_REMOVED_LOG_MSG = "Collection unlocked. Deleting generated time series zip files.";
    private static final String FILENAME = "timeseries-manifest.json";
    private static final String DATA_JSON = "data.json";

    private static ExecutorService executorService = Executors.newFixedThreadPool(20);
    private static final TimeSeriesManifestService instance = new TimeSeriesManifestService();

    public static TimeSeriesManifestService get() {
        return instance;
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
        TimeSeriesManifest manifest = getCollectionManifest(collection, dataIndex);

        if (!manifest.isEmpty()) {
            Optional<Set<Path>> deletes = manifest.getByDatasetId(datasetId);

            if (!deletes.isPresent()) {
                logDebug(NO_DELETES_LOG_MSG).collectionName(collection).user(session.email).dataSetId(datasetId).log();
                return false;
            }
            try {
                executorService.invokeAll(deletes.get()
                        .stream()
                        .map(targetUri -> cleanUpTask(collection, targetUri))
                        .collect(Collectors.toList()));
                manifest.removeDataset(datasetId);
                logInfo(LOG_MSG).collectionName(collection).user(session.email).dataSetId(datasetId).log();
                saveCollectionManifest(collection, manifest);
                return true;
            } catch (InterruptedException ex) {
                throw logError(ex, ERROR_LOG_MSG).collectionName(collection).user(session.email)
                        .logAndThrowX(BadRequestException.class);
            }
        }
        return false;
    }

    public boolean deleteGeneratedTimeSeriesZips(Collection collection, Session session, DataIndex dataIndex)
            throws ZebedeeException, IOException {
        TimeSeriesManifest manifest = getCollectionManifest(collection, dataIndex);
        Set<String> zips = manifest.getTimeseriesZips();
        boolean result = false;

        if (!zips.isEmpty()) {
            for (String path : zips) {
                collection.deleteFile(path);
                manifest.removeZip(Paths.get(path));
            }
            logDebug(ZIPS_REMOVED_LOG_MSG).collectionName(collection).user(session.email).log();
            saveCollectionManifest(collection, manifest);
            result = true;
        }
        return result;
    }

    private Callable<Boolean> cleanUpTask(Collection collection, Path uri) {
        return () -> {
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
    }

    public void saveCollectionManifest(Collection collection, TimeSeriesManifest manifest) throws IOException {
        try (OutputStream out = Files.newOutputStream(manifestPath(collection))) {
            IOUtils.write(new Gson().toJson(manifest).getBytes(), out);
        }
    }

    public TimeSeriesManifest getCollectionManifest(Collection collection, DataIndex dataIndex) throws ZebedeeException {
        try {
            Path manifestPath = manifestPath(collection);
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
                    .collectionPath(collection).logAndThrowX(BadRequestException.class);
        }
    }

    private Path manifestPath(Collection collection) {
        return collection.path.resolve(FILENAME);
    }
}
