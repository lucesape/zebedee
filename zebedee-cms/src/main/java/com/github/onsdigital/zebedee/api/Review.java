package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class Review {

    /**
     * Moves files between collections using the endpoint <code>/Review/[CollectionName]?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the collection does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If the content item does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If the uri specified a folder not a file:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If user not authorised to review:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>The review fails for some other reason:  {@link HttpStatus#BAD_REQUEST_400}</li>
     * @return a success status wrapped in a {@link ResultMessage} object
     * @throws IOException
     */
    @RequestMapping(value = "/review/{collectionID}", method = RequestMethod.POST)
    public ResultMessage review(HttpServletRequest request, HttpServletResponse response,
                                @PathVariable String collectionID) throws IOException, ZebedeeException {

        // Collate parameters
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);
        if (collection == null) {
            throw new NotFoundException("Collection not found");
        }

        Session session = Root.zebedee.getSessionsService().get(request);
        String uri = request.getParameter("uri");

        Boolean recursive = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("recursive"), "false"));

        // Run the review
        collection.review(session, uri, recursive);
        collection.save();

        Audit.Event.COLLECTION_MOVED_TO_REVIEWED
                .parameters()
                .host(request)
                .collection(collection)
                .user(session.getEmail())
                .log();
        return new ResultMessage("URI reviewed.");
    }
}
