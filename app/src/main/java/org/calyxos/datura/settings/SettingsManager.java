package org.calyxos.datura.settings;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkPolicyManager;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.provider.Settings;
import android.security.NetworkSecurityPolicy;
import android.security.net.config.ApplicationConfig;
import android.util.SparseIntArray;

import org.calyxos.datura.util.Util;

import java.util.ArrayList;
import java.util.List;

//import libcore.net.NetworkSecurityPolicy;

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
    private final INetworkManagementService netd;
    private SparseIntArray mUidPolicies = new SparseIntArray();
    private boolean mWhitelistInitialized;
    private boolean mBlacklistInitialized;

    public SettingsManager(Context context) {
        mContext =  context;
        mPolicyManager = NetworkPolicyManager.from(context);
        netd = INetworkManagementService.Stub.asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
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

    public boolean getAppRestrictCleartext(int uid) throws RemoteException {
        final int uidPolicy = netd.getUidCleartextNetworkPolicy(uid);
        return (uidPolicy == StrictMode.NETWORK_POLICY_ACCEPT);
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

    public boolean isPrivateDNSEnabled() {
        ConnectivityManager connectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            if (linkProperties != null) {
                return linkProperties.isPrivateDnsActive();
            }
        }
        return false;
    }

    public void blockCleartextTraffic(boolean block) {
        SystemProperties.set(StrictMode.GLOBAL_CLEARTEXT_PROPERTY, Boolean.toString(block));
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.CLEARTEXT_NETWORK_POLICY,
                block? StrictMode.NETWORK_POLICY_REJECT : StrictMode.NETWORK_POLICY_ACCEPT);
    }

    public boolean isCleartextBlocked() {
        return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.CLEARTEXT_NETWORK_POLICY, StrictMode.NETWORK_POLICY_ACCEPT)
                != StrictMode.NETWORK_POLICY_ACCEPT;
    }

    public void allowAppCleartext(int uid, boolean allow) throws RemoteException {
        setAppCleartextPolicy(uid, allow ? StrictMode.NETWORK_POLICY_ACCEPT : StrictMode.NETWORK_POLICY_REJECT);
    }

    private void setAppCleartextPolicy(int uid, int policy) throws RemoteException {
        netd.setUidCleartextNetworkPolicy(uid, policy);
    }

    /**
     * Returns whether cleartext network traffic (HTTP) is permitted for the provided package
     */
    public boolean isCleartextTrafficPermitted(String packageName) {
        ApplicationConfig appConfig;
        try {
            appConfig = NetworkSecurityPolicy.getApplicationConfigForPackage(mContext, packageName);
        } catch (PackageManager.NameNotFoundException e) {
            // Unknown package -- fail for safety
            return false;
        }
        return appConfig.isCleartextTrafficPermitted();
    }

    public boolean isAppsBackgroundDataBlocked(List<ApplicationInfo> apps) {
        //if all apps have their background data blocked then the global toggle should be unchecked as this
        //means background data is blocked for all apps
        List<Boolean> appStatuses = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                appStatuses.add(isBlacklisted(app.uid));
        }

        return !appStatuses.contains(false);
    }

    public boolean isAppsWIFIDataBlocked(List<ApplicationInfo> apps) {
        //if all apps have their WIFI data blocked then the global toggle should be unchecked as this
        //means background data is blocked for all apps
        List<Boolean> appStatuses = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                appStatuses.add(getAppRestrictWifi(app.uid));
        }

        return !appStatuses.contains(false);

    }

    public boolean isAppsMobileDataBlocked(List<ApplicationInfo> apps) {
        //if all apps have their background data blocked then the global toggle should be unchecked as this
        //means background data is blocked for all apps
        List<Boolean> appStatuses = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                appStatuses.add(getAppRestrictCellular(app.uid));
        }

        return !appStatuses.contains(false);
    }

    public boolean isAppsVPNDataBlocked(List<ApplicationInfo> apps) {
        //if all apps have their background data blocked then the global toggle should be unchecked as this
        //means background data is blocked for all apps
        List<Boolean> appStatuses = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                appStatuses.add(getAppRestrictVpn(app.uid));
        }

        return !appStatuses.contains(false);
    }

    public void allowAppsNetworkAccess(List<ApplicationInfo> apps, boolean checked) {
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                setAppRestrictAll(app.uid, !checked);
        }
    }

    public void allowAppsBackgroundData(List<ApplicationInfo> apps, boolean checked) {
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                setIsBlacklisted(app.uid, app.packageName, !checked);
        }
    }

    public void allowAppsWIFIData(List<ApplicationInfo> apps, boolean checked) {
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                setAppRestrictWifi(app.uid, !checked);
        }
    }

    public void allowAppsMobileData(List<ApplicationInfo> apps, boolean checked) {
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                setAppRestrictCellular(app.uid, !checked);
        }
    }

    public void allowAppsVPNData(List<ApplicationInfo> apps, boolean checked) {
        for (ApplicationInfo app : apps) {
            if (Util.isApp(app.uid))
                setAppRestrictVpn(app.uid, !checked);
        }
    }

}
