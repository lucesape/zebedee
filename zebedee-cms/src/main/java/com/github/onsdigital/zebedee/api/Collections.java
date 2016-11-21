package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.github.onsdigital.zebedee.util.Token.UserToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

@Api
public class Collections {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    /**
     * Get the collection defined by the given HttpServletRequest
     *
     * @param request the request containing the id of the collection to get.
     * @return
     * @throws IOException
     */
    public static Collection getCollection(HttpServletRequest request)
            throws IOException {
        String collectionId = getCollectionId(request);
        return Root.zebedee.getCollections().getCollection(collectionId);
    }

    public static String getCollectionId(HttpServletRequest request) {
        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionId = "";
        if (segments.size() > 1) {
            collectionId = segments.get(1);
        }
        return collectionId;
    }

    /**
     * Retrieves current {@link CollectionDescription} objects
     *
     * @param request
     * @param response
     * @return a List of {@link Collection#description}.
     * @throws IOException
     */
    @GET
    public CollectionDescriptions get(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        CollectionDescriptions result = null;
        try {
            UserToken.isValid(request).isViewer();
            //Session session = Root.zebedee.getSessions().get(request);
            result = new CollectionDescriptions();
            List<Collection> collections = Root.zebedee.getCollections().list();
            //CollectionOwner collectionOwner = zebedeeCmsService.getPublisherType(session.email);

            for (Collection collection : collections) {
                //if (Root.zebedee.getPermissions().canView(session, collection.description)
                //        && (collection.description.collectionOwner.equals(collectionOwner))) {

                CollectionDescription description = new CollectionDescription();
                description.id = collection.description.id;
                description.name = collection.description.name;
                description.publishDate = collection.description.publishDate;
                description.approvalStatus = collection.description.approvalStatus;
                description.type = collection.description.type;
                description.teams = collection.description.teams;
                result.add(description);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // sort the collections alphabetically by name.
        java.util.Collections.sort(result, (o1, o2) -> o1.name.compareTo(o2.name));

        return result;
    }
}
