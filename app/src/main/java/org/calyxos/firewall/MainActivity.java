package org.calyxos.firewall;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.firewall.adapter.AppAdapter;

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        //To avoid search list and real list mix up
        if (mSearchBar.getVisibility() == View.VISIBLE && !mSearchBar.getText().toString().isEmpty())
            return;

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

        if (mAppAdapter == null) {
            mAppAdapter = new AppAdapter(this, pm, instApps, sysApps);
            mAppList.setAdapter(mAppAdapter);
        } else
            mAppAdapter.notifyDataSetChanged();
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
