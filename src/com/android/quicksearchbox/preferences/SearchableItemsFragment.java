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

import com.android.quicksearchbox.R;

import android.os.Bundle;
import android.preference.PreferenceGroup;
import android.view.View;
import android.widget.ListView;

/**
 * Fragment for selecting searchable items
 */
public class SearchableItemsFragment extends SettingsFragmentBase {

    /*SPRD: add onCreate to setTitle @{*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getActivity().setTitle(R.string.search_sources);
    }
    /*@}*/

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView lv = (ListView) view.findViewById(android.R.id.list);
        if (lv != null) {
            lv.setDivider(null);
            lv.setBackgroundResource(R.drawable.search_bg);
        }
    }

    @Override
    protected int getPreferencesResourceId() {
        return R.xml.preferences_searchable_items;
    }

    @Override
    protected void handlePreferenceGroup(PreferenceGroup screen) {
        getController().handlePreference(screen);
    }
}
