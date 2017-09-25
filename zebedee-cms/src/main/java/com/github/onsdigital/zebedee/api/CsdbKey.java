package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class CsdbKey {

    @RequestMapping(value = "/csdbKey", method = RequestMethod.GET)
    public String getPublicKey(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {
        return Root.zebedee.getApplicationKeys().getEncodedPublicKey(CsdbImporter.APPLICATION_KEY_ID);
    }
}
