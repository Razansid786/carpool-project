package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etWorkplace, etWorkplaceId, etWorkplaceEmail, etWorkplaceAddress;
    private TextView tvName;
    private MaterialButton btnSave, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = view.findViewById(R.id.tvProfileName);
        etWorkplace = view.findViewById(R.id.etWorkplace);
        etWorkplaceId = view.findViewById(R.id.etWorkplaceId);
        etWorkplaceEmail = view.findViewById(R.id.etWorkplaceEmail);
        etWorkplaceAddress = view.findViewById(R.id.etWorkplaceAddress);
        btnSave = view.findViewById(R.id.btnSaveProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadProfileData();

        btnSave.setOnClickListener(v -> saveProfileData());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void loadProfileData() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvName.setText(doc.getString("name"));
                etWorkplace.setText(doc.getString("workplace"));
                etWorkplaceId.setText(doc.getString("workplaceId"));
                etWorkplaceEmail.setText(doc.getString("workplaceEmail"));
                etWorkplaceAddress.setText(doc.getString("workplaceAddress"));
            }
        });
    }

    private void saveProfileData() {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("workplace", etWorkplace.getText().toString().trim());
        updates.put("workplaceId", etWorkplaceId.getText().toString().trim());
        updates.put("workplaceEmail", etWorkplaceEmail.getText().toString().trim());
        updates.put("workplaceAddress", etWorkplaceAddress.getText().toString().trim());

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
    }
}
