package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Created by david on 12/03/2015.
 */
@RestController
public class Permission {

    /**
     * Grants the specified permissions.
     *
     * @param request              Should be a {@link PermissionDefinition} Json message.
     * @param response             <ul>
     *                             <li>If admin is True, grants administrator permission. If admin is False, revokes</li>
     *                             <li>If editor is True, grants editing permission. If editor is False, revokes</li>
     *                             <li>Note that admins automatically get editor permissions</li>
     *                             </ul>
     * @param permissionDefinition The email and permission details for the user.
     * @return A String message confirming that the user's permissions were updated.
     * @throws IOException           If an error occurs accessing data.
     * @throws UnauthorizedException If the logged in user is not an administrator.
     * @throws BadRequestException   If the user specified in the {@link PermissionDefinition} is not found.
     */
    @RequestMapping(value = "/permission", method = RequestMethod.POST)
    public String grantPermission(HttpServletRequest request, HttpServletResponse response,
                                  @RequestBody PermissionDefinition permissionDefinition)
            throws IOException, ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);

        // Administrator
        if (BooleanUtils.isTrue(permissionDefinition.admin)) {
            Root.zebedee.getPermissionsService().addAdministrator(permissionDefinition.email, session);
            // Admins must be publishers so update the permissions accordingly
            permissionDefinition.editor = true;
            Audit.Event.ADMIN_PERMISSION_ADDED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.email)
                    .log();
        } else if (BooleanUtils.isFalse(permissionDefinition.admin)) {
            Root.zebedee.getPermissionsService().removeAdministrator(permissionDefinition.email, session);
            Audit.Event.ADMIN_PERMISSION_REMOVED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.email)
                    .log();
        }

        if (BooleanUtils.isTrue(permissionDefinition.dataVisPublisher)) {
            Root.zebedee.getPermissionsService().addDataVisualisationPublisher(permissionDefinition.email, session);
            logInfo("Data Vis Publisher permission added to user.")
                    .user(permissionDefinition.email)
                    .addParameter("by", session.getEmail()).log();
        } else if (BooleanUtils.isFalse(permissionDefinition.dataVisPublisher)) {
            Root.zebedee.getPermissionsService().removeDataVisualisationPublisher(permissionDefinition.email, session);
            logInfo("Data Vis Publisher permission removed from user.")
                    .user(permissionDefinition.email)
                    .addParameter("by", session.getEmail()).log();
        }

        // Digital publishing
        if (BooleanUtils.isTrue(permissionDefinition.editor)) {
            Root.zebedee.getPermissionsService().addEditor(permissionDefinition.email, session);
            Audit.Event.PUBLISHER_PERMISSION_ADDED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.email)
                    .log();
        } else if (BooleanUtils.isFalse(permissionDefinition.editor)) {
            Root.zebedee.getPermissionsService().removeEditor(permissionDefinition.email, session);
            Audit.Event.PUBLISHER_PERMISSION_REMOVED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), permissionDefinition.email)
                    .log();
        }

        return "Permissions updated for " + permissionDefinition.email;
    }

    /**
     * Grants the specified permissions.
     *
     * @param request  Should be of the form {@code /permission?email=florence@example.com}
     * @param response A permissions object for that user
     * @return
     * @throws IOException           If an error occurs accessing data.
     * @throws UnauthorizedException If the user is not an administrator.
     * @throws BadRequestException   If the user specified in the {@link PermissionDefinition} is not found.
     */
    @RequestMapping(value = "/permission", method = RequestMethod.GET)
    public PermissionDefinition getPermissions(HttpServletRequest request, HttpServletResponse response)
            throws IOException, NotFoundException, UnauthorizedException {

        Session session = Root.zebedee.getSessionsService().get(request);
        String email = request.getParameter("email");

        PermissionDefinition permissionDefinition = Root.zebedee.getPermissionsService().userPermissions(email, session);

        return permissionDefinition;
    }

}
