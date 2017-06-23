package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.verification.http.ClientConfiguration;
import com.github.onsdigital.zebedee.verification.http.PooledHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by iankent on 06/06/2017.
 */
public class SMTPService {
    private static final String smtpServerHost = Configuration.getSMTPServerHost();
    private static final Integer smtpServerPort = Configuration.getSMTPServerPort();
    private static final String smtpServerUsername = Configuration.getSMTPServerUsername();
    private static final String smtpServerPassword = Configuration.getSMTPServerPassword();
    private static final String smtpServerSender = Configuration.getSMTPServerSender();

    //singleton
    private SMTPService() {
    }

    public static void SendVerificationEmail(String name, String ownerEmail, String email, String code) throws EmailException {
        Email mail = new SimpleEmail();
        mail.setHostName(smtpServerHost);
        mail.setSmtpPort(smtpServerPort);
        mail.setAuthenticator(new DefaultAuthenticator(smtpServerUsername, smtpServerPassword));
        mail.setStartTLSEnabled(true);
        mail.setFrom(smtpServerSender);
        mail.setSubject("Verify email address");
        mail.setMsg("Hi " + name + "\r\n\r\nAn account has been created on Florence/Ermintrude.\r\n\r\nPlease click the link below to verify your email address:\r\nhttp://localhost:8081/florence/index.html?email=" + email + "&verify=" + code + "\r\n\r\nThank you,\r\nFlorence");
        mail.addTo(ownerEmail);
        mail.send();
    }
}

