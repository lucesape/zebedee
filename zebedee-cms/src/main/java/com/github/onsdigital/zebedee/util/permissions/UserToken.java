package com.github.onsdigital.zebedee.util.permissions;


import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class UserToken {

    private static String USER_SERVICE = "http://localhost:8080/user/token/";

    public static VaultPermission isValid(String token) throws UnauthorizedException {
        try {
            HttpResponse<JsonNode> message =
                    Unirest
                    .get(USER_SERVICE + token + "/")
                    .asJson();
            return new VaultPermission(message.getBody());
        } catch (UnirestException e) {
            throw new UnauthorizedException("");
        }
    }
}
