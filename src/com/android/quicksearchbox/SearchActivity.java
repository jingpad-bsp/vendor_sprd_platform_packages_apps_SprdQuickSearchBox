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

package com.android.quicksearchbox;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
//import android.os.sprdpower.IPowerManagerEx;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.common.Search;
import com.android.quicksearchbox.ui.SearchActivityView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.android.quicksearchbox.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.sprd.recents.RecentViewHolder;
import com.sprd.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.Manifest.permission;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * The main activity for Quick Search Box. Shows the search UI.
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SearchActivity";

    private static final String SCHEME_CORPUS = "qsb.corpus";

    public static final String INTENT_ACTION_QSB_AND_SELECT_CORPUS
            = "com.android.quicksearchbox.action.QSB_AND_SELECT_CORPUS";

    private static final String INTENT_EXTRA_TRACE_START_UP = "trace_start_up";

    // Keys for the saved instance state.
    private static final String INSTANCE_KEY_CORPUS = "corpus";
    private static final String INSTANCE_KEY_QUERY = "query";

    private static final String ACTIVITY_HELP_CONTEXT = "search";

    private boolean mTraceStartUp;
    // Measures time from for last onCreate()/onNewIntent() call.
    private LatencyTracker mStartLatencyTracker;
    // Measures time spent inside onCreate()
    private LatencyTracker mOnCreateTracker;
    private int mOnCreateLatency;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    private SearchActivityView mSearchActivityView;

    private CorporaObserver mCorporaObserver;

    private Bundle mAppSearchData;

    /* SPRD: modify for bug 703218@{ */
    private final static int PERMISSION_REQUEST_CODE = 0;
    private final static int MISS_PERMISSIONS_ALERT_DIALOG = 0;
    private final static int REQUIRE_STORAGE_PERMISSION_DIALOG = 1;
    private Dialog mAlertDialog = null;
    /* @} */

    /* SPRD: 492414 show contact hint dialog @{ */
    private static AlertDialog.Builder mContactsDialog = null;
    private String mSecureCheck;
    private SecurityAccessContacts mContactsCorpus;
    /* @} */
    private Toast mToast; // UNISOC: Modify for bug1182324

    /* SPRD: Modify for bug 756979  @{ */
    //private IPowerManagerEx mPowerManagerEx;
    /* @} */

//    private static final String FILE_NAME_BLUR_BG = "blur_bg.png";

    private PackageManager mPackageManager;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions();
        }
    };

    private final Runnable mUpdateRecentsTask = new Runnable() {
        public void run() {
            updateRecents();
        }
    };

    private final Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
            mSearchActivityView.showInputMethodForQuery();
        }
    };

    private OnDestroyListener mDestroyListener;

    /* SPRD: modify for bug 658838 @{ */
    boolean mIntentIllegal = false;
    /* @} */

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /* SPRD: modify for bug 658838 @{ */
        mIntentIllegal = Util.checkIntent(getIntent());
        /* @} */

        mTraceStartUp = getIntent().hasExtra(INTENT_EXTRA_TRACE_START_UP);
        if (mTraceStartUp) {
            String traceFile = new File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath();
            Log.i(TAG, "Writing start-up trace to " + traceFile);
            Debug.startMethodTracing(traceFile);
        }
        recordStartTime();
        if (DBG) Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mIsFromNewIntent = false;

        /* SPRD: modify for bug 658838 @{ */
        if (mIntentIllegal) {
            finish();
            return;
        }
        /* @} */

        // This forces the HTTP request to check the users domain to be
        // sent as early as possible.
        QsbApplication.get(this).getSearchBaseUrlHelper();

        mSearchActivityView = setupContentView();
        mContactsCorpus = new SecurityAccessContacts(this); // UNISOC: Modify for bug1181387

        if (getConfig().showScrollingSuggestions()) {
            mSearchActivityView.setMaxPromotedSuggestions(getConfig().getMaxPromotedSuggestions());
        } else {
            mSearchActivityView.limitSuggestionsToViewHeight();
        }
        if (getConfig().showScrollingResults()) {
            mSearchActivityView.setMaxPromotedResults(getConfig().getMaxPromotedResults());
        } else {
            mSearchActivityView.limitResultsToViewHeight();
        }

        mSearchActivityView.setSearchClickListener(new SearchActivityView.SearchClickListener() {
            public boolean onSearchClicked(int method) {
                return SearchActivity.this.onSearchClicked(method);
            }
        });

        mSearchActivityView.setQueryListener(new SearchActivityView.QueryListener() {
            public void onQueryChanged() {
                updateSuggestionsBuffered();
            }
        });

        mSearchActivityView.setSuggestionClickListener(new ClickHandler());

        mSearchActivityView.setVoiceSearchButtonClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onVoiceSearchClicked();
            }
        });

        View.OnClickListener finishOnClick = new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        };
        mSearchActivityView.setExitClickListener(finishOnClick);

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSearchActivityView.start();

        mCorporaObserver = new CorporaObserver();
        getCorpora().registerDataSetObserver(mCorporaObserver);
        recordOnCreateDone();

        /* SPRD: modify for bug 703218@{ */
        checkAndRequestPermissions();
        /* @} */
        /* SPRD: Modify for bug 756979  @{ */
        //mPowerManagerEx = IPowerManagerEx.Stub.asInterface(ServiceManager.getService("power_ex"));
        /* @} */
    }

    /**
     * load bitmap from wallpaper
     *
     * @return
     */
    private Bitmap loadWallPaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(SearchActivity.this);
        return ((BitmapDrawable) wallpaperManager.getDrawable()).getBitmap();
    }

//    private void createScript() {
//        mRS = RenderScript.create(this);
//
//        mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn);
//
//        mOutAllocation = Allocation.createFromBitmap(mRS, mBitmapOut);
//
//        mScriptBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
//    }

    /**
     * blur value range 1.0-25.0 extends to 0-100
     *
     * @param i
     * @return
     */
    private static float getFilterParameter(int i) {
        float f = 0.f;

        final float max = 25.0f;
        final float min = 1.f;
        f = (float) ((max - min) * (i / 100.0) + min);
        return f;

    }

    protected SearchActivityView setupContentView() {
        setContentView(R.layout.search_activity);
        return (SearchActivityView) findViewById(R.id.search_activity_view);
    }

    protected SearchActivityView getSearchActivityView() {
        return mSearchActivityView;
    }

    public volatile boolean mIsFromNewIntent = false;

    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) Log.d(TAG, "onNewIntent()");
        mIsFromNewIntent = true;
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
        /* SPRD: modify for bug 703218@{ */
        dismissAlertDialog();
        checkAndRequestPermissions();
        /* @} */
    }

    private void recordStartTime() {
        mStartLatencyTracker = new LatencyTracker();
        mOnCreateTracker = new LatencyTracker();
        mStarting = true;
        mTookAction = false;
    }

    private void recordOnCreateDone() {
        mOnCreateLatency = mOnCreateTracker.getLatency();
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String corpusName = savedInstanceState.getString(INSTANCE_KEY_CORPUS);
        String query = savedInstanceState.getString(INSTANCE_KEY_QUERY);
        setCorpus(corpusName);
        setQuery(query, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.

        outState.putString(INSTANCE_KEY_CORPUS, getCorpusName());
        outState.putString(INSTANCE_KEY_QUERY, getQuery());
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        String corpusName = getCorpusNameFromUri(intent.getData());
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        boolean selectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);

        if (DBG) Log.d(TAG, "setupFromIntent()->corpusName = " + corpusName);
        if (DBG) Log.d(TAG, "setupFromIntent()->query = " + query);

        setCorpus(corpusName);
        setQuery(query, selectAll);
        mAppSearchData = appSearchData;

        if (startedIntoCorpusSelectionDialog()) {
            mSearchActivityView.showCorpusSelectionDialog();
        }
    }

    public boolean startedIntoCorpusSelectionDialog() {
        return INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(getIntent().getAction());
    }

    /**
     * Removes corpus selector intent action, so that BACK works normally after
     * dismissing and reopening the corpus selector.
     */
    public void clearStartedIntoCorpusSelectionDialog() {
        Intent oldIntent = getIntent();
        if (SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(oldIntent.getAction())) {
            Intent newIntent = new Intent(oldIntent);
            newIntent.setAction(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
            setIntent(newIntent);
        }
    }

    public static Uri getCorpusUri(Corpus corpus) {
        if (corpus == null) return null;
        return new Uri.Builder()
                .scheme(SCHEME_CORPUS)
                .authority(corpus.getName())
                .build();
    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_CORPUS.equals(uri.getScheme())) return null;
        return uri.getAuthority();
    }

    private Corpus getCorpus() {
        return mSearchActivityView.getCorpus();
    }

    private String getCorpusName() {
        return mSearchActivityView.getCorpusName();
    }

    private void setCorpus(String name) {
        mSearchActivityView.setCorpus(name);
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    protected SearchSettings getSettings() {
        return getQsbApplication().getSettings();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @VisibleForTesting
    public void setOnDestroyListener(OnDestroyListener l) {
        mDestroyListener = l;
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");

        /* SPRD: modify for bug 658838 @{ */
        if (!mIntentIllegal) {
            getCorpora().unregisterDataSetObserver(mCorporaObserver);
            mSearchActivityView.destroy();
        }
        /* @} */

        super.onDestroy();
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed();
        }

//        if (null != mBitmapIn) {
//            mBitmapIn.recycle();
//        }

//        if (null != mBitmapOut) {
//            mBitmapOut.recycle();
//        }
    }

    /*add 20121208 Spreadst of 101673  IllegalStateException start */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        finish();
    }

    /*add 20121208 Spreadst of 101673  IllegalStateException end */
    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening the search
            // settings, or clicking a notification in the status bar.
            // TODO we should log both sets of suggestions in 2-pane mode
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSearchActivityView.clearSuggestions();
        getQsbApplication().getShortcutRefresher().reset();
        mSearchActivityView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (DBG) Log.d(TAG, "onPause()");
        mSearchActivityView.onPause();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        if (DBG) Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        /* UNISOC: Modify for bug 1012283 @{ */
        updateRecentsBuffered();
        if (!TextUtils.isEmpty(getQuery())) {
            updateSuggestionsBuffered();
        }
        /* @} */
        mSearchActivityView.onResume();
        if (mTraceStartUp) Debug.stopMethodTracing();
    }

    /* UNISOC: Modify for bug1149320 @{ */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
    /* @} */

    public void createMenuItems(Menu menu, boolean showDisabled) {
        getSettings().addMenuItems(menu, showDisabled);
        getQsbApplication().getHelp().addHelpMenuItem(menu, ACTIVITY_HELP_CONTEXT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Launch the IME after a bit
            mHandler.postDelayed(mShowInputMethodTask, 0);
        }
    }

    protected String getQuery() {
        return mSearchActivityView.getQuery();
    }

    protected void setQuery(String query, boolean selectAll) {
        mSearchActivityView.setQuery(query, selectAll);
    }

    public CorpusSelectionDialog getCorpusSelectionDialog() {
        Log.d(TAG, "crteteCorpusDialog");
        CorpusSelectionDialog dialog = createCorpusSelectionDialog();
        dialog.setOwnerActivity(this);
        dialog.setOnDismissListener(new CorpusSelectorDismissListener());
        return dialog;
    }

    protected CorpusSelectionDialog createCorpusSelectionDialog() {
        Log.d(TAG, "new CorpusDialog...");
        return new CorpusSelectionDialog(this, getSettings());
    }

    /**
     * @return true if a search was performed as a result of this click, false otherwise.
     */
    protected boolean onSearchClicked(int method) {
        String query = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) == 0) return false;

        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return false;

        mTookAction = true;

        // Log search start
        getLogger().logSearch(getCorpus(), method, query.length());

        // Start search
        startSearch(searchCorpus, query);
        return true;
    }

    protected void startSearch(Corpus searchCorpus, String query) {
        Intent intent = searchCorpus.createSearchIntent(query, mAppSearchData);
        launchIntent(intent);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return;

        mTookAction = true;

        // Log voice search start
        getLogger().logVoiceSearch(searchCorpus);

        // Start voice search
        Intent intent = searchCorpus.createVoiceSearchIntent(mAppSearchData);
        launchIntent(intent);
    }

    protected Corpus getSearchCorpus() {
        return mSearchActivityView.getSearchCorpus();
    }

    protected SuggestionCursor getCurrentSuggestions() {
        return mSearchActivityView.getCurrentPromotedSuggestions();
    }

    protected SuggestionPosition getCurrentSuggestions(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition pos = adapter.getSuggestion(id);
        if (pos == null) {
            return null;
        }
        SuggestionCursor suggestions = pos.getCursor();
        int position = pos.getPosition();
        if (suggestions == null) {
            return null;
        }
        int count = suggestions.getCount();
        if (position < 0 || position >= count) {
            Log.w(TAG, "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        suggestions.moveTo(position);
        return pos;
    }

    protected Set<Corpus> getCurrentIncludedCorpora() {
        Suggestions suggestions = mSearchActivityView.getSuggestions();
        return suggestions == null ? null : suggestions.getIncludedCorpora();
    }

    protected void launchIntent(Intent intent) {
        if (DBG) Log.d(TAG, "launchIntent " + intent);
        if (intent == null) {
            return;
        }
        try {
            startActivity(intent);
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return false;

        if (DBG) Log.d(TAG, "Launching suggestion " + id);
        mTookAction = true;

        //hide ime
        mSearchActivityView.hideInputMethod();

        // Log suggestion click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_LAUNCH);

        // Create shortcut
        // UX Requirements: do not create shortcut and make a rank
        //getShortcutRepository().reportClick(suggestion.getCursor(), suggestion.getPosition());

        // Launch intent
        launchSuggestion(suggestion.getCursor(), suggestion.getPosition());

        return true;
    }

    protected void launchSuggestion(SuggestionCursor suggestions, int position) {
        suggestions.moveTo(position);
        Intent intent = SuggestionUtils.getSuggestionIntent(suggestions, mAppSearchData);
        Uri uri = Uri.parse("content://applications/applications/" +
                "com.android.quicksearchbox/com.android.quicksearchbox.SearchActivity");
        if (intent == null || intent.getData() == null || 0 != intent.getData().compareTo(uri)) {
            launchIntent(intent);
        /* UNISOC: Modify for bug1182324 @{ */
        } else {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(SearchActivity.this, R.string.repeat_start_search, Toast.LENGTH_SHORT);
            mToast.show();
        }
        /* @} */
    }

    protected void removeFromHistoryClicked(final SuggestionsAdapter<?> adapter,
                                            final long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;
        CharSequence title = suggestion.getSuggestionText1();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(R.string.remove_from_history)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: what if the suggestions have changed?
                                removeFromHistory(adapter, id);
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    protected void removeFromHistory(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;
        removeFromHistory(suggestion.getCursor(), suggestion.getPosition());
        // TODO: Log to event log?
    }

    protected void removeFromHistory(SuggestionCursor suggestions, int position) {
        removeShortcut(suggestions, position);
        removeFromHistoryDone(true);
    }

    protected void removeFromHistoryDone(boolean ok) {
        Log.i(TAG, "Removed query from history, success=" + ok);
        updateSuggestionsBuffered();
        if (!ok) {
            Toast.makeText(this, R.string.remove_from_history_failed, Toast.LENGTH_SHORT).show();
        }
    }

    protected void removeShortcut(SuggestionCursor suggestions, int position) {
        if (suggestions.isSuggestionShortcut()) {
            if (DBG) Log.d(TAG, "Removing suggestion " + position + " from shortcuts");
            getShortcutRepository().removeFromHistory(suggestions, position);
        }
    }

    protected void clickedQuickContact(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;

        if (DBG) Log.d(TAG, "Used suggestion " + suggestion.getPosition());
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_QUICK_CONTACT);

        // Create shortcut
        //getShortcutRepository().reportClick(suggestion.getCursor(), suggestion.getPosition());
    }

    protected void refineSuggestion(SuggestionsAdapter<?> adapter, long id) {
        if (DBG) Log.d(TAG, "query refine clicked, pos " + id);
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) {
            return;
        }
        String query = suggestion.getSuggestionQuery();
        if (TextUtils.isEmpty(query)) {
            return;
        }

        // Log refine click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_REFINE);

        // Put query + space in query text view
        String queryWithSpace = query + ' ';
        setQuery(queryWithSpace, false);
        updateSuggestions();
        mSearchActivityView.focusQueryTextView();
    }

    private void updateSuggestionsBuffered() {
        if (DBG) Log.d(TAG, "updateSuggestionsBuffered()");
        mHandler.removeCallbacks(mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        mHandler.postDelayed(mUpdateSuggestionsTask, delay);
    }

    private void updateRecentsBuffered() {
        if (DBG) Log.d(TAG, "updateRecentsBuffered()");
        mHandler.removeCallbacks(mUpdateRecentsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();// same as suggestions
        mHandler.postDelayed(mUpdateRecentsTask, delay);
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (mStarting) {
            mStarting = false;
            String source = getIntent().getStringExtra(Search.SOURCE);
            int latency = mStartLatencyTracker.getLatency();
            getLogger().logStart(mOnCreateLatency, latency, source, getCorpus(),
                    suggestions == null ? null : suggestions.getExpectedCorpora());
            getQsbApplication().onStartupComplete();
        }
    }

    private void getCorporaToQuery(Consumer<List<Corpus>> consumer) {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            getCorpusRanker().getCorporaInAll(Consumers.createAsyncConsumer(mHandler, consumer));
        } else {
            List<Corpus> corpora = new ArrayList<Corpus>();
            Corpus searchCorpus = getSearchCorpus();
            if (searchCorpus != null) corpora.add(searchCorpus);
            consumer.consume(corpora);
        }
    }

    protected void getShortcutsForQuery(String query, Collection<Corpus> corporaToQuery,
                                        final Suggestions suggestions) {
        ShortcutRepository shortcutRepo = getShortcutRepository();
        if (shortcutRepo == null) return;
        if (query.length() == 0 && !getConfig().showShortcutsForZeroQuery()) {
            return;
        }
        Consumer<ShortcutCursor> consumer = Consumers.createAsyncCloseableConsumer(mHandler,
                new Consumer<ShortcutCursor>() {
                    public boolean consume(ShortcutCursor shortcuts) {
                        suggestions.setShortcuts(shortcuts);
                        return true;
                    }
                });
        shortcutRepo.getShortcutsForQuery(query, corporaToQuery,
                getSettings().allowWebSearchShortcuts(), consumer);
    }

    public void updateSuggestions() {
        if (DBG) Log.d(TAG, "updateSuggestions()");
        final String query = CharMatcher.WHITESPACE.trimLeadingFrom(getQuery());
        getQsbApplication().getSourceTaskExecutor().cancelPendingTasks();
        getCorporaToQuery(new Consumer<List<Corpus>>() {
            @Override
            public boolean consume(List<Corpus> corporaToQuery) {
                updateSuggestions(query, corporaToQuery);
                return true;
            }
        });
    }

    public void updateRecents() {
        if (DBG) Log.d(TAG, "updateRecents()");

        mSearchActivityView.setRecents(queryLimitedRecentAppInfo(20));
    }

    /**
     * Cache 10 recents for performance
     */
    private final LruCache<ComponentName, RecentViewHolder> mRecentInfoCache =
            new LruCache<ComponentName, RecentViewHolder>(10);

    /**
     * only show 4 at most
     */
    private final static int MAX_NUM_RECENTS = 4;

    /**
     * excluding app package name list
     */
    /* SPRD: Modify for bug 723636  @{ */
    private static final List<String> PKG_EXCLUDE_FROM_RECENTS =
            Arrays.asList(new String[]{"com.android.quicksearchbox", "com.sprd.recents"});
    /* @} */

    /**
     * get application info through package name
     *
     * @param pkgName
     * @return
     */
    private RecentViewHolder getAppInfoFromPms(ActivityManager.RecentTaskInfo recentTaskInfo) {
        if (DBG) Log.d(TAG, "getAndUpdateRecentInfo()");
        final String pkgName = recentTaskInfo.baseIntent.getComponent().getPackageName();

        /* SPRD: Modify for bug 756979 788799  @{ */
        //final Set<String> cat = recentTaskInfo.baseIntent.getCategories(); // UNISOC: Modify for bug1210540
        if ((pkgName == null) || PKG_EXCLUDE_FROM_RECENTS.contains(pkgName)) {// normal exclude
            return null;
        }
        /*} else if ((Utils.getPowerSaveMode(mPowerManagerEx) == Utils.ULTRA_POWER_SAVE_MODE)) {// power save mode
            if (Utils.PKG_EXCLUDE_FROM_RECENTS_POWER_SAVE_MODE.equals(pkgName)) return null;
            // SPRD Bug 792111, null pointer exception
            if ((cat != null) && cat.contains(Utils.EXCLUDE_BY_CATEGORY)) return null;
        }*/
        /* @} */

        /* SPRD: Modify for bug 819657,1014181 @{ */
        mPackageManager = this.getPackageManager();
        RecentViewHolder result = null;
        if (pkgName.equals(Utils.GALLERY_AND_VideoPlayer_PackageName)) {
            result = getAppInfoFromActivity(recentTaskInfo);
        } else {
            List<ApplicationInfo> listAppcations = mPackageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
            if(DBG) Log.i(TAG, "getAppInfoFromPms->applicationInfo.pkgName = " + pkgName);
            for (ApplicationInfo applicationInfo : listAppcations) {
                if (applicationInfo.packageName.equals(pkgName)) {
                    if(DBG) Log.i(TAG, "getAppInfoFromPms->applicationInfo.packageName = " + applicationInfo.packageName);
                    Resources r = null;
                    try {
                        r = mPackageManager.getResourcesForApplication(applicationInfo);
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(applicationInfo.icon <= 0) {
                        return null;
                    }
                    final Drawable icon = r.getDrawable(applicationInfo.icon, null);
                    /* UNISOC: Modify for bug 1037654 @{ */
                    final CharSequence title = applicationInfo.loadLabel(mPackageManager);
                    /* @} */
                    result = new RecentViewHolder();
                    result.persistId = recentTaskInfo.persistentId;
                    result.baseIntent = recentTaskInfo.baseIntent;
                    result.icon = icon;
                    result.title = title;
                    break;
                }
            }
        }
        return result;
    }

    public ActivityInfo getActivityInfo(ComponentName cn) {
        if (mPackageManager == null) return null;

        try {
            return mPackageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    /* @} */

    /* UNISOC: Modify for bug 1014181 @{ */
    private RecentViewHolder getAppInfoFromActivity(ActivityManager.RecentTaskInfo recentTaskInfo) {
        final ComponentName compName = recentTaskInfo.baseIntent.getComponent();
        RecentViewHolder result = null;
        ActivityInfo activityInfo = getActivityInfo(compName);
        if (activityInfo != null) {
            Drawable icon = activityInfo.loadIcon(mPackageManager);
            CharSequence title = activityInfo.loadLabel(mPackageManager);
            result = new RecentViewHolder();
            result.persistId = recentTaskInfo.persistentId;
            result.baseIntent = recentTaskInfo.baseIntent;
            result.icon = icon;
            result.title = title;
        }
        return result;
    }
    /* @} */

    /**
     * get recent info from cache, if not exists, get it from PackageManger with package name
     *
     * @param rti
     * @return
     */
    private RecentViewHolder getAndUpdateRecentInfo(ActivityManager.RecentTaskInfo rti) {
        if (DBG) Log.d(TAG, "getAndUpdateRecentInfo()");
        if (null == rti || null == rti.baseIntent) {
            return null;
        }

        final ComponentName cn = rti.baseIntent.getComponent();
        /* SPRD: Modify for bug 756979 788799  @{ */
        final Set<String> cat = rti.baseIntent.getCategories();
        /*if ((null == cn)
                || ((Utils.getPowerSaveMode(mPowerManagerEx) == Utils.ULTRA_POWER_SAVE_MODE)
                && Utils.PKG_EXCLUDE_FROM_RECENTS_POWER_SAVE_MODE.equals(cn.getPackageName()))) {
            return null;
        }*/

        // SPRD Bug 895079,1014181 get rid of non-category task
        //if (cat == null) return null;
        /* @} */
        // SPRD Bug 792111, null pointer exception
        if ((cat != null) && cat.contains(Utils.EXCLUDE_BY_CATEGORY)) return null;
        /* @} */

        RecentViewHolder rvh = mRecentInfoCache.get(cn);
        /* UNISOC: Modify for bug1015122 @{ */
        if (null != rvh && (rvh.persistId != rti.persistentId)) {
            mRecentInfoCache.remove(cn);
            rvh = getAppInfoFromPms(rti);
            if (null != rvh) {
                mRecentInfoCache.put(cn, rvh);
            }
        } else if (null == rvh) {
            rvh = getAppInfoFromPms(rti);
            if (null != rvh) {
                mRecentInfoCache.put(cn, rvh);
            }
        }
        /* @} */
        return rvh;
    }

    private List<RecentViewHolder> queryLimitedRecentAppInfo(int maxNum) {
        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        int flag = ActivityManager.RECENT_WITH_EXCLUDED |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE;
        List<ActivityManager.RecentTaskInfo> recentTaskInfos = null;
        try {
            recentTaskInfos = mActivityManager.getService().getRecentTasks(
                    ActivityManager.getMaxRecentTasksStatic(),
                    flag,
                    UserHandle.USER_CURRENT).getList();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        int recentCount = 0;
        List<RecentViewHolder> recents = new ArrayList<RecentViewHolder>();
        /* SPRD: Modify for bug 819657,1014181 @{ */
        Set<String> pkgNames = new HashSet<String>();
        Set<String> clsNames = new HashSet<String>();
        for (ActivityManager.RecentTaskInfo recentTaskInfo : recentTaskInfos) {
            if (DBG)
                Log.i(TAG, "queryLimitedRecentAppInfo->recentTaskInfos persistentId = " + recentTaskInfo.persistentId);
            if (DBG)
                Log.i(TAG, "queryLimitedRecentAppInfo->recentTaskInfos baseIntent = " + recentTaskInfo.baseIntent);
            RecentViewHolder rvh = getAndUpdateRecentInfo(recentTaskInfo);
            final String pkgName = recentTaskInfo.baseIntent.getComponent().getPackageName();
            final String clsName = recentTaskInfo.baseIntent.getComponent().getClassName();
            if (null != rvh && (recentTaskInfo.persistentId != 0)) {// if persistentId == 0 this task is home stack
                if (pkgName.equals(Utils.GALLERY_AND_VideoPlayer_PackageName)) {
                    if (!clsNames.contains(clsName)) {
                        recents.add(rvh);
                        clsNames.add(clsName);
                        ++recentCount;
                    }
                } else if (!pkgNames.contains(pkgName)) {// if not include in the list
                    recents.add(rvh);
                    pkgNames.add(pkgName);
                    ++recentCount;
                }
            }
            if (recentCount == MAX_NUM_RECENTS) {
                break;
            }
        }
        /* @} */
        return recents;
    }

    protected void updateSuggestions(String query, List<Corpus> corporaToQuery) {
        if (DBG) Log.d(TAG, "updateSuggestions(\"" + query + "\"," + corporaToQuery + ")");
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(
                query, corporaToQuery);
        getShortcutsForQuery(query, corporaToQuery, suggestions);

        // Log start latency if this is the first suggestions update
        gotSuggestions(suggestions);

        showSuggestions(suggestions);
    }

    protected void showSuggestions(Suggestions suggestions) {
        mSearchActivityView.setSuggestions(suggestions);
    }

    private class ClickHandler implements SuggestionClickListener {

        public void onSuggestionQuickContactClicked(SuggestionsAdapter<?> adapter, long id) {
            clickedQuickContact(adapter, id);
        }

        public void onSuggestionClicked(SuggestionsAdapter<?> adapter, long id) {
            launchSuggestion(adapter, id);
        }

        public void onSuggestionRemoveFromHistoryClicked(SuggestionsAdapter<?> adapter, long id) {
            removeFromHistoryClicked(adapter, id);
        }

        public void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> adapter, long id) {
            refineSuggestion(adapter, id);
        }
    }

    private class CorpusSelectorDismissListener implements DialogInterface.OnDismissListener {
        public void onDismiss(DialogInterface dialog) {
            if (DBG) Log.d(TAG, "Corpus selector dismissed");
            clearStartedIntoCorpusSelectionDialog();
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            setCorpus(getCorpusName());
            updateSuggestions();
        }
    }

    public interface OnDestroyListener {
        void onDestroyed();
    }

    /* SPRD: 504763 check storage permission  @{ */
    private void showAlertDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
        switch (id) {
            case MISS_PERMISSIONS_ALERT_DIALOG:
                builder.setMessage(R.string.storage_permission_missed_hint) // UNISOC: Modify for bug1153009
                        .setNegativeButton(R.string.dialog_dismiss,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        /* SPRD: Bug 580635 java.lang.IllegalArgumentException: delete invalid code @{ */
                                        //dialog.dismiss();
                                        finish();
                                    }
                                })
                        .setOnKeyListener(new Dialog.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    finish();
                                }
                                return true;
                            }
                        })
                        .setCancelable(false);
                break;
            default:
                break;
        }

        mAlertDialog = builder.create();
        /* SPRD: modify for bug 746239 @{ */
        if (!SearchActivity.this.isFinishing()) {
            mAlertDialog.show();
        }
        /* @} */
    }

    /* UNISOC: Modify for bug492414,1147914 show contact hint dialog @{ */
    private void secureCheck() {
        mSecureCheck = SystemProperties.get("persist.support.securetest", "1");
        if (mSecureCheck.equals("1")) {
            mContactsCorpus.updateContactsCorpus();
        }
    }
    /* @} */

    /* SPRD: modify for bug 703218@{ */
    public boolean checkAndRequestPermissions() {
        ArrayList<String> permssionsToRequest = new ArrayList<String>();

        if (checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permssionsToRequest.add(permission.READ_EXTERNAL_STORAGE);
        }

        /*
        if (checkSelfPermission(permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permssionsToRequest.add(permission.READ_CONTACTS);
        }

        if (checkSelfPermission(permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permssionsToRequest.add(permission.READ_CALENDAR);
        }

        if (checkSelfPermission(permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permssionsToRequest.add(permission.READ_SMS);
        }
        */

        if (checkSelfPermission(permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permssionsToRequest.add(permission.READ_CALL_LOG);
        }

        /* UNISOC: Modify for bug1147914 @{ */
        if (!permssionsToRequest.isEmpty()) {
            requestPermissions((String[]) permssionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            permssionsToRequest.clear();
            return false;
        } else {
            secureCheck();
        }
        /* @} */

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        dismissAlertDialog();
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (DBG) Log.d(TAG, "onRequestPermissionsResult(): permissions are "
                        + Arrays.toString(permissions) + ", result is " + Arrays.toString(grantResults));
                boolean resultsAllGranted = true;
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (PackageManager.PERMISSION_GRANTED != result) {
                            resultsAllGranted = false;
                            break;
                        }
                    }
                } else {
                    resultsAllGranted = false;
                }

                /* UNISOC: Modify for bug1147914 @{ */
                if (!resultsAllGranted) {
                    showAlertDialog(MISS_PERMISSIONS_ALERT_DIALOG);
                } else {
                    secureCheck();
                }
                break;
            default:
                break;
        }
    }
    /* @} */

    /*SPRD: 511908 add dismiss method @{ */
    private void dismissAlertDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
    /* @} */
}
