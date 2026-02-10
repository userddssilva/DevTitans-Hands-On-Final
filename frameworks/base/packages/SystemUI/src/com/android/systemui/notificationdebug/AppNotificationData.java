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

import java.util.ArrayList;
import java.util.List;

/**
 * Estrutura para armazenar todas as notificações de um aplicativo específico.
 */
public class AppNotificationData {
    private final String mPackageName;
    private final UserHandle mUserHandle;
    private final List<StatusBarNotification> mNotifications;

    public AppNotificationData(String packageName, UserHandle userHandle) {
        mPackageName = packageName;
        mUserHandle = userHandle;
        mNotifications = new ArrayList<>();
    }

    public String getPackageName() {
        return mPackageName;
    }

    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    public List<StatusBarNotification> getNotifications() {
        return mNotifications;
    }

    public void addNotification(StatusBarNotification notification) {
        mNotifications.add(notification);
    }

    public void removeNotification(String key) {
        mNotifications.removeIf(n -> n.getKey().equals(key));
    }

    public int getNotificationCount() {
        return mNotifications.size();
    }

    public String getKey() {
        return mPackageName + "|" + mUserHandle.getIdentifier();
    }

    @Override
    public String toString() {
        return "AppNotificationData{" +
                "packageName='" + mPackageName + '\'' +
                ", userId=" + mUserHandle.getIdentifier() +
                ", notificationCount=" + mNotifications.size() +
                '}';
    }
}

