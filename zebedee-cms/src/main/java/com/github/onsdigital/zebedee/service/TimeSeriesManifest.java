package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static java.text.MessageFormat.format;

/**
 *
 */
public class TimeSeriesManifest {

    private static final String STRING_ARGO_INVALID_MSG = TimeSeriesManifest.class.getSimpleName()
            + ": parameter {0} is required and cannot be null or empty";
    private static final String CDID_PARAM = "CDID";
    private static final String DATASET_ID = "DATASET_ID";

    private transient DataIndex dataIndex;
    private Map<String, TreeSet<String>> dataSetMapping;
    private Set<String> timeseriesZips;

    TimeSeriesManifest(DataIndex dataIndex) {
        this.dataSetMapping = new HashMap<>();
        this.dataIndex = dataIndex;
        this.timeseriesZips = new TreeSet<>();
    }

    public TimeSeriesManifest addManifestEntry(TimeSeries t) {
        final String datasetId = t.getDescription().getDatasetId();
        return addManifestEntry(datasetId, getCDIDUri(t.getCdid(), datasetId));
    }

    public TimeSeriesManifest addManifestEntry(String dataSetId, Path filePath) {
        TreeSet<String> filesByDataSet = dataSetMapping.get(dataSetId);
        if (filesByDataSet == null) {
            filesByDataSet = new TreeSet<>();
        }
        filesByDataSet.add(filePath.toString());
        dataSetMapping.put(dataSetId.toUpperCase(), filesByDataSet);
        return this;
    }

    private Path getCDIDUri(String cdid, String datasetId) {
        try {
            return Paths.get(dataIndex.getUriForCdid(validate(cdid, CDID_PARAM))).resolve(validate(datasetId, DATASET_ID)
                    .toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw logError(ex).uncheckedException(ex);
        }
    }

    private String validate(String value, String name) throws IllegalArgumentException {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(format(STRING_ARGO_INVALID_MSG, name));
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
}
