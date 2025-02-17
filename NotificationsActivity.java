package com.onecodeman.driply;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onecodeman.driply.adapters.NotificationsAdapter;
import com.onecodeman.driply.models.NotificationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationsAdapter adapter;
    private List<NotificationModel> notifications;

    private DatabaseReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications);
        notificationsRecyclerView.setAdapter(adapter);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);

        fetchNotifications();
    }

    private void fetchNotifications() {
        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifications.clear();
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    NotificationModel notification = notificationSnapshot.getValue(NotificationModel.class);
                    notifications.add(notification);
                }
                Collections.sort(notifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationsActivity.this, "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
