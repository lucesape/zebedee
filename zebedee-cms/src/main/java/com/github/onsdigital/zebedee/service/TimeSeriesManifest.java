package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.DATASET_ID_NULL_OR_EMPTY;
import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.FILE_PATH_NULL;
import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.TIME_SERIES_DESC_NULL;
import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.TIME_SERIES_NULL;

/**
 *
 */
public class TimeSeriesManifest {

    static final String DATASET_ID_REQUIRED_MSG = "DatasetId is required and cannot be null.";
    static final String CDID_PARAM = "CDID";
    static final String DATASET_ID = "DATASET_ID";

    private transient DataIndex dataIndex;
    private Map<String, TreeSet<String>> dataSetMapping;
    private Set<String> timeseriesZips;

    TimeSeriesManifest(DataIndex dataIndex) {
        this.dataSetMapping = new HashMap<>();
        this.dataIndex = dataIndex;
        this.timeseriesZips = new TreeSet<>();
    }

    /**
     * Add an entry to the manifest.
     *
     * @param t {@link TimeSeries} to add the entry from.
     * @throws TimeSeriesManifestException problem adding entry to manifest.
     */
    public TimeSeriesManifest addManifestEntry(TimeSeries t) throws TimeSeriesManifestException {
        if (t == null) {
            throw new TimeSeriesManifestException(TIME_SERIES_NULL);
        }
        if (t.getDescription() == null) {
            throw new TimeSeriesManifestException(TIME_SERIES_DESC_NULL);
        }
        if (StringUtils.isEmpty(t.getDescription().getDatasetId())) {
            throw new TimeSeriesManifestException(DATASET_ID_NULL_OR_EMPTY);
        }

        final String datasetId = t.getDescription().getDatasetId();
        return addManifestEntry(datasetId, getCDIDUri(t.getCdid(), datasetId));
    }

    /**
     * Add an entry to the manifest.
     *
     * @param dataSetId the datasetId to add.
     * @param filePath the path to the file.
     * @return
     * @throws TimeSeriesManifestException problem adding entry to manifest.
     */
    public TimeSeriesManifest addManifestEntry(String dataSetId, Path filePath) throws TimeSeriesManifestException {
        if (StringUtils.isEmpty(dataSetId)) {
            throw new TimeSeriesManifestException(DATASET_ID_NULL_OR_EMPTY);
        }
        if (filePath == null) {
            throw new TimeSeriesManifestException(FILE_PATH_NULL);
        }

        TreeSet<String> filesByDataSet = dataSetMapping.get(dataSetId.toUpperCase());
        if (filesByDataSet == null) {
            filesByDataSet = new TreeSet<>();
        }
        filesByDataSet.add(filePath.toString());
        dataSetMapping.put(dataSetId.toUpperCase(), filesByDataSet);
        return this;
    }

    private Path getCDIDUri(String cdid, String datasetId) throws TimeSeriesManifestException {
        return Paths.get(dataIndex.getUriForCdid(validate(cdid, CDID_PARAM))).resolve(validate(datasetId, DATASET_ID)
                .toLowerCase());
    }

    private String validate(String value, String name) throws TimeSeriesManifestException {
        if (StringUtils.isEmpty(value)) {
            throw new TimeSeriesManifestException(name);
        }
        return value;
    }

    public TimeSeriesManifest addTimeSeriesZip(Path zipPath) {
        if (zipPath != null) {
            this.timeseriesZips.add(zipPath.toString());
        }
        return this;
    }

    public Set<String> getTimeseriesZips() {
        return timeseriesZips;
    }

    public boolean containsDataset(Path pageUri) {
        if (pageUri == null) {
            return false;
        }
        String fileName = FilenameUtils.removeExtension(pageUri.getFileName().toString().toUpperCase());
        return this.dataSetMapping.containsKey(fileName);
    }

    public Optional<Set<Path>> getByDatasetId(String datasetId) {
        if (StringUtils.isNotEmpty(datasetId)) {
            datasetId = datasetId.toUpperCase();
        }
        Set<String> paths = this.dataSetMapping.get(datasetId);
        if (paths == null || paths.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(paths.stream().map(string -> Paths.get(string)).collect(Collectors.toSet()));
    }

    public TimeSeriesManifest removeDataset(String datasetId) {
        if (StringUtils.isNotEmpty(datasetId)) {
            datasetId = datasetId.toUpperCase();
        }
        this.dataSetMapping.remove(datasetId);
        return this;
    }

    public TimeSeriesManifest removeZip(String zipPath) {
        this.timeseriesZips.remove(zipPath);
        return this;
    }

    public Map<String, TreeSet<String>> getDataSetMapping() {
        return dataSetMapping;
    }

    public void setDataIndex(DataIndex dataIndex) {
        this.dataIndex = dataIndex;
    }

    public boolean isEmpty() {
        return this.dataSetMapping.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TimeSeriesManifest manifest = (TimeSeriesManifest) obj;
        return new EqualsBuilder().append(dataSetMapping, manifest.dataSetMapping).append(timeseriesZips,
                manifest.timeseriesZips).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(dataSetMapping).append(timeseriesZips).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("dataSetMapping", dataSetMapping)
                .append("timeseriesZips", timeseriesZips)
                .toString();
    }
}
