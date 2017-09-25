package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.publishing.PublishedCollectionSearchResult;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class PublishedCollections {

    @RequestMapping(value = "/publishedCollections/{collectionID}", method = RequestMethod.GET)
    public PublishedCollectionSearchResult get(HttpServletRequest request, HttpServletResponse response,
                                               @PathVariable String collectionID) throws IOException {
        if (StringUtils.isNotEmpty(collectionID)) {
            return Root.zebedee.getPublishedCollections().search(ElasticSearchClient.getClient(), collectionID);
        }
        return Root.zebedee.getPublishedCollections().search(ElasticSearchClient.getClient());
    }
}