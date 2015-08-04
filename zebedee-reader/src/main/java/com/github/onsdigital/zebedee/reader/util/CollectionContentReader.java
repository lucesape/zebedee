package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.collection.Collection;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * Created by bren on 30/07/15.
 */
public class CollectionContentReader {

    private Path collections;

    /**
     * @param collectionsFolderPath path of the collections folder
     */
    public CollectionContentReader(String collectionsFolderPath) {
        if (collectionsFolderPath == null) {
            throw new NullPointerException("Collections folder can not be null");
        }
        this.collections = Paths.get(collectionsFolderPath);
    }

    /**
     * Reads content under a given collection root folder.
     * Tries finding content under in progress, completePath and reviewedPath folders respectively. Throws not found exception if not found
     *
     * @param path path of requested content under requested root folder
     * @return
     * @throws NotFoundException
     * @throws IOException
     */
    public Page getContent(String collectionId, String path) throws ZebedeeException, IOException {
        URI dataFilePath = URI.create(URIUtils.removeTrailingSlash(path) + "/").resolve(getConfiguration().getDataFileName());
        Resource resource = findResource(collectionId, dataFilePath.toString());
        return ContentUtil.deserialiseContent(resource.getData());
    }


    public Resource getResource(String collectionId, String path) throws ZebedeeException, IOException {
        return findResource(collectionId, path);
    }

    /**
     *
     *
     * @param collectionId
     * @param path
     * @return uri-node mapping
     * @throws ZebedeeException
     * @throws IOException
     */
    public Map<URI,ContentNode> getChildren(String collectionId, String path) throws ZebedeeException, IOException {
        Path collectionPath = findCollectionPath(collectionId);
        ContentReader inProgress = getContentReader(collectionPath, getConfiguration().getInProgressFolderName());
        ContentReader complete = getContentReader(collectionPath, getConfiguration().getCompleteFolderName());
        ContentReader reviewed = getContentReader(collectionPath, getConfiguration().getReviewedFolderName());

        Map<URI, ContentNode> children = new TreeMap<>();
        //TODO: Same document should not be in two different state, it should be safe to overwrite if it appears in multiple places?.
        // Is there a validation mechanism ? Might be needed
        children.putAll(getChildrenQuite(path, reviewed));
        children.putAll(getChildrenQuite(path, complete));
        children.putAll(getChildrenQuite(path, inProgress));
        return children;
    }

    private Resource findResource(String collectionId, String path) throws IOException, ZebedeeException {
        return find(findCollectionPath(collectionId), path);
    }

    private Resource find(Path collectionPath, String path) throws ZebedeeException, IOException {
        ContentReader inProgress = getContentReader(collectionPath, getConfiguration().getInProgressFolderName());
        ContentReader complete = getContentReader(collectionPath, getConfiguration().getCompleteFolderName());
        ContentReader reviewed = getContentReader(collectionPath, getConfiguration().getReviewedFolderName());
        Resource resource = getQuite(path, inProgress);
        if (resource == null) {
            resource = getQuite(path, complete);
            if (resource == null) {
                resource = reviewed.getResource(path);
            }
        }
        return resource;
    }

    //If content not found with given reader do not shout
    private Resource getQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getResource(path);
        } catch (NotFoundException e) {
            return null;
        }
    }

    //If content not found with given reader do not shout
    private Map<URI,ContentNode> getChildrenQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getChildren(path);
        } catch (NotFoundException e) {
            return Collections.emptyMap();
        }
    }

    //TODO: If collection folder names were ids or we saved cookie as collection's name we would not have search collection, just read the path
    //Finds collection name with given id

    private Path findCollectionPath(String collectionId) throws IOException, NotFoundException, CollectionNotFoundException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collections)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                } else {
                    try (InputStream fileStream = Files.newInputStream(path)) {
                        Collection collection = ContentUtil.deserialise(fileStream, Collection.class);
                        if (StringUtils.equalsIgnoreCase(collection.getId(), collectionId)) {
                            return collections.resolve(collection.getName());

                        }
                    }
                }
            }
            throw new CollectionNotFoundException("Collection with given id not found, id:" + collectionId);
        }
    }


    private ContentReader getContentReader(Path collectionPath, String folderName) {
        return new ContentReader(collectionPath.resolve(folderName));
    }
}
