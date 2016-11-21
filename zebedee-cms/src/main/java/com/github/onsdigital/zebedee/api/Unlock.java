package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.util.Token.TokenDetails;
import com.github.onsdigital.zebedee.util.Token.UserToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
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
    @POST
    public boolean unlockCollection(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        //Session session = Root.zebedee.getSessions().get(request);
        TokenDetails token = UserToken.isValid(request);
        token.isAdminOrPublisher();
        boolean result = Root.zebedee.getCollections().unlock(collection, token.getEmail());
        if (result) {
            Audit.Event.COLLECTION_UNLOCKED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .actionedBy(token.getEmail())
                    .log();
        }

        return result;
    }
}
