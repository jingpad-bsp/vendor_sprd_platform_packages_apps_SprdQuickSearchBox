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
package com.android.quicksearchbox.preferences;

import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.util.Util;
import com.sprd.activities.AppCompatPreferenceActivity;
import com.sprd.utils.Utils;

import android.preference.PreferenceActivity;
import android.support.v4.os.BuildCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
/**
 * Activity for setting global search preferences.
 */
public class SearchSettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "QSB.SearchSettingsActivity";
    private static final boolean DBG = false;

    private static final String CLEAR_SHORTCUTS_FRAGMENT = DeviceSearchFragment.class.getName();

    private static final String ACTIVITY_HELP_CONTEXT = "settings";

    /* SPRD: modify for bug 658838 @{ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.search_bg));
        }
    }
    /* @} */

//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);

//        final Window window = getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
//                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
//        window.setStatusBarColor(R.color.statusbar_bg_color);
//    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
        onHeadersBuilt(target);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getQsbApplication().getHelp().addHelpMenuItem(menu, ACTIVITY_HELP_CONTEXT, true);
        return true;
    }

    protected QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    /**
     * Get the name of the fragment that contains only a 'clear shortcuts' preference, and hence
     * can be removed if zero-query shortcuts are disabled. Returns null if no such fragment exists.
     */
    protected String getShortcutsOnlyFragment() {
        return null;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    protected void onHeadersBuilt(List<Header> target) {
        String shortcutsFragment = getShortcutsOnlyFragment();
        if (shortcutsFragment == null) return;
        if (DBG) Log.d(TAG, "onHeadersBuilt shortcutsFragment=" + shortcutsFragment);
        if (!QsbApplication.get(this).getConfig().showShortcutsForZeroQuery()) {
            // remove 'clear shortcuts'
            for (int i = 0; i < target.size(); ++i) {
                String fragment = target.get(i).fragment;
                if (DBG) Log.d(TAG, "fragment " + i + ": " + fragment);
                if (shortcutsFragment.equals(fragment)) {
                    target.remove(i);
                    break;
                }
            }
        }

        // SPRD: 492772 In Guest mode remove 'Default Search Engine'
        removeDefaultSearchEngineIfInGuest(target);
    }

    /* SPRD: 492772 In Guest mode remove 'Default Search Engine' @{ */
    protected void removeDefaultSearchEngineIfInGuest(List<Header> target) {
        if ( UserHandle.myUserId() == UserHandle.USER_OWNER || target == null || target.isEmpty()) return;

        for ( int i = 0; i < target.size(); i++ ) {
            String fragement = target.get(i).fragment;
            if (SearchEngineFragment.class.getName().equals(fragement)) {
                target.remove(i);
                break;
            }
        }
    }
    /* @} */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item); // UNISOC: Modify for bug1217930
    }

}
