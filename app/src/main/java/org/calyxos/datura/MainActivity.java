package org.calyxos.datura;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.adapter.AppAdapter;
import org.calyxos.datura.adapter.GlobalSettingsAdapter;
import org.calyxos.datura.fragment.AboutDialogFragment;
import org.calyxos.datura.service.DefaultConfigService;
import org.calyxos.datura.settings.SettingsManager;
import org.calyxos.datura.util.Constants;
import org.calyxos.datura.util.Util;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mAppList;
    private AppAdapter mAppAdapter;
    private GlobalSettingsAdapter mGlobalSettingsAdapter;
    private EditText mSearchBar;
    private ImageView mSearchIcon, mSearchClear;

    private static MainActivity mainActivity;


    public void startDefaultConfigService () {
        Log.d(TAG, "Service about to be started");
        Intent serviceIntent = new Intent(MainActivity.this, DefaultConfigService.class);
        //Service connection to bind the service to this context because of startForegroundService issues
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.e(TAG, "Service connected");
                DefaultConfigService.ServiceBinder binder = (DefaultConfigService.ServiceBinder) service;
                DefaultConfigService configService = binder.getService();
                startForegroundService(serviceIntent);
                configService.startForeground(Constants.DEFAULT_CONFIG_NOTIFICATION_ID, configService.getNotification());
            }

            @Override
            public void onBindingDied(ComponentName name) {
                Log.w(TAG, "Binding has dead.");
            }

            @Override
            public void onNullBinding(ComponentName name) {
                Log.w(TAG, "Bind was null.");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w(TAG, "Service is disconnected..");
            }
        };

        try {
            Log.d(TAG, "Service bound");
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (RuntimeException ignored) {
            Log.d(TAG, "Runtime exception");
            //Use the normal way and accept it will fail sometimes
            startForegroundService(serviceIntent);
        }
    }

    public void stopDefaultConfigService() {
        stopService(new Intent(this, DefaultConfigService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.app_name);

        mSearchIcon = findViewById(R.id.search_icon);
        mSearchIcon.setOnClickListener(this);
        mSearchBar = findViewById(R.id.search_bar);
        mSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSearchBar.getText().toString().isEmpty())
                    mSearchClear.setVisibility(View.GONE);
                else
                    mSearchClear.setVisibility(View.VISIBLE);

                //search list
                if (mAppAdapter != null)
                    mAppAdapter.getFilter().filter(mSearchBar.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mSearchClear = findViewById(R.id.search_clear);
        mSearchClear.setOnClickListener(this);

        mAppList = findViewById(R.id.app_list);

        mainActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final PackageManager pm = getPackageManager();
        final UserManager um = getSystemService(UserManager.class);

        //To avoid search result list and real list mix up
        if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
            return;

        List<ApplicationInfo> packages = new ArrayList<>();

        for (UserInfo user : um.getProfiles(UserHandle.myUserId())) {
            packages.addAll(pm.getInstalledApplicationsAsUser(PackageManager.GET_META_DATA, user.id));
        }

        //filter system and installed apps
        List<ApplicationInfo> sysApps = new ArrayList<>();
        List<ApplicationInfo> instApps = new ArrayList<>();

        for (ApplicationInfo ai : packages) {
            // Skip anything that isn't an "app" since we can't set policies for those, as
            // the framework code which handles setting the policies has a similar check.
            if (Util.isApp(ai.uid)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    sysApps.add(ai);
                } else {
                    instApps.add(ai);
                }
            }
        }

        if (mAppAdapter == null) {
            mAppAdapter = new AppAdapter(this, pm, instApps, sysApps);
            mGlobalSettingsAdapter = new GlobalSettingsAdapter(this, pm);
            ConcatAdapter concatAdapter = new ConcatAdapter(mGlobalSettingsAdapter, mAppAdapter);
            mAppList.setAdapter(concatAdapter);
        } else
            mAppAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.firewall_settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (mSearchBar.getVisibility() == View.VISIBLE) {
                    mSearchBar.setText("");
                    mSearchBar.setVisibility(View.GONE);
                    mSearchClear.setVisibility(View.GONE);
                    mSearchIcon.setVisibility(View.VISIBLE);

                    //reset list
                    mAppAdapter.getFilter().filter(mSearchBar.getText().toString());

                    //remove virtual keypad
                    //mSearchIcon.requestFocus();
                    InputMethodManager imm =(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearchIcon.getWindowToken(), 0);
                } else
                    onBackPressed();
                break;
            }

            case R.id.action_sort: {
                if (item.getTitle().equals(getString(R.string.sort_by_name))) {
                    //Check and call a different sort function for search result list
                    if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
                        mAppAdapter.sortResultListByName();
                    else mAppAdapter.sortListByName();

                    item.setTitle(getString(R.string.sort_by_last_used));
                } else {
                    if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
                        mAppAdapter.sortResultListByLastUsed();
                    else mAppAdapter.sortListByLastUsed();

                    item.setTitle(getString(R.string.sort_by_name));
                }

                break;
            }
            
            case R.id.action_about: {
                new AboutDialogFragment().show(getSupportFragmentManager(), AboutDialogFragment.TAG);
                break;
            }

            case R.id.action_advanced: {
                startActivity(new Intent(this, GlobalSettingsActivity.class));
                break;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.search_icon: {
                mSearchIcon.setVisibility(View.GONE);
                mSearchBar.setVisibility(View.VISIBLE);
                mSearchBar.requestFocus();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mSearchBar, InputMethodManager.SHOW_IMPLICIT);

                break;
            }

            case R.id.search_clear: {
                mSearchBar.setText("");
                break;
            }
        }
    }

    public static MainActivity getInstance() {
        return mainActivity;
    }

    public void notifyDataSetChanged() {
        if (mAppAdapter != null)
            mAppAdapter.notifyDataSetChanged(); //NOTE include this in a thread/service as well
    }
}
