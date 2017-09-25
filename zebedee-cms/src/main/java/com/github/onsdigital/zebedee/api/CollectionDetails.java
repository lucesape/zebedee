package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Events;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
public class CollectionDetails {

    private static ContentDeleteService contentDeleteService = ContentDeleteService.getInstance();

    /**
     * Retrieves a CollectionDetail object at the endpoint /CollectionDetails/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDetail.
     * @throws IOException
     */
    @RequestMapping(value = "/collectionDetails/{collectionID}", method = RequestMethod.GET)
    public CollectionDetail get(HttpServletRequest request, HttpServletResponse response, @PathVariable String collectionID)
            throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(collectionID);

        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canView(session.getEmail(), collection.description)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return null;
        }

        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);

        CollectionDetail result = new CollectionDetail();
        result.id = collection.description.id;
        result.name = collection.description.name;
        result.type = collection.description.type;
        result.publishDate = collection.description.publishDate;
        result.teams = collection.description.teams;
        result.releaseUri = collection.description.releaseUri;
        result.collectionOwner = collection.description.collectionOwner;
        result.pendingDeletes = contentDeleteService.getDeleteItemsByCollection(collection);

        result.inProgress = ContentDetailUtil.resolveDetails(collection.inProgress, collectionReader.getInProgress());
        result.complete = ContentDetailUtil.resolveDetails(collection.complete, collectionReader.getComplete());
        result.reviewed = ContentDetailUtil.resolveDetails(collection.reviewed, collectionReader.getReviewed());

        result.approvalStatus = collection.description.approvalStatus;
        result.events = collection.description.events;
        result.timeseriesImportFiles = collection.description.timeseriesImportFiles;

        addEventsForDetails(result.inProgress, result, collection);
        addEventsForDetails(result.complete, result, collection);
        addEventsForDetails(result.reviewed, result, collection);

        Set<Integer> teamIds = Root.zebedee.getPermissionsService().listViewerTeams(collection.description, session);
        List<Team> teams = Root.zebedee.getTeamsService().resolveTeams(teamIds);
        teams.forEach(team -> {
            collection.description.teams.add(team.getName());
        });

        return result;
    }


    private void addEventsForDetails(
            List<ContentDetail> detailsToAddEventsFor,
            CollectionDetail result,
            com.github.onsdigital.zebedee.model.Collection collection
    ) {

        for (ContentDetail contentDetail : detailsToAddEventsFor) {
            String language = contentDetail.description.language;
            if (language == null) {
                language = "";
            } else {
                language = "_" + contentDetail.description.language;
            }
            if (collection.description.eventsByUri != null) {
                Events eventsForFile = collection.description.eventsByUri.get(contentDetail.uri + "/data" + language + ".json");
                contentDetail.events = eventsForFile;
            } else {
                contentDetail.events = new Events();
            }
        }
    }
}
