package com.onecodeman.driply.utils;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PointsManager {

    private static final String TAG = "PointsManager";

    // Method to add points for an action
    public static void addPoints(String userId, int pointsToAdd, PointsCallback callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int currentPoints = snapshot.child("points").getValue(Integer.class) != null ?
                            snapshot.child("points").getValue(Integer.class) : 0;
                    int dailyPoints = snapshot.child("dailyPoints").getValue(Integer.class) != null ?
                            snapshot.child("dailyPoints").getValue(Integer.class) : 0;
                    int multiplier = snapshot.child("capMultiplier").getValue(Integer.class) != null ?
                            snapshot.child("capMultiplier").getValue(Integer.class) : 1; // Default multiplier
                    int baseDailyCap = snapshot.child("dailyCap").getValue(Integer.class) != null ?
                            snapshot.child("dailyCap").getValue(Integer.class) : 50; // Default daily cap
                    int dailyCap = baseDailyCap * multiplier; // Apply multiplier
                    String lastReset = snapshot.child("lastReset").getValue(String.class);

                    // Reset daily points if it's a new day
                    String today = getCurrentDate();
                    if (!today.equals(lastReset)) {
                        dailyPoints = 0;
                        userRef.child("dailyPoints").setValue(0);
                        userRef.child("lastReset").setValue(today);
                    }

                    // Calculate points to add
                    int remainingPoints = dailyCap - dailyPoints;
                    int pointsToGrant = Math.min(pointsToAdd, remainingPoints);

                    if (pointsToGrant > 0) {
                        userRef.child("points").setValue(currentPoints + pointsToGrant);
                        userRef.child("dailyPoints").setValue(dailyPoints + pointsToGrant)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Points added: " + pointsToGrant);
                                    if (callback != null) callback.onPointsAdded(pointsToGrant);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update points: " + e.getMessage());
                                    if (callback != null) callback.onError(e.getMessage());
                                });
                    } else {
                        Log.d(TAG, "Daily cap reached. No points added.");
                        if (callback != null) callback.onCapReached();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching user data: " + error.getMessage());
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    // Helper method to get the current date as a string
    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Callback interface
    public interface PointsCallback {
        void onPointsAdded(int pointsAdded);

        void onCapReached();

        void onError(String error);
    }
}
