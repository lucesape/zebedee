package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
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
    @POST
    public ResultMessage complete(HttpServletRequest request, HttpServletResponse response) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        // Locate the collection:
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.sessions.get(request);
        String uri = request.getParameter("uri");

        Root.zebedee.collections.complete(collection, uri, session);

        return new ResultMessage("URI reviewed.");
    }
}

