package com.cats.notifications;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;

import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.security.user.UserPropertiesService;
import com.day.cq.commons.Externalizer;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;

/**
 * Created by ETP7307 on 24/05/2017.
 */

/*** Configuration OSGI */

@Component(metatype = true, label = "Credit Agricole's AEM DAM Expiry Notification Job", description = "Cats AEM DAM Expiry Notification")
public class CatsExpiryNotification {
	/*
	 * private static final Logger log =
	 * LoggerFactory.getLogger(CatsExpiryNotification.class);
	 */

	@Reference
	private Scheduler scheduler;

	/*
	 * The ResourceResolverFactory defines the service API to get and create
	 * ResourceResolvers.
	 */

	@Reference
	private ResourceResolverFactory resolverFactory;
	@Reference
	private QueryBuilder queryBuilder;
	@Reference
	Externalizer externalizerService = null;
	@Reference
	UserPropertiesService userPropertiesService;
	@Reference
	private Replicator replicator;

	/*
	 * Propriétés de la configuration OSGI
	 * 
	 */
	@Property(boolValue = {
			true }, label = "Activation des notifications ", description = "Activate or desactivate the scheduler")
	public static final String SCHEDULER_ACTIVED = "cats.dam.expiry.notification.scheduler.activation";
	@Property(value = {
			"/content/dam/assetsca/" }, name = "path_dam", label = "Chemin de l'entité", description = "Noeud sur lequel on veut paramétrer les notifications")
	public static final String PATH_DAM = "path_dam";
	@Property(value = { "master" }, name = "groupe_entite", label = "Groupe Gestionnaire de l'entité")
	public static final String GROUPE_ENTITE = "groupe_entite";

	/*
	 * Scheduler périodique : période de délenchement du scheduler après
	 * activation
	 * 
	 */

	@Property(longValue = {
			30L }, name = "prior_notification_seconds", label = "Durée", description = "Période avant l'expiration de l'asset, durant laquelle des notification sont envoyées")
	public static final String PRIOR_NOTIFICATION_SECONDS = "prior_notification_seconds";

	@Property(longValue = {
			1000L }, label = "Fréquence", description = "Fréquence de notification pendant cette période")
	public static final String SCHEDULER_PERIOD = "cats.dam.expiry.notification.scheduler.period.rule";

	/* Notification envoyée X secondes avant l'expiration des assets */

	/*
	 * private static final String MESSAGE_NOTIFICATION_EXPIRATION =
	 * "Les Assets suivants sont expirés"; private static final String
	 * TITRE_NOTIFICATION_EXPIRATION = "Assets expirés";
	 */
	private static final String MESSAGE_NOTIFICATION_PRE_EXPIRATION = "Les Assets suivants vont expirer";
	private static final String TITRE_NOTIFICATION_PRE_EXPIRATION = "Assets sur le point d'expirer";

	private static final String EXPIRY_MONITORING_SERVICE = "expirymonitoringhelper";
	private static final String NOTIFICATION_SERVICE = "notificationhelper";

	private static Calendar THIS_RUN;

	private long priorNotificationInSeconds;
	private String pathDam;
	private String groupeEntite;
	private String jobName;

	public CatsExpiryNotification() {
	}

	/* Activation du Scheduler */
	/*
	 * On va utiliser un scheduler deux méthodes pour déclencher un scheduler
	 * 
	 * schedule(Object paramObject, ScheduleOptions paramScheduleOptions);
	 * planifie un job basé sur les options
	 * 
	 * unschedule(String paramString); Supprime un job avec son nom
	 * 
	 * NOW(int times, long period) retourne une objet de type scheduleOptions
	 * créer une option schedule pour faire marcher le scheduler plus qu'une
	 * fois et periodiquement
	 * 
	 * EXPR(String expression) créer une option scheduler pour faire marcher le
	 * scheduler à partir d'une expression
	 * 
	 */
	protected void activate(ComponentContext componentContext) throws Exception {

		this.pathDam = (String) componentContext.getProperties().get(PATH_DAM);
		/* log.debug("path : " + pathDam); */
		jobName = this.getClass().getSimpleName().toString() + "/" + pathDam.replace("/", "-");
		/* log.debug("jobName : " + jobName); */

		/*
		 * création du job avec dans la méthode run tout le travail qu'il va
		 * effectuer
		 * 
		 * 
		 */
		ImmediateJob job = new ImmediateJob();

		ScheduleOptions options = null; /*
										 * options est un objet de type
										 * scheduleOptions
										 */

		/*
		 * Schheduler basé sur la période on récupère la période qu'on stocke
		 * dans periodinsecs et on assigne à options l'option scheduler pour
		 * faire marcher le scheduler avec la période fixée -1 pour sans arrêt
		 * NOW(int times, long period) times - The number of times this job
		 * should be started
		 */

		long periodinsecs = ((Long) componentContext.getProperties().get(SCHEDULER_PERIOD)).longValue();
		options = scheduler.NOW(-1, periodinsecs);

		this.priorNotificationInSeconds = ((Long) componentContext.getProperties().get(PRIOR_NOTIFICATION_SECONDS))
				.longValue();
		this.groupeEntite = (String) componentContext.getProperties().get(GROUPE_ENTITE);

		/*
		 * jobname and canRunConcurrently - Whether this job can run even if
		 * previous scheduled runs are still running.
		 */
		options.name(jobName);
		options.canRunConcurrently(false);
		/* log.debug("options : " + options); */

		/*
		 * schedule(Object paramObject, ScheduleOptions paramScheduleOptions);
		 * planifie un job basé sur les options
		 */
		boolean schedulerActivated = ((Boolean) componentContext.getProperties().get(SCHEDULER_ACTIVED)).booleanValue();

		/* On active le scheduler si activé */
		if (schedulerActivated) {
			scheduler.schedule(job, options);
		}
		/* Si désactivé alors déplanification du job portant le nom jobname */

		else {
			scheduler.unschedule(jobName);
		}

	}

	protected void deactivate(ComponentContext componentContext) {
		/* log.debug("Deactivating the expiry notification scheduler"); */
		/* unschedule(String paramString); Supprime un job avec son nom */

		this.scheduler.unschedule(jobName);
	}

	/**
	 * ImmediateJob is an inner class that implements Runnable.
	 *
	 * The benefit of making this an inner class is it allows access ot OSGi
	 * services @Reference'd, and consolidates the logic into a single file.
	 */
	public class ImmediateJob implements Runnable {

		public ImmediateJob() {
			//
		}

		/**
		 * Run is the entry point for initiating the work to be done by this
		 * job. The Sling job management mechanism will call run() to process
		 * the job.
		 */

		public void run() {
			/*
			 * Travail effectué par le job
			 * 
			 * The ResourceResolver defines the service API which is used to
			 * resolve Resource objects. A resource resolver can also be created
			 * through the ResourceResolverFactory.
			 */

			ResourceResolver assetResolver = null;
			ResourceResolver notificationResolver = null;
			/*
			 * récupération de la date pour marquer l'instant 0 de l'éxécution
			 * du job
			 */

			THIS_RUN = Calendar.getInstance();

			try {
				/*
				 * The ResourceResolverFactory defines the service API to get
				 * and create ResourceResolvers.
				 */
				/*
				 * getServiceResourceResolver(Map<String,Object>
				 * authenticationInfo) Returns a new ResourceResolver instance
				 * with privileges assigned to the service provided by the
				 * calling bundle.
				 */
				assetResolver = resolverFactory.getServiceResourceResolver(
						Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, EXPIRY_MONITORING_SERVICE));

				long priorNotificationInMillis = priorNotificationInSeconds * 1000L;
				long thisRunTime = THIS_RUN.getTimeInMillis();

				/* Récupération de la collection des assets qui vont expirer */
				Collection<Hit> toExpireAssets = QueryUtils.getToexpireAssetHits(queryBuilder, assetResolver,
						thisRunTime, priorNotificationInMillis, pathDam);

				UserPropertiesManager upm = userPropertiesService
						.createUserPropertiesManager((Session) assetResolver.adaptTo(Session.class), assetResolver);

				/*
				 * The ResourceResolverFactory defines the service API to get
				 * and create ResourceResolvers.
				 */
				/*
				 * getServiceResourceResolver(Map<String,Object>
				 * authenticationInfo) Returns a new ResourceResolver instance
				 * with privileges assigned to the service provided by the
				 * calling bundle.
				 */
				notificationResolver = resolverFactory.getServiceResourceResolver(
						Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, NOTIFICATION_SERVICE));

				/* Création de la notification */

				Notification notification = new Notification(notificationResolver, assetResolver, toExpireAssets, upm);

				Map<String, Set<String>> toExpireAssetsWithUser = notification.toExpireAssetsWithUser;

				/*
				 * Envoi d'un rappel d'expiration prochaine d'une asset
				 */

				notification.sendNotification(groupeEntite, toExpireAssetsWithUser, MESSAGE_NOTIFICATION_PRE_EXPIRATION,
						TITRE_NOTIFICATION_PRE_EXPIRATION, pathDam, notificationResolver);

			} catch (Exception var16) {
				/* log.error("Error in execute.", var16); */
			} finally {
				// Always close resource resolvers you open
				if (notificationResolver != null) {
					notificationResolver.close();
				}
				if (assetResolver != null) {
					assetResolver.close();
				}
			}
		}
	}

}