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

/**
 * Resposta do serviço de resumo de notificações.
 */
public class NotificationSummaryResponse {
    private final String mSummaryText;
    private final String mTitle;
    private final int mTotalNotifications;
    private final int mTotalApps;

    public NotificationSummaryResponse(String title, String summaryText, 
                                       int totalNotifications, int totalApps) {
        mTitle = title;
        mSummaryText = summaryText;
        mTotalNotifications = totalNotifications;
        mTotalApps = totalApps;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSummaryText() {
        return mSummaryText;
    }

    public int getTotalNotifications() {
        return mTotalNotifications;
    }

    public int getTotalApps() {
        return mTotalApps;
    }
}

