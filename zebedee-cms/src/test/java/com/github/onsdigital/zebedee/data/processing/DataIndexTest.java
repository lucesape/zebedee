package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/25/16.
 */
public class DataIndexTest {

    Zebedee zebedee;
    Builder bob;
    Session publisher;
    Session reviewer;

    ContentReader publishedReader;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;

    DataPagesSet published;

    /**
     * Setup generates an instance of zebedee, a collection, and various DataPagesSet objects (that are test framework generators)
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        bob = new Builder(DataPublicationDetailsTest.class);
        zebedee = new Zebedee(bob.zebedee, false);

        publisher = zebedee.openSession(bob.publisher1Credentials);
        reviewer = zebedee.openSession(bob.reviewer1Credentials);

        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        publishedReader = new ContentReader(zebedee.published.path);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);
    }

    @After
    public void tearDown() throws IOException {
        bob.delete();
    }

    @Test
    public void dataIndex_givenContent_buildsIndex() throws IOException, InterruptedException, BadRequestException {
        // Given
        // content
        ContentReader contentReader = publishedReader;

        // When
        // we build a DataIndex
        DataIndex dataIndex = new DataIndex(contentReader);
        dataIndex.pauseUntilComplete(60);

        // Then
        // indexing should complete with the published timeseries referenced
        assertTrue(dataIndex.cdids().size() > 0);
    }

}