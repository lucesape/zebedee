package com.github.onsdigital.zebedee.model.decryption;


import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.util.SlackNotification;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;


public class DecryptedContentWriter extends ContentWriter {

    public DecryptedContentWriter(Path rootFolder, String token) {
        super(rootFolder);
    }

    @Override
    public OutputStream getOutputStream(String uri) throws IOException, BadRequestException {
        // Call encryption service using the token then return a stream
        Path path = resolvePath(uri);;
        logInfo("Writing unencrypted content in collection")
                .addParameter("uri", uri)
                .collectionName("............")
                .log();

        return FileUtils.openOutputStream(path.toFile());
    }

}
