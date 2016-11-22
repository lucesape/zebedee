package com.github.onsdigital.zebedee.util.token;


import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import javax.servlet.http.HttpServletRequest;

public class UserToken {

    private static String USER_SERVICE = "http://localhost:8080/user/token/";

    public static TokenDetails isValid(HttpServletRequest request) throws UnauthorizedException {
        return isValid(RequestUtils.getSessionId(request));
    }

    public static TokenDetails isValid(String token) throws UnauthorizedException {
        try {
            HttpResponse<JsonNode> message =
                    Unirest
                    .get(USER_SERVICE + token + "/")
                    .asJson();
            return new TokenDetails(message.getBody());
        } catch (UnirestException e) {
            throw new UnauthorizedException("");
        }
    }
}
