package com.sprd.utils;


import android.app.Activity;
//import android.os.sprdpower.IPowerManagerEx;
//import android.os.sprdpower.PowerManagerEx;
import android.os.RemoteException;
import android.view.Window;
import android.view.WindowManager;

/**
 * General utilities.
 */
public class Utils {

    public static final boolean DEBUG = false;

    private static final String TAG = "Sprd.QSB.Utils";

    public static void setWindowDim(Activity activity, float dimAmount) {
        if ((dimAmount < 0.0f) && (dimAmount > 1.0f)) {
            return;
        }
        final Window window = activity.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = dimAmount;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    /**
     * excluding app package name including settings in power save mode
     */
    /* SPRD: Modify for bug 756979 788799 @{ */
    public static final String PKG_EXCLUDE_FROM_RECENTS_POWER_SAVE_MODE = "com.android.settings";
    public static final String URI_EXCLUDE_POWER_SAVE_MODE = "content://applications/applications/com.android.settings";
    public static final int ULTRA_POWER_SAVE_MODE = 4;
    public static final String EXCLUDE_BY_CATEGORY = "android.intent.category.HOME";
    /* @} */

    /* UNISOC: Modify for bug 1014181 @{ */
    public static final String GALLERY_AND_VideoPlayer_PackageName = "com.android.gallery3d";
    /* @} */

    /* SPRD: Modify for bug 756979  @{ */
    /*public static int getPowerSaveMode(IPowerManagerEx powerManagerEx) {
        int powerMode = -1;
        try {
            if (powerManagerEx != null) {
                powerMode = powerManagerEx.getPowerSaveMode();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return powerMode;
    }*/
    /* @} */
}
