package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

public class AdminMainActivity extends AppCompatActivity {

    private boolean isHardcodedAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        isHardcodedAdmin = getIntent().getBooleanExtra("isHardcodedAdmin", false);

        MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        TabLayout tabLayout = findViewById(R.id.adminTabLayout);
        
        toolbar.setNavigationOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminMainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Default fragment
        loadFragment(new AdminUsersFragment());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadFragment(new AdminUsersFragment());
                } else {
                    loadFragment(new AdminChatsFragment());
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    public boolean isHardcodedAdmin() {
        return isHardcodedAdmin;
    }

    private void loadFragment(Fragment fragment) {
        // Pass the admin flag to the fragment via Arguments
        Bundle args = new Bundle();
        args.putBoolean("isHardcodedAdmin", isHardcodedAdmin);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit();
    }
}
