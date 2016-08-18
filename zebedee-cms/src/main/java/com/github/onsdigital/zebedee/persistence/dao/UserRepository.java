package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;

import java.io.IOException;

/**
 * Interface for user persistence.
 */
public interface UserRepository {

    /**
     * Get the user with the given email address.
     *
     * @param email
     * @return
     * @throws IOException
     */
    User getUser(String email) throws IOException;

    /**
     * Get all users.
     *
     * @return
     * @throws IOException
     */
    UserList getAllUsers() throws IOException;

    /**
     * Delete the user with the given email address.
     *
     * @param email
     * @throws IOException
     */
    void deleteUser(String email) throws IOException;

    /**
     * Return true if a user with the given email exists.
     *
     * @param email
     * @return
     */
    boolean userExists(String email);

    /**
     * Save the given user.
     *
     * @param result
     * @throws IOException
     */
    void saveUser(User result) throws IOException;
}
