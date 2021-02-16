package uni.fmi.masters.covid_19statistic.ui.profile;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.grpc.internal.LogExceptionRunnable;
import uni.fmi.masters.covid_19statistic.MainActivity;
import uni.fmi.masters.covid_19statistic.R;
import uni.fmi.masters.covid_19statistic.RegisterActivity;

public class ProfileFragment extends Fragment {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final String TAG = "TAG";
    public static final int GALLERY_REQUEST_CODE = 105;
    EditText emailET, firstNameET, lastNameET;
    Button cameraB, galleryB, editProfileB, updateB;
    ImageView avatarIV;
    String userID;
    String currentPhotoPath;

    FirebaseAuth fAuth;
    FirebaseUser fUser;
    FirebaseFirestore db;
    DocumentReference documentReference;
    StorageReference storageReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        emailET = root.findViewById(R.id.emailEditText);
        firstNameET = root.findViewById(R.id.firstNameEditText);
        lastNameET = root.findViewById(R.id.lastNameEditText);
        avatarIV = root.findViewById(R.id.avatarImageView);
        editProfileB = root.findViewById(R.id.editProfileButton);
        updateB = root.findViewById(R.id.updateButton);
        cameraB = root.findViewById(R.id.cameraButton);
        galleryB = root.findViewById(R.id.galleryButton);

        // we will need fAuth to get current users id witch match the document id
        fAuth = FirebaseAuth.getInstance();
        fUser = fAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        userID = fAuth.getCurrentUser().getUid();

        // Take collection "users" and document with id = current user id
        documentReference = db.collection(RegisterActivity.COLLECTION_USERS).document(userID);

        // this listener will return data adn will listen for data update
        // and we the data is updated, the listener will return the new data automatically
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                Uri avatarUri = Uri.parse(value.getString(RegisterActivity.FIREBASE_AVATAR_PATH));
                String email = value.getString(RegisterActivity.FIREBASE_EMAIL);
                String fname = value.getString(RegisterActivity.FIREBASE_FIRST_NAME);
                String lname = value.getString(RegisterActivity.FIREBASE_LAST_NAME);

                emailET.setText(email);
                firstNameET.setText(fname);
                lastNameET.setText(lname);
                avatarIV.setImageURI(avatarUri);


            }
        });

        cameraB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermission();
            }
        });

        galleryB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
            }
        });

        editProfileB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailET.setEnabled(true);
                firstNameET.setEnabled(true);
                lastNameET.setEnabled(true);
                updateB.setEnabled(true);
                editProfileB.setEnabled(false);
            }
        });

        updateB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String fname = firstNameET.getText().toString();
                String lname = lastNameET.getText().toString();

                if (email.isEmpty() || fname.isEmpty() || lname.isEmpty()) {
                    Toast.makeText(getContext(), "Field can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                fUser.updateEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        documentReference = db.collection(RegisterActivity.COLLECTION_USERS).document(userID);

                        Map<String, Object> user = new HashMap<>();
                        user.put(RegisterActivity.FIREBASE_EMAIL, email);
                        user.put(RegisterActivity.FIREBASE_FIRST_NAME, fname);
                        user.put(RegisterActivity.FIREBASE_LAST_NAME, lname);

                        documentReference.update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                emailET.setEnabled(false);
                                firstNameET.setEnabled(false);
                                lastNameET.setEnabled(false);
                                updateB.setEnabled(false);
                                editProfileB.setEnabled(true);
                                Toast.makeText(getContext(), "Profile data update was successful.", Toast.LENGTH_LONG).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Error:" + e.getMessage());
                            }
                        });

                        Toast.makeText(getContext(), "Authentication was update successful.", Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error" + e.getMessage());
                    }
                });
            }
        });

        return root;
    }

    // check if we have permission or will need to ask again
    // and if permission i granted, open the camera
    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // set the captured picture to the "avatarIV"
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);
                avatarIV.setImageURI(Uri.fromFile(f));
                Log.d(TAG, "Absolute Url of the image is: " + Uri.fromFile(f));

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                getActivity().sendBroadcast(mediaScanIntent);

                uploadImageToFirebase(f.getName(), contentUri);

            }
        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d(TAG, "onActivityResult Gallery image Uri: " + imageFileName);
                avatarIV.setImageURI(contentUri);

                uploadImageToFirebase(imageFileName, contentUri);

            }
        }
    }

    private void uploadImageToFirebase(String name, Uri contentUri) {

        documentReference = db.collection(RegisterActivity.COLLECTION_USERS).document(userID);
        Map<String, Object> user = new HashMap<>();
        user.put(RegisterActivity.FIREBASE_AVATAR_PATH, contentUri.toString());
        documentReference.update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "Avatar path was updated in Firestore");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Update avatar error:" + e.getMessage());
            }
        });

        StorageReference image = storageReference.child(userID + "/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d(TAG, "Uploaded image url is: " + uri.toString());
                    }
                });

                Toast.makeText(getContext(), "Image is Uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    Context applicationContext = MainActivity.getContextOfApplication();

    private String getFileExt(Uri contentUri) {

        ContentResolver c = getActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    private File createImageFile() throws IOException {
        //Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // this save pictures in the phone gallery
//        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        //Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getContext(),
                        "uni.fmi.masters.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }
}