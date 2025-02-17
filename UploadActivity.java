package com.onecodeman.driply;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.onecodeman.driply.utils.PointsManager;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;

    private ImageView selectedImage;
    private EditText postDescription;
    private Uri imageUri;
    private Bitmap capturedImage;

    private FirebaseDatabase database;
    private DatabaseReference postsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize Firebase references
        database = FirebaseDatabase.getInstance();
        postsRef = database.getReference("activePosts");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference("post_images");

        auth = FirebaseAuth.getInstance();

        // Initialize Views
        selectedImage = findViewById(R.id.selected_image);
        postDescription = findViewById(R.id.post_description);
        Button chooseImageButton = findViewById(R.id.choose_image_button);
        Button captureImageButton = findViewById(R.id.capture_image_button);
        Button uploadPostButton = findViewById(R.id.upload_post_button);

        // Handle image selection
        chooseImageButton.setOnClickListener(v -> openFileChooser());
        captureImageButton.setOnClickListener(v -> openCamera());
        uploadPostButton.setOnClickListener(v -> uploadPost());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                selectedImage.setImageURI(imageUri); // Display selected image
                capturedImage = null; // Clear captured image
            } else if (requestCode == CAPTURE_IMAGE_REQUEST && data != null) {
                capturedImage = (Bitmap) data.getExtras().get("data");
                selectedImage.setImageBitmap(capturedImage); // Display captured image
                imageUri = null; // Clear selected image
            }
        }
    }

    private void uploadPost() {
        String description = postDescription.getText().toString().trim();
        FirebaseUser currentUser = auth.getCurrentUser();

        if ((imageUri == null && capturedImage == null) || description.isEmpty()) {
            Toast.makeText(this, "Please select an image and provide a description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Please log in to post.", Toast.LENGTH_SHORT).show();
            return;
        }

        String posterId = currentUser.getUid();
        long timestamp = System.currentTimeMillis();
        StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");

        // Handle uploading depending on the source of the image
        if (imageUri != null) {
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> uploadPostData(fileRef, description, posterId, timestamp))
                    .addOnFailureListener(e -> showError("Failed to upload image."));
        } else if (capturedImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            capturedImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            fileRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> uploadPostData(fileRef, description, posterId, timestamp))
                    .addOnFailureListener(e -> showError("Failed to upload image."));
        }
    }

    private void uploadPostData(StorageReference fileRef, String description, String posterId, long timestamp) {
        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String imageUrl = uri.toString();
            String postId = postsRef.push().getKey();

            if (postId != null) {
                Map<String, Object> post = new HashMap<>();
                post.put("imageUrl", imageUrl);
                post.put("description", description);
                post.put("posterId", posterId);
                post.put("likes", 0); // Initialize likes to 0
                post.put("timestamp", timestamp); // Add timestamp
                post.put("postId", postId); // Add postId

                postsRef.child(postId).setValue(post)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show();

                            // Reward points for posting
                            PointsManager.addPoints(posterId, 5, new PointsManager.PointsCallback() {
                                @Override
                                public void onPointsAdded(int pointsAdded) {
                                    Toast.makeText(UploadActivity.this, "You earned " + pointsAdded + " points!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCapReached() {
                                    Toast.makeText(UploadActivity.this, "Daily cap reached. No more points earned today.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(UploadActivity.this, "Error adding points: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });

                            finish(); // Close the activity
                        })
                        .addOnFailureListener(e -> showError("Failed to save post data."));
            }
        });
    }


    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
