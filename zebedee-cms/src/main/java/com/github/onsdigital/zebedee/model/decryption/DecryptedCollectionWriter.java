package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

public class DecryptedCollectionWriter extends CollectionWriter {

    public DecryptedCollectionWriter(Collection collection, String token){

        ReaderConfiguration config = getConfiguration();
        System.err.println("****** writing to vault *******");
        inProgress = new DecryptedContentWriter(collection.path.resolve(config.getInProgressFolderName()), "");
        complete = new DecryptedContentWriter(collection.path.resolve(config.getCompleteFolderName()), "");
        reviewed = new DecryptedContentWriter(collection.path.resolve(config.getReviewedFolderName()), "");
        root = new DecryptedContentWriter(collection.path, "");
    }

}
