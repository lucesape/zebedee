package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

public class DecryptedCollectionReader extends CollectionReader {

    public DecryptedCollectionReader(Collection collection) throws BadRequestException, IOException, UnauthorizedException, NotFoundException {


        // Authorisation
        init(collection);
    }

    private void init(Collection collection) throws NotFoundException, UnauthorizedException, IOException {

        if (collection == null) {
            throw new NotFoundException("Collection not found");
        }

        inProgress = new DecryptedContentReader(collection.path.resolve(getConfiguration().getInProgressFolderName()));
        complete = new DecryptedContentReader(collection.path.resolve((getConfiguration().getCompleteFolderName())));
        reviewed = new DecryptedContentReader(collection.path.resolve((getConfiguration().getReviewedFolderName())));
        root = new DecryptedContentReader(collection.path);
    }

}
