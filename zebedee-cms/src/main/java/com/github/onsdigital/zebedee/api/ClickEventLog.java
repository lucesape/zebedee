package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.logging.click.event.ClickEventLogFactory;
import com.github.onsdigital.zebedee.model.ClickEvent;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * API endpoint for logging user click events.
 */
@RestController
public class ClickEventLog {

    private static ClickEventLogFactory clickEventLogFactory = ClickEventLogFactory.getInstance();

    /**
     * Log a click event.
     */
    @RequestMapping(value = "/clickeventlog", method = RequestMethod.POST)
    public void logEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody ClickEvent clickEvent)
            throws IOException {
        clickEventLogFactory.log(clickEvent);
    }

    private ClickEvent clickEventDetails(HttpServletRequest request) throws
            IOException {
        return new ObjectMapper().readValue(IOUtils.toByteArray(request.getInputStream()), ClickEvent.class);
    }
}
