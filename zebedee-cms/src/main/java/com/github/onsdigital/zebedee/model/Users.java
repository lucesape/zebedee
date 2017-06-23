package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.service.SMTPService;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.bouncycastle.crypto.generators.BCrypt;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by david on 12/03/2015.
 * <p/>
 * Class to handle user management functions
 */
public class Users {
    private Path users;
    private Zebedee zebedee;

    public Users(Path users, Zebedee zebedee) {
        this.users = users;
        this.zebedee = zebedee;
    }

    /**
     * Creates a user.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the {@link User} to be created.
     * @param password The plaintext password for this admin user.
     * @param session  An administrator session.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createPublisher(Zebedee zebedee, User user, String password, Session session) throws IOException, UnauthorizedException, ConflictException, BadRequestException, NotFoundException, EmailException {
        zebedee.getUsers().create(session, user);
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = password;
        zebedee.getUsers().setPassword(session, credentials);
        zebedee.getPermissions().addEditor(user.email, session);
    }

    /**
     * Creates the initial system user.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the system {@link User}.
     * @param password The plaintext password for the user.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createSystemUser(Zebedee zebedee, User user, String password) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, EmailException {

        if (zebedee.getPermissions().hasAdministrator()) {
            // An initial system user already exists
            return;
        }

        // Create the user at a lower level because we don't have a Session at this point:
        zebedee.getUsers().create(user, "system");
        zebedee.getUsers().resetPassword(user, password, "system");
        zebedee.getPermissions().addEditor(user.email, null);
        zebedee.getPermissions().addAdministrator(user.email, null);
    }

    /**
     * TODO: This is a temporary method and should be deleted once all users are set up with encryption.
     * This is going to take a couple of releases moving through from develop to live/sandpit before we're all set.
     *
     * @param user     The user who has just logged in
     * @param password The user's plaintext password
     */
    public static void migrateToEncryption(Zebedee zebedee, User user, String password) throws IOException {

        // Update this user if necessary:
        migrateUserToEncryption(zebedee, user, password);

        int withKeyring = 0;
        int withoutKeyring = 0;
        UserList users = zebedee.getUsers().listAll();
        for (User otherUser : users) {
            if (user.keyring() != null) {
                withKeyring++;
            } else {
                // Migrate test users automatically:
                if (migrateUserToEncryption(zebedee, otherUser, "Dou4gl") || migrateUserToEncryption(zebedee, otherUser, "password"))
                    withKeyring++;
                else
                    withoutKeyring++;
            }
        }

        logDebug("User info")
                .addParameter("numberOfUsers", users.size())
                .addParameter("withKeyRing", withKeyring)
                .addParameter("withoutKeyRing", withoutKeyring).log();
    }

    /**
     * Remove keys for collections that no longer exist.
     */
    public static void cleanupCollectionKeys(Zebedee zebedee, User user) throws IOException {
        if (user.keyring != null) {

            List<String> keysToRemove = new ArrayList<>();

            Collections.CollectionList collections = zebedee.getCollections().list();

            for (String key : user.keyring.list()) {
                boolean keyIsValid = false;

                if (zebedee.getApplicationKeys().containsKey(key)) {
                    keyIsValid = true;
                } else {
                    for (Collection collection : collections) {
                        if (collection.description.id.equals(key)) {
                            keyIsValid = true;
                        }
                    }
                }

                if (!keyIsValid) {
                    keysToRemove.add(key);
                }
            }

            for (String key : keysToRemove) {
                logDebug("Removing stale key").addParameter("key", key).log();
                user.keyring.remove(key);
            }

            if (keysToRemove.size() > 0)
                zebedee.getUsers().update(user, user, user.lastAdmin);
        }
    }

    /**
     * TODO: This is a temporary method and should be deleted once all users are set up with encryption.
     * This is going to take a couple of releases moving through from develop to live/sandpit before we're all set.
     *
     * @param user     A user to be migrated
     * @param password The user's plaintext password. Migration will only happen if this password can be verified,
     *                 otherwise there's no point because the user's {@link java.security.PrivateKey}
     *                 will be encrypted using this password, so would be unrecoverable with their actual password.
     */
    private static boolean migrateUserToEncryption(Zebedee zebedee, User user, String password) throws IOException {
        boolean result = false;
        if (user.keyring() == null && user.authenticate(password)) {
            // The keyring has not been generated yet,
            // so reset the password to the current password
            // in order to generate a keyring and associated key pair:
            logDebug("Generating keyring").addParameter("user", user.email).log();
            user.resetPassword(password);

            zebedee.getUsers().update(user, user, "Encryption migration");
        }
        return result;
    }

    /**
     * Lists all users of the system.
     *
     * @return The list of users on the system.
     * @throws IOException If a general filesystem error occurs.
     */
    public UserList list() throws IOException {
        return zebedee.getUsers().listAll();
    }

    /**
     * Gets the record for an existing user.
     *
     * @param email The user's email in order to locate the user record.
     * @return The requested user, unless the email is blank or no record exists for this email.
     * @throws IOException         If a general filesystem error occurs.
     * @throws NotFoundException   If the email cannot be found
     * @throws BadRequestException If the email is left blank
     */
    public User get(String email) throws IOException, NotFoundException, BadRequestException {

        // Check email isn't blank (though this should redirect to userlist)
        if (StringUtils.isBlank(email)) {
            throw new BadRequestException("User email cannot be blank");
        }

        // Check user exists
        if (!exists(email)) {
            throw new NotFoundException("User for email " + email + " not found");
        }

        return read(email);
    }

    /**
     * Creates a new user. This is designed to be called by an admin user, through the API.
     *
     * @param user The specification for the new user to be created. The name and email will be used.
     * @return The newly created user, unless a user already exists, or the supplied {@link com.github.onsdigital.zebedee.json.User} is not valid.
     * @throws IOException If a filesystem error occurs.
     */
    public User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException, BadRequestException, EmailException {

        // Check the user has create permissions
        if (!zebedee.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException("This account is not permitted to create users.");
        }

        if (zebedee.getUsers().exists(user)) {
            throw new ConflictException("User " + user.email + " already exists");
        }

        if (!valid(user)) {
            throw new BadRequestException("Insufficient user details given (name, email, ownerEmail)");
        }

        return create(user, session.email);
    }

    /**
     * Creates a new user. This is designed to be used internally to create a user directly.
     *
     * @param user      The specification for the new user to be created. The name and email will be used.
     * @param lastAdmin The email address of the user creating this record.
     * @return The newly created user, unless a user already exists, or the supplied {@link com.github.onsdigital.zebedee.json.User User} is not valid.
     * @throws IOException If a filesystem error occurs.
     */
    User create(User user, String lastAdmin) throws IOException, EmailException {
        User result = null;

        // TODO bcrypt this code
        String code = UUID.randomUUID().toString();

        if (valid(user) && !exists(user.email)) {

            result = new User();
            result.email = user.email;
            result.name = user.name;
            result.inactive = true;
            result.temporaryPassword = true;
            result.lastAdmin = lastAdmin;
            result.ownerEmail = user.ownerEmail;

            result.verificationHash = code;
            result.verificationRequired = true;

            write(result);

            if(user.ownerEmail != null && user.ownerEmail.contains("@")) {
                SMTPService.SendVerificationEmail(user.name, user.ownerEmail, user.email, code);
            }
        }

        return result;
    }

    /**
     * Update user details
     * <p>
     * At present user email cannot be updated
     *
     * @param user
     * @param updatedUser    - a user object with the new details  @return
     * @throws IOException
     * @throws UnauthorizedException - Session does not have update permissions
     * @throws NotFoundException     - user account does not exist
     * @throws BadRequestException   - problem with the update
     */
    public User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, EmailException {

        // TODO bcrypt this code
        String code = UUID.randomUUID().toString();

        if (zebedee.getPermissions().isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.getUsers().exists(user)) {
            throw new NotFoundException("User " + user.email + " could not be found");
        }

//        if (!valid(user)) {
//            throw new BadRequestException("Insufficient user details given (name, email)");
//        }

        if(updatedUser.ownerEmail != user.ownerEmail) {
            updatedUser.verificationHash = code;
            updatedUser.verificationRequired = true;
        }

        // Update
        User updated = update(user, updatedUser, session.email);

        if(updatedUser.verificationRequired) {
            SMTPService.SendVerificationEmail(user.name, user.ownerEmail, user.email, code);
        }

        // We'll allow changing the email at some point.
        // It entails renaming the json file and checking
        // that the new email doesn't already exist.

        return updated;
    }

    /**
     * Save the user file
     * <p>
     * To avoid concurrency issues this deserialises the saved user and updates
     * details atomically
     *
     * @param user      the user
     * @param lastAdmin the last person to administrate the user
     * @return
     * @throws IOException
     */
    synchronized User update(User user, User updatedUser, String lastAdmin) throws IOException {

        if (user != null) {

            if (updatedUser.name != null && updatedUser.name.length() > 0) {
                user.name = updatedUser.name;
            }

            // Create adminOptions object if user doesn't already have it
            if (user.adminOptions == null) {
                user.adminOptions = new AdminOptions();
            }

            // Update adminOptions object if updatedUser options are different to stored user options
            if (updatedUser.adminOptions != null) {
                if (updatedUser.adminOptions.rawJson != user.adminOptions.rawJson) {
                    user.adminOptions.rawJson = updatedUser.adminOptions.rawJson;
                    System.out.println(user.adminOptions.rawJson);
                }
            }

            user.lastAdmin = lastAdmin;

            write(user);
        }
        return user;
    }

    /**
     * Save the user file after a keyring update
     *
     * @param user
     * @return
     * @throws IOException
     */
    public User updateKeyring(User user) throws IOException {
        User updated = read(user.email);
        if (updated != null) {
            updated.keyring = user.keyring.clone();

            // Only set this to true if explicitly set:
            updated.inactive = BooleanUtils.isTrue(user.inactive);
            write(updated);
        }
        return updated;
    }

    /**
     * Delete a user account
     *
     * @param session - an admin user session
     * @param user    - a user object to delete
     * @return
     * @throws UnauthorizedException
     * @throws IOException
     * @throws NotFoundException
     */
    public boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException {

        if (zebedee.getPermissions().isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.getUsers().exists(user)) {
            throw new NotFoundException("User " + user.email + " does not exist");
        }

        Path path = userPath(user.email);
        return Files.deleteIfExists(path);
    }

    /**
     * Determines whether the given {@link com.github.onsdigital.zebedee.json.User} exists.
     *
     * @param user Can be null.
     * @return If the given user can be mapped to a user record, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean exists(User user) throws IOException {
        return user != null && exists(user.email);
    }

    /**
     * Determines whether a {@link com.github.onsdigital.zebedee.json.User} record exists for the given email.
     *
     * @param email Can be null.
     * @return If the given email can be mapped to a user record, true.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean exists(String email) throws IOException {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }

    public boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {
        boolean result = false;

        if (session == null) {
            throw new UnauthorizedException("Not authenticated.");
        }

        // Check the request
        if (credentials == null || !zebedee.getUsers().exists(credentials.email)) {
            throw new BadRequestException("Please provide credentials (email, password[, oldPassword])");
        }

        User user = read(credentials.email);

        // Ensure the old password is correct
        if (!user.authenticate(credentials.oldPassword)) {
            throw new UnauthorizedException("Authentication failed with old password.");
        }

        if (credentials.email.equalsIgnoreCase(session.email) && StringUtils.isNotBlank(credentials.password)) {
            // User changing their own password
            result = changePassword(user, credentials.oldPassword, credentials.password);
        }

        return result;
    }

    public boolean createPassword(Credentials credentials) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {
        boolean result = false;

        // Check the request
        if (credentials == null || !zebedee.getUsers().exists(credentials.email)) {
            throw new BadRequestException("Please provide credentials (email, password[, oldPassword])");
        }

        User user = read(credentials.email);

        // Ensure the old password is correct
        if (!user.verify(credentials.oldPassword)) {
            throw new UnauthorizedException("Verification failed with code");
        }

        return changePassword(user, credentials.oldPassword, credentials.password);
    }

    /**
     * Changes the user's password and sets the account to active.
     * This is done by the user themselves so the password is marked as not temporary.
     *
     * @param user        The user.
     * @param oldPassword The current password.
     * @param oldPassword The new password to set.
     * @throws IOException If a filesystem error occurs.
     */
    private boolean changePassword(User user, String oldPassword, String newPassword) throws IOException {
        boolean result = false;

        result = user.changePassword(oldPassword, newPassword);
        if (result) {
            user.inactive = false;
            user.lastAdmin = user.email;
            user.temporaryPassword = false;
            write(user);
            result = true;
        }

        return result;
    }

    /**
     * Resets the specified user's password and sets the account to active.
     * This is done by an admin so the password is marked as temporary.
     *
     * @param user       The user.
     * @param password   The password to set.
     * @param adminEmail The user resetting the password.
     * @throws IOException If a filesystem error occurs.
     */

    private void resetPassword(User user, String password, String adminEmail) throws IOException {
        user.resetPassword(password);
        user.inactive = false;
        user.lastAdmin = adminEmail;
        user.temporaryPassword = true;
        write(user);
    }

    private void refreshKeyring(Session session, Keyring keyring, Keyring originalKeyring) throws NotFoundException, BadRequestException, IOException {
        Keyring adminKeyring = zebedee.getKeyringCache().get(session);

        Set<String> collections = originalKeyring.list();
        for (String collectionId : collections) {
            SecretKey key = adminKeyring.get(collectionId);
            if (key != null) {
                keyring.put(collectionId, key);
            }
        }
    }

    /**
     * Determines whether the given {@link com.github.onsdigital.zebedee.json.User} is valid.
     *
     * @param user The object to check.
     * @return If the user is not null and neither email nor name ar blank, true.
     */
    private boolean valid(User user) {
        return user != null && StringUtils.isNoneBlank(user.email, user.name, user.ownerEmail);
    }

    /**
     * Reads a user record from disk.
     *
     * @param email The identifier for the record to be read.
     * @return The read user, if any.
     * @throws IOException
     */
    private User read(String email) throws IOException {
        User result = null;
        if (exists(email)) {
            Path userPath = userPath(email);
            result = Serialiser.deserialise(userPath, User.class);
        }
        return result;
    }

    /**
     * Writes a user record to disk.
     *
     * @param user The record to be written.
     * @throws IOException If a filesystem error occurs.
     */
    private void write(User user) throws IOException {
        user.email = normalise(user.email);
        Path userPath = userPath(user.email);
        Serialiser.serialise(userPath, user);
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
            result = users.resolve(userFileName);
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

    /**
     * Return a collection of all users registered in the system
     *
     * @return A list of all users.
     */
    public UserList listAll() throws IOException {
        UserList result = new UserList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(users)) {
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
}
