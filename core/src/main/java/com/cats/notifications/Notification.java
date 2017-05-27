package com.cats.notifications;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

/**
 * Created by ETP7307 on 15/05/2017.
 */
import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskAction;
import com.adobe.granite.taskmanagement.TaskManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.taskmanagement.TaskManagerFactory;
import com.day.cq.dam.api.Asset;
import com.day.cq.search.result.Hit;

/**
 * Created by ETP7307 on 24/05/2017.
 */
public class Notification {
	ResourceResolver assetResolver;
	ResourceResolver notificationResolver;
	Map<String, Set<String>> toExpireAssetsWithUser;
	UserPropertiesManager upm;

	public static final String TASK_TYPE = "Notification";
	public static final String TASK_ACTION = "Remove";
	public static final String JCR_CREATEDBY = "jcr:createdBy";

	public Notification(ResourceResolver notificationResolver, ResourceResolver assetResolver,
			Collection<Hit> toExpireAssets, UserPropertiesManager upm) throws RepositoryException {

		this.assetResolver = assetResolver;
		this.notificationResolver = notificationResolver;
		this.upm = upm;
		this.toExpireAssetsWithUser = processExpiredHitAssets(toExpireAssets);

	}

	/*
	 * Assets expirés ou qui vont expirer passés en paramètre Retourne une Map
	 * Clé (createdBy) ---> Valeur (Collection de chemin des assets expirés ou
	 * qui vont expirer)
	 */
	private Map<String, Set<String>> processExpiredHitAssets(Collection<Hit> assets) throws RepositoryException {
		Map<String, Set<String>> result = new HashMap<>();
		for (Hit each : assets) {
			processExpiredAsset(each.getPath(), result);
		}
		return result;
	}

	/*
	 * Map Clé (createdBy) ---> Valeur (Collection de chemin des assets expirés
	 * ou qui vont expirer)
	 */
	private void processExpiredAsset(String assetPath, Map<String, Set<String>> result) {

		Resource res = assetResolver.getResource(assetPath);
		if (null != res) {
			Asset asset = (Asset) res.adaptTo(Asset.class);
			if (null != asset) {
				String createdBy = (String) ((ValueMap) res.adaptTo(ValueMap.class)).get(JCR_CREATEDBY, String.class);
				Set<String> set = result.get(createdBy);

				if (result.containsKey(createdBy)) {
					set.add(assetPath);
				} else {
					set = new HashSet<>();
					set.add(assetPath);
					result.put(createdBy, set);
				}
			}
		}
	}

	/*
	 * Envoi des notification d'expiration ou de pré expiration
	 */
	public void sendNotification(String groupeEntite, Map<String, Set<String>> result, String notificationMessage,
			String taskName, String pathDam, ResourceResolver notificationResolver) throws TaskManagerException {
		/*
		 * Si le toExpireAssetWithUser Map de createdby avec un set de paths
		 * contient des éléments qui vont expirer On itere sur le set de path et
		 * on ajoute le path dans la description de la notif construction de
		 * l'alerte
		 * 
		 */
		/*
		 * pour les assets /content/dam/assetsca/master -> envoi notif au groupe
		 * concerné pour les assets /content/dam/assetsca/crxxxx -> envoi notif
		 * au groupe concerné pour les assets /content/dam/assetsca/national ->
		 * envoi notif au groupe concerné
		 */
		if (null != result && !result.isEmpty()) {
			Iterator entries = result.entrySet().iterator();
			TaskManager tm = (TaskManager) notificationResolver.adaptTo(TaskManager.class);
			TaskManagerFactory tmf = tm.getTaskManagerFactory();
			StringBuffer description = new StringBuffer(notificationMessage);
			while (entries.hasNext()) {
				Map.Entry entry = (Map.Entry) entries.next();
				Set asstePaths = (Set) entry.getValue();
				Iterator newTask = asstePaths.iterator();
				while (newTask.hasNext()) {
					String newTaskAction = (String) newTask.next();
					description
							.append("\n" + newTaskAction); /* path toexpire */
				}
				Task newTask1 = tmf.newTask(TASK_TYPE);
				newTask1.setName(taskName);
				TaskAction newTaskAction1 = tmf.newTaskAction(TASK_ACTION);
				newTask1.setActions(Collections.singletonList(newTaskAction1));
				newTask1.setContentPath(pathDam);
				newTask1.setCurrentAssignee(groupeEntite);
				newTask1.setDescription(description.toString());
				tm.createTask(newTask1);

			}
		}
	}

}