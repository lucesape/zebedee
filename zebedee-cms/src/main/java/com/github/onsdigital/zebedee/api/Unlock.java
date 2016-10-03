package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.service.TimeSeriesManifestService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

@Api
public class Unlock {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static TimeSeriesManifestService timeSeriesManifestService = TimeSeriesManifestService.get();
    private static final String ZIPS_DELETE_LOG_MSG = "Timeseries generated zips deleted as collection was unlocked.";

    /**
     * Unlock a collection after it has been approved.
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ConflictException
     * @throws com.github.onsdigital.zebedee.exceptions.BadRequestException
     * @throws UnauthorizedException
     */
    @POST
    public boolean unlockCollection(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);
        DataIndex dataIndex = zebedeeCmsService.getZebedee().getDataIndex();

        boolean result = zebedeeCmsService.getZebedee().getCollections().unlock(collection, session);
        boolean zipsDeleted = timeSeriesManifestService.deleteGeneratedTimeSeriesZips(collection, session, dataIndex);

        if (zipsDeleted) {
            logInfo(ZIPS_DELETE_LOG_MSG).collectionName(collection).user(session.email).log();
        }

        if (result) {
            Audit.Event.COLLECTION_UNLOCKED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .actionedBy(session.email)
                    .log();
        }

        return result;
    }
}
