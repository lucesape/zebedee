package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.service.TimeSeriesManifestService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import java.io.IOException;

/**
 * Provide API for deleting generated timeseries files from a collection.
 */
@Api
public class TimeSeriesContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static TimeSeriesManifestService timeSeriesManifestService = TimeSeriesManifestService.get();

    static final String DATASET_ID = "datasetId";
    static final String COLLECTION_REQUIRED_MSG = "Collection is required.";
    static final String DATASET_ID_REQUIRED_MSG = "Parameter datasetId is required.";
    static final String INVALID_SESS_MSG = "You are not authorised to perform this action";

    @DELETE
    public Response cleanUp(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);
        String datasetId = request.getParameter(DATASET_ID);

        validate(collection, session, datasetId);

        return new Response(timeSeriesManifestService.deleteGeneratedTimeSeriesFilesByDataId(datasetId,
                        zebedeeCmsService.getZebedee().getDataIndex(), collection, session));
    }

    /**
     * @param collection
     * @param session
     * @param datasetId
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws IOException
     */
    private void validate(Collection collection, Session session, String datasetId)
            throws BadRequestException, UnauthorizedException, IOException {
        if (collection == null) {
            throw new BadRequestException(COLLECTION_REQUIRED_MSG);
        }
        if (StringUtils.isEmpty(datasetId)) {
            throw new BadRequestException(DATASET_ID_REQUIRED_MSG);
        }
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session.email)) {
            throw new UnauthorizedException(INVALID_SESS_MSG);
        }
    }

    /**
     * Class to encapsulate the response.
     */
    public static class Response {
        private int status = HttpStatus.SC_OK;
        private boolean isDelete;

        Response(boolean isDelete) {
            this.isDelete = isDelete;
        }

        public int getStatus() {
            return status;
        }

        public boolean isDelete() {
            return isDelete;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Response response = (Response) obj;
            return new EqualsBuilder()
                    .append(status, response.status)
                    .append(isDelete, response.isDelete)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(status).append(isDelete).toHashCode();
        }
    }
}
