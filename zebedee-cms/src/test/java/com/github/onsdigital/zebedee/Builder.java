package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.persistence.dao.impl.FileUserRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * This is a utility class to build a known {@link Zebedee} structure for
 * testing.
 *
 * @author david
 */
public class Builder {

    private static int teamId;
    private static User administratorTemplate;
    private static User publisher1Template;
    private static User publisher2Template;
    private static User reviewer1Template;
    private static User reviewer2Template;
    private static boolean usersInitialised = false;

    public String[] collectionNames = {"Inflation Q2 2015", "Labour Market Q2 2015"};
    public String[] teamNames = {"Economy Team", "Labour Market Team"};
    public Path parent;
    public Path zebedee;
    public List<Path> collections;
    public List<String> teams;
    public List<String> contentUris;
    public User administrator;
    public User publisher1;
    public User publisher2;
    public User reviewer1;
    public User reviewer2;
    public Credentials administratorCredentials;
    public Credentials publisher1Credentials;
    public Credentials publisher2Credentials;
    public Credentials reviewer1Credentials;
    public Credentials reviewer2Credentials;
    public Team labourMarketTeam;
    public Team inflationTeam;

    /**
     * Constructor to build a known {@link Zebedee} structure with minimal structure for testing.
     *
     * @throws IOException
     */
    public Builder() throws IOException, CollectionNotFoundException {

        setupUsers();

        Root.env = new HashMap<>();



        // Create the structure:
        parent = Files.createTempDirectory(Random.id());
        zebedee = createZebedee(parent);

        // Create the collections:
        collections = new ArrayList<>();
        for (String collectionName : collectionNames) {
            Path collection = createCollection(collectionName, zebedee);
            collections.add(collection);
        }

        // Create the teams
        teams = new ArrayList<>();

        // Create some published content:
        Path folder = zebedee.resolve(Zebedee.PUBLISHED);
        contentUris = new ArrayList<>();
        String contentUri;
        Path contentPath;

        // Something for Economy:
        contentUri = "/economy/inflationandpriceindices/bulletins/consumerpriceinflationjune2014.html";
        contentPath = folder.resolve(contentUri.substring(1));
        Files.createDirectories(contentPath.getParent());
        Files.createFile(contentPath);
        contentUris.add(contentUri);

        // Something for Labour market:
        contentUri = "/employmentandlabourmarket/peopleinwork/earningsandworkinghours/bulletins/uklabourmarketjuly2014.html";
        contentPath = folder.resolve(contentUri.substring(1));
        Files.createDirectories(contentPath.getParent());
        Files.createFile(contentPath);
        contentUris.add(contentUri);

        // A couple of users:
        Path users = zebedee.resolve(FileUserRepository.USERS_DIR);
        Files.createDirectories(users);

        administrator = clone(administratorTemplate);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(administrator.email) + ".json"))) {
            Serialiser.serialise(outputStream, administrator);
        }

        publisher1 = clone(publisher1Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(publisher1.email) + ".json"))) {
            Serialiser.serialise(outputStream, publisher1);
        }

        publisher2 = clone(publisher2Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(publisher2.email) + ".json"))) {
            Serialiser.serialise(outputStream, publisher2);
        }

        reviewer1 = clone(reviewer1Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(reviewer1.email) + ".json"))) {
            Serialiser.serialise(outputStream, reviewer1);
        }

        reviewer2 = clone(reviewer2Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(reviewer2.email) + ".json"))) {
            Serialiser.serialise(outputStream, reviewer2);
        }

        administratorCredentials = userCredentials(administrator);
        publisher1Credentials = userCredentials(publisher1);
        publisher2Credentials = userCredentials(publisher2);
        reviewer1Credentials = userCredentials(reviewer1);
        reviewer2Credentials = userCredentials(reviewer2);

        Path sessions = zebedee.resolve(Zebedee.SESSIONS);
        Files.createDirectories(sessions);

        // Set up some permissions:
        Path permissions = zebedee.resolve(Zebedee.PERMISSIONS);
        Files.createDirectories(permissions);
        Path teams = zebedee.resolve(Zebedee.TEAMS);
        Files.createDirectories(teams);

        AccessMapping accessMapping = new AccessMapping();

        accessMapping.administrators = new HashSet<>();
        accessMapping.digitalPublishingTeam = new HashSet<>();

        accessMapping.administrators.add(administrator.email);
        accessMapping.digitalPublishingTeam.add(publisher1.email);
        accessMapping.digitalPublishingTeam.add(publisher2.email);

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.id = Random.id();
        accessMapping.collections = new HashMap<>();

        Zebedee z = new Zebedee(zebedee, false);
        inflationTeam = createTeam(reviewer1, teamNames[0], teams);
        labourMarketTeam = createTeam(reviewer2, teamNames[1], teams);
        accessMapping.collections.put(new Collection(collections.get(0), z).description.id, set(inflationTeam));
        accessMapping.collections.put(new Collection(collections.get(1), z).description.id, set(labourMarketTeam));

        Path path = permissions.resolve("accessMapping.json");
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
    }

    /**
     * Constructor to build an instance of zebedee using a predefined set of content
     *
     * @param bootStrap
     * @throws IOException
     */
    public Builder(Path bootStrap) throws IOException, CollectionNotFoundException {
        this();

        FileUtils.deleteDirectory(this.zebedee.resolve(Zebedee.PUBLISHED).toFile());
        FileUtils.deleteDirectory(this.zebedee.resolve(Zebedee.LAUNCHPAD).toFile());
        FileUtils.deleteDirectory(this.zebedee.resolve(Zebedee.COLLECTIONS).toFile());
        Files.createDirectory(this.zebedee.resolve(Zebedee.PUBLISHED));
        Files.createDirectory(this.zebedee.resolve(Zebedee.LAUNCHPAD));
        Files.createDirectory(this.zebedee.resolve(Zebedee.COLLECTIONS));

        FileUtils.copyDirectory(bootStrap.resolve(Zebedee.PUBLISHED).toFile(), this.zebedee.resolve(Zebedee.PUBLISHED).toFile());
        if (Files.exists(bootStrap.resolve(Zebedee.LAUNCHPAD))) {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.LAUNCHPAD).toFile(), this.zebedee.resolve(Zebedee.LAUNCHPAD).toFile());
        } else {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.PUBLISHED).toFile(), this.zebedee.resolve(Zebedee.LAUNCHPAD).toFile()); // Not bothering with distinct launchpad
        }

        if (Files.exists(bootStrap.resolve(Zebedee.COLLECTIONS))) {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.COLLECTIONS).toFile(), this.zebedee.resolve(Zebedee.COLLECTIONS).toFile());
        }
    }

    static User clone(User user) {
        User clone = new User();

        clone.name = user.name;
        clone.email = user.email;
        clone.inactive = user.inactive;
        clone.temporaryPassword = user.temporaryPassword;
        clone.lastAdmin = user.lastAdmin;
        clone(clone, user, "passwordHash");
        clone(clone, user, "keyring");
        return clone;
    }

    static void clone(User clone, User user, String fieldName) {
        try {
            Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(clone, field.get(user));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error cloning user", e);
        }
    }

    private synchronized void setupUsers() {

        if (!usersInitialised) {
            usersInitialised = true;

            // Set ISO date formatting in Gson to match Javascript Date.toISODate()
            Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

            logDebug("Generating test users and keys...").log();

            User jukesie = new User();
            jukesie.name = "Matt Jukes";
            jukesie.email = "jukesie@example.com";
            jukesie.inactive = false;
            administratorTemplate = jukesie;
            jukesie.resetPassword("password");

            User patricia = clone(jukesie);
            patricia.name = "Patricia Pumpkin";
            patricia.email = "patricia@example.com";
            patricia.inactive = false;
            publisher1Template = patricia;

            User bernard = clone(jukesie);
            bernard.name = "Bernard Black";
            bernard.email = "bernard@example.com";
            bernard.inactive = false;
            publisher2Template = bernard;

            User freddy = clone(jukesie);
            freddy.name = "freddy Pumpkin";
            freddy.email = "freddy@example.com";
            freddy.inactive = false;
            reviewer1Template = freddy;

            User ronny = clone(jukesie);
            ronny.name = "Ronny Roller";
            ronny.email = "ronny@example.com";
            ronny.inactive = false;
            reviewer2Template = ronny;
        }
    }

    private Credentials userCredentials(User user) {
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = "password";
        return credentials;
    }

    private Set<Integer> set(Team team) {
        Set<Integer> ids = new HashSet<>();
        ids.add(team.id);
        return ids;
    }

    private Team createTeam(User user, String name, Path teams) throws IOException {
        Team team = new Team();

        team.id = ++teamId;
        team.name = name;
        team.members = new HashSet<>();
        team.members.add(user.email);
        Path labourMarketTeamPath = teams.resolve(PathUtils.toFilename(team.name + ".json"));
        try (OutputStream output = Files.newOutputStream(labourMarketTeamPath)) {
            Serialiser.serialise(output, team);
        }

        return team;
    }

    public void delete() throws IOException {
        FileUtils.deleteDirectory(parent.toFile());
    }

    /**
     * Creates a published file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createPublishedFile(String uri) throws IOException {

        Path published = zebedee.resolve(Zebedee.PUBLISHED);
        Path content = published.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    /**
     * Creates an reviewed file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createReviewedFile(String uri) throws IOException {

        return createFile(Collection.REVIEWED, uri);
    }

    /**
     * Creates a complete file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createCompleteFile(String uri) throws IOException {

        return createFile(Collection.COMPLETE, uri);
    }

    /**
     * Creates a complete file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createInProgressFile(String uri) throws IOException {

        return createFile(Collection.IN_PROGRESS, uri);
    }

    /**
     * Creates a file in the given directory.
     *
     * @param directory The directory to be created.
     * @param uri       The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createFile(String directory, String uri) throws IOException {

        Path inProgress = collections.get(1).resolve(directory);
        Path content = inProgress.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    /**
     * Creates an reviewed file in a different {@link com.github.onsdigital.zebedee.model.Collection}.
     *
     * @param uri        The URI to be created.
     * @param collection The {@link com.github.onsdigital.zebedee.model.Collection} in which to create the content.
     * @throws IOException If a filesystem error occurs.
     */
    public void isBeingEditedElsewhere(String uri, int collection) throws IOException {

        Path reviewed = collections.get(collection)
                .resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED);
        Path content = reviewed.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    public Session createSession(String email) throws IOException {

        // Build the session object
        Session session = new Session();
        session.id = Random.id();
        session.email = email;

        // Determine the path in which to create the session Json
        Path sessionPath;
        String sessionFileName = PathUtils.toFilename(session.id);
        sessionFileName += ".json";
        sessionPath = zebedee.resolve(Zebedee.SESSIONS).resolve(sessionFileName);

        // Serialise
        try (OutputStream output = Files.newOutputStream(sessionPath)) {
            Serialiser.serialise(output, session);
        }

        return session;
    }

    public Session createSession(User user) throws IOException {

        // Build the session object
        Session session = new Session();
        session.id = Random.id();
        session.email = user.email;

        // Determine the path in which to create the session Json
        Path sessionPath;
        String sessionFileName = PathUtils.toFilename(session.id);
        sessionFileName += ".json";
        sessionPath = zebedee.resolve(Zebedee.SESSIONS).resolve(sessionFileName);

        // Serialise
        try (OutputStream output = Files.newOutputStream(sessionPath)) {
            Serialiser.serialise(output, session);
        }

        return session;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionaly copied from {@link Zebedee#create(Path)}. This
     * ensures there's a fixed expectation, rather than relying on a method that
     * will be tested as part of the test suite.
     *
     * @param parent The parent folder, in which the {@link Zebedee} structure will
     *               be built.
     * @return The root {@link Zebedee} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createZebedee(Path parent) throws IOException {
        Path path = Files.createDirectory(parent.resolve(Zebedee.ZEBEDEE));
        Files.createDirectory(path.resolve(Zebedee.PUBLISHED));
        Files.createDirectory(path.resolve(Zebedee.COLLECTIONS));
        Files.createDirectory(path.resolve(Zebedee.SESSIONS));
        Files.createDirectory(path.resolve(Zebedee.PERMISSIONS));
        Files.createDirectory(path.resolve(Zebedee.TEAMS));
        Files.createDirectory(path.resolve(Zebedee.LAUNCHPAD));
        return path;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionally copied from
     * <p>
     * This ensures there's a fixed expectation, rather than relying on a method that will be tested as part
     * of the test suite.
     *
     * @param root The root of the {@link Zebedee} structure
     * @param name The name of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @return The root {@link com.github.onsdigital.zebedee.model.Collection} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createCollection(String name, Path root) throws IOException {

        String filename = PathUtils.toFilename(name);
        Path collections = root.resolve(Zebedee.COLLECTIONS);

        // Create the folders:
        Path collection = collections.resolve(filename);
        Files.createDirectory(collection);
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.COMPLETE));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS));

        // Create the description:
        Path collectionDescription = collections.resolve(filename + ".json");
        CollectionDescription description = new CollectionDescription();
        description.id = Random.id();
        description.name = name;
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }

    /**
     * Builds simple random walk timeseries to
     *
     * @param name
     * @return
     */
    public Path randomWalkTimeSeries(String name) {
        return randomWalkTimeSeries(name, true, true, true, 10, 2015);
    }

    public Path randomWalkTimeSeries(String name, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate) {
        return randomWalkTimeSeries(name, withYears, withQuarters, withMonths, yearsToGenerate, 2015);
    }

    public Path randomWalkTimeSeries(String name, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");
        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int y = finalYear - yearsToGenerate + 1; y <= finalYear; y++) {
            if (withYears) {
                TimeSeriesValue value = new TimeSeriesValue();

                value.date = y + "";
                value.year = y + "";
                value.value = String.format("%.1f", val);
                timeSeries.years.add(value);
            }
            for (int q = 0; q < 4; q++) {
                if (withQuarters) {
                    TimeSeriesValue value = new TimeSeriesValue();
                    value.year = y + "";
                    value.quarter = quarters[q];

                    value.date = y + " " + quarters[q];
                    value.value = String.format("%.1f", val);
                    timeSeries.quarters.add(value);
                }
                for (int mInQ = 0; mInQ < 3; mInQ++) {
                    if (withMonths) {
                        TimeSeriesValue value = new TimeSeriesValue();
                        value.month = months[3 * q + mInQ];
                        value.year = y + "";

                        value.date = y + " " + months[3 * q + mInQ];
                        value.value = String.format("%.1f", val);
                        timeSeries.months.add(value);
                    }
                    val = val + distribution.sample();
                }
            }
        }


        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path randomWalkQuarters(String name, int startYear, int startQuarter, int datapoints) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int pt = 0; pt < datapoints; pt++) {
            val = val + distribution.sample();

            TimeSeriesValue value = new TimeSeriesValue();
            int quarter = (startQuarter + pt) % 4;
            int year = startYear + ((pt + startQuarter) / 4);

            value.date = year + " " + quarters[quarter];
            value.value = String.format("%.1f", val);

            timeSeries.quarters.add(value);

        }

        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    public Path randomWalkMonths(String name, int startYear, int startMonth, int datapoints) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int pt = 0; pt < datapoints; pt++) {
            val = val + distribution.sample();

            TimeSeriesValue value = new TimeSeriesValue();
            int month = (startMonth + pt) % 12;
            int year = startYear + ((pt + startMonth) / 12);

            value.date = year + " " + months[month];
            value.value = String.format("%.1f", val);

            timeSeries.months.add(value);

        }

        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }
}
