/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.notificationdebug;

import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coletor de notificações que organiza notificações por aplicativo.
 */
public class NotificationCollector {
    private static final String TAG = "NotificationCollector";
    private static final boolean DEBUG = true;
    
    private final Map<String, AppNotificationData> mNotificationsByApp = new HashMap<>();

    /**
     * Adiciona uma notificação ao coletor.
     */
    public void addNotification(StatusBarNotification notification) {
        if (notification == null) {
            return;
        }
        
        String packageName = notification.getPackageName();
        UserHandle userHandle = notification.getUser();
        String key = packageName + "|" + userHandle.getIdentifier();
        
        AppNotificationData appData = mNotificationsByApp.get(key);
        if (appData == null) {
            appData = new AppNotificationData(packageName, userHandle);
            mNotificationsByApp.put(key, appData);
        }
        
        appData.addNotification(notification);
        
        if (DEBUG) {
            Log.d(TAG, "Added notification: " + notification.getKey() 
                    + " from " + packageName + " (total: " + appData.getNotificationCount() + ")");
        }
    }

    /**
     * Remove uma notificação do coletor.
     */
    public void removeNotification(StatusBarNotification notification) {
        if (notification == null) {
            return;
        }
        
        String packageName = notification.getPackageName();
        UserHandle userHandle = notification.getUser();
        String key = packageName + "|" + userHandle.getIdentifier();
        
        AppNotificationData appData = mNotificationsByApp.get(key);
        if (appData != null) {
            appData.removeNotification(notification.getKey());
            
            if (DEBUG) {
                Log.d(TAG, "Removed notification: " + notification.getKey() 
                        + " from " + packageName + " (remaining: " + appData.getNotificationCount() + ")");
            }
            
            // Remove o app se não houver mais notificações
            if (appData.getNotificationCount() == 0) {
                mNotificationsByApp.remove(key);
            }
        }
    }

    /**
     * Retorna todas as notificações organizadas por app.
     */
    public List<AppNotificationData> getAllAppNotifications() {
        return new ArrayList<>(mNotificationsByApp.values());
    }

    /**
     * Retorna o número total de notificações.
     */
    public int getTotalNotificationCount() {
        return mNotificationsByApp.values().stream()
                .mapToInt(AppNotificationData::getNotificationCount)
                .sum();
    }

    /**
     * Retorna o número de apps com notificações.
     */
    public int getAppCount() {
        return mNotificationsByApp.size();
    }

    /**
     * Limpa todas as notificações.
     */
    public void clear() {
        mNotificationsByApp.clear();
        if (DEBUG) {
            Log.d(TAG, "Cleared all notifications");
        }
    }

    /**
     * Gera um resumo das notificações atuais.
     */
    public NotificationSummaryRequest createSummaryRequest() {
        return new NotificationSummaryRequest(getAllAppNotifications());
    }
}

