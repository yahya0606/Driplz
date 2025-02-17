package com.onecodeman.driply;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onecodeman.driply.adapters.ViewPagerAdapter;
import com.onecodeman.driply.utils.MyApp;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    private TextView usernamePlaceholder;
    private TextView pointsPlaceholder;
    private ImageView profilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        // Check for fragment selection intent
        int fragmentToOpen = getIntent().getIntExtra("fragment", 1); // Default to fragment 1
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setCurrentItem(fragmentToOpen); // Set the correct fragment
        viewPager.setUserInputEnabled(false);

        ImageView notificationIcon = findViewById(R.id.notification_icon);

        notificationIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        saveFcmToken();

        // Initialize AdMob SDK
        MobileAds.initialize(this, initializationStatus -> {
            Log.d("AdMob", "AdMob SDK initialized");

        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Disable default title
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Points placeholder in Toolbar
        pointsPlaceholder = toolbar.findViewById(R.id.drips_counter);

        drawerLayout = findViewById(R.id.drawer_layout);
        // Set up DrawerLayout
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //toggle.setDrawerIndicatorEnabled(false); // Disable default icon
        //toolbar.setNavigationIcon(R.drawable.); // Set custom icon
        toolbar.setNavigationOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START); // Open navigation drawer
        });

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup NavigationView
        navigationView = findViewById(R.id.nav_view);

        // Get Drawer Header Views
        usernamePlaceholder = navigationView.getHeaderView(0).findViewById(R.id.username);
        profilePic = navigationView.getHeaderView(0).findViewById(R.id.profileImage);

        navigationView.setNavigationItemSelectedListener(item -> {
            handleMenuSelection(item);
            return true;
        });

        // Setup TabLayout and ViewPager2
        setupTabLayoutAndViewPager();

        // Setup FAB
        //FloatingActionButton fab = findViewById(R.id.fab);
        //fab.setOnClickListener(view -> {
        //    Intent intent = new Intent(MainActivity.this, UploadActivity.class);
       //     startActivity(intent);
        //});

        // Add Firebase Auth state listener
        mAuth.addAuthStateListener(firebaseAuth -> {
            updateNavigationMenu(); // Update menu when auth state changes
            fetchUserData(); // Fetch username and points dynamically
        });

        // Update Navigation Drawer menu based on login status
        updateNavigationMenu();
        fetchUserData();
    }

    private void handleMenuSelection(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_redeem) {
            startActivity(new Intent(MainActivity.this, RedeemActivity.class));
            finish();
        } else if (id == R.id.nav_settings) {
            // Navigate to Settings Activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            updateNavigationMenu();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_login) {
            // Navigate to Login Activity
            Intent intent = new Intent(this, EmailAuthActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawers();
    }

    private void updateNavigationMenu() {
        Menu menu = navigationView.getMenu();

        if (menu != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            MenuItem redeemItem = menu.findItem(R.id.nav_redeem);
            MenuItem settingsItem = menu.findItem(R.id.nav_settings);
            MenuItem logoutItem = menu.findItem(R.id.nav_logout);
            MenuItem loginItem = menu.findItem(R.id.nav_login);

            if (currentUser != null) {
                // User is logged in
                if (redeemItem != null) redeemItem.setVisible(true);
                if (settingsItem != null) settingsItem.setVisible(true);
                if (logoutItem != null) logoutItem.setVisible(true);
                if (loginItem != null) loginItem.setVisible(false);
            } else {
                // User is not logged in
                if (redeemItem != null) redeemItem.setVisible(false);
                if (settingsItem != null) settingsItem.setVisible(false);
                if (logoutItem != null) logoutItem.setVisible(false);
                if (loginItem != null) loginItem.setVisible(true);

                // Clear username and points when logged out
                usernamePlaceholder.setText("Guest");
                pointsPlaceholder.setText("0");
            }
        }
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            usersRef.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        Long points = snapshot.child("points").getValue(Long.class);
                        String profilePicUrl = snapshot.child("profilePicture").getValue(String.class);

                        usernamePlaceholder.setText(username != null ? username : "Guest");
                        pointsPlaceholder.setText(points != null ? String.valueOf(points) : "0");

                        if (profilePicUrl != null && !profilePicUrl.isEmpty() && !isFinishing() && !isDestroyed()) {
                            Glide.with(MainActivity.this)
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.ic_profile_pic) // Default profile picture
                                    .into(profilePic);
                        } else {
                            profilePic.setImageResource(R.drawable.ic_profile_pic);
                        }
                    } else {
                        usernamePlaceholder.setText("Guest");
                        pointsPlaceholder.setText("0");
                        profilePic.setImageResource(R.drawable.ic_profile_pic);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Error fetching user data: " + error.getMessage());
                    Toast.makeText(MainActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            usernamePlaceholder.setText("Guest");
            pointsPlaceholder.setText("0");
            profilePic.setImageResource(R.drawable.ic_profile_pic);
        }
    }



    private void setupTabLayoutAndViewPager() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager = findViewById(R.id.view_pager);

        // Set up the adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Connect TabLayout and ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 1) {
                //tab.setText("Deck");
                tab.setIcon(R.drawable.ic_home);
            } else if (position == 2) {
                //tab.setText("Weather");
                tab.setIcon(R.drawable.ic_rain);
            }else {
                //tab.setText("Leader");
                tab.setIcon(R.drawable.ic_leaderboard);
            }
        }).attach();
        viewPager.setCurrentItem(1);

        // Add a custom tab with a button
        View customTab = getLayoutInflater().inflate(R.layout.custom_tab_button, null);
        ImageButton tabButton = customTab.findViewById(R.id.tab_button);

        // Set up click listener for the button
        tabButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UploadActivity.class);
            startActivity(intent);
        });

        // Add the custom view as a new tab
        TabLayout.Tab newTab = tabLayout.newTab();
        newTab.setCustomView(customTab);
        tabLayout.addTab(newTab);
    }

    // Method to fetch and save FCM token
    private void saveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    // Get the FCM token
                    String fcmToken = task.getResult();
                    Log.d("FCM", "FCM Token: " + fcmToken);

                    // Save the token to the database
                    saveTokenToDatabase(fcmToken);
                });
    }

    // Save the token to the user's node in Firebase Realtime Database
    private void saveTokenToDatabase(String token) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUserId);

            userRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Token saved successfully"))
                    .addOnFailureListener(e -> Log.e("FCM", "Error saving token", e));
        } else {
            Log.w("FCM", "User is not logged in. Token not saved.");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth != null) {
            mAuth.removeAuthStateListener(firebaseAuth -> updateNavigationMenu());
            //usersRef.removeEventListener(valueEventListener); // Replace `valueEventListener` with your actual listener reference
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        ((MyApp) getApplication()).showOpenAppAd();
    }
}
