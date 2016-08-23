package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.KeyringReader;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by thomasridd on 18/11/15.
 */
public class KeyManager {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

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

    public static void disributeApplicationKey(Zebedee zebedee, String application, SecretKey secretKey) throws IOException {
        for (User user : zebedee.users.list()) {
            distributeApplicationKeyToUser(zebedee, application, secretKey, user);
        }
    }

    private static void distributeApplicationKeyToUser(Zebedee zebedee, String application, SecretKey secretKey, User user) throws IOException {
        if (userShouldHaveApplicationKey(zebedee, user)) {
            // Add the key
            assignKeyToUser(zebedee, user, application, secretKey);
        } else {
            removeKeyFromUser(zebedee, user, application);
        }
    }


    private static boolean userShouldHaveApplicationKey(Zebedee zebedee, User user) throws IOException {
        return zebedee.permissions.isAdministrator(user.email) || zebedee.permissions.canEdit(user.email);
    }

    /**
     * Determine if the user should have the key assigned or removed for the given collection.
     *
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
            assignKeyToUser(zebedee, user, collection.description.id, key);
        } else {
            removeKeyFromUser(zebedee, user, collection.description.id);
        }
    }

    /**
     * @param zebedee
     * @param user
     * @param key
     * @throws IOException
     */
    public static void assignKeyToUser(Zebedee zebedee, User user, String keyIdentifier, SecretKey key) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.getKeyring() == null) return;

        HashMap<String, SecretKey> keysToAdd = new HashMap<>();
        keysToAdd.put(keyIdentifier, key);
        zebedee.users.addKeysToUser(user, keysToAdd);

        // If the user is logged in assign the key to their cached keyring
        Session session = zebedee.sessions.find(user.email);
        if (session != null) {
            Keyring keyring = zebedee.keyringCache.get(session);
            try {
                if (keyring != null)
                    keyring.put(keyIdentifier, key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove the collection key for the given user.
     * This method is intentionally private as the distribute* methods should be used
     * to re-evaluate whether a key should be removed instead of just removing it.
     *
     * @param zebedee
     * @param user
     * @throws IOException
     */
    private static void removeKeyFromUser(Zebedee zebedee, User user, String keyIdentifier) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.getKeyring() == null) return;

        Set<String> keysToRemove = new HashSet<>();
        keysToRemove.add(keyIdentifier);
        zebedee.users.removeKeysFromUser(user, keysToRemove);

        // If the user is logged in remove the key from their cached keyring
        Session session = zebedee.sessions.find(user.email);
        if (session != null) {
            Keyring keyring = zebedee.keyringCache.get(session);
            try {
                if (keyring != null)
                    keyring.remove(keyIdentifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Given a source keyring - determine what keys should belong to the given collection owner.
     * @param sourceKeyring
     * @param collectionOwner
     * @return
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws IOException
     */
    public static Map<String, SecretKey> determineKeysToAdd(KeyringReader sourceKeyring, CollectionOwner collectionOwner)
            throws NotFoundException, BadRequestException, IOException {
        Map<String, SecretKey> keys = new HashMap<>();

        if (sourceKeyring == null) {
            return new HashMap<>();
        }

        sourceKeyring.list()
                .stream()
                .forEach(collectionId -> {
                    if (StringUtils.equals(collectionId, CsdbImporter.APPLICATION_KEY_ID)) {
                        // csdb-import is a special case always add this.
                        keys.put(collectionId, sourceKeyring.get(collectionId));
                    } else {
                        Collection collection = getCollection(collectionId);
                        if (collection != null
                                && collection.description.collectionOwner != null
                                && collection.description.collectionOwner.equals(collectionOwner)) {
                            keys.put(collectionId, sourceKeyring.get(collectionId));
                        } else {
                            if (CollectionOwner.PUBLISHING_SUPPORT.equals(collectionOwner)) {
                                keys.put(collectionId, sourceKeyring.get(collectionId));
                            }
                        }
                    }
                });
        return keys;
    }

    private static boolean userShouldHaveKey(Zebedee zebedee, User user, Collection collection) throws IOException {
        if (zebedee.permissions.isAdministrator(user.email)
                || zebedee.permissions.canView(user, collection.description)) return true;
        return false;
    }

    private static Collection getCollection(String id) {
        try {
            return zebedeeCmsService.getCollection(id);
        } catch (ZebedeeException e) {
            logError(e, "failed to get collection").addParameter("collectionId", id).log();
            throw new RuntimeException("failed to get collection with collectionId " + id, e);
        }
    }

    public static void setZebedeeCmsService(ZebedeeCmsService zebedeeCmsService) {
        KeyManager.zebedeeCmsService = zebedeeCmsService;
    }
}
