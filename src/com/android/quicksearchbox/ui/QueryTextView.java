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
package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * The query text field.
 */
public class QueryTextView extends EditText {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.QueryTextView";

    private CommitCompletionListener mCommitCompletionListener;

    private Drawable mDeleteImg;
    private Drawable mSearchIcon;

    public QueryTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public QueryTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QueryTextView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mDeleteImg = getContext().getResources().getDrawable(R.drawable.ic_clear_black_24dp);
        mSearchIcon = getContext().getResources().getDrawable(R.drawable.ic_search_black_24dp);
    }

    /**
     * set search & delete icon inside search text field
     */
    public void setSearchTextImage() {
        if (length() < 1) {
            setCompoundDrawablesWithIntrinsicBounds(mSearchIcon, null, null, null);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(mSearchIcon, null, mDeleteImg, null);
        }
    }
    /**
     * Sets the text selection in the query text view.
     *
     * @param selectAll If {@code true}, selects the entire query.
     *        If {@false}, no characters are selected, and the cursor is placed
     *        at the end of the query.
     */
    public void setTextSelection(boolean selectAll) {
        if (selectAll) {
            selectAll();
        } else {
            setSelection(length());
        }
    }

    protected void replaceText(CharSequence text) {
        clearComposingText();
        setText(text);
        setTextSelection(false);
    }

    public void setCommitCompletionListener(CommitCompletionListener listener) {
        mCommitCompletionListener = listener;
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void showInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null) {
            imm.showSoftInput(this, 0);
        }
    }

    public void hideInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public void onCommitCompletion(CompletionInfo completion) {
        if (DBG) Log.d(TAG, "onCommitCompletion(" + completion + ")");
        hideInputMethod();
        replaceText(completion.getText());
        if (mCommitCompletionListener != null) {
            mCommitCompletionListener.onCommitCompletion(completion.getPosition());
        }
    }

    public interface CommitCompletionListener {
        void onCommitCompletion(int position);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDeleteImg != null && event.getAction() == MotionEvent.ACTION_UP) {
            int eventX = (int) event.getRawX();
            int eventY = (int) event.getRawY();
            Log.i(TAG, "eventX = " + eventX + "; eventY = " + eventY);
            Rect rect = new Rect();
            getGlobalVisibleRect(rect);
            rect.left = rect.right - 100;
            if (rect.contains(eventX, eventY)) {
                setText("");
            }
        }
        return super.onTouchEvent(event);
    }

}
