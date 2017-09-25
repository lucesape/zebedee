package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;

/**
 * API returning the collection event history for the specified collection. API collectionID as the last section of the
 * URI.
 */
@RestController
public class CollectionHistory {

    private static CollectionHistoryDao collectionHistoryDao = getCollectionHistoryDao();
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    /**
     * Get the collection event history for the specified collection.
     *
     * @param request
     * @param response
     * @return
     * @throws ZebedeeException
     */
    @RequestMapping(value = "/collectionHistory/{collectionID}", method = RequestMethod.GET)
    public com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory getCollectionEventHistory(
            HttpServletRequest request, HttpServletResponse response, @PathVariable String collectionId)
            throws ZebedeeException, IOException {

        Session session = zebedeeCmsService.getSession(request);
        checkPermission(session);

        if (StringUtils.isEmpty(collectionId)) {
            throw new BadRequestException("collectionId was not specified.");
        }
        return new com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory(
                getCollectionHistoryDao().getCollectionEventHistory(collectionId));
    }

    private void checkPermission(Session session) throws ZebedeeException {
        try {
            if (session == null || !zebedeeCmsService.getPermissions().canEdit(session.getEmail())) {
                throw new UnauthorizedException("You are not authorised to create collections.");
            }
        } catch (IOException io) {
            logError(io, "Unexpected error while trying to access permissions")
                    .user(session.getEmail())
                    .logAndThrow(UnexpectedErrorException.class);
        }
    }
}
