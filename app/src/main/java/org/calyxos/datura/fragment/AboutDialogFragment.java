package org.calyxos.datura.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.calyxos.datura.R;

public class AboutDialogFragment extends DialogFragment {

    public static final String TAG = AboutDialogFragment.class.getSimpleName();
    private TextView versionView, licenseView, authorView, designView, sponsorView;

    public AboutDialogFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container);

        versionView = view.findViewById(R.id.versionView);
        licenseView = view.findViewById(R.id.licenseView);
        authorView = view.findViewById(R.id.authorView);
        designView = view.findViewById(R.id.designView);
        sponsorView = view.findViewById(R.id.sponsorView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MovementMethod linkMovementMethod = LinkMovementMethod.getInstance();
        licenseView.setMovementMethod(linkMovementMethod);
        authorView.setMovementMethod(linkMovementMethod);
        designView.setMovementMethod(linkMovementMethod);
        sponsorView.setMovementMethod(linkMovementMethod);
    }
}
