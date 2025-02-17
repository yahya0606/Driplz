package com.onecodeman.driply.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;


public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    private AppOpenAd appOpenAd;
    private boolean isAdShowing = false;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        FirebaseApp.initializeApp(this);
        //loadOpenAppAd();
    }

    private void loadOpenAppAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(this, "ca-app-pub-7662096701692256/8500379530", adRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd ad) {
                        appOpenAd = ad;
                        showOpenAppAd();
                        Log.d("AdMob", "Open app ad loaded successfully.");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.e("AdMob", "Failed to load open app ad: " + loadAdError.getMessage());
                    }
                });
    }

    public void showOpenAppAd() {
        if (appOpenAd != null && !isAdShowing && currentActivity != null) {
            isAdShowing = true;

            // Set the FullScreenContentCallback to reset the ad state
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    isAdShowing = false;
                    appOpenAd = null; // Clear the ad reference
                    Log.d("AdMob", "Ad dismissed.");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    isAdShowing = false;
                    Log.e("AdMob", "Ad failed to show: " + adError.getMessage());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isAdShowing = true;
                    Log.d("AdMob", "Ad is showing.");
                }
            });

            // Show the ad
            appOpenAd.show(currentActivity);
        } else {
            Log.d("AdMob", "Open app ad is not ready or activity is null.");
        }
    }

    // Register the current activity
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    // Other lifecycle callbacks you can override if needed
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override
    public void onActivityStarted(Activity activity) {}
    @Override
    public void onActivityStopped(Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override
    public void onActivityDestroyed(Activity activity) {}
}
