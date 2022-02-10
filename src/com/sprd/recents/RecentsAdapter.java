package com.sprd.recents;

import java.util.ArrayList;
import java.util.List;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchActivity;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class RecentsAdapter extends BaseAdapter {

    private static final String TAG = "QSB_RecentsAdapter";
    private static final boolean DBG = false;

    private List<RecentViewHolder> mApps;
    private LayoutInflater mInflater;
    private Context mContext;
    IActivityManager mIam;

    public RecentsAdapter(Context context) {
        super();
        mApps = new ArrayList<>();
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
        mIam = ActivityManagerNative.getDefault();
    }

    @Override
    public final int getCount() {
        return mApps.size();
    }

    @Override
    public final Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public final long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.recents_item, parent, false);
        }

        ImageView icon = (ImageView) convertView.findViewById(R.id.image);
        TextView label = (TextView) convertView.findViewById(R.id.text);

        final RecentViewHolder holder = mApps.get(position);

        icon.setImageDrawable(holder.icon);
        icon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ActivityOptions options = ActivityOptions.makeBasic();// TODO does it need to init from persist data?
                try {
                    /* UNISOC: Modify for bug 1018161*/
                    if (!ActivityManager.getService().isInLockTaskMode()) {
                        mIam.startActivityFromRecents(holder.persistId, options == null ? null : options.toBundle());
                    } else {
                        Toast.makeText(mContext, R.string.screen_pinning_toast,Toast.LENGTH_SHORT).show();
                    }
                    /* @} */
                } catch (Exception e) {
                    Log.e(TAG, "launcher activity error", e);
                }
            }
        });
        label.setText(holder.title);

        return convertView;
    }

    public void setAppListInfo(List<RecentViewHolder> appList) {
        mApps = appList;
        notifyDataSetChanged();
    }

    public List<RecentViewHolder> getAppListInfo() {
        return mApps;
    }
}
