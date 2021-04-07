package org.calyxos.firewall.settings;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.util.SparseIntArray;

import static android.net.NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_ALL;
import static android.net.NetworkPolicyManager.POLICY_REJECT_CELLULAR;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_REJECT_VPN;
import static android.net.NetworkPolicyManager.POLICY_REJECT_WIFI;

public class SettingsManager {

    private static final String TAG = SettingsManager.class.getSimpleName();
    private Context mContext;
    private NetworkPolicyManager mPolicyManager;
    private SparseIntArray mUidPolicies = new SparseIntArray();
    private boolean mWhitelistInitialized;
    private boolean mBlacklistInitialized;

    public SettingsManager(Context context) {
        mContext =  context;
        mPolicyManager = NetworkPolicyManager.from(context);
    }

    public void setIsBlacklisted(int uid, String packageName, boolean blacklisted) throws IllegalArgumentException {
        final int policy = blacklisted ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE;
        mUidPolicies.put(uid, policy);
        if (blacklisted) {
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
        } else {
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
        }
        mPolicyManager.removeUidPolicy(uid, POLICY_ALLOW_METERED_BACKGROUND);
    }

    public boolean isBlacklisted(int uid) {
        loadBlacklist();
        return mUidPolicies.get(uid, POLICY_NONE) == POLICY_REJECT_METERED_BACKGROUND;
    }

    private void loadBlacklist() {
        if (mBlacklistInitialized) {
            return;
        }
        for (int uid : mPolicyManager.getUidsWithPolicy(POLICY_REJECT_METERED_BACKGROUND)) {
            mUidPolicies.put(uid, POLICY_REJECT_METERED_BACKGROUND);
        }
        mBlacklistInitialized = true;
    }

    public boolean getAppRestrictAll(int uid) {
        return getAppRestriction(uid, POLICY_REJECT_ALL);
    }

    public boolean getAppRestrictCellular(int uid) {
        return getAppRestriction(uid, POLICY_REJECT_CELLULAR);
    }

    public boolean getAppRestrictVpn(int uid) {
        return getAppRestriction(uid, POLICY_REJECT_VPN);
    }

    public boolean getAppRestrictWifi(int uid) {
        return getAppRestriction(uid, POLICY_REJECT_WIFI);
    }

    private boolean getAppRestriction(int uid, int policy) {
        final int uidPolicy = mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & policy) != 0;
    }

    public void setAppRestrictAll(int uid, boolean restrict) throws RuntimeException {
        setAppRestriction(uid, POLICY_REJECT_ALL, restrict);
    }

    public void setAppRestrictCellular(int uid, boolean restrict) throws RuntimeException {
        setAppRestriction(uid, POLICY_REJECT_CELLULAR, restrict);
    }

    public void setAppRestrictVpn(int uid, boolean restrict) throws  RuntimeException {
        setAppRestriction(uid, POLICY_REJECT_VPN, restrict);
    }

    public void setAppRestrictWifi(int uid, boolean restrict) throws RuntimeException {
        setAppRestriction(uid, POLICY_REJECT_WIFI, restrict);
    }

    private void setAppRestriction(int uid, int policy, boolean restrict) throws RuntimeException {
        if (restrict) {
            mPolicyManager.addUidPolicy(uid, policy);
        } else {
            mPolicyManager.removeUidPolicy(uid, policy);
        }
    }
}
