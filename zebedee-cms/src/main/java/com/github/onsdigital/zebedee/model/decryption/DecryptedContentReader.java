package com.github.onsdigital.zebedee.model.decryption;

import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DecryptedContentReader extends FileSystemContentReader {

    public DecryptedContentReader(Path rootPath) {
        super(rootPath);
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
        resource.setData(getInputStream(path));
        return resource;
    }

    private InputStream getInputStream(Path path) throws IOException {
        InputStream inputStream;
        inputStream = Files.newInputStream(path);

        return inputStream;
    }
}
