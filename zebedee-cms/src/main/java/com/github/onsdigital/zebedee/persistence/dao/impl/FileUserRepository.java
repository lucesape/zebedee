package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.persistence.dao.UserRepository;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

public class FileUserRepository implements UserRepository {

    public static final String USERS_DIR = "users";
    private Path userDirectory;

    private static ConcurrentMap<Path, ReadWriteLock> userLocks = new ConcurrentHashMap<>();

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

        userLocks.remove(path);
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

        Path userPath = userPath(user.email);

        getWriteLock(userPath);
        try {
            // get the user to ensure we have the latest copy of the keyring.
            User savedUser = getUser(user.email);

            if (savedUser != null && savedUser.getKeyring() != null) {

                // add any missing keys to the in memory object.
                Set<String> savedKeys = savedUser.getKeyring().list();
                Set<String> keys = user.getKeyring().list();

                for (String savedKey : savedKeys) {
                    if (!keys.contains(savedKey)) {
                        ((Keyring) user.getKeyring()).put(savedKey, savedUser.getKeyring().get(savedKey));
                    }
                }
            }

            Serialiser.serialise(userPath, user);

        } finally {
            userLocks.get(userPath).writeLock().unlock();
        }
    }

    @Override
    public User removeKeysFromUser(String email, Set<String> keysToRemove) throws IOException {
        Path userPath = userPath(email);
        User user;

        getWriteLock(userPath);
        try {
            user = getUser(email);
            Keyring keyring = (Keyring) user.getKeyring();
            for (String keyId : keysToRemove) {
                keyring.remove(keyId);
            }
            saveUser(user);
        } finally {
            userLocks.get(userPath).writeLock().unlock();
        }

        return user;
    }

    @Override
    public User addKeysToUser(String email, Map<String, SecretKey> keysToAdd) throws IOException {
        Path userPath = userPath(email);
        User user;
        getWriteLock(userPath);
        try {
            user = getUser(email);
            Keyring keyring = (Keyring) user.getKeyring();
            for (Map.Entry<String, SecretKey> entry : keysToAdd.entrySet()) {
                keyring.put(entry.getKey(), entry.getValue());
            }

            saveUser(user);
        } finally {
            userLocks.get(userPath).writeLock().unlock();
        }


        return user;
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

            getReadLock(userPath);
            try {
                result = Serialiser.deserialise(userPath, User.class);
            } finally {
                userLocks.get(userPath).readLock().unlock();
            }
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

                    getReadLock(path);
                    try (InputStream input = Files.newInputStream(path)) {
                        User user = Serialiser.deserialise(input, User.class);
                        result.add(user);
                    } catch (JsonSyntaxException e) {
                        logError(e, "Error deserialising user").addParameter("path", path.toString()).log();
                    } finally {
                        userLocks.get(path).readLock().unlock();
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

    private void getWriteLock(Path userPath) {
        userLocks.putIfAbsent(userPath, new ReentrantReadWriteLock());
        userLocks.get(userPath).writeLock().lock();
    }

    private void getReadLock(Path userPath) {
        userLocks.putIfAbsent(userPath, new ReentrantReadWriteLock());
        userLocks.get(userPath).readLock().lock();
    }
}
