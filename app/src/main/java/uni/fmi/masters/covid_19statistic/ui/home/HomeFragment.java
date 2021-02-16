package uni.fmi.masters.covid_19statistic.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import uni.fmi.masters.covid_19statistic.LoginActivity;
import uni.fmi.masters.covid_19statistic.R;
import uni.fmi.masters.covid_19statistic.RegisterActivity;

public class HomeFragment extends Fragment {

    ImageView homeAvatarIV;
    TextView greetingsTV;
    Button homeLogoutB;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    DocumentReference documentReference;
    ListenerRegistration registration;
    String userID;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        homeAvatarIV = root.findViewById(R.id.homeImageView);
        greetingsTV = root.findViewById(R.id.greetingsTextView);
        homeLogoutB = root.findViewById(R.id.homeLogoutBbutton);

        fAuth = FirebaseAuth.getInstance();
        userID = fAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        documentReference = db.collection(RegisterActivity.COLLECTION_USERS).document(userID);
        registration = documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                Uri avatarUri = Uri.parse(value.getString(RegisterActivity.FIREBASE_AVATAR_PATH));
                homeAvatarIV.setImageURI(avatarUri);
                greetingsTV.setText("Welcome, " + value.getString(RegisterActivity.FIREBASE_FIRST_NAME));
            }
        });

        homeLogoutB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registration.remove();
                fAuth.signOut();
                startActivity(new Intent(getContext(), LoginActivity.class));
//                finish();
            }
        });

        return root;
    }
}