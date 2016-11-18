package com.github.onsdigital.zebedee.util.permissions;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.mashape.unirest.http.JsonNode;

public class VaultPermission {
    private boolean isAdmin;
    private boolean isPublisher;

    VaultPermission(JsonNode json) {
        isAdmin = json.getObject().getBoolean("admin");
        isPublisher = json.getObject().getBoolean("editor");
    }

    public void isAdmin() throws UnauthorizedException {
        if (!isAdmin) throw new UnauthorizedException("User is not a admin");
    }


    public void isPublisher() throws UnauthorizedException {
        if (!isPublisher) throw new UnauthorizedException("User is not a publisher");
    }

    public void isAdminOrPublisher() throws UnauthorizedException {
        if(!isPublisher || !isAdmin) throw new UnauthorizedException("User is not a admin or publisher");
    }

}
