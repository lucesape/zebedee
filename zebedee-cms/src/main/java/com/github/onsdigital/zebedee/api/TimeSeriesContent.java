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
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
@Api
public class TimeSeriesContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String DATASET_ID = "datasetId";

    private static TimeSeriesManifestService timeSeriesCleanUpSeries = TimeSeriesManifestService.get();

    @DELETE
    public Response cleanUp(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);
        String datasetId = request.getParameter(DATASET_ID);

        validate(collection, session, datasetId);

        return new Response(timeSeriesCleanUpSeries.deleteGeneratedTimeSeriesFilesByDataId(datasetId,
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
            throw new BadRequestException("Collection is required.");
        }
        if (StringUtils.isEmpty(datasetId)) {
            throw new BadRequestException("Parameter datasetId is required.");
        }
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session.email)) {
            throw new UnauthorizedException("You are not authorised to perform this action");
        }
    }


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
    }
}
