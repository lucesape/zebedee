package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.PingRequest;
import com.github.onsdigital.zebedee.json.PingResponse;
import com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by thomasridd on 14/07/15.
 */
@RestController
public class Ping {

    private static MetricsService metricsService = MetricsService.getInstance();

    /**
     * Sends a message to a user session requiring it to stay alive
     * <p>
     * Returns true if the session is alive, false otherwise
     */
    @RequestMapping(value = "/ping", method = RequestMethod.POST)
    public PingResponse ping(HttpServletRequest request, HttpServletResponse response,
                             @RequestBody PingRequest pingRequest) throws IOException {
        if (pingRequest.lastPingTime != null && pingRequest.lastPingTime > 0) {
            metricsService.capturePing(pingRequest.lastPingTime);
        }

        PingResponse pingResponse = new PingResponse();
        setSessionDetails(request, pingResponse);
        return pingResponse;
    }

    private void setSessionDetails(HttpServletRequest request, PingResponse pingResponse) {
        try {
            SessionsService sessionsService = Root.zebedee.getSessionsService();
            String token = RequestUtils.getSessionId(request);
            if (sessionsService.exists(token)) {
                Session session = sessionsService.read(token);
                if (session != null && !sessionsService.expired(session)) {
                    pingResponse.hasSession = true;
                    pingResponse.sessionExpiryDate = sessionsService.getExpiryDate(session);
                }
            }
        } catch (IOException e) {
            ZebedeeReaderLogBuilder.logError(e).log();
        }
    }
}