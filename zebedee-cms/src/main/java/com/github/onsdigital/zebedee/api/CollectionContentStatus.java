package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

/**
 * Created by carlhembrough on 27/09/2016.
 */
@Api
public class CollectionContentStatus {


    /**
     * Return the name of the collection a file exists in, if it exists in a collection other than the one specified.
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ZebedeeException
     */
    @GET
    public String get(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {

        String uri = request.getParameter("uri");
        String collectionId = Collections.getCollectionId(request);

        Zebedee zebedee = Root.zebedee;

        Session session = zebedee.getSessions().get(request);
        if (!zebedee.getPermissions().isPublisher(session) && !zebedee.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException("Only publishers and admins can see if content is being edited elsewhere.");
        }

        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        String collectionIdForUri = zebedee.getCollections().getCollectionIdForUri(uri, collectionId);

        return collectionIdForUri;
    }
}
