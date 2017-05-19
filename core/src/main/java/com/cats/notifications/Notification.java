package com.cats.notifications;


/**
 * Created by ETP7307 on 15/05/2017.
 */
import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.taskmanagement.*;
import com.day.cq.dam.api.Asset;
import com.day.cq.search.result.Hit;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.RepositoryException;
import java.util.*;
/**
 * Created by ETP7307 on 15/05/2017.
 */
public class Notification {
    ResourceResolver assetResolver;
    ResourceResolver notificationResolver;
    Map<String, Set<String>> expiredAssetsWithUser;
    Map<String, Set<String>> toExpireAssetsWithUser;
    Map<String, Set<String>> expiredRefAssetsWithUser;
    UserPropertiesManager upm;
    public Notification(ResourceResolver notificationResolver, ResourceResolver assetResolver,
                        Collection<Hit> expiredRefAssets, Collection<Hit> expiredAssets, Collection<Asset> toExpireAssets,
                        UserPropertiesManager upm) throws RepositoryException {
        this.assetResolver = assetResolver;
        this.notificationResolver = notificationResolver;
        this.upm = upm;
        this.expiredAssetsWithUser = new HashMap();
        this.expiredRefAssetsWithUser = new HashMap();
        this.toExpireAssetsWithUser = new HashMap();
        Iterator iter = expiredAssets.iterator();
        Hit each;
        Resource res;
        Asset asset;
        String createdBy;
        HashSet set;
        while (iter.hasNext()) {
            each = (Hit) iter.next();
            res = assetResolver.getResource(each.getPath());
            asset = null;
            if (null != res) {
                asset = (Asset) res.adaptTo(Asset.class);
                if (null != asset) {
                    createdBy = (String) ((ValueMap) res.adaptTo(ValueMap.class)).get("jcr:createdBy", String.class);
                    if (this.expiredAssetsWithUser.containsKey(createdBy)) {
                        ((Set) this.expiredAssetsWithUser.get(createdBy)).add(each.getPath());
                    } else {
                        set = new HashSet();
                        set.add(each.getPath());
                        this.expiredAssetsWithUser.put(createdBy, set);
                    }
                }
            }
        }
        iter = toExpireAssets.iterator();
        while (iter.hasNext()) {
            each = (Hit) iter.next();
            res = assetResolver.getResource(each.getPath());
            asset = null;
            if (null != res) {
                asset = (Asset) res.adaptTo(Asset.class);
                if (null != asset) {
                    createdBy = (String) ((ValueMap) res.adaptTo(ValueMap.class)).get("jcr:createdBy", String.class);
                    if (this.toExpireAssetsWithUser.containsKey(createdBy)) {
                        ((Set) this.toExpireAssetsWithUser.get(createdBy)).add(each.getPath());
                    } else {
                        set = new HashSet();
                        set.add(each.getPath());
                        this.toExpireAssetsWithUser.put(createdBy, set);
                    }
                }
            }
        }
        iter = expiredRefAssets.iterator();
        while (iter.hasNext()) {
            Asset each1 = (Asset) iter.next();
            res = assetResolver.getResource(each1.getPath());
            asset = null;
            if (null != res) {
                asset = (Asset) res.adaptTo(Asset.class);
                if (null != asset) {
                    createdBy = (String) ((ValueMap) res.adaptTo(ValueMap.class)).get("jcr:createdBy", String.class);
                    if (this.expiredRefAssetsWithUser.containsKey(createdBy)) {
                        ((Set) this.expiredRefAssetsWithUser.get(createdBy)).add(each1.getPath());
                    } else {
                        set = new HashSet();
                        set.add(each1.getPath());
                        this.expiredRefAssetsWithUser.put(createdBy, set);
                    }
                }
            }
        }
    }
    public void sendPriorInboxNotification(String groupeEntite) throws TaskManagerException {
        if (null != this.toExpireAssetsWithUser && !this.toExpireAssetsWithUser.isEmpty()) {
            Iterator entries = this.toExpireAssetsWithUser.entrySet().iterator();
            TaskManager tm = (TaskManager) this.notificationResolver.adaptTo(TaskManager.class);
            TaskManagerFactory tmf = tm.getTaskManagerFactory();
            StringBuffer description = new StringBuffer("Les Assets suivants sont sur le point d'expirer:\n");
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Set asstePaths = (Set) entry.getValue();
                Iterator newTask = asstePaths.iterator();
                while (newTask.hasNext()) {
                    String newTaskAction = (String) newTask.next();
                    description.append("\n" + newTaskAction);
                }
                Task newTask1 = tmf.newTask("Notification");
                newTask1.setName("Assets sur le point d'expirer");
                TaskAction newTaskAction1 = tmf.newTaskAction("Remove");
                newTask1.setActions(Collections.singletonList(newTaskAction1));
                newTask1.setContentPath("/content/dam");
                newTask1.setCurrentAssignee(groupeEntite);
                newTask1.setDescription(description.toString());
                tm.createTask(newTask1);
            }
        }
    }
    public void sendExpiryInBoxNotification(String groupeEntite) throws TaskManagerException {
        if (null != this.expiredAssetsWithUser && !this.expiredAssetsWithUser.isEmpty()) {
            Iterator entries = this.expiredAssetsWithUser.entrySet().iterator();
            TaskManager tm = (TaskManager) this.notificationResolver.adaptTo(TaskManager.class);
            TaskManagerFactory tmf = tm.getTaskManagerFactory();
            StringBuffer description = new StringBuffer("Les Assets suivants sont expirés:\n");
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Set asstePaths = (Set) entry.getValue();
                Iterator newTask = asstePaths.iterator();
                while (newTask.hasNext()) {
                    String newTaskAction = (String) newTask.next();
                    description.append("\n" + newTaskAction);
                }
                Task newTask1 = tmf.newTask("Notification");
                newTask1.setName("Assets expirés");
                TaskAction newTaskAction1 = tmf.newTaskAction("Remove");
                newTask1.setActions(Collections.singletonList(newTaskAction1));
                newTask1.setContentPath("/content/dam");
                newTask1.setCurrentAssignee(groupeEntite);
                newTask1.setDescription(description.toString());
                tm.createTask(newTask1);
            }
        }
    }
    public void sendRefExpiryInBoxNotification(String groupeEntite) throws TaskManagerException {
        if (null != this.expiredRefAssetsWithUser && !this.expiredRefAssetsWithUser.isEmpty()) {
            Iterator entries = this.expiredRefAssetsWithUser.entrySet().iterator();
            TaskManager tm = (TaskManager) this.notificationResolver.adaptTo(TaskManager.class);
            TaskManagerFactory tmf = tm.getTaskManagerFactory();
            StringBuffer description = new StringBuffer("Les Assets suivants ont des sous-assets expirés:\n");
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Set asstePaths = (Set) entry.getValue();
                Iterator newTask = asstePaths.iterator();
                while (newTask.hasNext()) {
                    String newTaskAction = (String) newTask.next();
                    description.append("\n" + newTaskAction);
                }
                Task newTask1 = tmf.newTask("Notification");
                newTask1.setName("SOUS ASSETS EXPIRES");
                TaskAction newTaskAction1 = tmf.newTaskAction("Remove");
                newTask1.setActions(Collections.singletonList(newTaskAction1));
                newTask1.setContentPath("/content/dam");
                newTask1.setCurrentAssignee((String) entry.getKey());
                newTask1.setDescription(groupeEntite);
                tm.createTask(newTask1);
            }
        }
    }
}

