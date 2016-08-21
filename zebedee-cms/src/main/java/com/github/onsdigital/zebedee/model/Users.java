package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.persistence.dao.UserRepository;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.security.Key;
import java.util.*;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Created by david on 12/03/2015.
 * <p>
 * Class to handle user management functions
 */
public class Users {
    private Zebedee zebedee;
    private UserRepository userRepository;

    public Users(Zebedee zebedee, UserRepository userRepository) {
        this.zebedee = zebedee;
        this.userRepository = userRepository;
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
    public static void createPublisher(Zebedee zebedee, User user, String password, Session session) throws IOException, UnauthorizedException, ConflictException, BadRequestException, NotFoundException {
        zebedee.users.create(session, user);
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = password;
        zebedee.users.setPassword(session, credentials);
        zebedee.permissions.addEditor(user.email, session);
    }

    /**
     * Creates the initial system user.
     *
     * @param zebedee  A {@link Zebedee} instance.
     * @param user     The details of the system {@link User}.
     * @param password The plaintext password for the user.
     * @throws IOException If a filesystem error occurs.
     */
    public static void createSystemUser(Zebedee zebedee, User user, String password) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        if (zebedee.permissions.hasAdministrator()) {
            // An initial system user already exists
            return;
        }

        // Create the user at a lower level because we don't have a Session at this point:
        zebedee.users.create(user, "system");
        zebedee.users.resetPassword(user, password, "system");
        zebedee.permissions.addEditor(user.email, null);
        zebedee.permissions.addAdministrator(user.email, null);
    }

    /**
     * Remove keys for collections that no longer exist.
     */
    public void cleanupCollectionKeys(Zebedee zebedee, User user) throws IOException {
        if (user.getKeyring() != null) {

            List<String> keysToRemove = new ArrayList<>();

            Collections.CollectionList collections = zebedee.collections.list();

            for (String key : user.getKeyring().list()) {
                boolean keyIsValid = false;

                if (zebedee.applicationKeys.containsKey(key)) {
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

            userRepository.deleteUserKeys(keysToRemove);
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
        if (user.getKeyring() == null && user.authenticate(password)) {
            // The keyring has not been generated yet,
            // so reset the password to the current password
            // in order to generate a keyring and associated key pair:
            logDebug("Generating keyring").addParameter("user", user.email).log();
            user.resetPassword(password);

            zebedee.users.update(user, user, "Encryption migration");
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
        return userRepository.getAllUsers();
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

        return userRepository.getUser(email);
    }

    /**
     * Creates a new user. This is designed to be called by an admin user, through the API.
     *
     * @param user The specification for the new user to be created. The name and email will be used.
     * @return The newly created user, unless a user already exists, or the supplied {@link com.github.onsdigital.zebedee.json.User} is not valid.
     * @throws IOException If a filesystem error occurs.
     */
    public User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException, BadRequestException {

        // Check the user has create permissions
        if (!zebedee.permissions.isAdministrator(session)) {
            throw new UnauthorizedException("This account is not permitted to create users.");
        }

        if (zebedee.users.exists(user)) {
            throw new ConflictException("User " + user.email + " already exists");
        }

        if (!valid(user)) {
            throw new BadRequestException("Insufficient user details given (name, email)");
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
    User create(User user, String lastAdmin) throws IOException {
        User result = null;

        if (valid(user) && !exists(user.email)) {

            result = new User();
            result.email = user.email;
            result.name = user.name;
            result.inactive = true;
            result.temporaryPassword = true;
            result.lastAdmin = lastAdmin;
            userRepository.saveUser(result);
        }

        return result;
    }

    /**
     * Update user details
     * <p>
     * At present user email cannot be updated
     *
     * @param user
     * @param updatedUser - a user object with the new details  @return
     * @throws IOException
     * @throws UnauthorizedException - Session does not have update permissions
     * @throws NotFoundException     - user account does not exist
     * @throws BadRequestException   - problem with the update
     */
    public User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        if (zebedee.permissions.isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.users.exists(user)) {
            throw new NotFoundException("User " + user.email + " could not be found");
        }

//        if (!valid(user)) {
//            throw new BadRequestException("Insufficient user details given (name, email)");
//        }

        // Update
        User updated = update(user, updatedUser, session.email);

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

            userRepository.saveUser(user);
        }
        return user;
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

        if (zebedee.permissions.isAdministrator(session.email) == false) {
            throw new UnauthorizedException("Administrator permissions required");
        }

        if (!zebedee.users.exists(user)) {
            throw new NotFoundException("User " + user.email + " does not exist");
        }

        userRepository.deleteUser(user.email);
        return true;
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
        return userRepository.userExists(email);
    }

    public boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {
        boolean result = false;

        if (session == null) {
            throw new UnauthorizedException("Not authenticated.");
        }

        // Check the request
        if (credentials == null || !zebedee.users.exists(credentials.email)) {
            throw new BadRequestException("Please provide credentials (email, password[, oldPassword])");
        }

        User user = userRepository.getUser(credentials.email);

        // If own user updating, ensure the old password is correct
        if (!zebedee.permissions.isAdministrator(session) && !user.authenticate(credentials.oldPassword)) {
            throw new UnauthorizedException("Authentication failed with old password.");
        }

        if (credentials.email.equalsIgnoreCase(session.email) && StringUtils.isNotBlank(credentials.password)) {
            // User changing their own password
            result = changePassword(user, credentials.oldPassword, credentials.password);
        } else if (zebedee.permissions.isAdministrator(session.email) || !zebedee.permissions.hasAdministrator()) {
            // Administrator reset, or system setup

            // Grab current keyring (null if this is system setup)
            KeyringReader originalKeyring = null;
            if (user.getKeyring() != null) originalKeyring = user.getKeyring().clone();

            resetPassword(user, credentials.password, session.email);

            // Restore the user keyring (or not if this is system setup)
            if (originalKeyring != null) {

                KeyringReader keyringReader = zebedee.keyringCache.get(session);
                Set<Key> keys = new HashSet<>();
                for (String keyId : originalKeyring.list()) {
                    keys.add(keyringReader.get(keyId));
                }

                this.addKeysToUser(user, keys);
            }
            // Save the user
            userRepository.saveUser(user);

            result = true;
        }

        return result;
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
            userRepository.saveUser(user);
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
        userRepository.saveUser(user);
    }

    /**
     * Determines whether the given {@link com.github.onsdigital.zebedee.json.User} is valid.
     *
     * @param user The object to check.
     * @return If the user is not null and neither email nor name ar blank, true.
     */
    private boolean valid(User user) {
        return user != null && StringUtils.isNoneBlank(user.email, user.name);
    }

    public void addKeysToUser(User user, Map<String, Key> keysToAdd) {
    }
}
