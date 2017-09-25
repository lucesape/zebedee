package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController("/collection")
public class Collection {

    /**
     * Retrieves a CollectionDescription object at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDescription.
     * @throws IOException
     */
    @RequestMapping(value = "/collection/{collectionID}")
    public CollectionDescription get(HttpServletRequest request, HttpServletResponse response,
                                     @PathVariable String collectionID) throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(collectionID);

        // Check whether we found the collection:
        if (collection == null) {
            throw new NotFoundException("The collection you are trying to delete was not found.");
        }
        // Check whether we have access
        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canView(session.getEmail(), collection.description)) {
            throw new UnauthorizedException("You are not authorised to delete collections.");
        }

        // Collate the result:
        CollectionDescription result = new CollectionDescription();
        result.id = collection.description.id;
        result.name = collection.description.name;
        result.publishDate = collection.description.publishDate;
        result.inProgressUris = collection.inProgressUris();
        result.completeUris = collection.completeUris();
        result.reviewedUris = collection.reviewedUris();
        result.eventsByUri = collection.description.eventsByUri;
        result.approvalStatus = collection.description.approvalStatus;
        result.type = collection.description.type;
        result.teams = collection.description.teams;
        result.isEncrypted = collection.description.isEncrypted;
        result.releaseUri = collection.description.releaseUri;
        result.collectionOwner = collection.description.collectionOwner;
        return result;
    }

    /**
     * Creates or updates collection details the endpoint /Collection/
     * <p>
     * Checks if a collection exists using {@link CollectionDescription#name}
     *
     * @param request               This should contain a X-Florence-Token header for the current session
     * @param response              <ul>
     *                              <li>If no name has been passed:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                              <li>If user cannot create collections:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                              <li>If collection with name already exists:  {@link HttpStatus#CONFLICT_409}</li>
     *                              </ul>
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/collection", method = RequestMethod.POST)
    public CollectionDescription create(HttpServletRequest request, HttpServletResponse response,
                                        @RequestBody CollectionDescription collectionDescription) throws IOException,
            ZebedeeException {

        if (StringUtils.isBlank(collectionDescription.name)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return null;
        }

        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canEdit(session.getEmail())) {
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        Keyring keyring = Root.zebedee.getKeyringCache().get(session);
        if (keyring == null) {
            throw new UnauthorizedException("Keyring is not initialised.");
        }

        collectionDescription.name = StringUtils.trim(collectionDescription.name);
        if (Root.zebedee.getCollections().list().hasCollection(
                collectionDescription.name)) {
            throw new ConflictException("Could not create collection. A collection with this name already exists.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = com.github.onsdigital.zebedee.model.Collection.create(
                collectionDescription, Root.zebedee, session);

        if (collection.description.type.equals(CollectionType.scheduled)) {
            Root.schedulePublish(collection);
        }

        Audit.Event.COLLECTION_CREATED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        return collection.description;
    }

    @RequestMapping(value = "/collection/{collectionID}", method = RequestMethod.PUT)
    public CollectionDescription update(HttpServletRequest request, HttpServletResponse response,
                                        @RequestBody CollectionDescription collectionDescription
    ) throws IOException, ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canEdit(session.getEmail())) {
            throw new UnauthorizedException("You are not authorised to update collections.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionDescription.getId());
        com.github.onsdigital.zebedee.model.Collection updatedCollection = collection.update(
                collection,
                collectionDescription,
                Root.zebedee,
                Root.getScheduler(),
                session);

        Audit.Event.COLLECTION_UPDATED
                .parameters()
                .host(request)
                .fromTo(collection.path.toString(), updatedCollection.path.toString())
                .actionedBy(session.getEmail())
                .log();

        return updatedCollection.description;
    }


    /**
     * Deletes the collection details at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If user is not authorised to delete this collection:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection has contents preventing deletion:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/collection/{collectionID}")
    public boolean deleteCollection(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable String collectionID) throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(collectionID);
        Session session = Root.zebedee.getSessionsService().get(request);

        Root.zebedee.getCollections().delete(collection, session);

        Root.cancelPublish(collection);

        Audit.Event.COLLECTION_DELETED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        return true;
    }
}
