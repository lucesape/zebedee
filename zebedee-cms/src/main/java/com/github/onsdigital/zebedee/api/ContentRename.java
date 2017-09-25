package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
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
public class ContentRename {

    /**
     * Just like content move but has additional checks to ensure only a rename takes place in the same directory.
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    @RequestMapping(value = "/contentRename/{collectionID}", method = RequestMethod.POST)
    public boolean RenameContent(HttpServletRequest request, HttpServletResponse response, @PathVariable String
            collectionID) throws IOException, ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);

        String uri = request.getParameter("uri");
        String toUri = request.getParameter("toUri");

        Root.zebedee.getCollections().renameContent(session, collection, uri, toUri);
        Audit.Event.CONTENT_RENAMED
                .parameters()
                .host(request)
                .collection(collection)
                .fromTo(uri, toUri)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }
}
