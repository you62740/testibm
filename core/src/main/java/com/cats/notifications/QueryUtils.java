package com.cats.notifications;

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

/**
 * Created by ETP7307 on 24/05/2017.
 */

public class QueryUtils {

	/* Retourne une collection de hit d'assets sur le point d'expirer */
	public static Collection<Hit> getToexpireAssetHits(QueryBuilder queryBuilder, ResourceResolver resolver,
			long thisRunTime, long priorNotificationInMillis, String pathDam) {
		long startTime = thisRunTime;
		long endTime = thisRunTime + priorNotificationInMillis;

		// create query description as hash map (simplest way, same as form
		// post)

		Map<String, String> map = new HashMap<String, String>();
		map.put("path", pathDam);
		map.put("type", "dam:Asset");
		map.put("1_daterange.property", "jcr:content/metadata/prism:expirationDate");
		map.put("1_daterange.lowerBound", "" + startTime);
		map.put("1_daterange.upperBound", "" + endTime);
		map.put("property", "jcr:content/cq:lastReplicationAction");
		map.put("property.value", "Activate");
		Session session = (Session) resolver.adaptTo(Session.class);
		Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
		/*
		 * SearchResult represents a résultat de recherche d'une requêtre JCR,
		 * returned by Query
		 * 
		 */
		SearchResult results = query.getResult();

		return results.getHits();
	}

}
