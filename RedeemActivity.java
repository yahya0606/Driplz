package com.onecodeman.driply;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RedeemActivity extends AppCompatActivity {

    private TextView pointsCounter;
    private Button redeemCashButton, redeemMultiplierButton,redeemMultiplierButton7,redeemBrand;

    private DatabaseReference usersRef;
    private String userId;
    private int userPoints = 0;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem);

        pointsCounter = findViewById(R.id.points_counter);
        redeemCashButton = findViewById(R.id.redeem_cash_button);
        redeemMultiplierButton = findViewById(R.id.redeem_multiplier_button_2);
        redeemMultiplierButton7 = findViewById(R.id.redeem_multiplier_button_7);
        redeemBrand = findViewById(R.id.redeem_brands);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        fetchUserPoints();

        redeemCashButton.setOnClickListener(v -> redeemPointsForCash());
        redeemMultiplierButton.setOnClickListener(v -> redeemPointsForMultiplier(2));
        redeemMultiplierButton7.setOnClickListener(v -> redeemPointsForMultiplier(7));

        redeemBrand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RedeemActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // Initialize gesture detector for swipe gestures
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 200) {
                    navigateToMainActivity();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back arrow click
        navigateToMainActivity();
        return true;
    }

    @Override
    public void onBackPressed() {
        // Handle Android back button
        super.onBackPressed();
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RedeemActivity.this, MainActivity.class);
        intent.putExtra("fragment", 1); // Indicate fragment 1 should be opened
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Close SettingsActivity
    }

    private void fetchUserPoints() {
        usersRef.child("points").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userPoints = snapshot.getValue(Integer.class);
                    pointsCounter.setText(userPoints+" Drips");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RedeemActivity.this, "Failed to fetch points", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redeemPointsForCash() {
        if (userPoints >= 25000) {
            usersRef.child("points").setValue(userPoints - 25000)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "€5 redeemed!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to redeem.", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Insufficient points for cash redemption", Toast.LENGTH_SHORT).show();
        }
    }

    private void redeemPointsForMultiplier(int days) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int currentPoints = snapshot.child("points").getValue(Integer.class) != null ?
                            snapshot.child("points").getValue(Integer.class) : 0;
                    Long lastMultiplierActivated = snapshot.child("lastMultiplierActivated").getValue(Long.class);

                    long currentTime = System.currentTimeMillis();
                    long cooldownPeriod = days * 24 * 60 * 60 * 1000;

                    if (lastMultiplierActivated != null && (currentTime - lastMultiplierActivated) < cooldownPeriod) {
                        long remainingTime = cooldownPeriod - (currentTime - lastMultiplierActivated);
                        long hours = remainingTime / (60 * 60 * 1000);
                        long minutes = (remainingTime % (60 * 60 * 1000)) / (60 * 1000);

                        redeemMultiplierButton.setText("Available in " + hours + "h " + minutes + "m");
                        redeemMultiplierButton.setEnabled(false);
                        return;
                    }

                    if (currentPoints >= 500) {
                        usersRef.child("points").setValue(currentPoints - 500);
                        usersRef.child("capMultiplier").setValue(2);
                        usersRef.child("lastMultiplierActivated").setValue(currentTime);

                        Toast.makeText(RedeemActivity.this, "Multiplier activated for " + days + " days!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RedeemActivity.this, "Insufficient points!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RedeemActivity.this, "Failed to activate multiplier.", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
