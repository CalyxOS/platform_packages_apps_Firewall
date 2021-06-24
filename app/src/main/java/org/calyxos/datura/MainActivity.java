package org.calyxos.datura;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.adapter.AppAdapter;
import org.calyxos.datura.adapter.GlobalSettingsAdapter;
import org.calyxos.datura.fragment.AboutDialogFragment;
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
    private SharedPreferences sharedPreferences;

    private static MainActivity mainActivity;

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

        sharedPreferences = getSharedPreferences(Constants.SORT_PREFERENCE, MODE_PRIVATE);

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

        //filter out packages without internet access permissions
        List<ApplicationInfo> internetPackages = new ArrayList<>();
        for (ApplicationInfo applicationInfo : packages) {
            if (isSystemApp(applicationInfo)) {
                try {
                    PackageInfo packageInfo = pm.getPackageInfo(applicationInfo.packageName, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
                    if (packageInfo.requestedPermissions != null && hasInternetPermission(packageInfo.requestedPermissions)) {
                        internetPackages.add(applicationInfo);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        packages = internetPackages;

        //filter system and installed apps
        List<ApplicationInfo> sysApps = new ArrayList<>();
        List<ApplicationInfo> instApps = new ArrayList<>();

        for (ApplicationInfo ai : packages) {
            // Skip anything that isn't an "app" since we can't set policies for those, as
            // the framework code which handles setting the policies has a similar check.
            if (Util.isApp(ai.uid)) {
                if (isSystemApp(ai)) {
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

        //initialize sort by name menu item
        MenuItem sortItem = menu.findItem(R.id.action_sort);
        String sort = sharedPreferences.getString(Constants.SORT_BY, Constants.NAME);
        if (sort.equals(Constants.NAME)) {
            mAppAdapter.sortListByName();
            sortItem.setTitle(getString(R.string.sort_by_last_used));
        } else {
            mAppAdapter.sortListByLastUsed();
            sortItem.setTitle(getString(R.string.sort_by_name));
        }

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
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearchIcon.getWindowToken(), 0);
                } else
                    onBackPressed();
                break;
            }

            case R.id.action_sort: {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (item.getTitle().equals(getString(R.string.sort_by_name))) {
                    //Check and call a different sort function for search result list
                    if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
                        mAppAdapter.sortResultListByName();
                    else {
                        mAppAdapter.sortListByName();
                        editor.putString(Constants.SORT_BY, Constants.NAME).apply();
                    }

                    item.setTitle(getString(R.string.sort_by_last_used));
                } else {
                    if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
                        mAppAdapter.sortResultListByLastUsed();
                    else {
                        mAppAdapter.sortListByLastUsed();
                        editor.putString(Constants.SORT_BY, Constants.LAST_USED).apply();
                    }

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

    private boolean isSystemApp(ApplicationInfo ai) {
        return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    private boolean hasInternetPermission(String[] permissions) {
        for (String s : permissions) {
            if (s.equalsIgnoreCase(Manifest.permission.INTERNET))
                return true;
        }
        return false;
    }

    public static MainActivity getInstance() {
        return mainActivity;
    }

    public void notifyDataSetChanged() {
        if (mAppAdapter != null)
            mAppAdapter.notifyDataSetChanged(); //NOTE include this in a thread/service as well
    }

    public GlobalSettingsAdapter getGlobalSettingsAdapter() {
        return mGlobalSettingsAdapter;
    }
}
