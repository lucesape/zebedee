package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.exceptions.CollectionAuditException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.HibernateUtil;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import org.hibernate.Session;

import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Implementation of the {@link CollectionHistoryDao} interface. Provides methods for saving {@link CollectionHistoryEvent}'s
 * and getting events by collection ID.
 */
public class CollectionHistoryDaoImpl extends CollectionHistoryDao {

    private static final String COLLECTION_ID = "collection_id";

    private static final String SELECT_BY_COLLECTION_ID =
            "SELECT collection_history_event_id, collection_id, collection_name, " +
                    "event_date, event_type, exception_text, file_uri, page_uri, florence_user" +
                    " FROM collection_history " +
                    "WHERE collection_id = :collection_id " +
                    "ORDER BY event_date ASC";

    CollectionHistoryDaoImpl() {
        // Hide constructor and force use of singleton instance.
    }

    /**
     * Save a {@link CollectionHistoryEvent} to the database.
     *
     * @param event
     * @throws ZebedeeException
     */
    public void saveCollectionHistoryEvent(CollectionHistoryEvent event) throws ZebedeeException {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();
            session.save(event);
            session.getTransaction().commit();
        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to save collection audit event")
                    .addParameter("event", event.toString())
                    .logAndThrow(CollectionAuditException.class);
        }
    }

    @Override
    public void saveCollectionHistoryEvent(Collection collection, com.github.onsdigital.zebedee.json.Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collection.description.id, collection.description.name,
                session, collectionEventType, metaValues));
    }

    @Override
    public void saveCollectionHistoryEvent(String collectionId, String collectionName,
                                           com.github.onsdigital.zebedee.json.Session session,
                                           CollectionEventType collectionEventType, CollectionEventMetaData... metaValues)
            throws ZebedeeException {
        this.saveCollectionHistoryEvent(new CollectionHistoryEvent(collectionId, collectionName, session,
                collectionEventType, metaValues));
    }

    public List<CollectionHistoryEvent> getCollectionEventHistory(String collectionId) throws ZebedeeException {
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            session.beginTransaction();

            List<CollectionHistoryEvent> events = (List<CollectionHistoryEvent>) session
                    .createSQLQuery(SELECT_BY_COLLECTION_ID)
                    .addEntity(CollectionHistoryEvent.class)
                    .setString(COLLECTION_ID, collectionId)
                    .list();

            session.getTransaction().commit();
            return events;

        } catch (Exception e) {
            logError(e, "Unexpected error while attempting to save collection audit event")
                    .logAndThrow(CollectionAuditException.class);
        }
        return null;
    }
}
