package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ContentTree;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class CollectionBrowseTree {

    /**
     * Retrieves a CollectionBrowseTree object at the endpoint /CollectionBrowseTree/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionBrowseTree.
     * @throws java.io.IOException
     */
    @RequestMapping(value = "/collectionBrowseTree/{collectionID}", method = RequestMethod.GET)
    public ContentDetail get(HttpServletRequest request, HttpServletResponse response, @PathVariable String
            collectionID) throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(collectionID);

        Session session = Root.zebedee.getSessionsService().get(request);

        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);
        return ContentTree.getOverlayed(collection, collectionReader);
    }
}
