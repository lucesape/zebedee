package com.github.onsdigital.zebedee.util.token;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.mashape.unirest.http.JsonNode;

public class TokenDetails {
    private String email;
    private boolean isAdmin;
    private boolean isPublisher;
    private boolean isdataVisPublisher;

    TokenDetails(JsonNode json) {
        email = json.getObject().getString("email");
        isAdmin = json.getObject().getBoolean("admin");
        isPublisher = json.getObject().getBoolean("editor");
        isdataVisPublisher = json.getObject().getBoolean("dataVisPublisher");

    }

    public void isAdmin() throws UnauthorizedException {
        if (!isAdmin) throw new UnauthorizedException("User is not a admin");
    }


    public void isPublisher() throws UnauthorizedException {
        if (!isPublisher) throw new UnauthorizedException("User is not a publisher");
    }

    public void isAdminOrPublisher() throws UnauthorizedException {
        if(!isPublisher && !isAdmin) throw new UnauthorizedException("User is not a admin or publisher");
    }

    public void isViewer() throws UnauthorizedException {
        if (!isPublisher && !isAdmin && !isdataVisPublisher) throw new UnauthorizedException("User can not view this data");
    }

    public String getEmail() {
        return email;
    }
}
