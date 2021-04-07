package org.calyxos.datura.adapter;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.R;
import org.calyxos.datura.settings.SettingsManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> implements Filterable {

    private static final String TAG = AppAdapter.class.getSimpleName();
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final SettingsManager mSettingsManager;
    private final List<ApplicationInfo> mTotalApps = new ArrayList<>();
    private List<ApplicationInfo> mAppsFiltered, mInstApps, mSysApps;
    private String currentSort = "name";
    private static boolean isSearching = false;
    private static String searchTerm = "";

    public AppAdapter(Context context, PackageManager packageManager, List<ApplicationInfo> instApps, List<ApplicationInfo> sysApps) {
        mInstApps = instApps;
        mSysApps = sysApps;

        //add a placeholder for header text
        ApplicationInfo ai = new ApplicationInfo();
        ai.processName = "Header1";
        instApps.add(0, ai);

        ApplicationInfo ai1 = new ApplicationInfo();
        ai1.processName = "Header2";
        sysApps.add(0, ai1);

        //merge lists
        mTotalApps.addAll(instApps);
        mTotalApps.addAll(sysApps);

        mAppsFiltered = mTotalApps;
        mContext = context;
        mPackageManager = packageManager;
        mSettingsManager = new SettingsManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_app_setting_accordion, parent, false);
        return new ViewHolder(view, mContext, mPackageManager, mSettingsManager);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo app = mAppsFiltered.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        if(mAppsFiltered != null)
            return mAppsFiltered.size();
        else return 0;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                if (charString.isEmpty()) {
                    isSearching = false;

                    mAppsFiltered = mTotalApps;
                } else {
                    List<ApplicationInfo> filteredList = new ArrayList<>();
                    for (ApplicationInfo app : mTotalApps) {
                        if (app.loadLabel(mPackageManager) != null) {
                            if (app.loadLabel(mPackageManager).toString().toLowerCase().contains(constraint.toString().toLowerCase())) {
                                filteredList.add(app);
                            }
                        }
                    }

                    isSearching = true;
                    searchTerm = charString;

                    mAppsFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mAppsFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mAppsFiltered = (ArrayList<ApplicationInfo>) results.values;

                notifyDataSetChanged();
            }
        };
    }

    public void sortListByLastUsed() {
        List<UsageStats> usageStats = getUsageStats();

        List<ApplicationInfo> instApps = removeNullLabelApps(mInstApps);
        instApps.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                long lhsTime = 0L, rhsTime = 0L;
                for (UsageStats us : usageStats) {
                    if (us.getPackageName().equals(lhs.packageName))
                        lhsTime = us.getLastTimeUsed();

                    if (us.getPackageName().equals(rhs.packageName))
                        rhsTime = us.getLastTimeUsed();
                }
                return Long.compare(rhsTime, lhsTime);//descending order
            }
        });
        ApplicationInfo ai = new ApplicationInfo();
        ai.processName = "Header1";
        instApps.add(0, ai);

        List<ApplicationInfo> sysApps = removeNullLabelApps(mSysApps);
        sysApps.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                long lhsTime = 0L, rhsTime = 0L;
                for (UsageStats us : usageStats) {
                    if (us.getPackageName().equals(lhs.packageName))
                        lhsTime = us.getLastTimeUsed();

                    if (us.getPackageName().equals(rhs.packageName))
                        rhsTime = us.getLastTimeUsed();
                }
                return Long.compare(rhsTime, lhsTime);//descending order
            }
        });
        ApplicationInfo ai1 = new ApplicationInfo();
        ai1.processName = "Header2";
        sysApps.add(0, ai1);

        //merge lists
        mAppsFiltered.clear();
        mAppsFiltered.addAll(instApps);
        mAppsFiltered.addAll(sysApps);

        currentSort = "last_used";

        notifyDataSetChanged();
    }

    public void sortResultListByLastUsed() {
        List<UsageStats> usageStats = getUsageStats();

        mAppsFiltered.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                long lhsTime = 0L, rhsTime = 0L;
                for (UsageStats us : usageStats) {
                    if (us.getPackageName().equals(lhs.packageName))
                        lhsTime = us.getLastTimeUsed();

                    if (us.getPackageName().equals(rhs.packageName))
                        rhsTime = us.getLastTimeUsed();
                }
                return Long.compare(rhsTime, lhsTime); //descending order
            }
        });

        currentSort = "last_used";

        notifyDataSetChanged();
    }

    public void sortListByName() {
        List<ApplicationInfo> instApps = removeNullLabelApps(mInstApps);
        instApps.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.loadLabel(mPackageManager).toString().compareTo(rhs.loadLabel(mPackageManager).toString());
            }
        });

        //add a placeholder for header text
        ApplicationInfo ai = new ApplicationInfo();
        ai.processName = "Header1";
        instApps.add(0, ai);

        List<ApplicationInfo> sysApps = removeNullLabelApps(mSysApps);
        sysApps.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.loadLabel(mPackageManager).toString().compareTo(rhs.loadLabel(mPackageManager).toString());
            }
        });

        ApplicationInfo ai1 = new ApplicationInfo();
        ai1.processName = "Header2";
        sysApps.add(0, ai1);

        //merge lists
        mAppsFiltered.clear();
        mAppsFiltered.addAll(instApps);
        mAppsFiltered.addAll(sysApps);

        currentSort = "name";

        notifyDataSetChanged();
    }

    public void sortResultListByName() {
        mAppsFiltered.sort(new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return lhs.loadLabel(mPackageManager).toString().compareTo(rhs.loadLabel(mPackageManager).toString());
            }
        });

        currentSort = "name";

        notifyDataSetChanged();
    }

    public void reApplySort() {
        if (currentSort.equals("name"))
            sortListByName();
        else sortListByLastUsed();
    }

    private List<ApplicationInfo> removeNullLabelApps(List<ApplicationInfo> list) {
        //Remove apps with null labels
        List<ApplicationInfo> temp = new ArrayList<>();
        for (ApplicationInfo app : list) {
            if (app.loadLabel(mPackageManager) != null)
                temp.add(app);
        }
        list = temp;

        return list;
    }

    private List<UsageStats> getUsageStats() {
        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, -1);
        long startTime = calendar.getTimeInMillis();
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        private Context mContext;
        private PackageManager mPackageManager;
        private SettingsManager mSettingsManager;
        private LinearLayout mLinearLayout, mAccordion;
        private SwitchCompat mMainToggle, mBackgroundToggle, mWifiToggle, mMobileToggle, mVpnToggle;
        private TextView appName, header, settingStatus;
        private ImageView appIcon, accordionIcon;

        private ApplicationInfo app;

        public ViewHolder(@NonNull View itemView, Context context, PackageManager packageManager, SettingsManager settingsManager) {
            super(itemView);

            mContext = context;
            mPackageManager = packageManager;
            mSettingsManager = settingsManager;

            mMainToggle = itemView.findViewById(R.id.main_toggle);
            mBackgroundToggle = itemView.findViewById(R.id.app_allow_background_toggle);
            mWifiToggle = itemView.findViewById(R.id.app_allow_wifi_toggle);
            mMobileToggle = itemView.findViewById(R.id.app_allow_mobile_toggle);
            mVpnToggle = itemView.findViewById(R.id.app_allow_vpn_toggle);

            appName = itemView.findViewById(R.id.app_name);
            settingStatus = itemView.findViewById(R.id.setting_status);
            appIcon = itemView.findViewById(R.id.app_icon);
            accordionIcon = itemView.findViewById(R.id.accordion_icon);

            mAccordion = itemView.findViewById(R.id.accordion);
            header = itemView.findViewById(R.id.list_header_text);
            mLinearLayout = itemView.findViewById(R.id.accordion_contents);

            itemView.setOnClickListener(this);
            accordionIcon.setOnClickListener(this);
            //set on click listeners instead of checked change for actual settings API calls because known issues
            //that comes with that
            mMainToggle.setOnClickListener(this);
            mBackgroundToggle.setOnClickListener(this);
            mWifiToggle.setOnClickListener(this);
            mMobileToggle.setOnClickListener(this);
            mVpnToggle.setOnClickListener(this);

            //set check changed here for status text updates
            mMainToggle.setOnCheckedChangeListener(this);
            mBackgroundToggle.setOnCheckedChangeListener(this);
            mWifiToggle.setOnCheckedChangeListener(this);
            mMobileToggle.setOnCheckedChangeListener(this);
            mVpnToggle.setOnCheckedChangeListener(this);
        }

        public void bind(ApplicationInfo app) {
            this.app = app;
            //check for header placeholder
            if (app.processName.equals("Header1") || app.processName.equals("Header2")) {
                header.setVisibility(View.VISIBLE);
                mAccordion.setVisibility(View.GONE);

                header.setText(app.processName.equals("Header1") ? mContext.getString(R.string.installed_apps) : mContext.getString(R.string.system_apps));
            } else {
                header.setVisibility(View.GONE);
                mAccordion.setVisibility(View.VISIBLE);

                try {
                    //here just in case
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfo(app.packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                appIcon.setImageDrawable(app.loadIcon(mPackageManager));
                String name = app.loadLabel(mPackageManager).toString();
                if (isSearching) {
                    int ind = name.toLowerCase().indexOf(searchTerm.toLowerCase());
                    appName.setText(Html.fromHtml(name.replaceAll("(?i)" + searchTerm,
                            "<b>" + name.substring(ind, (ind + searchTerm.length())) + "</b>")));
                } else
                    appName.setText(name);

                //initialize toggle states
                //check if it's an app
                //if (UserHandle.isApp(app.uid)) {
                //get background data status
                mBackgroundToggle.setChecked(!mSettingsManager.isBlacklisted(app.uid));

                //get wifi status
                mWifiToggle.setChecked(!mSettingsManager.getAppRestrictWifi(app.uid));

                //get mobile status
                mMobileToggle.setChecked(!mSettingsManager.getAppRestrictCellular(app.uid));

                //get vpn status
                mVpnToggle.setChecked(!mSettingsManager.getAppRestrictVpn(app.uid));

                //initialize main toggle
                mMainToggle.setChecked(!mSettingsManager.getAppRestrictAll(app.uid));

                // Set status text
                setStatusText();

                /*} else {
                mMainToggle.setEnabled(false);
                mBackgroundToggle.setEnabled(false);
                mWifiToggle.setEnabled(false);
                mMobileToggle.setEnabled(false);
                mVpnToggle.setEnabled(false);
                }*/
            }
        }

        @Override
        public void onClick(View v) {
            if (v.equals(itemView) || v.getId() == R.id.accordion_icon) {
                if (mLinearLayout.getVisibility() == View.VISIBLE) {
                    mLinearLayout.setVisibility(View.GONE);
                    accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_down, null));
                } else {
                    mLinearLayout.setVisibility(View.VISIBLE);
                    accordionIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_accordion_up, null));
                }
            } else {
                try {
                    switch (v.getId()) {
                        case R.id.main_toggle:
                            if (mMainToggle.isChecked()) {
                                mSettingsManager.setAppRestrictAll(app.uid, false);
                                // Re-enable all other toggles
                                mBackgroundToggle.setEnabled(true);
                                mWifiToggle.setEnabled(true);
                                mMobileToggle.setEnabled(true);
                                mVpnToggle.setEnabled(true);
                            }
                            else {
                                mSettingsManager.setAppRestrictAll(app.uid, true);
                                // Disable all other toggles
                                mBackgroundToggle.setEnabled(false);
                                mWifiToggle.setEnabled(false);
                                mMobileToggle.setEnabled(false);
                                mVpnToggle.setEnabled(false);
                            }
                            break;

                        case R.id.app_allow_background_toggle:
                            if (mBackgroundToggle.isChecked())
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, false);
                            else
                                mSettingsManager.setIsBlacklisted(app.uid, app.packageName, true);
                            break;

                        case R.id.app_allow_wifi_toggle:
                            if (mWifiToggle.isChecked())
                                mSettingsManager.setAppRestrictWifi(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictWifi(app.uid, true);
                            break;

                        case R.id.app_allow_mobile_toggle:
                            if (mMobileToggle.isChecked())
                                mSettingsManager.setAppRestrictCellular(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictCellular(app.uid, true);
                            break;

                        case R.id.app_allow_vpn_toggle:
                            if (mVpnToggle.isChecked())
                                mSettingsManager.setAppRestrictVpn(app.uid, false);
                            else
                                mSettingsManager.setAppRestrictVpn(app.uid, true);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());

                    //disable that app's toggles
                    mMainToggle.setEnabled(false);
                    mBackgroundToggle.setEnabled(false);
                    mWifiToggle.setEnabled(false);
                    mMobileToggle.setEnabled(false);
                    mVpnToggle.setEnabled(false);

                    Toast.makeText(mContext, mContext.getString(R.string.error_setting_preference, appName.getText()), Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            // Set status text
            setStatusText();
        }

        private void setStatusText() {
            // Keep it as-is if all toggles are checkec
            if (mMainToggle.isChecked() && mBackgroundToggle.isChecked() && mWifiToggle.isChecked() && mMobileToggle.isChecked() && mVpnToggle.isChecked()) {
                settingStatus.setVisibility(View.VISIBLE);
                return;
            }
            // It's no longer "default settings" if even a single toggle is changed, hide the status
            settingStatus.setVisibility(View.GONE);
        }
    }
}
