package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by bren on 01/07/15.
 * <p>
 * Starts download for requested file in content directory
 */

@RestController
public class File {

    @RequestMapping(value = "/file", method = RequestMethod.GET)
    public Object post(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        try (Resource resource = RequestUtils.getResource(request)) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + resource.getName() + "\"");
            ReaderResponseResponseUtils.sendResponse(resource, response);
            return null;
        }
    }
}
