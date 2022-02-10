/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.quicksearchbox;

import android.database.DatabaseUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * UNISOC: Modify for bug1002428
 * Listens for broadcasts that locale changed.
 */
public class LocaleChangedReceiver extends BroadcastReceiver {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.LocaleChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            if (DBG) Log.d(TAG, "onReceive(" + intent + ")");
            DatabaseUtils.resetCollator();
        }
    }
}
