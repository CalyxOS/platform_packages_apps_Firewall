package org.calyxos.datura;

import android.content.Context;
import android.content.pm.ApplicationInfo;
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
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.adapter.AppAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mAppList;
    private AppAdapter mAppAdapter;
    private EditText mSearchBar;
    private ImageView mSearchIcon, mSearchClear;

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
            if (UserHandle.isApp(ai.uid)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    sysApps.add(ai);
                } else {
                    instApps.add(ai);
                }
            }
        }

        if (mAppAdapter == null) {
            mAppAdapter = new AppAdapter(this, pm, instApps, sysApps);
            mAppList.setAdapter(mAppAdapter);
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
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search_icon) {
            mSearchIcon.setVisibility(View.GONE);
            mSearchBar.setVisibility(View.VISIBLE);
            mSearchBar.requestFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mSearchBar, InputMethodManager.SHOW_IMPLICIT);
        }

        if (v.getId() == R.id.search_clear) {
            mSearchBar.setText("");
        }
    }
}
