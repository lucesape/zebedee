package com.github.onsdigital.zebedee.model.approval.tasks.timeseries;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.service.TimeSeriesManifest;
import com.github.onsdigital.zebedee.util.ZipUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class TimeSeriesCompressor {

    /**
     * Find each time series directory in a collection and create a zip file for it.
     * <p>
     * Uses content
     *
     * @param contentReader
     * @param contentWriter
     * @throws BadRequestException
     * @throws IOException
     */
    public List<TimeseriesCompressionResult> compressFiles(ContentReader contentReader, ContentWriter contentWriter,
                                                           boolean isEncrypted, TimeSeriesManifest manifest)
            throws ZebedeeException, IOException {

        List<Path> timeSeriesDirectories = contentReader.listTimeSeriesDirectories();
        List<TimeseriesCompressionResult> results = new ArrayList<>();

        for (Path timeSeriesDirectory : timeSeriesDirectories) {
            String saveUri = getZipUri(contentReader, timeSeriesDirectory);
            manifest.addTimeSeriesZip(Paths.get(saveUri));
            int filesAdded = compressFile(contentReader, contentWriter, isEncrypted, timeSeriesDirectory, saveUri);
            results.add(new TimeseriesCompressionResult(timeSeriesDirectory, Paths.get(saveUri), filesAdded));
        }

        return results;
    }

    /**
     * Compress each file defined in the compression results.
     *
     * @param contentReader
     * @param contentWriter
     * @param isEncrypted
     * @param zipFilesToCompress
     * @return
     * @throws IOException
     * @throws ZebedeeException
     */
    public List<TimeseriesCompressionResult> compressFiles(ContentReader contentReader, ContentWriter contentWriter,
                                                           boolean isEncrypted, TimeSeriesManifest manifest,
                                                           List<TimeseriesCompressionResult> zipFilesToCompress)
            throws IOException, ZebedeeException {
        for (TimeseriesCompressionResult result : zipFilesToCompress) {
            // Add to manifest. - may need to be independent of dataset id.
            String zipUri = getZipUri(contentReader, result.sourcePath);
            int filesAdded = compressFile(contentReader, contentWriter, isEncrypted, result.sourcePath, zipUri);
            manifest.addTimeSeriesZip(Paths.get(zipUri));
            result.numberOfFiles = filesAdded;
        }

        return zipFilesToCompress;
    }

    /**
     * Resolve the zip uri.
     *
     * @param contentReader
     * @param sourcePath
     * @return
     */
    private String getZipUri(ContentReader contentReader, Path sourcePath) {
        return contentReader.getRootFolder().relativize(sourcePath).toString() + "-to-publish.zip";
    }

    public int compressFile(ContentReader contentReader, ContentWriter contentWriter, boolean isEncrypted,
                            Path timeSeriesDirectory, String saveUri) throws IOException, ZebedeeException {
        logInfo("Compressing time series directory").addParameter("directory", timeSeriesDirectory.toString()).log();
        if (!isEncrypted) {
            try (OutputStream outputStream = contentWriter.getOutputStream(saveUri)) {
                return ZipUtils.zipFolder(timeSeriesDirectory.toFile(), outputStream, url -> VersionedContentItem.isVersionedUri(url));
            }
        } else {
            return ZipUtils.zipFolderWithEncryption(contentReader, contentWriter, timeSeriesDirectory.toFile().toString(),
                    saveUri, url -> VersionedContentItem.isVersionedUri(url));
        }
    }
}
