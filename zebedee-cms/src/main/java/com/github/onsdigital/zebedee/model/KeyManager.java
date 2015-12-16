package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Set;

/**
 * Created by thomasridd on 18/11/15.
 */
public class KeyManager {

    /**
     * Distributes an encryption key to all users
     *
     * @param session    session for a user that possesses the key
     * @param collection a collection
     */
    public static void distributeCollectionKey(Zebedee zebedee, Session session, Collection collection) throws NotFoundException, BadRequestException, IOException, UnauthorizedException {
        // Get the
        SecretKey key = zebedee.keyringCache.get(session).get(collection.description.id);

        // Distribute to all users that should have access
        for (User user : zebedee.users.list()) {
            distributeKeyToUser(zebedee, collection, key, user);
        }

        // Add to the cached scheduler keyring
        zebedee.keyringCache.schedulerCache.put(collection.description.id, key);
    }

    /**
     * Determine if the user should have the key assigned or removed for the given collection.
     * @param zebedee
     * @param collection
     * @param session
     * @param user
     * @throws IOException
     */
    public static void distributeKeyToUser(Zebedee zebedee, Collection collection, Session session, User user) throws IOException {
        SecretKey key = zebedee.keyringCache.get(session).get(collection.description.id);
        distributeKeyToUser(zebedee, collection, key, user);
    }

    private static void distributeKeyToUser(Zebedee zebedee, Collection collection, SecretKey key, User user) throws IOException {
        if (userShouldHaveKey(zebedee, user, collection)) {
            // Add the key
            assignKeyToUser(zebedee, user, collection, key);
        } else {
            removeKeyFromUser(zebedee, user, collection);
        }
    }

    /**
     * @param zebedee
     * @param user
     * @param collection
     * @param key
     * @throws IOException
     */
    public static void assignKeyToUser(Zebedee zebedee, User user, Collection collection, SecretKey key) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.keyring == null) return;

        // Add the key to the user keyring and save
        user.keyring.put(collection.description.id, key);
        zebedee.users.updateKeyring(user);

        // If the user is logged in assign the key to their cached keyring
        Session session = zebedee.sessions.find(user.email);
        if (session != null) {
            Keyring keyring = zebedee.keyringCache.get(session);
            try {
                if (keyring != null)
                    keyring.put(collection.description.id, key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove the collection key for the given user.
     * This method is intentionally private as the distribute* methods should be used
     * to re-evaluate whether a key should be removed instead of just removing it.
     * @param zebedee
     * @param user
     * @param collection
     * @throws IOException
     */
    private static void removeKeyFromUser(Zebedee zebedee, User user, Collection collection) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.keyring == null) return;

        // Remove the key from the users keyring and save
        user.keyring.remove(collection.description.id);
        zebedee.users.updateKeyring(user);

        // If the user is logged in remove the key from their cached keyring
        Session session = zebedee.sessions.find(user.email);
        if (session != null) {
            Keyring keyring = zebedee.keyringCache.get(session);
            try {
                if (keyring != null)
                    keyring.remove(collection.description.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Transfer a set of secret keys from the source keyring to the target
     *
     * @param targetKeyring the keyring to be populated
     * @param sourceKeyring the keyring to take keys from
     * @param collectionIds the keys to transfer
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws IOException
     */
    public static void transferKeyring(Keyring targetKeyring, Keyring sourceKeyring, Set<String> collectionIds) throws NotFoundException, BadRequestException, IOException {

        for (String collectionId : collectionIds) {
            SecretKey key = sourceKeyring.get(collectionId);
            if (key != null) {
                targetKeyring.put(collectionId, key);
            }
        }
    }

    /**
     * Transfer all secret keys from the source keyring to the target
     *
     * @param targetKeyring the keyring to be populated
     * @param sourceKeyring the keyring to take keys from
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws IOException
     */
    public static void transferKeyring(Keyring targetKeyring, Keyring sourceKeyring) throws NotFoundException, BadRequestException, IOException {
        Set<String> collectionIds = sourceKeyring.list();
        transferKeyring(targetKeyring, sourceKeyring, collectionIds);
    }

    private static boolean userShouldHaveKey(Zebedee zebedee, User user, Collection collection) throws IOException {
        if (zebedee.permissions.isAdministrator(user.email)
                || zebedee.permissions.canView(user, collection.description)) return true;
        return false;
    }
}
