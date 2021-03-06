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

import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusSelectionDialog;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/**
 * Finishes the containing activity on BACK, even if input method is showing.
 */
public class SearchActivityViewSinglePane extends SearchActivityView {

    private String LOG = "QSB.SearchActivityViewSinglePane";

    private CorpusSelectionDialog mCorpusSelectionDialog;

//    private ImageButton mCorpusIndicator;

    public SearchActivityViewSinglePane(Context context) {
        super(context);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        mCorpusIndicator = (ImageButton) findViewById(R.id.corpus_indicator);
//        mCorpusIndicator.setOnKeyListener(mButtonsKeyListener);
//        mCorpusIndicator.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View v) {
//                showCorpusSelectionDialog();
//            }});
    }

    @Override
    public void onResume() {
        if (!isCorpusSelectionDialogShowing()) {
            focusQueryTextView();
        }
        //SPRD: if mCorpus is not Enabled then update view to default corpus
        if (getCorpus() != null && !getQsbApplication().getSettings().isCorpusEnabled(getCorpus())){
            onCorpusSelected(null);
        }
    }

    /* UNISOC: Modify for bug1011584 @{ */
    /* SPRD: modify for bug 758734 @{ */
    //@Override
    //public void onPause() {
    //    super.onPause();
    //    getActivity().overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
    //}
    /* @} */
    /* @} */

    @Override
    public void onStop() {
        dismissCorpusSelectionDialog();
    }

    @Override
    protected void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);

//        if (mCorpusIndicator != null) {
//            Drawable sourceIcon;
//            if (corpus == null) {
//                sourceIcon = getContext().getResources().getDrawable(R.mipmap.search_app_icon);
//            } else {
//                sourceIcon = corpus.getCorpusIcon();
//            }
//            mCorpusIndicator.setImageDrawable(sourceIcon);
//        }
    }

    @Override
    protected Promoter createSuggestionsPromoter() {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            return getQsbApplication().createBlendingPromoter();
        } else {
            return getQsbApplication().createSingleCorpusPromoter(corpus);
        }
    }

    /**
     * Gets the corpus to use for any searches. This is the web corpus in "All" mode,
     * and the selected corpus otherwise.
     */
    @Override
    public Corpus getSearchCorpus() {
        Corpus corpus = getCorpus();
        return corpus == null ? getWebCorpus() : corpus;
    }

    @Override
    public void showCorpusSelectionDialog() {
        Log.d(LOG, "showDialog....");
        if (mCorpusSelectionDialog == null) {
            mCorpusSelectionDialog = getActivity().getCorpusSelectionDialog();
            mCorpusSelectionDialog.setOnCorpusSelectedListener(new CorpusSelectionListener());
        }
        /* Modify 20130108 Spreadst of 112947 ,monkey bug start */
        /*
         * mCorpusSelectionDialog.show(getCorpus());
         */
        if (getActivity() != null) {
            mCorpusSelectionDialog.show(getCorpus());
        }
        /* Modify 20130108 Spreadst of 112947 ,monkey bug end */
    }

    protected boolean isCorpusSelectionDialogShowing() {
        return mCorpusSelectionDialog != null && mCorpusSelectionDialog.isShowing();
    }

    protected void dismissCorpusSelectionDialog() {
       /*Modify 20121226 Spreadst of 97632  Unable to stop activity  start */
        try {
            if (mCorpusSelectionDialog != null && mCorpusSelectionDialog.isShowing()) {
                mCorpusSelectionDialog.dismiss();
            }
        } catch (Exception e) {
            // TODO: handle exception
            mCorpusSelectionDialog = null;
        }
        /*Modify 20121226 Spreadst of 97632  Unable to stop activity  end */
    }

    @Override
    public void considerHidingInputMethod() {
        mQueryTextView.hideInputMethod();
    }

    private class CorpusSelectionListener
            implements CorpusSelectionDialog.OnCorpusSelectedListener {
        public void onCorpusSelected(String corpusName) {
            SearchActivityViewSinglePane.this.onCorpusSelected(corpusName);
        }
    }

}
