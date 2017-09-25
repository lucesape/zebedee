package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class Unlock {

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
    @RequestMapping(value = "/unlock/{collectionID}", method = RequestMethod.POST)
    public boolean unlockCollection(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable String collectionID) throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);
        Session session = Root.zebedee.getSessionsService().get(request);
        boolean result = Root.zebedee.getCollections().unlock(collection, session);
        if (result) {
            Audit.Event.COLLECTION_UNLOCKED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .actionedBy(session.getEmail())
                    .log();
        }

        return result;
    }
}
