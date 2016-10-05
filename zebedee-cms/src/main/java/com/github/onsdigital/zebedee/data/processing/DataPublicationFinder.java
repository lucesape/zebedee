package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.service.TimeSeriesManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.util.CommonStrings.DATA_JSON;
import static com.github.onsdigital.zebedee.util.CommonStrings.SLASH_DATA_JSON;

public class DataPublicationFinder {

    private static final String NO_TIME_SERIES_IN_PUBLISH_MSG = "Publication does not contain any TimeSeries to generate.";
    private static final String NO_NEW_TIME_SERIES_IN_PUBLISH_MSG = "Time series for this publication have already been generated.";
    private static final String TIME_SERIES_IN_PUBLISH_MSG = "Time series content will be generated during Approval of this publication";

    /**
     * Find all pages in a collection that need to be published using the data publisher
     *
     * @param publishedContentReader a content reader for published content
     * @param reviewedContentReader  a content reader for the collection reviewed content
     * @return a list of DataPublication objects
     * @throws IOException
     * @throws ZebedeeException
     */
    public List<DataPublication> findPublications(ContentReader publishedContentReader, ContentReader reviewedContentReader,
                                                  TimeSeriesManifest manifest) throws IOException, ZebedeeException {
        List<DataPublication> results = new ArrayList<>();

        // Loop through the uri's in the collection
        for (String reviewedUri : reviewedContentReader.listUris()) {

            // Ignoring previous versions loop through the pages
            if (!reviewedUri.toLowerCase().contains("/previous/") && reviewedUri.toLowerCase().endsWith(DATA_JSON)) {

                // Strip off data.json
                String pageUri = reviewedUri.substring(0, reviewedUri.length() - SLASH_DATA_JSON.length());

                // Find all timeseries_datasets
                Page page = reviewedContentReader.getContent(pageUri);
                if (page != null && page.getType() == PageType.timeseries_dataset) {

                    DataPublication newPublication = new DataPublication(publishedContentReader,
                            reviewedContentReader, pageUri);
                    Path filePath = Paths.get(newPublication.getDetails().fileUri);

                    if (!manifest.containsDataset(filePath)) {
                        results.add(newPublication);
                    } else {
                        logDebug("Existing TimeSeries manifest entry. Skipping TimeSeries file generation")
                                .path(pageUri)
                                .log();
                    }
                }
            }
        }
        logDebug(NO_NEW_TIME_SERIES_IN_PUBLISH_MSG).log();
        return logOutcomeAndReturnResult(results, manifest);
    }

    private List<DataPublication> logOutcomeAndReturnResult(List<DataPublication> results, TimeSeriesManifest manifest) {
        String logMessage;
        if (results.isEmpty() && manifest.isEmpty()) {
            logMessage = NO_TIME_SERIES_IN_PUBLISH_MSG;
        } else if (results.isEmpty() && !manifest.isEmpty()) {
            logMessage = NO_NEW_TIME_SERIES_IN_PUBLISH_MSG;
        } else {
            logMessage = TIME_SERIES_IN_PUBLISH_MSG;
        }
        logDebug(logMessage).log();
        return results;
    }
}
