package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.Permissions;
import com.github.onsdigital.zebedee.service.TimeSeriesManifestService;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static com.github.onsdigital.zebedee.api.TimeSeriesContent.COLLECTION_REQUIRED_MSG;
import static com.github.onsdigital.zebedee.api.TimeSeriesContent.DATASET_ID;
import static com.github.onsdigital.zebedee.api.TimeSeriesContent.DATASET_ID_REQUIRED_MSG;
import static com.github.onsdigital.zebedee.api.TimeSeriesContent.INVALID_SESS_MSG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the {@link TimeSeriesContent} api class.
 */
public class TimeSeriesContentTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private TimeSeriesManifestService manifestServiceMock;

    @Mock
    private Permissions permissionsMock;

    @Mock
    private DataIndex dataIndexMock;

    private TimeSeriesContent api;
    private TimeSeriesContent.Response expectedResponse;

    @Override
    protected void customSetUp() throws Exception {
        api = new TimeSeriesContent();

        ReflectionTestUtils.setField(api, "zebedeeCmsService", zebedeeCmsServiceMock);
        ReflectionTestUtils.setField(api, "timeSeriesManifestService", manifestServiceMock);
    }

    @Override
    protected Object getAPIName() {
        return this.getClass().getSimpleName();
    }

    @Test(expected = BadRequestException.class)
    public void shouldReturnCollectionRequired() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock)).thenReturn(null);

        try {
            api.cleanUp(requestMock, responseMock);
        } catch (BadRequestException e) {
            assertThat("Incorrect error reason.", e.getMessage(), equalTo(COLLECTION_REQUIRED_MSG));

            verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
            verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
            verify(requestMock, times(1)).getParameter(DATASET_ID);
            verifyZeroInteractions(manifestServiceMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldReturnDatasetIdRequired() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock)).thenReturn(collectionMock);

        try {
            api.cleanUp(requestMock, responseMock);
        } catch (BadRequestException e) {
            assertThat("Incorrect error reason.", e.getMessage(), equalTo(DATASET_ID_REQUIRED_MSG));

            verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
            verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
            verify(requestMock, times(1)).getParameter(DATASET_ID);
            verifyZeroInteractions(manifestServiceMock);
            throw e;
        }
    }


    @Test(expected = UnauthorizedException.class)
    public void shouldReturnSessionNull() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock)).thenReturn(collectionMock);
        when(requestMock.getParameter(DATASET_ID)).thenReturn(DATASET_ID);

        try {
            api.cleanUp(requestMock, responseMock);
        } catch (BadRequestException e) {
            assertThat("Incorrect error reason.", e.getMessage(), equalTo(INVALID_SESS_MSG));

            verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
            verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
            verify(requestMock, times(1)).getParameter(DATASET_ID);
            verifyZeroInteractions(manifestServiceMock);
            verify(zebedeeCmsServiceMock, never()).getPermissions();
            throw e;
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldReturnNotAuthorised() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock)).thenReturn(collectionMock);
        when(requestMock.getParameter(DATASET_ID)).thenReturn(DATASET_ID);
        when(zebedeeCmsServiceMock.getSession(requestMock)).thenReturn(session);
        when(zebedeeCmsServiceMock.getPermissions()).thenReturn(permissionsMock);
        when(permissionsMock.canEdit(session)).thenReturn(false);

        try {
            api.cleanUp(requestMock, responseMock);
        } catch (BadRequestException e) {
            assertThat("Incorrect error reason.", e.getMessage(), equalTo(INVALID_SESS_MSG));

            verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
            verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
            verify(requestMock, times(1)).getParameter(DATASET_ID);
            verify(zebedeeCmsServiceMock, times(1)).getPermissions();
            verify(permissionsMock, times(1)).canEdit(session.email);
            verifyZeroInteractions(manifestServiceMock);
            throw e;
        }
    }

    @Test
    public void shouldDeleteTimeSeriesData() throws Exception {
        expectedResponse = setMockBehaviour(true);
        TimeSeriesContent.Response actual = api.cleanUp(requestMock, responseMock);
        verifySuccessful(actual);
    }

    @Test
    public void shouldReturnValidIndicatingNoFilesWereDeleted() throws Exception {
        expectedResponse = setMockBehaviour(false);
        TimeSeriesContent.Response actual = api.cleanUp(requestMock, responseMock);
        verifySuccessful(actual);
    }

    private TimeSeriesContent.Response setMockBehaviour(boolean isDeletes) throws Exception {
        when(zebedeeCmsServiceMock.getCollection(requestMock)).thenReturn(collectionMock);
        when(requestMock.getParameter(DATASET_ID)).thenReturn(DATASET_ID);
        when(zebedeeCmsServiceMock.getSession(requestMock)).thenReturn(session);
        when(zebedeeCmsServiceMock.getPermissions()).thenReturn(permissionsMock);
        when(permissionsMock.canEdit(session.email)).thenReturn(true);
        when(zebedeeCmsServiceMock.getZebedee()).thenReturn(zebedeeMock);
        when(zebedeeMock.getDataIndex()).thenReturn(dataIndexMock);
        when(manifestServiceMock.deleteGeneratedTimeSeriesFilesByDataId(DATASET_ID, dataIndexMock, collectionMock,
                session)).thenReturn(isDeletes);
        return new TimeSeriesContent.Response(isDeletes);
    }

    private void verifySuccessful(TimeSeriesContent.Response actual) throws Exception {
        assertThat("Unexpected response", actual, equalTo(expectedResponse));
        verify(zebedeeCmsServiceMock, times(1)).getCollection(requestMock);
        verify(zebedeeCmsServiceMock, times(1)).getSession(requestMock);
        verify(requestMock, times(1)).getParameter(DATASET_ID);
        verify(zebedeeCmsServiceMock, times(1)).getPermissions();
        verify(permissionsMock, times(1)).canEdit(session.email);
        verify(zebedeeCmsServiceMock, times(1)).getZebedee();
        verify(zebedeeMock, times(1)).getDataIndex();
        verify(manifestServiceMock, times(1)).deleteGeneratedTimeSeriesFilesByDataId(DATASET_ID, dataIndexMock,
                collectionMock, session);
    }
}
