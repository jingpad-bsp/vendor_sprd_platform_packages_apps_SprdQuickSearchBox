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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SuggestionPosition;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Holds a list of suggestions.
 */
public class SuggestionsView extends ListView implements SuggestionsListView<ListAdapter> {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsView";

    private boolean mLimitSuggestionsToViewHeight;
    private SuggestionsAdapter<ListAdapter> mSuggestionsAdapter;

    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSuggestionsAdapter(SuggestionsAdapter<ListAdapter> adapter) {
        super.setAdapter(adapter == null ? null : adapter.getListAdapter());
        mSuggestionsAdapter = adapter;
        if (null != mSuggestionsAdapter) {
            mSuggestionsAdapter.setListView(this);
        }
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    public SuggestionsAdapter<ListAdapter> getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setItemsCanFocus(true);
    }

    /**
     * Gets the position of the selected suggestion.
     *
     * @return A 0-based index, or {@code -1} if no suggestion is selected.
     */
    public int getSelectedPosition() {
        return getSelectedItemPosition();
    }

    /**
     * Gets the selected suggestion.
     *
     * @return {@code null} if no suggestion is selected.
     */
    public SuggestionPosition getSelectedSuggestion() {
        return (SuggestionPosition) getSelectedItem();
    }

    public void setLimitSuggestionsToViewHeight(boolean limit) {
        mLimitSuggestionsToViewHeight = limit;
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (mLimitSuggestionsToViewHeight) {
            setMaxPromotedByHeight();
        }
    }

    private void setMaxPromotedByHeight() {
        if (mSuggestionsAdapter != null) {
            float maxHeight;
            if (getParent() instanceof FrameLayout) {
                // We put the SuggestionView inside a frame layout so that we know what its
                // maximum height is. Since this views height is set to 'wrap content' (in two-pane
                // mode at least), we can't use our own height for these calculations.
                maxHeight = ((View) getParent()).getHeight();
                if (DBG) Log.d(TAG, "Parent height=" + maxHeight);
            } else {
                maxHeight = getHeight();
                if (DBG) Log.d(TAG, "This height=" + maxHeight);
            }
            float suggestionHeight =
                getContext().getResources().getDimension(R.dimen.suggestion_view_height);
            if (suggestionHeight != 0) {
                int suggestions = Math.max(1, (int) Math.floor(maxHeight / suggestionHeight));
                if (DBG) {
                    Log.d(TAG, "view height=" + maxHeight + " suggestion height=" +
                            suggestionHeight + " -> maxSuggestions=" + suggestions);
                }
                /*Modify spreadst of 98661 horizontal screen can not display all applications start*/
                //mSuggestionsAdapter.setMaxPromoted(suggestions);
                mSuggestionsAdapter.setMaxPromoted(200);
                /*Modify spreadst of 98661 horizontal screen can not display all applications end*/
            }
        }
    }

    /**
     * Adapter interface. The list adapter must implement this interface.
     */
    public interface PinnedHeaderAdapter {

        /**
         * Pinned header state: don't show the header.
         */
        public static final int PINNED_HEADER_GONE = 0;

        /**
         * Pinned header state: show the header at the top of the list.
         */
        public static final int PINNED_HEADER_VISIBLE = 1;

        /**
         * Pinned header state: show the header. If the header extends beyond
         * the bottom of the first shown element, push it up and clip.
         */
        public static final int PINNED_HEADER_PUSHED_UP = 2;

        /**
         * Computes the desired state of the pinned header for the given
         * position of the first visible list item. Allowed return values are
         * {@link #PINNED_HEADER_GONE}, {@link #PINNED_HEADER_VISIBLE} or
         * {@link #PINNED_HEADER_PUSHED_UP}.
         */
        int getPinnedHeaderState(int position);

        /**
         * Configures the pinned header view to match the first visible list
         * item.
         *
         * @param header
         *            pinned header view.
         * @param position
         *            position of the first visible list item.
         * @param alpha
         *            fading of the header view, between 0 and 255.
         */
        void configurePinnedHeader(View header, int position, int alpha);
    }

    private static final int MAX_ALPHA = 255;

    private View mHeaderView;
    private boolean mHeaderViewVisible;

    private int mHeaderViewWidth;

    private int mHeaderViewHeight;

    public void setPinnedHeaderView(View view) {
        mHeaderView = view;

        // Disable vertical fading when the pinned header is present
        // TODO change ListView to allow separate measures for top and bottom
        // fading edge;
        // in this particular case we would like to disable the top, but not the
        // bottom edge.
        if (mHeaderView != null) {
            this.setVerticalFadingEdgeEnabled(true);
            setFadingEdgeLength(50);
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
            mHeaderViewWidth = mHeaderView.getMeasuredWidth();
            mHeaderViewHeight = mHeaderView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mHeaderView != null) {
            mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
            configureHeaderView(getFirstVisiblePosition());
        }
    }

    public void configureHeaderView(int position) {
        if (mHeaderView == null || mSuggestionsAdapter == null) {
            return;
        }

        int state = mSuggestionsAdapter.getPinnedHeaderState(position);
        switch (state) {
            case PinnedHeaderAdapter.PINNED_HEADER_GONE: {
                mHeaderViewVisible = false;
                break;
            }

            case PinnedHeaderAdapter.PINNED_HEADER_VISIBLE: {
                mSuggestionsAdapter.configurePinnedHeader(mHeaderView, position, MAX_ALPHA);
                if (mHeaderView.getTop() != 0) {
                    mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
                }
                mHeaderViewVisible = true;
                break;
            }

            case PinnedHeaderAdapter.PINNED_HEADER_PUSHED_UP: {
                View firstView = getChildAt(0);
                if (null != firstView) {
                int bottom = firstView.getBottom();
                int headerHeight = mHeaderView.getHeight();
                int y;
                int alpha;
                if (bottom < headerHeight && 0 != headerHeight) {
                    y = (bottom - headerHeight);
                    alpha = MAX_ALPHA * (headerHeight + y) / headerHeight;
                } else {
                    y = 0;
                    alpha = MAX_ALPHA;
                }
                mSuggestionsAdapter.configurePinnedHeader(mHeaderView, position, alpha);
                if (mHeaderView.getTop() != y) {
                    mHeaderView.layout(0, y, mHeaderViewWidth, mHeaderViewHeight + y);
                }
                mHeaderViewVisible = true;
                }
                break;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        /* SPRD: 863689 IndexOutOfBoundsException @{ */
        try {
            super.dispatchDraw(canvas);
            if (mHeaderViewVisible) {
                // TODO  hide mHeaderView to remove this function
                mHeaderView.setAlpha(0);
                drawChild(canvas, mHeaderView, getDrawingTime());
            }
        } catch (IndexOutOfBoundsException e) {
                Log.d(TAG,"dataset contaminated, throwing exception" +e.toString());
                return;
        }
        /* @} */
    }

}
