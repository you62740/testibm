package com.cats.notifications;


/**
 * Created by ETP7307 on 15/05/2017.
 */
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
public class QueryUtils {
    public static Collection<Hit> getExpiredRefAssetHits(QueryBuilder queryBuilder, ResourceResolver resolver) {
        Map map = new HashMap();
        map.put("path", "/content/dam");
        map.put("type", "dam:Asset");
        map.put("1_property", "jcr:content/metadata/refExpired");
        map.put("1_property.value", "true");
        Session session = (Session) resolver.adaptTo(Session.class);
        SearchResult results = executeQuery(queryBuilder, session, map);
        return results.getHits();
    }
    public static Collection<Hit> getToexpireAssetHits(QueryBuilder queryBuilder, ResourceResolver resolver,
                                                       long thisRunTime, long lastRunTime, long priorNotificationInMillis) {
        long startTime = thisRunTime;
        long endTime = thisRunTime + priorNotificationInMillis;
        if (priorNotificationInMillis > thisRunTime - lastRunTime) {
            startTime = priorNotificationInMillis + lastRunTime;
        }
        Map map = new HashMap();
        map.put("path", "/content/dam");
        map.put("type", "dam:Asset");
        map.put("1_daterange.property", "jcr:content/metadata/prism:expirationDate");
        map.put("1_daterange.lowerBound", "" + startTime);
        map.put("1_daterange.upperBound", "" + endTime);
        Session session = (Session) resolver.adaptTo(Session.class);
        SearchResult results = executeQuery(queryBuilder, session, map);
        return results.getHits();
    }
    public static SearchResult executeQuery(QueryBuilder queryBuilder, Session session, Map map) {
        Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
        return query.getResult();
    }
}

