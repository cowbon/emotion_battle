package com.msp.emotionbattle.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by tomwang on 2017/2/28.
 */

public class StorageHelper {
    public static String getPersonGroupId(Context context) {
        SharedPreferences personGroupId = context.getSharedPreferences("PersonGroupId", Context.MODE_PRIVATE);
        //Return null object if preference does not exist
        return personGroupId.getString("GroupID", "");
    }

    public static void setPersonGroupID(String personGroupIdToAdd, Context context) {
        SharedPreferences personGroupId = context.getSharedPreferences("PersonGroupId", Context.MODE_PRIVATE);
        SharedPreferences.Editor personGroupIdEditor = personGroupId.edit();
        personGroupIdEditor.putString("GroupID", personGroupIdToAdd);
        personGroupIdEditor.commit();
        Log.d("Storage", personGroupId.getString("GroupID", ""));
    }

    public static void deletePersonGroups(String personGroupIdToDelete, Context context) {
        SharedPreferences personGroupId = context.getSharedPreferences("PersonGroupId", Context.MODE_PRIVATE);
        SharedPreferences.Editor personGroupIdEditor = personGroupId.edit();
        personGroupIdEditor.clear();
        personGroupIdEditor.commit();
    }

    public static Set<String> getAllPersonIds(String personGroupId, Context context) {
        SharedPreferences personIdSet = context.getSharedPreferences(personGroupId + "PersonIdSet", Context.MODE_PRIVATE);
        return personIdSet.getStringSet("PersonIdSet", new HashSet<String>());
    }

    public static String getPersonName(String personId, String personGroupId, Context context) {
        SharedPreferences personIdNameMap = context.getSharedPreferences(personGroupId + "PersonIdNameMap", Context.MODE_PRIVATE);
        return personIdNameMap.getString(personId, "");
    }

    public static void setPersonName(String personIdToAdd, String personName, String personGroupId, Context context) {
        SharedPreferences personIdNameMap = context.getSharedPreferences(personGroupId + "PersonIdNameMap", Context.MODE_PRIVATE);

        SharedPreferences.Editor personIdNameMapEditor = personIdNameMap.edit();
        personIdNameMapEditor.putString(personIdToAdd, personName);
        personIdNameMapEditor.commit();

        Set<String> personIds = getAllPersonIds(personGroupId, context);
        Set<String> newPersonIds = new HashSet<>();
        for (String personId: personIds) {
            newPersonIds.add(personId);
        }
        newPersonIds.add(personIdToAdd);
        SharedPreferences personIdSet = context.getSharedPreferences(personGroupId + "PersonIdSet", Context.MODE_PRIVATE);
        SharedPreferences.Editor personIdSetEditor = personIdSet.edit();
        personIdSetEditor.putStringSet("PersonIdSet", newPersonIds);
        personIdSetEditor.commit();
    }

    public static void deletePersons(List<String> personIdsToDelete, String personGroupId, Context context) {
        SharedPreferences personIdNameMap = context.getSharedPreferences(personGroupId + "PersonIdNameMap", Context.MODE_PRIVATE);
        SharedPreferences.Editor personIdNameMapEditor = personIdNameMap.edit();
        for (String personId: personIdsToDelete) {
            personIdNameMapEditor.remove(personId);
        }
        personIdNameMapEditor.commit();

        Set<String> personIds = getAllPersonIds(personGroupId, context);
        Set<String> newPersonIds = new HashSet<>();
        for (String personId: personIds) {
            if (!personIdsToDelete.contains(personId)) {
                newPersonIds.add(personId);
            }
        }
        SharedPreferences personIdSet = context.getSharedPreferences(personGroupId + "PersonIdSet", Context.MODE_PRIVATE);
        SharedPreferences.Editor personIdSetEditor = personIdSet.edit();
        personIdSetEditor.putStringSet("PersonIdSet", newPersonIds);
        personIdSetEditor.commit();
    }

    public static Set<String> getAllFaceIds(String personId, Context context) {
        SharedPreferences faceIdSet = context.getSharedPreferences(personId + "FaceIdSet", Context.MODE_PRIVATE);
        return faceIdSet.getStringSet("FaceIdSet", new HashSet<String>());
    }

    public static String getFaceUri(String faceId, Context context) {
        SharedPreferences faceIdUriMap = context.getSharedPreferences("FaceIdUriMap", Context.MODE_PRIVATE);
        return faceIdUriMap.getString(faceId, "");
    }

    public static void setFaceUri(String faceIdToAdd, String faceUri, String personId, Context context) {
        SharedPreferences faceIdUriMap = context.getSharedPreferences("FaceIdUriMap", Context.MODE_PRIVATE);

        SharedPreferences.Editor faceIdUriMapEditor = faceIdUriMap.edit();
        faceIdUriMapEditor.putString(faceIdToAdd, faceUri);
        faceIdUriMapEditor.commit();

        Set<String> faceIds = getAllFaceIds(personId, context);
        Set<String> newFaceIds = new HashSet<>();
        for (String faceId: faceIds) {
            newFaceIds.add(faceId);
        }
        newFaceIds.add(faceIdToAdd);
        SharedPreferences faceIdSet = context.getSharedPreferences(personId + "FaceIdSet", Context.MODE_PRIVATE);
        SharedPreferences.Editor faceIdSetEditor = faceIdSet.edit();
        faceIdSetEditor.putStringSet("FaceIdSet", newFaceIds);
        faceIdSetEditor.commit();
    }

    public static void deleteFaces(List<String> faceIdsToDelete, String personId, Context context) {
        Set<String> faceIds = getAllFaceIds(personId, context);
        Set<String> newFaceIds = new HashSet<>();
        for (String faceId: faceIds) {
            if (!faceIdsToDelete.contains(faceId)) {
                newFaceIds.add(faceId);
            }
        }
        SharedPreferences faceIdSet = context.getSharedPreferences(personId + "FaceIdSet", Context.MODE_PRIVATE);
        SharedPreferences.Editor faceIdSetEditor = faceIdSet.edit();
        faceIdSetEditor.putStringSet("FaceIdSet", newFaceIds);
        faceIdSetEditor.commit();
    }
}
