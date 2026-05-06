package com.example.carpool_project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private AdminUserAdapter adapter;
    private List<Person> userList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);
        db = FirebaseFirestore.getInstance();
        rvUsers = view.findViewById(R.id.rvAdminUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userList = new ArrayList<>();
        adapter = new AdminUserAdapter(userList);
        rvUsers.setAdapter(adapter);

        loadUsers();
        return view;
    }

    private void loadUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                userList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Person p = doc.toObject(Person.class);
                    if (p != null) {
                        p.userId = doc.getId();
                        userList.add(p);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {
        private List<Person> list;

        public AdminUserAdapter(List<Person> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Person p = list.get(position);
            holder.tvName.setText(p.name);
            holder.tvEmail.setText(p.email);
            holder.tvRole.setText("Role: " + p.role);

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete " + p.name + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("users").document(p.userId).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRole;
            ImageButton btnDelete;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAdminUserName);
                tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
                tvRole = itemView.findViewById(R.id.tvAdminUserRole);
                btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            }
        }
    }
}
