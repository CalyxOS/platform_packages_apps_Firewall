package org.calyxos.datura.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.calyxos.datura.MainActivity;
import org.calyxos.datura.R;
import org.calyxos.datura.settings.SettingsManager;

public class GlobalSettingsAdapter extends RecyclerView.Adapter<GlobalSettingsAdapter.ViewHolder> {

    private static final String TAG = GlobalSettingsAdapter.class.getSimpleName();
    private final SettingsManager mSettingsManager;

    public GlobalSettingsAdapter(Context context, PackageManager packageManager) {
        mSettingsManager = new SettingsManager(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_global_settings, parent, false);
        return new ViewHolder(view, mSettingsManager);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final SettingsManager mSettingsManager;
        private final SwitchCompat mGlobalClrTextToggle;

        public ViewHolder(@NonNull View itemView, SettingsManager settingsManager) {
            super(itemView);

            mSettingsManager = settingsManager;

            mGlobalClrTextToggle = itemView.findViewById(R.id.global_cleartext_toggle);


            //check if Private DNS is enabled
            mGlobalClrTextToggle.setEnabled(mSettingsManager.isPrivateDNSEnabled());
            //initialize cleartext toggle state
            mGlobalClrTextToggle.setChecked(mSettingsManager.isCleartextBlocked());

            //set on click listeners instead of checked change for actual settings API calls because known issues
            //that comes with that
            mGlobalClrTextToggle.setOnClickListener(this);
        }

        public void bind(ApplicationInfo app) {

        }

        @Override
        public void onClick(View v) {
            if (v.equals(mGlobalClrTextToggle)) {
                mSettingsManager.blockCleartextTraffic(mGlobalClrTextToggle.isChecked());
                //call a main activity function that refreshes the list of apps
                MainActivity.getInstance().notifyDataSetChanged();
            }
        }
    }
}
