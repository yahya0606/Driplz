package com.onecodeman.driply.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onecodeman.driply.NotificationsActivity;
import com.onecodeman.driply.R;
import com.onecodeman.driply.adapters.CardStackAdapter;
import com.onecodeman.driply.models.FashionPost;
import com.onecodeman.driply.utils.PointsManager;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeckFragment extends Fragment {

    private CardStackView cardStackView;
    private CardStackAdapter adapter;
    private CardStackLayoutManager manager;
    private DatabaseReference postsRef;
    private String currentUserId; // User's unique identifier
    private int swings = 0; // Track the number of card swipes
    private InterstitialAd interstitialAd; // Interstitial ad
    private Set<String> notifiedPosts = new HashSet<>(); // Track notified posts
    private Map<String, String> userCache = new HashMap<>(); // Cache usernames

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck, container, false);

        // Initialize CardStackView and its manager
        cardStackView = view.findViewById(R.id.cardStackView);
        manager = setupCardStackManager();

        cardStackView.setLayoutManager(manager);

        // Initialize adapter
        adapter = new CardStackAdapter(new ArrayList<>(), getContext());
        cardStackView.setAdapter(adapter);

        // Load interstitial ad
        //loadInterstitialAd();

        // Initialize Firebase references
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("DeckFragment", "FirebaseUser is null. User not logged in.");
        } else {
            currentUserId = currentUser.getUid();
            loadUserCache(); // Preload usernames
            loadPosts(); // Fetch and display posts
        }

        return view;
    }

    private CardStackLayoutManager setupCardStackManager() {
        return new CardStackLayoutManager(requireContext(), new CardStackListener() {
            @Override
            public void onCardSwiped(Direction direction) {
                swings++;
                int currentPosition = manager.getTopPosition() - 1;
                FashionPost currentPost = adapter.getPosts().get(currentPosition);

                if (currentPost != null) {
                    handleCardSwipe(currentPost, direction);
                }
            }

            @Override
            public void onCardDragging(Direction direction, float ratio) {
                // Optional: Add logic for card dragging, if needed
            }

            @Override
            public void onCardRewound() {
                // Optional: Add logic for when a card is rewound
            }

            @Override
            public void onCardCanceled() {
                // Optional: Add logic for when a card swipe is canceled
            }

            @Override
            public void onCardAppeared(View view, int position) {
                // Optional: Add logic for when a card appears
            }

            @Override
            public void onCardDisappeared(View view, int position) {
                // Optional: Add logic for when a card disappears
            }
        });
    }


    private void handleCardSwipe(FashionPost post, Direction direction) {
        DatabaseReference postRef = FirebaseDatabase.getInstance()
                .getReference("activePosts")
                .child(post.getPostId());

        if (direction == Direction.Right) {
            // Like the post
            postRef.child("likes").setValue(post.getLikes() + 1);
            rewardPoints(1); // Reward 1 point for liking
        } else if (direction == Direction.Left) {
            // Dislike the post
            postRef.child("likes").setValue(post.getLikes() - 1);
            rewardPoints(1);
        }

        adapter.getPosts().remove(post);
        adapter.notifyItemRemoved(manager.getTopPosition() - 1);

        if (swings % 4 == 0) {
            //showInterstitialAd();
            //loadInterstitialAd();
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(getContext(), "ca-app-pub-7662096701692256/2769277375", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.d("AdMob", "Interstitial ad failed to load: " + adError.getMessage());
            }
        });
    }

    private void showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd.show(getActivity());
        } else {
            Log.d("AdMob", "Interstitial ad not ready.");
        }
    }

    private void rewardPoints(int points) {
        PointsManager.addPoints(currentUserId, points, new PointsManager.PointsCallback() {
            @Override
            public void onPointsAdded(int pointsAdded) {
                Toast.makeText(getContext(), "You earned " + pointsAdded + " points!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCapReached() {
                Toast.makeText(getContext(), "Daily cap reached. No more points earned today.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error adding points: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserCache() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    if (userId != null && username != null) {
                        userCache.put(userId, username);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DeckFragment", "Failed to load user cache: " + error.getMessage());
            }
        });
    }

    private void loadPosts() {
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("following").child(currentUserId);
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("activePosts");

        // Use a persistent mechanism to avoid duplicate notifications (e.g., SharedPreferences)
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("notifications", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        followingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> followedUsers = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    followedUsers.add(child.getKey());
                }

                postsRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        FashionPost post = snapshot.getValue(FashionPost.class);
                        if (post != null) {
                            // Check if the post has already been notified
                            boolean isNotified = sharedPreferences.getBoolean(post.getPostId(), false);
                            if (!isNotified && followedUsers.contains(post.getPosterId())) {
                                notifiedPosts.add(post.getPostId());
                                editor.putBoolean(post.getPostId(), true).apply(); // Mark the post as notified

                                String username = userCache.getOrDefault(post.getPosterId(), "Unknown User");
                                showNotification("New Post", username + " posted: " + post.getDescription());
                            }
                            adapter.getPosts().add(post);
                            adapter.notifyItemInserted(adapter.getPosts().size() - 1);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(requireContext(), NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "general_notifications_channel";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.lego_driply)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for general updates");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
