package com.example.carpool_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private Button btnVerify;
    private String mVerificationId;
    private String phoneNumber;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        mAuth = FirebaseAuth.getInstance();
        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);

        phoneNumber = getIntent().getStringExtra("phoneNumber");

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Error: Phone number missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sendVerificationCode(phoneNumber);

        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();
            
            // Data Verification Check
            if (TextUtils.isEmpty(code) || code.length() < 6) {
                etOtp.setError("Enter 6-digit OTP");
                return;
            }
            
            if (mVerificationId == null) {
                Toast.makeText(this, "Please wait, sending code...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            verifyCode(code);
        });
    }

    private void sendVerificationCode(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-verification
                        String code = credential.getSmsCode();
                        if (code != null) {
                            etOtp.setText(code);
                            verifyCode(code);
                        }
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        String message = "Verification failed: " + e.getLocalizedMessage();
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            message = "Invalid phone number format.";
                        }
                        Toast.makeText(VerifyOtpActivity.this, message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        Toast.makeText(VerifyOtpActivity.this, "OTP Sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        linkPhoneWithUser(credential);
    }

    private void linkPhoneWithUser(PhoneAuthCredential credential) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(VerifyOtpActivity.this, "Phone Verified & Account Linked!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(VerifyOtpActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            etOtp.setError("Incorrect OTP code.");
                        } else {
                            Toast.makeText(VerifyOtpActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        } else {
            // If user somehow logged out, just sign in with phone (Fallback)
            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(VerifyOtpActivity.this, MainActivity.class));
                    finish();
                }
            });
        }
    }
}
