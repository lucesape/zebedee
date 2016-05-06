package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.importing.CsvTimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateImporter;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.debugMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Given as CSV indexed with the timeseries CDID, update each timeseries with the given data.
 */
public class TimeseriesUpdater {

    public static void updateTimeseries(CompoundContentReader contentReader, ContentWriter contentWriter, InputStream csvInput, DataIndex dataIndex) throws IOException {

        // read the CSV and update the timeseries titles.
        TimeseriesUpdateImporter importer = new CsvTimeseriesUpdateImporter(csvInput);

        debugMessage("Importing CSV file").log();
        ArrayList<TimeseriesUpdateCommand> updateCommandsImported = importer.importData();

        ArrayList<TimeseriesUpdateCommand> updateCommands = filterTimeseriesThatDoNotExist(dataIndex, updateCommandsImported);

        debugMessage("Updating timeseries with new metadata").log();
        updateTimeseriesMetadata(contentReader, contentWriter, updateCommands);
    }

    public static ArrayList<TimeseriesUpdateCommand> filterTimeseriesThatDoNotExist(DataIndex dataIndex, ArrayList<TimeseriesUpdateCommand> updateCommandsImported) {
        ArrayList<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();

        for (TimeseriesUpdateCommand command : updateCommandsImported) {
            String uri = dataIndex.getUriForCdid(command.cdid.toLowerCase());

            if (uri == null) {
                debugMessage("CDID not found in data index").addParameter("CDID", command.cdid).log();
                continue;
            } else {
                command.uri = uri;
                updateCommands.add(command);
            }
        }
        return updateCommands;
    }

    public static void updateTimeseriesMetadata(CompoundContentReader contentReader, ContentWriter contentWriter, ArrayList<TimeseriesUpdateCommand> updateCommands) throws IOException {
        for (TimeseriesUpdateCommand command : updateCommands) {

            try {
                boolean updated = false;
                TimeSeries page = (TimeSeries) contentReader.getContent(command.uri);

                command.sourceDatasets = page.sourceDatasets;

                if (command.title != null && command.title.length() > 0) {
                    page.getDescription().setTitle(command.title);
                    updated = true;
                }

                if (command.releaseDate != null) {
                    page.getDescription().setReleaseDate(command.releaseDate);
                    updated = true;
                }

                if (updated) {
                    contentWriter.writeObject(page, command.uri + "/data.json");
                }

            } catch (Exception e) {
                logError(e).errorContext("Failed to read timeseries page").addParameter("uri", command.uri).log();
            }
        }
    }
}
