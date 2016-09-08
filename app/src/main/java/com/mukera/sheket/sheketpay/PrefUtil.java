package com.mukera.sheket.sheketpay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by fuad on 9/8/16.
 */
public class PrefUtil {
    private static final String pref_user_id = "pref_user_id";
    public static void setUserId(Context context, long user_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(pref_user_id, user_id);
        editor.commit();
    }

    public static long getUserId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(pref_user_id, -1);
    }

    public static boolean isUserLoggedIn(Context context) {
        return getUserId(context) != -1;
    }

    public static void logoutUser(Context context) {
        setUserId(context, -1);
        setLoginCookie(context, "");
    }

    private static final String pref_login_cookie = "pref_login_cookie";
    public static void setLoginCookie(Context context, String cookie) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(pref_login_cookie, cookie);
        editor.commit();
    }

    public static String getLoginCookie(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(pref_login_cookie, "");
    }
}
