package com.onecodeman.driply;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profilePicture;
    private TextView usernameText, emailText;
    private Button editProfilePictureButton, deactivateAccountButton, deleteAccountButton, logoutButton;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private FirebaseStorage storage;
    private StorageReference profilePicsRef;
    private FirebaseUser currentUser;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        profilePicsRef = storage.getReference("profile_pictures");
        currentUser = mAuth.getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize Views
        profilePicture = findViewById(R.id.profile_picture);
        usernameText = findViewById(R.id.username_text);
        emailText = findViewById(R.id.email_text);
        editProfilePictureButton = findViewById(R.id.edit_profile_picture_button);
        deactivateAccountButton = findViewById(R.id.deactivate_account_button);
        deleteAccountButton = findViewById(R.id.delete_account_button);
        // Set user info
        if (currentUser != null) {
            emailText.setText(currentUser.getEmail());

            // Fetch the profile picture URL from the database
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Fetch the profile picture URL from the "profilePicture" key
                        String profilePicUrl = snapshot.child("profilePicture").getValue(String.class);
                        String userName = snapshot.child("username").getValue(String.class);

                        usernameText.setText(userName);
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            // Load the profile picture using Glide
                            Glide.with(SettingsActivity.this)
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.ic_profile_pic) // Default profile picture
                                    .into(profilePicture);
                        } else {
                            // Set a default profile picture if URL is null or empty
                            profilePicture.setImageResource(R.drawable.ic_profile_pic);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        // Enable back arrow button
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        // Set click listeners
        editProfilePictureButton.setOnClickListener(v -> openImageChooser());
        deactivateAccountButton.setOnClickListener(v -> confirmDeactivation());
        deleteAccountButton.setOnClickListener(v -> confirmDeletion());
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
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.putExtra("fragment", 1); // Indicate fragment 1 should be opened
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Close SettingsActivity
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfilePicture(imageUri);
        }
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (currentUser == null) return;

        StorageReference fileRef = profilePicsRef.child(currentUser.getUid() + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save the profile picture URL in the database
                    usersRef.child(currentUser.getUid()).child("profilePicture").setValue(uri.toString())
                            .addOnSuccessListener(unused -> Toast.makeText(SettingsActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show());
                }))
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to upload profile picture.", Toast.LENGTH_SHORT).show());
    }

    private void confirmDeactivation() {
        new AlertDialog.Builder(this)
                .setTitle("Deactivate Account")
                .setMessage("Are you sure you want to deactivate your account? You can reactivate it by logging in.")
                .setPositiveButton("Yes", (dialog, which) -> deactivateAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deactivateAccount() {
        if (currentUser == null) return;

        usersRef.child(currentUser.getUid()).child("deactivated").setValue(true)
                .addOnSuccessListener(unused -> Toast.makeText(SettingsActivity.this, "Account deactivated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to deactivate account.", Toast.LENGTH_SHORT).show());
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (currentUser == null) return;

        // Delete user data from the database
        usersRef.child(currentUser.getUid()).removeValue()
                .addOnSuccessListener(unused -> {
                    // Delete user authentication
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SettingsActivity.this, "Account deleted.", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SettingsActivity.this, EmailAuthActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(SettingsActivity.this, "Failed to delete account.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to delete account data.", Toast.LENGTH_SHORT).show());
    }
}
