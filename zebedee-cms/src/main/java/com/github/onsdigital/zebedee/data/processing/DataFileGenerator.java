package com.github.onsdigital.zebedee.data.processing;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds CSV, XLSX and potentially other files from a set of timeseries
 */
public class DataFileGenerator {
    private static final String DEFAULT_FILENAME = "data";

    ContentWriter contentWriter;

    public DataFileGenerator(ContentWriter collectionWriter) {
        this.contentWriter = collectionWriter;
    }

    /**
     * Generate the files for this data publication
     *
     * @param details details about the publication to derive the save path
     * @param serieses the timeseries
     */
    public List<DownloadSection> generateDataDownloads(DataPublicationDetails details, TimeSerieses serieses) throws IOException, BadRequestException {

        // turn our list of timeseries into a grid to write to file
        DataGrid grid = new DataGrid(serieses);

        // get the correct place to save data files
        String root = fileRoot(details);

        // write the files
        List<DownloadSection> sections = new ArrayList<>();
        sections.add( writeCSV(grid, this.contentWriter, root + ".csv") );
        sections.add( writeXLS(grid, this.contentWriter, root + ".xlsx") );

        return sections;
    }

    private String fileRoot(DataPublicationDetails details) {
        if (details.landingPage.getDescription().getDatasetId() == null || details.landingPage.getDescription().getDatasetId().trim().length() == 0) {
            return details.datasetPage.getUri().toString() + "/" + DEFAULT_FILENAME;
        } else {
            return details.datasetPage.getUri().toString() + "/" + details.landingPage.getDescription().getDatasetId().toLowerCase().trim();
        }
    }

    /**
     * Write a DataGrid as an xlsx file
     *
     * @param grid
     * @param contentWriter
     * @param xlsPath
     * @throws IOException
     * @throws BadRequestException
     */
    private DownloadSection writeXLS(DataGrid grid, ContentWriter contentWriter, String xlsPath) throws IOException, BadRequestException {
        Workbook wb = new SXSSFWorkbook(30);
        Sheet sheet = wb.createSheet("data");

        // Add the metadata
        int rownum = 0;
        for (DataGridRow dataGridRow : grid.metadata) {
            Row row = sheet.createRow(rownum++);

            int colnum = 0;
            row.createCell(colnum++).setCellValue(dataGridRow.label);
            for (String gridCell : dataGridRow.cells) {
                row.createCell(colnum++).setCellValue(gridCell);
            }
        }

        // Write the data
        for (DataGridRow dataGridRow : grid.rows) {
            Row row = sheet.createRow(rownum++);

            int colnum = 0;
            row.createCell(colnum++).setCellValue(dataGridRow.label);
            for (String gridCell : dataGridRow.cells) {
                row.createCell(colnum++).setCellValue(gridCell);
            }
        }

        try (OutputStream stream = contentWriter.getOutputStream(xlsPath)) {
            wb.write(stream);
        }

        return newDownloadSection("xlsx download", xlsPath);
    }

    /**
     *
     * @param grid
     * @param contentWriter
     * @param csvPath
     */
    private DownloadSection writeCSV(DataGrid grid, ContentWriter contentWriter, String csvPath) throws IOException, BadRequestException {

        OutputStream outputStream = contentWriter.getOutputStream(csvPath);
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream, Charset.forName("UTF8")), ',')) {
            for (DataGridRow dataGridRow : grid.metadata) {
                String[] row = new String[dataGridRow.cells.size() + 1];
                row[0] = dataGridRow.label;
                for(int i = 1; i <= dataGridRow.cells.size(); i++)
                    row[i] = dataGridRow.cells.get(i - 1);
                writer.writeNext(row);
            }

            for (DataGridRow dataGridRow : grid.rows) {
                String[] row = new String[dataGridRow.cells.size() + 1];
                row[0] = dataGridRow.label;
                for(int i = 1; i <= dataGridRow.cells.size(); i++)
                    row[i] = dataGridRow.cells.get(i - 1);
                writer.writeNext(row);
            }
        }
        return newDownloadSection("csv download", csvPath);
    }

    private DownloadSection newDownloadSection(String title, String file) {
        DownloadSection section = new DownloadSection();
        section.setTitle(title);
        section.setFile(file);
        return section;
    }
}