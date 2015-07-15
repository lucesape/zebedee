package com.github.onsdigital.zebedee.api;


import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.util.Librarian;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

/**
 * Created by thomasridd on 15/07/15.
 */
@Api
public class Utils {
    /**
     *
     */
    @GET
    public String utilMethods(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {

//        Session session = Root.zebedee.sessions.get(request);

//        // Check whether we have access - currently this requires any logged in permissions
//        if (Root.zebedee.permissions.isAdministrator(session) || Root.zebedee.permissions.canEdit(session)) {
//            response.setStatus(HttpStatus.UNAUTHORIZED_401);
//            return null;
//        }

        // Currently let's just

        Librarian librarian = new Librarian(Root.zebedee);
        librarian.validateJSON();
        return Serialiser.serialise(librarian.invalidJson);
    }
}
