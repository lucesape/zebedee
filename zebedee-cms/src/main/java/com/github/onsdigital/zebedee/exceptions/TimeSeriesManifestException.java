package com.github.onsdigital.zebedee.exceptions;

import java.text.MessageFormat;

import static com.github.onsdigital.zebedee.exceptions.TimeSeriesManifestException.ErrorType.PARAM_NULL_OR_EMPTY;

/**
 * Exception class for {@link com.github.onsdigital.zebedee.service.TimeSeriesManifest} errors.
 */
public class TimeSeriesManifestException extends ZebedeeException {

    /**
     * Constants for different error {@link com.github.onsdigital.zebedee.service.TimeSeriesManifest} error scenarios.
     */
    public enum ErrorType {
        DATASET_ID_NULL_OR_EMPTY("DatasetId was null"),

        TIME_SERIES_NULL("TimeSeries was null"),

        TIME_SERIES_DESC_NULL("TimeSeries.description was null"),

        FILE_PATH_NULL("Path was null"),

        PARAM_NULL_OR_EMPTY("{0} was null or empty");

        private final String msg;

        ErrorType(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }

    public TimeSeriesManifestException(ErrorType errorType) {
        super(errorType.getMsg(), 500);
    }

    public TimeSeriesManifestException(String paramName) {
        super(MessageFormat.format(PARAM_NULL_OR_EMPTY.getMsg(), paramName), 500);
    }
}
