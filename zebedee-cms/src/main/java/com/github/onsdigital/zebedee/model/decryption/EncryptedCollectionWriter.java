package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

public class EncryptedCollectionWriter extends CollectionWriter {

    public EncryptedCollectionWriter(Collection collection, String token){

        ReaderConfiguration config = getConfiguration();
        inProgress = new EncryptedContentWriter(collection.path.resolve(config.getInProgressFolderName()), "");
        complete = new EncryptedContentWriter(collection.path.resolve(config.getCompleteFolderName()), "");
        reviewed = new EncryptedContentWriter(collection.path.resolve(config.getReviewedFolderName()), "");
        root = new EncryptedContentWriter(collection.path, "");
    }

}