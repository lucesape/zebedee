package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.converter.JSONToFileConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * On the fly conversion of .json objects to CSV/ JSON-STAT/ yadayada
 * <p>
 * Dataservices is currently part of Zebedee for convenience
 * <p>
 * Later it should be implemented as part of Brian or whatever our data service is called
 * <p>
 * Created by thomasridd on 13/05/15.
 */
@RestController
public class DataServices {

    /**
     * Converts a .json object to data
     * <p>
     * Output should be data plus meta-data
     *
     * @param request
     * @param response <ul>
     *                 </ul>
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/dtaServices", method = RequestMethod.POST)
    public void convertFiles(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Grab parameters
        String output = request.getParameter("output");
        String input = request.getParameter("input");

        // And write
        JSONToFileConverter.writeRequestJSONToOutputFormat(request, response, input, output);

        return;
    }
}
