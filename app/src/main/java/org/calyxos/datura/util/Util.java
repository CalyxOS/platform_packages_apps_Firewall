package org.calyxos.datura.util;

public class Util {

    public static final int PER_USER_RANGE = 100000;
    public static final int FIRST_APPLICATION_UID = 10000;
    public static final int LAST_APPLICATION_UID = 19999;

    public static boolean isApp(int uid) {
        if (uid > 0) {
            final int appId = uid % PER_USER_RANGE;
            return appId >= FIRST_APPLICATION_UID && appId <= LAST_APPLICATION_UID;
        } else {
            return false;
        }
    }
}
