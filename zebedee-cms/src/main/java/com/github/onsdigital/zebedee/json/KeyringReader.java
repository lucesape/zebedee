package com.github.onsdigital.zebedee.json;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Set;

public interface KeyringReader extends Cloneable {
    int size();

    KeyringReader clone();

    boolean unlock(String password);

    SecretKey get(String collectionId);

    Set<String> list();

    PublicKey getPublicKey();

    boolean isUnlocked();


}
