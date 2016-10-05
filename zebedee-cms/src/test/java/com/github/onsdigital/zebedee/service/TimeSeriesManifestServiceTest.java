package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 *
 */
public class TimeSeriesManifestServiceTest {

    private static Path collectionPath = Paths.get("test-collection");
    private static final String FILENAME = "timeseries-manifest.json";
    private static Path manifestPath = collectionPath.resolve(FILENAME);
    private static final String CDID = "abml";
    private static final String DATASET_ID = "qna";
    private static final Path TS_FILE_PATH = Paths.get("/economy/grossvalueaddedgva/timeseries/abml/qna");

    @Mock
    private ExecutorService executorServiceMock;

    @Mock
    private Collection collectionMock;

    @Mock
    private Session sessionMock;

    @Mock
    private DataIndex dataIndexMock;

    @Mock
    private TimeSeriesManifest timeSeriesManifestMock;

    @Mock
    private BiFunction<Path, DataIndex, TimeSeriesManifest> getTimeSeriesManifestMock;

    @Mock
    private BiFunction<Path, TimeSeriesManifest, Boolean> saveCollectionManifestMock;

    private TimeSeriesManifestService service;
    private TimeSeriesManifest expectedManifest;
    private Path collectionRootPath;
    private Path timeSeriesManifestPath;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = TimeSeriesManifestService.get();

        expectedManifest = new TimeSeriesManifest(null).addManifestEntry(DATASET_ID, TS_FILE_PATH);


        setField(service, "executorService", executorServiceMock);
    }

    private void createManifestFile() throws Exception {
        collectionRootPath = Files.createTempDirectory(collectionPath.toString());
        timeSeriesManifestPath = Files.createFile(collectionRootPath.resolve(FILENAME));
    }

    @Test
    public void shouldSaveCollectionManifest() throws Exception {
        when(saveCollectionManifestMock.apply(any(Path.class), any(TimeSeriesManifest.class))).thenReturn(true);
        when(collectionMock.getPath()).thenReturn(collectionPath);

        setField(service, "saveCollectionManifest", saveCollectionManifestMock);

        assertThat("Expected service to return true", service.saveCollectionManifest(collectionMock, timeSeriesManifestMock), is(true));

        verify(saveCollectionManifestMock, times(1)).apply(collectionPath.resolve(FILENAME), timeSeriesManifestMock);
    }

    @Test
    public void shouldNotDeleteTimeSeriesFilesIfDataSetIdNotInManifest() throws Exception {
        String targetDatasetId = "abcde";
        TimeSeriesManifest manifest = new TimeSeriesManifest(dataIndexMock).addManifestEntry(DATASET_ID, TS_FILE_PATH);

        setField(service, "executorService", executorServiceMock);
        setField(service, "saveCollectionManifest", saveCollectionManifestMock);
        setField(service, "getTimeSeriesManifest", getTimeSeriesManifestMock);

        when(collectionMock.getPath())
                .thenReturn(collectionPath);

        when(getTimeSeriesManifestMock.apply(manifestPath, dataIndexMock))
                .thenReturn(manifest);

        boolean result = service.deleteGeneratedTimeSeriesFilesByDataId(targetDatasetId, dataIndexMock,
                collectionMock, sessionMock);

        assertThat("Expected result to be false for a datasetId not in the manifest", result, is(false));
        verify(getTimeSeriesManifestMock, times(1)).apply(manifestPath, dataIndexMock);
        verifyZeroInteractions(executorServiceMock, saveCollectionManifestMock);
    }

    /**
     * Test reads manifest from file system and checks it is as expected.
     */
    @Ignore
    @Test
    public void shouldGetExistingTimeSeriesManifest() throws Exception {
        createManifestFile();
        writeJson(timeSeriesManifestPath, expectedManifest);

        when(collectionMock.getPath()).thenReturn(collectionRootPath);

        TimeSeriesManifest actualManifest = service.getCollectionManifest(collectionMock, dataIndexMock);

        assertThat("Manifest not as expectedManifest", actualManifest, equalTo(expectedManifest));
    }

    @Test
    public void shouldReturnNewManifestIfOneDoesNotExist() throws Exception {
        setField(service, "getTimeSeriesManifest", getTimeSeriesManifestMock);

        when(collectionMock.getPath())
                .thenReturn(collectionPath);

        when(getTimeSeriesManifestMock.apply(manifestPath, dataIndexMock))
                .thenReturn(new TimeSeriesManifest(null));

        TimeSeriesManifest actualManifest = service.getCollectionManifest(collectionMock, dataIndexMock);
        assertThat("Manifest not as expectedManifest", actualManifest, equalTo(new TimeSeriesManifest(null)));

    }

    @Ignore
    @Test
    public void shouldDeleteTimeSeriesZips() throws Exception {
        createManifestFile();
        expectedManifest.addTimeSeriesZip(Paths.get("/test-zip.zip"));
        writeJson(timeSeriesManifestPath, expectedManifest);

        when(collectionMock.getPath()).thenReturn(collectionRootPath);

        boolean result = service.deleteGeneratedTimeSeriesZips(collectionMock, sessionMock, dataIndexMock);

        assertThat("Expected true", result, is(true));
        assertThat("Manifest is not as expected", getManifest().getTimeseriesZips().isEmpty());
        verify(collectionMock, times(1)).deleteFile("/test-zip.zip");
    }



    @After
    public void cleanUp() throws Exception {
        if (collectionRootPath != null) {
            FileUtils.deleteQuietly(collectionRootPath.toFile());
        }
    }

    private void writeJson(Path path, TimeSeriesManifest manifest) throws Exception {
        Files.write(path, new Gson().toJson(manifest).getBytes());
    }

    private TimeSeriesManifest getManifest() throws IOException {
        // Read the manifest into java object.
        StringWriter writer = new StringWriter();
        try (InputStream in = new FileInputStream(collectionRootPath.resolve(FILENAME).toFile())) {
            IOUtils.copy(in, writer);
        }
        return new Gson().fromJson(writer.toString(), TimeSeriesManifest.class);
    }


}
