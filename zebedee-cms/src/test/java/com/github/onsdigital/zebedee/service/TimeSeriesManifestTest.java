package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.DATASET_ID_NULL_OR_EMPTY;
import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.TIME_SERIES_DESC_NULL;
import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.TIME_SERIES_NULL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TimeSeriesManifestTest {

    static final String DATASET_ID = "ABCDE";
    static final String CDID = "FGHI";
    static final String CDID_URI = Paths.get("/somepath").resolve(CDID.toLowerCase()).toString();
    static final String EXPECTED_PATH = Paths.get(CDID_URI).resolve(DATASET_ID).toString().toLowerCase();

    @Mock
    private DataIndex dataIndexMock;

    @Mock
    private TimeSeries timeSeriesMock;

    @Mock
    private PageDescription pageDescriptionMock;

    private TimeSeriesManifest manifest;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        manifest = new TimeSeriesManifest(dataIndexMock);
    }

    @Test
    public void shouldAddEntryToManifest() throws Exception {
        timeSeriesSetUp();
        when(dataIndexMock.getUriForCdid(CDID)).thenReturn(CDID_URI);

        manifest.addManifestEntry(timeSeriesMock);

        TreeSet<String> expectedSet = new TreeSet<>();
        expectedSet.add(EXPECTED_PATH);
        Map<String, TreeSet<String>> expectedDateSetMapping = new HashMap<>();
        expectedDateSetMapping.put(DATASET_ID, expectedSet);

        assertThat("Incorrect number of entries in manifest", manifest.getDataSetMapping().size(), equalTo(1));
        assertThat("Incorrect value in manifest", manifest.getDataSetMapping(), equalTo(expectedDateSetMapping));
        verify(dataIndexMock, times(1)).getUriForCdid(CDID);
        verify(timeSeriesMock, times(1)).getCdid();
        verify(timeSeriesMock, times(3)).getDescription();
        verify(pageDescriptionMock, times(2)).getDatasetId();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfNoCdidUriFound() throws Exception {
        timeSeriesSetUp();

        try {
            manifest.addManifestEntry(timeSeriesMock);
        } catch (Exception ex) {
            assertThat("Incorrect number of entries in manifest", manifest.getDataSetMapping().size(), equalTo(0));
            verify(dataIndexMock, times(1)).getUriForCdid(CDID);
            throw ex;
        }
    }

    @Test(expected = TimeSeriesManifestException.class)
    public void shouldThrowExceptionIfCdidIsNull() throws Exception {
        timeSeriesSetUp();
        when(timeSeriesMock.getCdid()).thenReturn(null);

        try {
            manifest.addManifestEntry(timeSeriesMock);
        } catch (Exception ex) {
            assertThat("Incorrect number of entries in manifest", manifest.getDataSetMapping().size(), equalTo(0));
            verify(dataIndexMock, never()).getUriForCdid(CDID);
            throw ex;
        }
    }

    @Test
    public void shouldFindDatasetIdFromPath() throws Exception {
        manifest.addManifestEntry(DATASET_ID, Paths.get(EXPECTED_PATH));
        Path pageUri = Paths.get("some").resolve("random").resolve("path").resolve(DATASET_ID);
        assertThat("Expected true", manifest.containsDataset(pageUri), is(true));
    }

    @Test
    public void shouldReturnFalseForContainsDataSetIdWithNullArg() throws Exception {
        assertThat("Expected false for null.", manifest.containsDataset(null), is(false));
    }

    @Test
    public void shouldAddTimeSeriesZipPathIfNotNull() {
        Path path = Paths.get(EXPECTED_PATH);

        manifest.addTimeSeriesZip(path);

        assertThat("Expected size to be 1", manifest.getTimeseriesZips().size(), is(1));
        assertThat("Expected manifest to contain zip path.", manifest.getTimeseriesZips().contains(path.toString()), is(true));
    }

    @Test
    public void shouldNoyAddTimeSeriesZipPathIfNull() {
        Path path = Paths.get(EXPECTED_PATH);

        manifest.addTimeSeriesZip(null);

        assertThat("Expected size to be 1", manifest.getTimeseriesZips().size(), is(0));
        assertThat("Expected manifest to contain zip path.", manifest.getTimeseriesZips().contains(path.toString()), is(false));
    }

    @Test
    public void shouldGetPathsByDataSetIdAsPath() throws TimeSeriesManifestException {
        timeSeriesSetUp();
        when(dataIndexMock.getUriForCdid(CDID)).thenReturn(CDID_URI);

        manifest.addManifestEntry(timeSeriesMock);

        TreeSet<Path> set = new TreeSet<>();
        set.add(Paths.get(EXPECTED_PATH));

        Optional<Set<Path>> result = manifest.getByDatasetId(DATASET_ID);

        assertThat("Result incorrect", result, equalTo(Optional.of(set)));
    }

    @Test
    public void shouldAddEntryWithDataSetIdWithCorrectCase() throws TimeSeriesManifestException {
        timeSeriesSetUp();
        when(pageDescriptionMock.getDatasetId()).thenReturn(DATASET_ID.toLowerCase());

        when(dataIndexMock.getUriForCdid(CDID)).thenReturn(CDID_URI);

        manifest.addManifestEntry(timeSeriesMock);

        assertThat("Expected DatasetId to be present.", manifest.getDataSetMapping().containsKey(DATASET_ID), is(true));
        verify(dataIndexMock, times(1)).getUriForCdid(CDID);
    }

    @Test(expected = TimeSeriesManifestException.class)
    public void shouldThrowExceptionIfDataSetIdIsNull() throws TimeSeriesManifestException {
        timeSeriesSetUp();
        when(pageDescriptionMock.getDatasetId()).thenReturn(null);

        try {
            manifest.addManifestEntry(timeSeriesMock);
        } catch (TimeSeriesManifestException ex) {
            assertThat("Expected DatasetMapping to be empty.", manifest.getDataSetMapping().isEmpty(), is(true));
            assertThat("Incorrect Error message", ex.getMessage(), equalTo(DATASET_ID_NULL_OR_EMPTY.getMsg()));
            verify(dataIndexMock, never()).getUriForCdid(CDID);
            throw ex;
        }
    }

    @Test(expected = TimeSeriesManifestException.class)
    public void shouldThrowExceptionIfDataSetIdIsEmptyString() throws TimeSeriesManifestException {
        timeSeriesSetUp();
        when(pageDescriptionMock.getDatasetId()).thenReturn("");

        try {
            manifest.addManifestEntry(timeSeriesMock);
        } catch (TimeSeriesManifestException ex) {
            assertThat("Expected DatasetMapping to be empty.", manifest.getDataSetMapping().isEmpty(), is(true));
            assertThat("Incorrect Error message", ex.getMessage(), equalTo(DATASET_ID_NULL_OR_EMPTY.getMsg()));
            verify(dataIndexMock, never()).getUriForCdid(CDID);
            throw ex;
        }
    }

    @Test(expected = TimeSeriesManifestException.class)
    public void shouldThrowExceptionIfTimeSeriesDescriptionIsNull() throws TimeSeriesManifestException {
        when(timeSeriesMock.getDescription())
                .thenReturn(null);

        try {
            manifest.addManifestEntry(timeSeriesMock);
        } catch (TimeSeriesManifestException ex) {
            assertThat("Expected DatasetMapping to be empty.", manifest.getDataSetMapping().isEmpty(), is(true));
            assertThat("Incorrect Error message", ex.getMessage(), equalTo(TIME_SERIES_DESC_NULL.getMsg()));
            verify(timeSeriesMock, times(1)).getDescription();
            verify(pageDescriptionMock, never()).getDatasetId();
            verify(dataIndexMock, never()).getUriForCdid(CDID);
            throw ex;
        }
    }

    @Test(expected = TimeSeriesManifestException.class)
    public void shouldThrowExceptionIfTimeSeriesIsNull() throws TimeSeriesManifestException {
        try {
            manifest.addManifestEntry(null);
        } catch (TimeSeriesManifestException ex) {
            assertThat("Expected DatasetMapping to be empty.", manifest.getDataSetMapping().isEmpty(), is(true));
            assertThat("Incorrect Error message", ex.getMessage(), equalTo(TIME_SERIES_NULL.getMsg()));
            verify(timeSeriesMock, never()).getDescription();
            verify(pageDescriptionMock, never()).getDatasetId();
            verify(dataIndexMock, never()).getUriForCdid(CDID);
            throw ex;
        }
    }

    private void timeSeriesSetUp() {
        when(timeSeriesMock.getDescription()).thenReturn(pageDescriptionMock);
        when(pageDescriptionMock.getDatasetId()).thenReturn(DATASET_ID);
        when(timeSeriesMock.getCdid()).thenReturn(CDID);
    }
}
