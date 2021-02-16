package org.calyxos.firewall;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.NetworkPolicyManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.firewall.adapter.AppsAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView, mRecyclerView1;
    private AppsAdapter mAdapter, mAdapter1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.system_apps);
        mRecyclerView1 = findViewById(R.id.installed_apps);

    }

    @Override
    protected void onResume() {
        super.onResume();

        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

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

        if (mAdapter == null) {
            mAdapter = new AppsAdapter(this, pm, sysApps);
            mRecyclerView.setAdapter(mAdapter);
        } else
            mAdapter.notifyDataSetChanged();

        if (mAdapter1 == null) {
            mAdapter1 = new AppsAdapter(this, pm, instApps);
            mRecyclerView1.setAdapter(mAdapter1);
        } else
            mAdapter1.notifyDataSetChanged();
    }
}