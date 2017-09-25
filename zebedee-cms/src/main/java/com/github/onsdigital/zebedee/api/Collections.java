package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

@RestController
public class Collections {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    /**
     * Get the collection defined by the given HttpServletRequest
     *
     * @param request the request containing the id of the collection to get.
     * @return
     * @throws IOException
     */
    public static Collection getCollection(String collectionId)
            throws IOException {
        return Root.zebedee.getCollections().getCollection(collectionId);
    }



/*    public static Collection getCollection(HttpServletRequest request)
            throws IOException {
        String collectionId = getCollectionId(request);
        return Root.zebedee.getCollections().getCollection(collectionId);
    }*/

/*    public static String getCollectionId(HttpServletRequest request) {
        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionId = "";
        if (segments.size() > 1) {
            collectionId = segments.get(1);
        }
        return collectionId;
    }*/

    /**
     * Retrieves current {@link CollectionDescription} objects
     *
     * @param request
     * @param response
     * @return a List of {@link Collection#description}.
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.GET, value = "/collections")
    public CollectionDescriptions get(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        try {
            Session session = Root.zebedee.getSessionsService().get(request);
            CollectionDescriptions result = new CollectionDescriptions();
            List<Collection> collections = Root.zebedee.getCollections().list();
            CollectionOwner collectionOwner = zebedeeCmsService.getPublisherType(session.getEmail());

            for (Collection collection : collections) {
                if (Root.zebedee.getPermissionsService().canView(session, collection.description)
                        && (collection.description.collectionOwner.equals(collectionOwner))) {

                    CollectionDescription description = new CollectionDescription();
                    description.id = collection.description.id;
                    description.name = collection.description.name;
                    description.publishDate = collection.description.publishDate;
                    description.approvalStatus = collection.description.approvalStatus;
                    description.type = collection.description.type;
                    description.teams = collection.description.teams;
                    result.add(description);
                }
            }

            // sort the collections alphabetically by name.
            java.util.Collections.sort(result, new Comparator<CollectionDescription>() {
                @Override
                public int compare(CollectionDescription o1, CollectionDescription o2) {
                    return o1.name.compareTo(o2.name);
                }
            });

            return result;
        } catch (IOException e) {
            logError(e, "Unexpected error while attempting to get collections")
                    .logAndThrow(UnexpectedErrorException.class);
        }
        return null;
    }
}
