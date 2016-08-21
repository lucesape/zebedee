package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.persistence.dao.UserRepository;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

public class FileUserRepository implements UserRepository {

    public static final String USERS_DIR = "users";

    private Path userDirectory;

    /**
     * Create a new instance using the given root directory.
     *
     * @param root
     * @throws IOException
     */
    public FileUserRepository(Path root) throws IOException {
        userDirectory = root.resolve(USERS_DIR);

        if (!Files.exists(userDirectory)) {
            Files.createDirectory(userDirectory);
        }
    }

    /**
     * Delete the user with the given email.
     *
     * @param email
     * @throws IOException
     */
    @Override
    public void deleteUser(String email) throws IOException {
        Path path = userPath(email);
        Files.deleteIfExists(path);
    }

    /**
     * Return true if the user for the given email address exists in the repository.
     *
     * @param email
     * @return
     */
    @Override
    public boolean userExists(String email) {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }

    /**
     * Writes a user record to disk.
     *
     * @param user The record to be written.
     * @throws IOException If a filesystem error occurs.
     */
    @Override
    public void saveUser(User user) throws IOException {
        user.email = normalise(user.email);
        Path userPath = userPath(user.email);
        Serialiser.serialise(userPath, user);
    }

    public User deleteUserKeys(List<String> keysToRemove) {
        return null;
    }

    /**
     * Reads a user record from disk.
     *
     * @param email The identifier for the record to be read.
     * @return The read user, if any.
     * @throws IOException
     */
    @Override
    public User getUser(String email) throws IOException {
        User result = null;
        if (userExists(email)) {
            Path userPath = userPath(email);
            result = Serialiser.deserialise(userPath, User.class);
        }
        return result;
    }

    /**
     * Return a collection of all users registered in the system
     *
     * @return A list of all users.
     */
    @Override
    public UserList getAllUsers() throws IOException {
        UserList result = new UserList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDirectory)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    try (InputStream input = Files.newInputStream(path)) {
                        User user = Serialiser.deserialise(input, User.class);
                        result.add(user);
                    } catch (JsonSyntaxException e) {
                        logError(e, "Error deserialising user").addParameter("path", path.toString()).log();
                    }
                }
            }
        }

        return result;
    }

    /**
     * Generates a {@link java.nio.file.Path} for the given email address.
     *
     * @param email The email address to generate a {@link java.nio.file.Path} for.
     * @return A {@link java.nio.file.Path} to the specified user record.
     */
    private Path userPath(String email) {
        Path result = null;

        if (StringUtils.isNotBlank(email)) {
            String userFileName = PathUtils.toFilename(normalise(email));
            userFileName += ".json";
            result = userDirectory.resolve(userFileName);
        }

        return result;
    }

    /**
     * @param email An email address to be standardised.
     * @return The given email, trimmed and lowercased.
     */
    private String normalise(String email) {
        return StringUtils.lowerCase(StringUtils.trim(email));
    }
}
