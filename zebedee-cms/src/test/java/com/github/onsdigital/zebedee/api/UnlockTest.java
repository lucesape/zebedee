package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.service.TimeSeriesManifestService;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 03/10/2016.
 */
public class UnlockTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private TimeSeriesManifestService manifestServiceMock;

    @Mock
    private Zebedee zebedeeMock;

    @Mock
    private com.github.onsdigital.zebedee.model.Collections collections;

    @Mock
    private DataIndex dataIndex;

    private Unlock api;
    private CollectionDescription collectionDescription;

    @Override
    protected void customSetUp() throws Exception {
        api = new Unlock();

        collectionDescription = new CollectionDescription();

        ReflectionTestUtils.setField(api, "timeSeriesManifestService", manifestServiceMock);
        ReflectionTestUtils.setField(api, "zebedeeCmsService", zebedeeCmsServiceMock);
    }

    @Override
    protected Object getAPIName() {
        return this.getClass().getSimpleName();
    }

    @Test
    public void shouldUnlockWithNoZipsToDelete() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock))
                .thenReturn(collectionMock);
        when(zebedeeCmsServiceMock.getSession(requestMock))
                .thenReturn(session);
        when(zebedeeCmsServiceMock.getZebedee())
                .thenReturn(zebedeeMock);
        when(zebedeeMock.getCollections())
                .thenReturn(collections);
        when(collections.unlock(collectionMock, session))
                .thenReturn(true);
        when(zebedeeMock.getDataIndex())
                .thenReturn(dataIndex);
        when(manifestServiceMock.deleteGeneratedTimeSeriesZips(collectionMock, session, dataIndex))
                .thenReturn(true);
        when(collectionMock.getDescription())
                .thenReturn(collectionDescription);

        boolean result = api.unlockCollection(requestMock, responseMock);

        assertThat("Expected true", result, equalTo(true));

        verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
        verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
        verify(zebedeeCmsServiceMock, times(2)).getZebedee();
        verify(zebedeeMock, times(1)).getDataIndex();
        verify(zebedeeMock, times(1)).getCollections();
        verify(collections, times(1)).unlock(collectionMock, session);
        verify(manifestServiceMock, times(1)).deleteGeneratedTimeSeriesZips(collectionMock, session, dataIndex);
    }
}
