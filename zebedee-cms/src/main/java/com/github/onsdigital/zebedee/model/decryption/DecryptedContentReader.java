package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.encryption.EncryptionApi;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DecryptedContentReader extends FileSystemContentReader {

    private String token;

    private Collection collection;

    public DecryptedContentReader(Path rootPath, Collection collection) {
        super(rootPath);
        token = EncryptionApi.ROOT_TOKEN;
        this.collection = collection;
    }

    @Override
    protected long calculateContentLength(Path path) throws IOException {
        return super.calculateContentLength(path);
    }

    @Override
    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(determineMimeType(path));
        resource.setUri(toRelativeUri(path));
        resource.setData(decryptData(getInputStream(path)));
        return resource;
    }

    private InputStream getInputStream(Path path) throws IOException {
        InputStream inputStream;
        inputStream = Files.newInputStream(path);

        return inputStream;
    }

    private InputStream decryptData(InputStream encrypted) throws IOException {
        String data = IOUtils.toString(encrypted);
        // Check for vault encryption
        if (data.contains("vault:")) {
            String decrypted = null;
            try {
                decrypted = EncryptionApi.decrypt(collection.getDescription().id, data, token);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
            encrypted.close();
            return IOUtils.toInputStream(decrypted);
        }

        return encrypted;
    }
}
