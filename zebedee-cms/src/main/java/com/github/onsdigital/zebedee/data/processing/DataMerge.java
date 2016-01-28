package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;

import java.util.Date;
import java.util.Set;

/**
 * Created by thomasridd on 1/21/16.
 */
public class DataMerge {
    public int corrections = 0;
    public int insertions = 0;
    public TimeSeries merged;

    public TimeSeries merge(TimeSeries original, TimeSeries updates, String datasetId) {

        // Clone the original timeseries
        this.merged = ContentUtil.deserialise(ContentUtil.serialise(original), TimeSeries.class);

        // Merge in each of the yearly, quarterly, and monthly timeseries
        mergeTimeSeriesValueSet(merged, merged.years, updates.years, datasetId);
        mergeTimeSeriesValueSet(merged, merged.quarters, updates.quarters, datasetId);
        mergeTimeSeriesValueSet(merged, merged.months, updates.months, datasetId);

        return this.merged;
    }

    /**
     * Merge a
     * @param page
     * @param currentValues
     * @param updateValues
     * @param datasetId
     * @return
     */
    private void mergeTimeSeriesValueSet(TimeSeries page, Set<TimeSeriesValue> currentValues, Set<TimeSeriesValue> updateValues, String datasetId) {

        for (TimeSeriesValue value : updateValues) {
            // Find the current value of the data point
            TimeSeriesValue current = getCurrentValue(currentValues, value);

            if (current != null) { // A point already exists for this data

                if (!current.value.equalsIgnoreCase(value.value)) { // A point already exists for this data

                    // Update the point
                    current.value = value.value;
                    current.sourceDataset = datasetId;

                    current.updateDate = new Date();

                    // Log a correction has been made to existing data
                    this.corrections += 1;
                }
            } else {
                // Take a copy of the point and add it to our merged page
                TimeSeriesValue copy = ContentUtil.deserialise(ContentUtil.serialise(value), TimeSeriesValue.class);
                copy.sourceDataset = datasetId;
                copy.updateDate = new Date();

                page.add(copy);

                // Log that an insertion has been made to the timeseries
                this.insertions += 1;
            }
        }
    }

    /**
     * If a {@link TimeSeriesValue} for value.time exists in currentValues returns that.
     * Otherwise null
     *
     * @param currentValues a set of {@link TimeSeriesValue}
     * @param value         a {@link TimeSeriesValue}
     * @return a {@link TimeSeriesValue} from currentValues
     */
    private TimeSeriesValue getCurrentValue(Set<TimeSeriesValue> currentValues, TimeSeriesValue value) {
        if (currentValues == null) {
            return null;
        }

        for (TimeSeriesValue current : currentValues) {
            if (current.compareTo(value) == 0) {
                return current;
            }
        }
        return null;
    }
}