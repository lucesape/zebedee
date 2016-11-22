package com.github.onsdigital.zebedee.model.decryption;


import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.encryption.EncryptionApi;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;


public class EncryptedContentWriter extends ContentWriter {

    private String token;

    public EncryptedContentWriter(Path rootFolder, String token) {
        super(rootFolder);
        this.token = EncryptionApi.ROOT_TOKEN;
    }

    @Override
    public void writeObject(Object object, String uri) throws IOException, BadRequestException {
        String contentToEncrypt = ContentUtil.serialise(object);
        String encrypted = null;
        try {
            encrypted = EncryptionApi.encrypt(contentToEncrypt, token);
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        try(InputStream stream = IOUtils.toInputStream(encrypted)) {
            write(stream,uri);
        }
    }

    @Override
    public void write(InputStream input, String uri) throws IOException, BadRequestException {
        String contentToEncrypt = IOUtils.toString(input);
        String encrypted = null;
        try {
            encrypted = EncryptionApi.encrypt(contentToEncrypt, token);
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        try (OutputStream output = this.getOutputStream(uri)) {
            IOUtils.write(encrypted, output);
            //org.apache.commons.io.IOUtils.copy(encrypted, output);
        }
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
