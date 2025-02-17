package com.onecodeman.driply.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onecodeman.driply.R;
import com.onecodeman.driply.RetrofitClient;
import com.onecodeman.driply.WeatherApiService;
import com.onecodeman.driply.models.WeatherResponse;
import com.onecodeman.driply.utils.PointsManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherFragment extends Fragment {

    private TextView locationPicker, voteResult;
    private Button voteLight, voteModerate, voteHeavy;
    private String cityNamer;

    private DatabaseReference weatherVotesRef;
    private FirebaseAuth mAuth;
    private RelativeLayout mainCircle;

    private TextView actualTempTextView, feelsLikeTextView;
    private String cityName = "London"; // Default city (replace with dynamic location logic)
    private static final String API_KEY = "bce7e537abe51a167c07397906550288"; // Replace with your OpenWeatherMap API key

    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Initialize views
        locationPicker = view.findViewById(R.id.location_picker);
        voteResult = view.findViewById(R.id.vote_results);
        voteLight = view.findViewById(R.id.vote_light);
        voteModerate = view.findViewById(R.id.vote_moderate);
        voteHeavy = view.findViewById(R.id.vote_heavy);

        // Initialize views
        actualTempTextView = view.findViewById(R.id.actual_temp_text);
        feelsLikeTextView = view.findViewById(R.id.feels_like_text);

        mainCircle = view.findViewById(R.id.vote_circle);

        // Initialize Firebase references
        weatherVotesRef = FirebaseDatabase.getInstance().getReference("weatherVotes");
        mAuth = FirebaseAuth.getInstance();

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Fetch location and update city name
        fetchUserCity();

        // Set up voting buttons
        voteLight.setOnClickListener(v -> castVote("light"));
        voteModerate.setOnClickListener(v -> castVote("moderate"));
        voteHeavy.setOnClickListener(v -> castVote("heavy"));

        return view;
    }

    private void fetchVotesForCity(String cityName) {
        DatabaseReference votesRef = FirebaseDatabase.getInstance().getReference("weatherVotes");

        votesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int totalVotes = 0;
                    int option1Votes = 0;
                    int option2Votes = 0;
                    int option3Votes = 0;

                    // Count votes for the specified city
                    for (DataSnapshot userVote : snapshot.getChildren()) {
                        String voteCity = userVote.child("city").getValue(String.class);
                        String voteOption = userVote.child("vote").getValue(String.class);

                        if (cityName.equals(voteCity) && voteOption != null) {
                            totalVotes++;
                            switch (voteOption) {
                                case "light":
                                    option1Votes++;
                                    break;
                                case "moderate":
                                    option2Votes++;
                                    break;
                                case "heavy":
                                    option3Votes++;
                                    break;
                            }
                        }
                    }

                    // Calculate percentages and update the UI
                    updateVotePercentages(option1Votes, option2Votes, option3Votes, totalVotes);
                } else {
                    Toast.makeText(getContext(), "No votes found for this city.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch votes.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateVotePercentages(int option1Votes, int option2Votes, int option3Votes, int totalVotes) {
        if (totalVotes > 0) {
            int option1Percentage = (option1Votes * 100) / totalVotes;
            int option2Percentage = (option2Votes * 100) / totalVotes;
            int option3Percentage = (option3Votes * 100) / totalVotes;

            // Update the main circle with the leading suggestion
            if (option1Votes >= option2Votes && option1Votes >= option3Votes) {
                voteResult.setText(option1Percentage + " % say wear Light");
                mainCircle.setBackgroundResource(R.drawable.circle_background_light);
            } else if (option2Votes >= option1Votes && option2Votes >= option3Votes) {
                voteResult.setText(option2Percentage + " % say wear Moderate");
                mainCircle.setBackgroundResource(R.drawable.circle_background_moderate);
            } else if (option3Votes >= option1Votes && option3Votes >= option2Votes) {
                voteResult.setText(option3Percentage + " % say wear Heavy");
                mainCircle.setBackgroundResource(R.drawable.circle_background_heavy);
            }
        } else {
            voteResult.setText("No votes yet");
            mainCircle.setBackgroundResource(R.drawable.circle_background_cloral); // Default background
        }
    }


    private void castVote(String vote) {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) return;

        DatabaseReference userVoteRef = weatherVotesRef.child(userId);

        userVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentTime = System.currentTimeMillis();
                Long lastVoteTime = snapshot.child("lastVoteTime").getValue(Long.class);

                // Check if the user is allowed to vote
                if (lastVoteTime != null && (currentTime - lastVoteTime) < 2 * 60 * 60 * 1000) {
                    long remainingTime = (2 * 60 * 60 * 1000 - (currentTime - lastVoteTime)) / (60 * 1000); // Remaining time in minutes
                    Toast.makeText(getContext(), "You can vote again in " + remainingTime + " minutes.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Allow the user to vote
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("city", cityNamer);
                voteData.put("vote", vote);
                voteData.put("lastVoteTime", currentTime);

                userVoteRef.setValue(voteData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Vote submitted!", Toast.LENGTH_SHORT).show();
                            // Reward the user for voting
                            PointsManager.addPoints(userId, 3, new PointsManager.PointsCallback() {
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
                            fetchVotesForCity(cityNamer);
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to submit vote.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to check voting eligibility.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void fetchUserCity() {
        if (!isLocationEnabled()) {
            Toast.makeText(getContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                updateCityName(latitude, longitude);
            } else {
                Toast.makeText(getContext(), "Unable to fetch location. Try again later.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("WeatherFragment", "Error fetching location: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to fetch location. Try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void updateCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                String cityName = addresses.get(0).getLocality();
                cityNamer = cityName;
                locationPicker.setText("Current Location: " + cityName);
                // Fetch current vote results
                fetchVotesForCity(cityNamer);
                // Fetch weather data
                fetchWeatherData(cityName);
            } else {
                locationPicker.setText("Unable to determine city");
                cityNamer = "city";
                // Fetch current vote results
                fetchVotesForCity(cityNamer);
            }
        } catch (IOException e) {
            Log.e("WeatherFragment", "Geocoder error: " + e.getMessage());
            locationPicker.setText("Error fetching city name");
        }
    }

    private void fetchWeatherData(String city) {
        WeatherApiService apiService = RetrofitClient.getRetrofitInstance().create(WeatherApiService.class);
        Call<WeatherResponse> call = apiService.getWeather(city, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double actualTemp = response.body().getMain().getTemp();
                    double feelsLikeTemp = response.body().getMain().getFeelsLike();

                    actualTempTextView.setText(String.format("Actual: %.1f°C", actualTemp));
                    feelsLikeTextView.setText(String.format("Feels Like: %.1f°C", feelsLikeTemp));
                } else {
                    Toast.makeText(getContext(), "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e("WeatherFragment", "API Call Failed: " + t.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchUserCity();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
