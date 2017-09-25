package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by kanemorgan on 01/04/2015.
 */
@RestController
public class Approve {

    /**
     * Approves the content of a collection at the endpoint /Approve/[CollectionName].
     *
     * @param request
     * @param response <ul>
     *                 <li>If approval succeeds: {@link HttpStatus#OK_200}</li>
     *                 <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If the collection has incomplete items:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return Save successful status of the description.
     * @throws IOException
     */
    @RequestMapping(value = "/approve/{collectionID}", method = RequestMethod.POST)
    public boolean approveCollection(HttpServletRequest request, HttpServletResponse response,
                                     @PathVariable String collectionID) throws IOException, ZebedeeException {
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);
        Session session = Root.zebedee.getSessionsService().get(request);

        Root.zebedee.getCollections().approve(collection, session);

        Audit.Event.COLLECTION_APPROVED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        return true;
    }
}
