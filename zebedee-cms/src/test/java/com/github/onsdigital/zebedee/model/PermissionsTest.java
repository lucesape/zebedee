package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class PermissionsTest {

    Zebedee zebedee;
    Builder builder;
    Collection inflationCollection;
    Collection labourMarketCollection;

    CollectionDescription collectionDescription;
    Team team;
    String viewerEmail;

    @Mock
    private ZebedeeCmsService zebedeeCmsService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        builder = new Builder();
        zebedee = new Zebedee(builder.zebedee, false);
        inflationCollection = new Collection(builder.collections.get(0), zebedee);
        labourMarketCollection = new Collection(builder.collections.get(1), zebedee);

        Session session = zebedee.openSession(builder.administratorCredentials);

        // A new collection
        collectionDescription = new CollectionDescription();
        collectionDescription.name = this.getClass().getSimpleName() + "-" + Random.id();
        collectionDescription.type = CollectionType.scheduled;
        collectionDescription.publishDate = new Date();

        Collection.create(collectionDescription, zebedee, session);

        // A new team for the new collection
        team = zebedee.getTeams().createTeam(this.getClass().getSimpleName() + "-team-" + Random.id(), session);
        viewerEmail = builder.reviewer1.email;
        zebedee.getTeams().addTeamMember(viewerEmail, team, session);


        when(zebedeeCmsService.getCollection(anyString()))
                .thenReturn(inflationCollection);

        KeyManager.setZebedeeCmsService(zebedeeCmsService);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    //// Administrator tests ////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void administratorShouldOnlyHaveAdminPermission() throws IOException, NotFoundException, BadRequestException {

        // Given
        // The Administrator user (NB case-insensitive)
        String email = builder.administrator.email.toUpperCase();

        // When
        // We add the user as an administrator
        boolean adminPermission = zebedee.getPermissions().isAdministrator(email);
        boolean editPermission = zebedee.getPermissions().canEdit(email);
        boolean viewPermission = zebedee.getPermissions().canView(email, labourMarketCollection.description);

        // Then
        // The new user should get only admin permission:
        assertTrue(adminPermission);
        assertFalse(editPermission);
        assertFalse(viewPermission);
    }

    @Test
    public void onlyAdminShouldBeAdministrator() throws IOException {

        // Given
        // A bunch of user email addresses (NB case-insensitive)
        String administratorEmail = builder.administrator.email.toUpperCase();
        String publisherEmail = builder.publisher1.email.toUpperCase();
        String viewerEmail = builder.reviewer1.email.toUpperCase();
        String unknownEmail = "unknown@example.com";
        String nullEmail = null;

        // When
        // We ask if they are administrators:
        boolean adminIsAdministrator = zebedee.getPermissions().isAdministrator(administratorEmail);
        boolean publisherIsAdministrator = zebedee.getPermissions().isAdministrator(publisherEmail);
        boolean viewerIsAdministrator = zebedee.getPermissions().isAdministrator(viewerEmail);
        boolean unknownIsAdministrator = zebedee.getPermissions().isAdministrator(unknownEmail);
        boolean nullIsAdministrator = zebedee.getPermissions().isAdministrator(nullEmail);

        // Then
        // Only the administrator should be, and null should not give an error
        assertTrue(adminIsAdministrator);
        assertFalse(publisherIsAdministrator);
        assertFalse(viewerIsAdministrator);
        assertFalse(unknownIsAdministrator);
        assertFalse(nullIsAdministrator);
    }

    @Test

    public void shouldAddAdministrator() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A session for an administrator and a user without any Token
        String email = builder.reviewer1.email;
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.getPermissions().addAdministrator(email, session);

        // Then
        // The new user should get only admin permission:
        assertTrue(zebedee.getPermissions().isAdministrator(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfPublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A session for an administrator and a user without any Token
        String email = builder.reviewer1.email;
        Session session = zebedee.openSession(builder.publisher1Credentials);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfViewer() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A new Administrator user
        String email = builder.reviewer1.email;
        Session session = zebedee.openSession(builder.reviewer2Credentials);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfNotLoggedIn() throws IOException, UnauthorizedException {

        // Given
        // A new Administrator user
        String email = builder.reviewer1.email;
        Session session = null;

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldAddFirstAdministrator() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // The Administrator user has been removed (NB case-insensitive)
        String email = builder.administrator.email;
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.getPermissions().removeAdministrator(email.toUpperCase(), session);

        // When
        // We add an administrator with any session
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), null);

        // Then
        // The system should now have an administrator
        assertTrue(zebedee.getPermissions().hasAdministrator());
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddFirstAdministratorAgain() throws IOException, UnauthorizedException {

        // Given
        // An initial administrator user is present
        String email = "second.administrator@example.com";

        // When
        // We attempt to add a second administrator without having a session
        zebedee.getPermissions().addAdministrator(email, null);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldRemoveAdministrator() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A short-lived Administrator user (NB case-insensitive)
        String email = builder.reviewer1.email;
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), session);

        // When
        // We remove the user as an administrator
        zebedee.getPermissions().removeAdministrator(email, session);

        // Then
        // The new user should get no admin permission
        assertFalse(zebedee.getPermissions().isAdministrator(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfPublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // Users with insufficient permission
        Session publisher = zebedee.openSession(builder.publisher1Credentials);

        // When
        // We remove the user as an administrator
        zebedee.getPermissions().removeAdministrator(builder.administrator.email, publisher);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfViewer() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // Users with insufficient permission
        Session viewer = zebedee.openSession(builder.reviewer1Credentials);

        // When
        // We remove the user as an administrator
        zebedee.getPermissions().removeAdministrator(builder.administrator.email, viewer);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfNotLoggedIn() throws IOException, UnauthorizedException {

        // Given
        // No login session
        Session notLoggedIn = null;

        // When
        // We remove the user as an administrator
        zebedee.getPermissions().removeAdministrator(builder.administrator.email, notLoggedIn);

        // Then
        // The administrator should not have been removed
    }

    @Test
    public void shouldHaveAnAdministrator() throws IOException, UnauthorizedException {

        // Given
        // An Administrator user

        // When
        // We ask if there is an administrator
        boolean result = zebedee.getPermissions().hasAdministrator();

        // Then
        // There should be an administrator
        assertTrue(result);
    }

    @Test
    public void shouldHaveNoAdministrator() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // The Administrator user has been removed
        String email = builder.administrator.email;
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.getPermissions().removeAdministrator(email, session);

        // When
        // We ask if there is an administrator
        boolean result = zebedee.getPermissions().hasAdministrator();

        // Then
        // There should be no administrator
        assertFalse(result);
    }

    //// Publisher tests ////////////////////////////////////////////////////////////////////////////////////////////


    @Test
    public void publisherShouldHaveEditAndViewPermission() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A publisher user (NB case-insensitive)
        String email = builder.publisher1.email.toUpperCase();

        // When
        // We check the user's Token
        boolean adminPermission = zebedee.getPermissions().isAdministrator(email);
        boolean editPermission = zebedee.getPermissions().canEdit(email);
        boolean viewPermission = zebedee.getPermissions().canView(email, labourMarketCollection.description);

        // Then
        // The new should get edit and view:
        assertFalse(adminPermission);
        assertTrue(editPermission);
        assertTrue(viewPermission);
    }

    @Test
    public void shouldGetPermissionsForCaseInsensitiveDataVisEmail() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given a data vis user who logs in with an email containing captial letters
        String email = builder.dataVis.email.toUpperCase();

        Credentials credentials = builder.dataVisCredentials;
        credentials.email = credentials.email.toUpperCase();
        Session session = zebedee.openSession(credentials);

        // When we attempt to get user Token for that data vis publisher.
        PermissionDefinition permissionDefinition = zebedee.getPermissions().userPermissions(email, session);

        // Then no exception is thrown.
        Assert.notNull(permissionDefinition);
    }

    @Test
    public void onlyPublisherShouldBePublisher() throws IOException, UnauthorizedException {

        // Given
        // A bunch of user email addresses (NB case-insensitive)
        String administratorEmail = builder.administrator.email.toUpperCase();
        String publisherEmail = builder.publisher1.email.toUpperCase();
        String viewerEmail = builder.reviewer1.email.toUpperCase();
        String unknownEmail = "unknown@example.com";
        String nullEmail = null;

        // When
        // We ask if they are publishers:
        boolean adminIsPublisher = zebedee.getPermissions().canEdit(administratorEmail);
        boolean publisherIsPublisher = zebedee.getPermissions().canEdit(publisherEmail);
        boolean viewerIsPublisher = zebedee.getPermissions().canEdit(viewerEmail);
        boolean unknownIsPublisher = zebedee.getPermissions().canEdit(unknownEmail);
        boolean nullIsPublisher = zebedee.getPermissions().canEdit(nullEmail);

        // Then
        // Only the publisher should be, and null should not give an error
        assertFalse(adminIsPublisher);
        assertTrue(publisherIsPublisher);
        assertFalse(viewerIsPublisher);
        assertFalse(unknownIsPublisher);
        assertFalse(nullIsPublisher);
    }

    @Test

    public void shouldAddPublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A new publisher user
        String email = builder.reviewer1.email;
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.getPermissions().addEditor(email.toUpperCase(), session);

        // Then
        // The new user should get publish permission:
        assertTrue(zebedee.getPermissions().canEdit(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfPublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = zebedee.openSession(builder.publisher1Credentials);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.getPermissions().addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfViewer() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = zebedee.openSession(builder.reviewer1Credentials);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.getPermissions().addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfNotLoggedIn() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = null;

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.getPermissions().addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldRemovePublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // A short-lived publisher user (NB case-insensitive)
        String email = "Pearson.Longman@whitehouse.gov";
        Session session = zebedee.openSession(builder.administratorCredentials);
        zebedee.getPermissions().addAdministrator(email.toUpperCase(), session);

        // When
        // We remove the user as a publisher
        zebedee.getPermissions().removeEditor(email, session);

        // Then
        // The new user should get no publish permission
        assertFalse(zebedee.getPermissions().canEdit(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfPublisher() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // Users with insufficient permission
        Session publisher = zebedee.openSession(builder.publisher1Credentials);

        // When
        // We remove the user as an editor
        zebedee.getPermissions().removeEditor(builder.publisher1.email, publisher);

        // Then
        // The publisher should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfViewer() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        // Given
        // Users with insufficient permission
        Session viewer = zebedee.openSession(builder.reviewer1Credentials);

        // When
        // We remove the user as an publisher
        zebedee.getPermissions().removeEditor(builder.publisher1.email, viewer);

        // Then
        // The publisher should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfNotLoggedIn() throws IOException, UnauthorizedException {

        // Given
        // No login session
        Session notLoggedIn = null;

        // When
        // We remove the user as an publisher
        zebedee.getPermissions().removeEditor(builder.publisher1.email, notLoggedIn);

        // Then
        // The publisher should not have been removed
    }

    //// Viewer tests ////////////////////////////////////////////////////////////////////////////////////////////


    // TODO All viewer permission tests have been removed until Teams are implemented

}