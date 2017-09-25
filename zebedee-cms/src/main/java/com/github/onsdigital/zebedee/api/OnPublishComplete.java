package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.util.ContentTree;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Endpoint to be called when a publish takes place.
 * Can be used to clear cached items / search indexes etc.
 */
@RestController
public class OnPublishComplete {

    /**
     * Generates new reindexing key/hash values.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {
        String key = Random.password(64);
        logDebug("Key added to environment").addParameter("key", key).log();
        logDebug("Key hash (for REINDEX_KEY_HASH)").addParameter("keyHash", Password.hash(key)).log();
    }

    @RequestMapping(value = "/onPublishComplete", method = RequestMethod.POST)
    public Object onPublishComplete(HttpServletRequest request, HttpServletResponse response) throws IOException {

        logDebug("Clearing browser tree cache").log();
        ContentTree.dropCache();
        response.setStatus(HttpStatus.OK.value());
        return "OnPublishComplete handler finished";
    }
}
