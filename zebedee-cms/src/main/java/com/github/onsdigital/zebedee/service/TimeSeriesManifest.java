package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import org.apache.commons.io.FilenameUtils;
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

/**
 *
 */
public class TimeSeriesManifest {

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
        final String cdid = t.getCdid().toLowerCase();
        Path cdidPath = Paths.get(dataIndex.getUriForCdid(cdid))
                .resolve(datasetId.toLowerCase());
        return addManifestEntry(datasetId, cdidPath);
    }

    public TimeSeriesManifest addManifestEntry(String dataSetId, Path filePath) {
        TreeSet<String> filesByDataSet = dataSetMapping.get(dataSetId);
        if (filesByDataSet == null) {
            filesByDataSet = new TreeSet<>();
        }
        filesByDataSet.add(filePath.toString());
        dataSetMapping.put(dataSetId, filesByDataSet);
        return this;
    }

    public TimeSeriesManifest addManifestEntry(String datasetId, String cdid) {
        TreeSet<String> filesByDataSet = dataSetMapping.get(datasetId);
        if (filesByDataSet == null) {
            filesByDataSet = new TreeSet<>();
        }
        Path cdidPath = Paths.get(dataIndex.getUriForCdid(cdid)).resolve(datasetId.toLowerCase());
        filesByDataSet.add(cdidPath.toString());
        dataSetMapping.put(datasetId, filesByDataSet);
        return this;
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
        Set<String> paths = this.dataSetMapping.get(datasetId);
        if (paths == null || paths.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(paths.stream()
                .map(string -> Paths.get(string))
                .collect(Collectors.toSet()));
    }

    public TimeSeriesManifest removeDataset(String datasetId) {
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
        return new EqualsBuilder()
                .append(dataSetMapping, manifest.dataSetMapping)
                .append(timeseriesZips, manifest.timeseriesZips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(dataSetMapping).append(timeseriesZips).toHashCode();
    }
}
