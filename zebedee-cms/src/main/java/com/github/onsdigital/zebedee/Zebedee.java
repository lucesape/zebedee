package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.model.publishing.PublishedCollections;
import com.github.onsdigital.zebedee.persistence.dao.impl.FileUserRepository;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.verification.VerificationAgent;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

public class Zebedee {

    public static final String PUBLISHED = "master";
    public static final String COLLECTIONS = "collections";
    static final String PUBLISHED_COLLECTIONS = "publish-log";
    static final String ZEBEDEE = "zebedee";
    static final String SESSIONS = "sessions";
    static final String PERMISSIONS = "permissions";
    static final String TEAMS = "teams";
    static final String LAUNCHPAD = "launchpad";
    static final String APPLICATION_KEYS = "application-keys";

    public final Path path;
    public final Content published;
    public final Collections collections;
    public final PublishedCollections publishedCollections;
    public final Users users;
    public final KeyringCache keyringCache;
    public final ApplicationKeys applicationKeys;
    public final Sessions sessions;
    public final Permissions permissions;
    public final Teams teams;
    public final VerificationAgent verificationAgent;

    public final DataIndex dataIndex;

    public Zebedee(Path path, boolean useVerificationAgent) throws IOException {

        // Validate the directory:
        this.path = path;
        Path published = path.resolve(PUBLISHED);
        Path collections = path.resolve(COLLECTIONS);
        Path publishedCollections = path.resolve(PUBLISHED_COLLECTIONS);
        Path sessions = path.resolve(SESSIONS);
        Path permissions = path.resolve(PERMISSIONS);
        Path teams = path.resolve(TEAMS);
        Path applicationKeysPath = path.resolve(APPLICATION_KEYS);

        if (!Files.exists(published) || !Files.exists(collections) || !Files.exists(sessions) || !Files.exists(permissions) || !Files.exists(teams)) {
            throw new IllegalArgumentException(
                    "This folder doesn't look like a zebedee folder: "
                            + path.toAbsolutePath());
        }

        // Create published and ensure redirect
        this.published = new Content(published);
        this.dataIndex = new DataIndex(new FileSystemContentReader(this.published.path));

        Path redirectPath = this.published.path.resolve(Content.REDIRECT);
        if (!Files.exists(redirectPath)) {
            this.published.redirect = new RedirectTablePartialMatch(this.published);
            try {
                Files.createFile(redirectPath);
            } catch (IOException e) {
                logError(e, "Could not save redirect to requested path")
                        .addParameter("requestedPath", redirectPath.toString())
                        .log();
            }
        } else {
            this.published.redirect = new RedirectTablePartialMatch(this.published, redirectPath);
        }

        this.collections = new Collections(collections, this);
        this.publishedCollections = new PublishedCollections(publishedCollections);
        this.users = new Users(this, new FileUserRepository(path));
        this.keyringCache = new KeyringCache(this);
        this.applicationKeys = new ApplicationKeys(applicationKeysPath);
        this.sessions = new Sessions(sessions);
        this.permissions = new Permissions(permissions, this);
        this.teams = new Teams(teams, this);
        if (useVerificationAgent && Configuration.isVerificationEnabled()) {
            this.verificationAgent = new VerificationAgent(this);
        } else {
            this.verificationAgent = null;
        }
    }

    public Zebedee(Path path) throws IOException {
        this(path, true);
    }

    /**
     * Creates a new Zebedee folder in the specified parent Path.
     *
     * @param parent The directory in which the folder will be created.
     * @return A {@link Zebedee} instance representing the newly created folder.
     * @throws IOException If a filesystem error occurs.
     */
    public static Zebedee create(Path parent) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Create the folder structure
        Path path;
        if (!Files.exists(parent.resolve(ZEBEDEE))) {
            path = Files.createDirectory(parent.resolve(ZEBEDEE));
        } else {
            path = parent.resolve(ZEBEDEE);
        }
        if (!Files.exists(path.resolve(PUBLISHED))) {
            Files.createDirectory(path.resolve(PUBLISHED));
        }
        if (!Files.exists(path.resolve(COLLECTIONS))) {
            Files.createDirectory(path.resolve(COLLECTIONS));
        }
        if (!Files.exists(path.resolve(SESSIONS))) {
            Files.createDirectory(path.resolve(SESSIONS));
        }
        if (!Files.exists(path.resolve(PERMISSIONS))) {
            Files.createDirectory(path.resolve(PERMISSIONS));
        }
        if (!Files.exists(path.resolve(TEAMS))) {
            Files.createDirectory(path.resolve(TEAMS));
        }
        if (!Files.exists(path.resolve(LAUNCHPAD))) {
            Files.createDirectory(path.resolve(LAUNCHPAD));
        }

        Path redirectPath = path.resolve(PUBLISHED).resolve(Content.REDIRECT);
        if (!Files.exists(redirectPath)) {
            Files.createFile(redirectPath);
        }

        Zebedee zebedee = new Zebedee(path);

        // Create the initial user
        User user = new User();
        user.email = "florence@magicroundabout.ons.gov.uk";
        user.name = "Florence";
        String password = "Doug4l";
        Users.createSystemUser(zebedee, user, password);

        return zebedee;
    }

    /**
     * This method works out how many {@link com.github.onsdigital.zebedee.model.Collection}s contain the given URI.
     * The intention is to allow double-checking in case of concurrent editing.
     * This should be 0 in order for someone to be allowed to edit a URI and
     * should be 1 after editing is initiated. If this returns more than 1 after
     * initiating editing then the current attempt to edit should be reverted -
     * presumably a race condition.
     *
     * @param uri The URI to check.
     * @return The number of {@link com.github.onsdigital.zebedee.model.Collection}s containing the given URI.
     * @throws IOException
     */
    public int isBeingEdited(String uri) throws IOException {
        int result = 0;

        // Is this URI present anywhere else?
        for (Collection collection : collections.list()) {
            if (collection.isInCollection(uri)) {
                result++;
            }
        }

        return result;
    }

    public Path find(String uri) throws IOException {
        // There's currently only one place to look for content.
        // We may add one or more staging layers later.

        return published.get(uri);
    }


    public String toUri(Path path) {
        if (path == null) {
            return null;
        }

        // Remove zebedee section of path
        Path uriPath = this.path.relativize(path);

        // Strip off either launchpad, master or collections/mycollection/inprogress etc
        if (uriPath.startsWith("collections")) {
            uriPath = uriPath.subpath(3, uriPath.getNameCount());
        } else {
            uriPath = uriPath.subpath(1, uriPath.getNameCount());
        }

        // Return URI
        String s = uriPath.toString();
        if (s.startsWith("..")) {
            return null;
        } else if (s.endsWith("data.json")) {
            return "/" + s.substring(0, s.length() - "/data.json".length());
        } else {
            return "/" + s;
        }
    }

    public String toUri(String string) {
        return toUri(Paths.get(string));
    }

    /**
     * Deletes a collection folder structure. This method only deletes folders
     * and will throw an exception if any of the folders aren't empty. This
     * ensures that only a release that has been published can be deleted.
     *
     * @param path The {@link Path} to the collection folder.
     * @throws IOException If any of the subfolders is not empty or if a filesystem
     *                     error occurs.
     */
    public void delete(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path directory : stream) {
                // Recursively delete directories only
                if (Files.isDirectory(directory)) {
                    delete(directory);
                }
                if (directory.endsWith(".DS_Store")) { // delete any .ds_store hidden files
                    Files.delete(directory);
                }
                if (directory.endsWith(Content.REDIRECT)) { // also delete redirect table
                    Files.delete(directory);
                }
            }
        }
        Files.delete(path);
    }

    /**
     * Open a user session
     * <p>
     * This is a zebedee level operation since we need to unlock the keyring
     *
     * @param credentials
     * @return
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    public Session openSession(Credentials credentials) throws IOException, NotFoundException, BadRequestException {
        if (credentials == null) {
            logDebug("Null session due to credentials being null").log();
            return null;
        }

        // Get the user
        User user = users.get(credentials.email);

        if (user == null) {
            logDebug("Null session due to users.get returning null").log();
            return null;
        }

        // Create a session
        Session session = sessions.create(user);

        // Unlock and cache keyring
        user.keyring.unlock(credentials.password);
        applicationKeys.populateCacheFromUserKeyring(user.keyring);
        keyringCache.put(user, session);

        // Return a session
        return session;
    }
}
