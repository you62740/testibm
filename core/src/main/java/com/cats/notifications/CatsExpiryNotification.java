package com.cats.notifications;

/**
 * Created by ETP7307 on 15/05/2017.
 */

import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.security.user.UserPropertiesService;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

/***  Created by ETP7307 on 11/05/2017 */
/*** Configuration OSGI */

@Component(metatype = true, label = "Credit Agricole's AEM DAM Expiry Notification Job", description = "Cats AEM DAM Expiry Notification")
public class CatsExpiryNotification {
	private static final Logger log = LoggerFactory.getLogger(CatsExpiryNotification.class);
	@Reference
	private Scheduler scheduler;
	/*
	 * https://sling.apache.org/documentation/bundles/scheduler-service-commons-
	 * scheduler.html
	 */
	@Reference
	private ResourceResolverFactory resolverFactory;
	/* Resources are pieces of content on which Sling acts */
	@Reference
	private QueryBuilder queryBuilder;
	@Reference
	Externalizer externalizerService = null;
	@Reference
	UserPropertiesService userPropertiesService;
	@Reference
	private Replicator replicator;
	/*
	 * The Replicator is the replication service. It can be used to replicate
	 * content.
	 */

	@Property(boolValue = {
			true }, label = "Activation Scheduler", description = "Activate or desactivate the scheduler")
	public static final String SCHEDULE_ACTIVATION = "cq.dam.expiry.notification.scheduler.activation";
	@Property(value = {
			"/content/dam/" }, name = "path_dam", label = "Path du DAM", description = "Noeud sur lequel on veut paramétrer les notifications")
	public static final String PATH_DAM = "path_dam";
	@Property(value = {
			"gu_gie_a_bdig_supergestionnaire_metier " }, name = "groupe_entite", label = "Groupe Gestionnaire de l'entité")
	public static final String GROUPE_ENTITE = "groupe_entite";

	/*
	 * SI Scheduler Basé sur l'heure True - Propriété pour un scheduler qui se
	 * lance à une heure précise
	 */
	@Property(boolValue = {
			true }, label = "Time based Scheduler", description = "Whether to schedule a time based schedular")
	public static final String SCHEDULE_TIME_BASED = "cq.dam.expiry.notification.scheduler.istimebased";
	@Property(value = {
			"0 0 0 * * ?" }, label = "Time Based Scheduler Rule", description = "Regular expression for time based Scheduler. Eg: \'0 0 0 * * ?\'. The example expression triggers the Job @ 00 hrs. This expression get picked if Time Based Scheduler is true")
	public static final String SCHEDULER_TIMEBASED_RULE = "cq.dam.expiry.notification.scheduler.timebased.rule";

	/*
	 * SINON Scheduler Periodique - saisir Période de temps au bout de laquelle
	 * le scheduler se relance
	 */

	@Property(longValue = {
			1500L }, label = "Periodic Scheduler", description = "Time in seconds for periodic scheduler. This expression get picked if Time Based Scheduler is set false")
	public static final String SCHEDULER_PERIOD = "cq.dam.expiry.notification.scheduler.period.rule";
	/*
	 * Temps en seconde saisi pour notifier qu'un asset EST SUR LE POINT
	 * d'expirer
	 */
	@Property(longValue = {
			3600L }, name = "prior_notification_seconds", label = "Prior notification in seconds", description = "Number of seconds before which a notification should be sent before an asset expires")
	public static final String PRIOR_NOTIFICATION_SECONDS = "prior_notification_seconds";

	public static final String TASK_TYPE = "Notification";
	private static final String EXPIRY_MONITORING_SERVICE = "expirymonitoringhelper";
	private static final String NOTIFICATION_SERVICE = "notificationhelper";

	private static Calendar LAST_RUN;
	private static Calendar THIS_RUN;
	private long priorNotificationInSeconds;
	private String path_dam;
	private String groupe_entite;
	private String jobName;

	public CatsExpiryNotification() {
	}

	/*
	 * Activation du Scheduler
	 */
	protected void activate(ComponentContext componentContext) throws Exception {
		this.path_dam = (String) componentContext.getProperties().get("path_dam");
		log.debug("path : " + path_dam);
		jobName = this.getClass().getSimpleName().toString() + "/" + path_dam.replace("/", "-");
		log.debug("jobName : " + jobName);
		ImmediateJob job = new ImmediateJob(path_dam);
		boolean e = ((Boolean) componentContext.getProperties().get("cq.dam.expiry.notification.scheduler.istimebased"))
				.booleanValue();
		ScheduleOptions options = null;
		if (e) {
			String periodinsecs = (String) componentContext.getProperties()
					.get("cq.dam.expiry.notification.scheduler.timebased.rule");
			options = scheduler.EXPR(periodinsecs);
		} else {
			long periodinsecs1 = ((Long) componentContext.getProperties()
					.get("cq.dam.expiry.notification.scheduler.period.rule")).longValue();
			options = scheduler.NOW(-1, periodinsecs1);
		}
		this.priorNotificationInSeconds = ((Long) componentContext.getProperties().get("prior_notification_seconds"))
				.longValue();
		this.groupe_entite = (String) componentContext.getProperties().get("groupe_entite");
		options.name(jobName);
		options.canRunConcurrently(false);
		log.debug("options : " + options);
		scheduler.schedule(job, options);
	}

	private Collection<Hit> getExpiredAssetHits(ResourceResolver resolver) {
		Map map = new HashMap();
		map.put("path", "/content/dam");
		map.put("type", "dam:Asset");
		map.put("1_daterange.property", "jcr:content/metadata/prism:expirationDate");
		map.put("1_daterange.lowerBound", "" + LAST_RUN.getTimeInMillis());
		map.put("1_daterange.upperBound", "" + THIS_RUN.getTimeInMillis());
		Query query = this.queryBuilder.createQuery(PredicateGroup.create(map),
				(Session) resolver.adaptTo(Session.class));
		SearchResult results = query.getResult();
		return results.getHits();
	}

	private Collection<Asset> propagateExpiry(ResourceResolver resolver, Collection<Hit> hits)
			throws RepositoryException {
		AssetManager assetManager = resolver.adaptTo(AssetManager.class);
		Session session = (Session) resolver.adaptTo(Session.class);
		Collection<Asset> expiredSinceLastRun = getExpiredAssets(resolver, hits);
		Collection<Asset> expireRefererAssets = new ArrayList();
		for (Asset each : expiredSinceLastRun) {
			String path = each.getPath();
			Collection<Asset> refererAssets = DamUtil.getRefererAssets(resolver, each.getPath());
			Resource res = resolver.getResource(path);
			if (DamUtil.isSubAsset(res)) {
				Asset asset = DamUtil.getParentAsset(res);
				if (null != asset) {
					refererAssets.add(asset);
				}
			}
			expireRefererAssets.addAll(refererAssets);
			for (Asset refAsset : refererAssets) {
				Node refNode = (Node) resolver.getResource(refAsset.getPath()).adaptTo(Node.class);
				refNode.getNode("jcr:content/metadata").setProperty("refExpired", true);
			}
		}
		session.save();
		return expireRefererAssets;
	}

	private Collection<Asset> getExpiredAssets(ResourceResolver resolver, Collection<Hit> hits)
			throws RepositoryException {
		List expiredSinceLastRun = new ArrayList();
		if (hits.size() > 0) {
			for (Hit hit : hits) {
				Resource res = resolver.getResource(hit.getPath());
				Asset asset = null;
				if (null != res) {
					asset = (Asset) res.adaptTo(Asset.class);
					if (null != asset) {
						expiredSinceLastRun.add(asset);
					}
				}
			}
		}
		return expiredSinceLastRun;
	}

	private void dePropagateExpiry(ResourceResolver resolver) throws RepositoryException {
		Calendar now = Calendar.getInstance();
		Collection<Asset> expiredRefAssets = getExpiredRefAssets(resolver);
		for (Asset each : expiredRefAssets) {
			Collection<Asset> subAssets = DamUtil.getReferencedSubAssets(resolver.getResource(each.getPath()));
			subAssets.addAll(DamUtil.getSubAssets(resolver.getResource(each.getPath())));
			boolean toChangeStatus = true;
			for (Asset each1 : subAssets) {
				Node node = (Node) resolver.getResource(each1.getPath()).adaptTo(Node.class);
				if (!(node.hasProperty("jcr:content/metadata/prism:expirationDate"))) {
					continue;
				}
				Calendar expiryTime = node.getProperty("jcr:content/metadata/prism:expirationDate").getDate();
				if (expiryTime.before(now)) {
					toChangeStatus = false;
					break;
				}
			}
			if (toChangeStatus) {
				Node eachNode = (Node) resolver.getResource(each.getPath()).adaptTo(Node.class);
				eachNode.getNode("jcr:content/metadata").setProperty("refExpired", false);
			}
		}
		((Session) resolver.adaptTo(Session.class)).save();
	}

	private Collection<Asset> getExpiredRefAssets(ResourceResolver resolver) throws RepositoryException {
		Collection<Hit> hits = QueryUtils.getExpiredRefAssetHits(queryBuilder, resolver);
		List expiredRefAssets = new ArrayList();
		if (hits.size() > 0) {
			for (Hit hit : hits) {
				Resource res = resolver.getResource(hit.getPath());
				Asset asset = null;
				if (null != res) {
					asset = (Asset) res.adaptTo(Asset.class);
					if (null != asset) {
						expiredRefAssets.add(asset);
					}
				}
			}
		}
		return expiredRefAssets;
	}

	protected void deactivate(ComponentContext componentContext) {
		log.debug("Deactivating the expiry notification scheduler");
		this.scheduler.unschedule(jobName);
	}

	/**
	 * ImmediateJob is an inner class that implements Runnable.
	 *
	 * The benefit of making this an inner class is it allows access ot OSGi
	 * services @Reference'd, and consolidates the logic into a single file.
	 */
	public class ImmediateJob implements Runnable {
		private final String path;

		/**
		 * The constructor can be used to pass in serializable state that will
		 * be used during the Job processing.
		 *
		 * @param path
		 *            example parameter passed in from the event
		 */
		public ImmediateJob(String path) {
			// Maintain job state
			this.path = path;
		}

		/**
		 * Run is the entry point for initiating the work to be done by this
		 * job. The Sling job management mechanism will call run() to process
		 * the job.
		 */
		public void run() {
			ResourceResolver assetResolver = null;
			ResourceResolver notificationResolver = null;
			THIS_RUN = Calendar.getInstance();
			try {
				assetResolver = resolverFactory.getServiceResourceResolver(
						Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, EXPIRY_MONITORING_SERVICE));
				long priorNotificationInMillis = priorNotificationInSeconds * 1000L;
				long thisRunTime = THIS_RUN.getTimeInMillis();
				long lastRunTime = LAST_RUN.getTimeInMillis();
				// Access data passed into the Job from the Event
				Resource resource = assetResolver.getResource(path);
				if (resource != null) {
					ValueMap properties = resource.getValueMap();
					// Do some work w this resource..
				}
				Collection expiredAssets = getExpiredAssetHits(assetResolver);
				Collection toExpireAssets = QueryUtils.getToexpireAssetHits(queryBuilder, assetResolver, thisRunTime,
						lastRunTime, priorNotificationInMillis);
				Collection expireRefererAssets = propagateExpiry(assetResolver, expiredAssets);
				dePropagateExpiry(assetResolver);
				UserPropertiesManager upm = userPropertiesService
						.createUserPropertiesManager((Session) assetResolver.adaptTo(Session.class), assetResolver);
				notificationResolver = resolverFactory.getServiceResourceResolver(
						Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, NOTIFICATION_SERVICE));
				Notification notification = new Notification(notificationResolver, assetResolver, expireRefererAssets,
						expiredAssets, toExpireAssets, upm);
				notification.sendPriorInboxNotification(groupe_entite);
				notification.sendExpiryInBoxNotification(groupe_entite);
				notification.sendRefExpiryInBoxNotification(groupe_entite);
			} catch (Exception var16) {
				log.error("Error in execute.", var16);
			} finally {
				// Always close resource resolvers you open
				if (notificationResolver != null) {
					notificationResolver.close();
				}
				if (assetResolver != null) {
					assetResolver.close();
				}
			}
			LAST_RUN = THIS_RUN;
		}
	}
}
