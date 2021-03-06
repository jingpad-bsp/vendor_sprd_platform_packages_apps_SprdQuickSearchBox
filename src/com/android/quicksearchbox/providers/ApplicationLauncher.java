/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox.providers;

import android.view.ViewGroup;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;

import com.android.quicksearchbox.providers.Applications;
import com.android.quicksearchbox.util.Util;

import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.quicksearchbox.R;

/**
 * This class is purely here to get launch intents.
 */
public class ApplicationLauncher extends ListActivity {

    private static final String TAG = "ApplicationLauncher";

    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* SPRD: modify for bug 658838 @{ */
        if (Util.checkIntent(getIntent())) {
            finish();
            return;
        }
        /* @} */

        /* SPRD: add 20130904 Spreadst of 209330 add reminder when no searchResult @{ */
        getListView().setBackgroundColor(Color.WHITE);
        getListView().setDivider(getApplicationContext().getResources().getDrawable(R.color.gray));
        getListView().setDividerHeight(1);

        TextView emptyView = new TextView(this);
        ((ViewGroup) getListView().getParent()).addView(emptyView);
        emptyView.setText(R.string.empty_suggestion);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextColor(Color.BLACK);
        emptyView.setBackgroundColor(Color.WHITE);
        getListView().setEmptyView(emptyView);

        /* @} */
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_MAIN.equals(action)) {
            Uri contentUri = intent.getData();
            launchApplication(contentUri);
            finish();
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            showSearchResults(query);
        }
    }

    private void showSearchResults(String query) {
        setTitle(query);
        mCursor = Applications.search(getContentResolver(), query);
        startManagingCursor(mCursor);

        ApplicationsAdapter adapter = new ApplicationsAdapter(this, mCursor);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (mCursor == null) {
            Log.e(TAG, "Got click on position " + position + " but there is no cursor");
            return;
        }
        if (mCursor.isClosed()) {
            Log.e(TAG, "Got click on position " + position + " but the cursor is closed");
            return;
        }
        if (!mCursor.moveToPosition(position)) {
            Log.e(TAG, "Failed to move to position " + position);
            return;
        }
        Uri uri = ApplicationsAdapter.getColumnUri(mCursor, Applications.ApplicationColumns.URI);
        launchApplication(uri);
    }

    private void launchApplication(Uri uri) {
        ComponentName componentName = Applications.uriToComponentName(uri);
        Log.i(TAG, "Launching " + componentName);
        if (componentName != null) {
            /* Bug 846456  adding category property for launch Intent @{ */
            Intent launchIntent = new Intent(Intent.ACTION_MAIN)
                                            .addCategory(Intent.CATEGORY_LAUNCHER)
                                            .setComponent(componentName)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            /* @{ */
            try {
                startActivity(launchIntent);
            } catch (ActivityNotFoundException ex) {
                Log.w(TAG, "Activity not found: " + componentName);
            }
        }
    }
}
