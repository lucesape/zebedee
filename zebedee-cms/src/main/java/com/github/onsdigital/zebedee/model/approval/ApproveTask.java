package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.model.approval.tasks.ReleasePopulator;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.service.BabbagePdfService;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.*;

/**
 * Callable implementation for the approval process.
 */
public class ApproveTask implements Callable<Boolean> {

    private final Collection collection;
    private final String email;
    private final CollectionReader collectionReader;
    private final CollectionWriter collectionWriter;
    private final ContentReader publishedReader;
    private final DataIndex dataIndex;

    public ApproveTask(
            Collection collection,
            String email,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            ContentReader publishedReader,
            DataIndex dataIndex
    ) {
        this.collection = collection;
        this.email = email;
        this.collectionReader = collectionReader;
        this.collectionWriter = collectionWriter;
        this.publishedReader = publishedReader;
        this.dataIndex = dataIndex;
    }

    public static void generateTimeseries(
            Collection collection,
            ContentReader publishedReader,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            DataIndex dataIndex
    ) throws IOException, ZebedeeException, URISyntaxException {

        // Import any time series update CSV file
        List<TimeseriesUpdateCommand> updateCommands = ImportUpdateCommandCsvs(collection, publishedReader, collectionReader);

        // Generate time series if required.
        new DataPublisher().preprocessCollection(
                publishedReader,
                collectionReader,
                collectionWriter.getReviewed(), true, dataIndex, updateCommands);
    }

    public static List<TimeseriesUpdateCommand> ImportUpdateCommandCsvs(Collection collection, ContentReader publishedReader, CollectionReader collectionReader) throws ZebedeeException, IOException {
        List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
        if (collection.description.timeseriesImportFiles != null) {
            for (String importFile : collection.description.timeseriesImportFiles) {
                CompoundContentReader compoundContentReader = new CompoundContentReader(publishedReader);
                compoundContentReader.add(collectionReader.getReviewed());

                InputStream csvInput = collectionReader.getRoot().getResource(importFile).getData();

                // read the CSV and update the timeseries titles.
                TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

                logInfo("Importing CSV file").addParameter("filename", importFile).log();
                updateCommands.addAll(importer.importData());
            }
        }
        return updateCommands;
    }

    public static PublishNotification createPublishNotification(CollectionReader collectionReader, Collection collection) {
        List<String> uriList = collectionReader.getReviewed().listUris();

        // only provide relevent uri's
        //  - remove versioned uris
        //  - add associated uris? /previous /data etc?

        List<ContentDetail> contentToDelete = new ArrayList<>();
        List<PendingDelete> pendingDeletes = collection.getDescription().getPendingDeletes();


        for (PendingDelete pendingDelete : pendingDeletes) {

            ContentTreeNavigator.getInstance().search(pendingDelete.getRoot(), node -> {
                logDebug("Adding uri to delete to the publish notification " + node.uri);
                if (!contentToDelete.contains(node.uri)) {
                    ContentDetail contentDetailToDelete = new ContentDetail();
                    contentDetailToDelete.uri = node.uri;
                    contentDetailToDelete.type = node.type;
                    contentToDelete.add(contentDetailToDelete);
                }
            });
        }

        return new PublishNotification(collection, uriList, contentToDelete);
    }

    @Override
    public Boolean call() {

        try {

            List<ContentDetail> collectionContent = ContentDetailUtil.resolveDetails(collection.reviewed, collectionReader.getReviewed());

            populateReleasePage(collectionContent);
            generateTimeseries(collection, publishedReader, collectionReader, collectionWriter, dataIndex);
            generatePdfFiles(collectionContent);

            PublishNotification publishNotification = createPublishNotification(collectionReader, collection);

            compressZipFiles(collection, collectionReader, collectionWriter);
            approveCollection();

            // Send a notification to the website with the publish date for caching.
            publishNotification.sendNotification(EventType.APPROVED);

            return true;

        } catch (IOException | ZebedeeException | URISyntaxException e) {

            logError(e, "Exception approving collection").collectionName(collection).log();

            collection.description.approvalStatus = ApprovalStatus.ERROR;
            try {
                collection.save();
            } catch (IOException e1) {
                logError(e, "Exception saving collection after approval exception").collectionName(collection).log();
            }

            SlackNotification.alarm(String.format("Exception approving collection %s : %s", collection.description.name, e.getMessage()));
            return false;
        }
    }

    private void compressZipFiles(Collection collection, CollectionReader collectionReader, CollectionWriter collectionWriter) throws ZebedeeException, IOException {
        TimeSeriesCompressionTask timeSeriesCompressionTask = new TimeSeriesCompressionTask();
        boolean verified = timeSeriesCompressionTask.compressTimeseries(collection, collectionReader, collectionWriter);

        if (!verified) {
            String message = "Failed verification of time series zip files";
            logInfo(message).collectionName(collection).log();
            SlackNotification.alarm(message + " in collection " + collection.description.name + ". Unlock the collection and re-approve to try again.");
        }
    }

    public void approveCollection() throws IOException {
        // set the approved state on the collection
        collection.description.approvalStatus = ApprovalStatus.COMPLETE;
        collection.description.AddEvent(new Event(new Date(), EventType.APPROVED, email));
        collection.save();
    }

    public void populateReleasePage(List<ContentDetail> collectionContent) throws IOException {
        // If the collection is associated with a release then populate the release page.
        ReleasePopulator.populateQuietly(collection, collectionReader, collectionWriter, collectionContent);
    }

    public void generatePdfFiles(List<ContentDetail> collectionContent) {
        // BabbagePdf needs a look at!
        CollectionPdfGenerator pdfGenerator = new CollectionPdfGenerator(new BabbagePdfService(new Session(), collection));
        pdfGenerator.generatePdfsInCollection(collectionWriter, collectionContent);
    }
}
