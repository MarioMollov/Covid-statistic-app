package uni.fmi.masters.covid_19statistic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = "TAG";
    public static final String FIREBASE_FIRST_NAME = "firstName";
    public static final String FIREBASE_LAST_NAME = "lastName";
    public static final String FIREBASE_EMAIL = "email";
    public static final String FIREBASE_PASSWORD = "password";
    public static final String FIREBASE_AVATAR_PATH = "avatarPath";
    public static final String COLLECTION_USERS = "users";


    EditText emailET, fnameET, lnameET, passwordET, repeatPasswordET;
    Button registerB;
    TextView alreadyRegisterTV;
    ProgressBar progressBar;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailET = findViewById(R.id.emailEditText);
        fnameET = findViewById(R.id.fNameEditText);
        lnameET = findViewById(R.id.lNameEditText);
        passwordET = findViewById(R.id.passwordEditText);
        repeatPasswordET = findViewById(R.id.repeatPasswordEditText);
        registerB = findViewById(R.id.registerButton);
        alreadyRegisterTV = findViewById(R.id.alreadyRegisteredTextView);

        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.registerProgressBar);

        if(fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }


        registerB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                String repeatPass = repeatPasswordET.getText().toString();
                String fname = fnameET.getText().toString();
                String lname = lnameET.getText().toString();

                if(email.isEmpty()){
                    emailET.setError("Email is required!");
                    return;
                }

                if(password.isEmpty()){
                    passwordET.setError("Password is required!");
                    return;
                }

                if(password.length() < 6){
                    passwordET.setError("Password must be 6 or more characters!");
                    return;
                }

                if(!password.equals(repeatPass)){
                    repeatPasswordET.setError("Password Do not match! ");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);


                // register the user in firebase
                fAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Toast.makeText(RegisterActivity.this, "User Created", Toast.LENGTH_SHORT).show();

                        // getting automatically generated user id from the user that we just create
                        userId = fAuth.getCurrentUser().getUid();

                        //create (if there is no) new collection(this represent table) named "users"
                        // and new document( this represent record) with id equal to the current user id
                        DocumentReference documentReference = db.collection(COLLECTION_USERS).document(userId);

                        //create user data using hashmap
                        Map<String, Object> user = new HashMap<>();
                        user.put(FIREBASE_FIRST_NAME,fname);
                        user.put(FIREBASE_LAST_NAME,lname);
                        user.put(FIREBASE_EMAIL,email);
                        user.put(FIREBASE_PASSWORD, password);
                        user.put(FIREBASE_AVATAR_PATH," https://firebasestorage.googleapis.com/v0/b/covid-19-statistic-83f" +
                                "02.appspot.com/o/4c1I4KGQouRTQXpETkD8z53iUfm2%2FJPEG_15022021_003428.jpg?alt=media&token=" +
                                "9ffe96b3-e683-4fda-bb8b-207f0515183e");

                        // save created user data
                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "profile is created for user with id: " + userId);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: " + e.toString());
                            }
                        });

                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        alreadyRegisterTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),LoginActivity.class));
            }
        });

    }
}