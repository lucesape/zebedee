package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class Complete {

    /**
     * Creates or updates collection details the endpoint <code>/Complete/[CollectionName]/?uri=[uri]</code>
     * <p>Marks a content item complete</p>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If collection does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If uri is not currently inProgress:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If user cannot delete the file:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>Complete fails for another reason:  {@link HttpStatus#BAD_REQUEST_400}</li>
     * @return a success status wrapped in a {@link ResultMessage} object
     * @throws IOException
     */
    @RequestMapping(value = "/complete/{collectionID}")
    public ResultMessage complete(HttpServletRequest request, HttpServletResponse response,
                                  @PathVariable String collectionID) throws IOException, ZebedeeException {

        // Locate the collection:
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);
        Session session = Root.zebedee.getSessionsService().get(request);
        String uri = request.getParameter("uri");

        Boolean recursive = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("recursive"), "false"));

        Root.zebedee.getCollections().complete(collection, uri, session, recursive);

        Audit.Event.COLLECTION_MOVED_TO_REVIEWED
                .parameters()
                .host(request)
                .collection(collection)
                .user(session.getEmail())
                .log();

        return new ResultMessage("URI reviewed.");
    }
}

