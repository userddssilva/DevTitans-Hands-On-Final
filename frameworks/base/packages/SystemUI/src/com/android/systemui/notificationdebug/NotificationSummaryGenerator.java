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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Gerador de notificação resumida baseado na resposta do serviço.
 */
public class NotificationSummaryGenerator {
    private static final String TAG = "NotificationSummaryGen";
    private static final boolean DEBUG = true;
    private static final String CHANNEL_ID = "notification_summary_channel";
    private static final String CHANNEL_NAME = "Resumo de Notificações";
    private static final int SUMMARY_NOTIFICATION_ID = 99999;

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public NotificationSummaryGenerator(Context context, NotificationManager notificationManager) {
        mContext = context;
        mNotificationManager = notificationManager;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Canal para exibir resumos de notificações");
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Gera e exibe uma notificação resumida baseada na resposta do serviço.
     */
    public void generateSummaryNotification(NotificationSummaryResponse response) {
        if (DEBUG) {
            Log.d(TAG, "Generating summary notification: " + response.getSummaryText());
        }

        Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(response.getTitle())
                .setContentText(response.getSummaryText())
                .setStyle(new Notification.BigTextStyle()
                        .bigText(response.getSummaryText()))
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        Notification notification = builder.build();
        mNotificationManager.notify(SUMMARY_NOTIFICATION_ID, notification);

        if (DEBUG) {
            Log.d(TAG, "Summary notification posted: " + response.getTitle());
        }
    }
}

