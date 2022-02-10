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

import android.content.ComponentName;

/**
 * A Suggestion that delegates all calls to other suggestions.
 */
public abstract class AbstractSuggestionWrapper implements Suggestion {

    /**
     * Gets the current suggestion.
     */
    protected abstract Suggestion current();

    public String getShortcutId() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getShortcutId();
    }

    public String getSuggestionFormat() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionFormat();
    }

    public String getSuggestionIcon1() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionIcon1();
    }

    public String getSuggestionIcon2() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionIcon2();
    }

    public String getSuggestionIntentAction() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionIntentAction();
    }

    public ComponentName getSuggestionIntentComponent() {
        return current().getSuggestionIntentComponent();
    }

    public String getSuggestionIntentDataString() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionIntentDataString();
    }

    public String getSuggestionIntentExtraData() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionIntentExtraData();
    }

    public String getSuggestionLogType() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionLogType();
    }

    public String getSuggestionQuery() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionQuery();
    }

    public Source getSuggestionSource() {
        /* SPRD 562762 @{ */
        /* New a blank CursorBackedSourceResult to return*/
        if(current() == null){
            return new CursorBackedSourceResult(null, null).getSuggestionSource();
        }
        /* @} */
        return current().getSuggestionSource();
    }

    public String getSuggestionText1() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionText1();
    }

    public String getSuggestionText2() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionText2();
    }

    public String getSuggestionText2Url() {
        // SPRD: add if current() is null,return null to avoid nullpointer
        if (current() == null) {
            return "";
        }
        return current().getSuggestionText2Url();
    }

    public boolean isSpinnerWhileRefreshing() {
        /* SPRD 562762 @{ */
        if(current() == null){
            return false;
        }
        /* @} */
        return current().isSpinnerWhileRefreshing();
    }

    public boolean isSuggestionShortcut() {
        // SPRD: add if current() is null,return false to avoid nullpointer
        if (current() == null) {
            return false;
        }
        return current().isSuggestionShortcut();
    }

    public boolean isWebSearchSuggestion() {
        // SPRD: add if current() is null,return false to avoid nullpointer
        if (current() == null) {
            return false;
        }
        return current().isWebSearchSuggestion();
    }

    public boolean isHistorySuggestion() {
        // SPRD: Add 20140516 Spreadst bug313172, if current() is null,return false to avoid nullpointer
        if (current() == null) {
            return false;
        }
        return current().isHistorySuggestion();
    }

    public SuggestionExtras getExtras() {
        /* SPRD 562762 @{ */
        /* The method getExtras maybe return null,if current() is null,just return null*/
        if(current() == null){
            return null;
        }
        /* @} */
        return current().getExtras();
    }

}
