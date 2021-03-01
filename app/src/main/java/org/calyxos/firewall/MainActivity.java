package org.calyxos.firewall;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.firewall.adapter.AppsAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mSystemAppsList, mInstalledAppsList;
    private AppsAdapter mSystemAppsAdapter, mInstalledAppsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSystemAppsList = findViewById(R.id.system_apps);
        mInstalledAppsList = findViewById(R.id.installed_apps);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        //List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_INSTRUMENTATION);//Works in Kotlin but not in java?

        //filter system and installed apps
        List<ApplicationInfo> sysApps = new ArrayList<>();
        List<ApplicationInfo> instApps = new ArrayList<>();

        for (ApplicationInfo ai : packages) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                sysApps.add(ai);
            } else {
                instApps.add(ai);
            }
        }

        if (mSystemAppsAdapter == null) {
            mSystemAppsAdapter = new AppsAdapter(this, pm, sysApps);
            mSystemAppsList.setAdapter(mSystemAppsAdapter);
        } else
            mSystemAppsAdapter.notifyDataSetChanged();

        if (mInstalledAppsAdapter == null) {
            mInstalledAppsAdapter = new AppsAdapter(this, pm, instApps);
            mInstalledAppsList.setAdapter(mInstalledAppsAdapter);
        } else
            mInstalledAppsAdapter.notifyDataSetChanged();
    }
}