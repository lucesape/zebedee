package com.github.onsdigital.zebedee.util.encryption;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Base64;

public class EncryptionApi {

    public static final String ROOT_TOKEN = System.getenv("VAULT_TOKEN");

    private static final String ENCRYPT_URI_AI = "http://localhost:8100";

    public static void createKey(final String collectionId, final String token) throws UnirestException {
        final String message = "{\"collectionId\":\"" + collectionId + "\"}";

        Unirest.post(ENCRYPT_URI_AI + "/key")
                .header("Content-Type", "application/json")
                .header("Cookie", "access_token=" + token + ";")
                .body(message)
                .asJson();
    }

    public static String encrypt(final String id, final String data, String token) throws UnirestException {

        String message = "{\"id\":\""+ id +"\",\"data\": \"" + encodeBase64(data) + "\"}";

        HttpResponse<JsonNode> response = Unirest.post(ENCRYPT_URI_AI + "/encrypt")
                .header("Content-Type", "application/json")
                .header("Cookie", "access_token=" + token + ";")
                .body(message)
                .asJson();

        return response
                .getBody()
                .getObject()
                .getString("data");

    }

    public static String decrypt(final String id,final String data, String token) throws UnirestException {

        String message = "{\"id\":\""+ id +"\",\"data\": \"" + data + "\"}";

        HttpResponse<JsonNode> response = Unirest.post(ENCRYPT_URI_AI + "/decrypt")
                .header("Content-Type", "application/json")
                .header("Cookie", "access_token=" + token + ";")
                .body(message)
                .asJson();

        return decodeBase64(response
                .getBody()
                .getObject()
                .getString("data"));

    }

    private static String encodeBase64(String data) {
        return new String(Base64.getEncoder().encode(data.getBytes()));
    }

    private static String decodeBase64(String data) {
        return new String(Base64.getDecoder().decode(data.getBytes()));
    }
}
