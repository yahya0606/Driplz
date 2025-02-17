package com.onecodeman.driply.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onecodeman.driply.R;
import com.onecodeman.driply.adapters.LeaderboardAdapter;
import com.onecodeman.driply.models.LeaderboardEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> leaderboard;

    private DatabaseReference usersRef;

    private AdView adView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        leaderboardRecyclerView = view.findViewById(R.id.leaderboard_recycler_view);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        leaderboard = new ArrayList<>();
        adapter = new LeaderboardAdapter(leaderboard);
        leaderboardRecyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize Mobile Ads
        initializeAdMob();

        // Find the AdView
        adView = view.findViewById(R.id.adView);
        // Load an ad
        loadBannerAd();

        fetchLeaderboardData();

        return view;
    }

    private void initializeAdMob() {
        if (getContext() != null) {
            MobileAds.initialize(getContext(), initializationStatus -> {
                Log.d("AdMob", "Mobile Ads SDK initialized.");
            });
        } else {
            Log.e("AdInitialization", "Context is null. Unable to initialize Mobile Ads SDK.");
        }
    }

    private void loadBannerAd() {
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            Log.e("AdView", "AdView is null. Unable to load banner ad.");
        }
    }

    private void fetchLeaderboardData() {
        usersRef.orderByChild("points").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                leaderboard.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    Integer points = userSnapshot.child("points").getValue(Integer.class);

                    if (username != null && points != null) {
                        leaderboard.add(new LeaderboardEntry(username, points));
                    }
                }
                Collections.sort(leaderboard, (a, b) -> b.getPoints() - a.getPoints()); // Sort in descending order
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load leaderboard.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
