package com.example.googlesignin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ImageView googleAuth;
    FirebaseAuth auth;
    FirebaseDatabase database;
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 20;
    HashMap<String, Object> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleAuth = findViewById(R.id.google_btn);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        String serverClientId = getString(R.string.default_web_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        map = new HashMap<>();
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();

                    DatabaseReference userRef = database.getReference().child("users").child(user.getUid());

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String phone = dataSnapshot.child("phone").getValue(String.class);
                                String role = dataSnapshot.child("role").getValue(String.class);

                                map.put("id", user.getUid());
                                map.put("name", user.getDisplayName());
                                map.put("phone", phone != null ? phone : "0000000000");
                                map.put("role", role != null ? role : "ROLE");
                                map.put("profile", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

                                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                                intent.putExtra("userId", user.getUid());
                                intent.putExtra("userName", user.getDisplayName());
                                intent.putExtra("userPhone", phone != null ? phone : "0000000000");
                                intent.putExtra("userRole", role != null ? role : "ROLE");
                                intent.putExtra("userProfile", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                                startActivity(intent);
                            } else {
                                addUserToDatabase(user);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MainActivity", "Database error: " + databaseError.getMessage());
                            Toast.makeText(MainActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addUserToDatabase(final FirebaseUser user) {
        DatabaseReference userRef = database.getReference().child("users").child(user.getUid());
        userRef.child("name").setValue(user.getDisplayName());
        userRef.child("email").setValue(user.getEmail());

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra("userId", user.getUid());
        intent.putExtra("userName", user.getDisplayName());
        intent.putExtra("userPhone", "0000000000"); // Default phone number
        intent.putExtra("userRole", "ROLE"); // Default role
        intent.putExtra("userProfile", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""); // Profile picture URL if available
        startActivity(intent);
    }
}
