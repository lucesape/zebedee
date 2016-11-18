package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
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
