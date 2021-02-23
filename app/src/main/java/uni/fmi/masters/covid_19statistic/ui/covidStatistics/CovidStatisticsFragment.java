package uni.fmi.masters.covid_19statistic.ui.covidStatistics;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import uni.fmi.masters.covid_19statistic.R;
import uni.fmi.masters.covid_19statistic.RegisterActivity;

public class CovidStatisticsFragment extends Fragment {

    public static final String FIREBASE_LAST_UPDATED = "lastUpdated";
    public static final String FIREBASE_DEATHS = "deaths";
    public static final String FIREBASE_RECOVERED = "recovered";
    public static final String FIREBASE_CRITICAL_CASES = "criticalCases";
    public static final String FIREBASE_ACTIVE_CASES = "activeCases";
    public static final String FIREBASE_NEW_CASES = "newCases";
    public static final String COLLECTION_COVID_INFORMATION = "covidInformation";
    public static final String COLLECTION_HISTORY = "history";

    Spinner spinner;
    String selectedCountry, url, userID, timeStamp;
    TextView dateTV, newCasesTV, activeTV, criticalTV, recoveredTV, deathsTV, lastUpdatedTV ;
    Button checkForUpdatesB;
    FirebaseAuth fAuth;
    FirebaseFirestore db;
    DocumentReference documentReference;
    DatePickerDialog.OnDateSetListener dateSetListener;
    Dialog customeDialog;
    private RequestQueue myQueue;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_covid_statistics, container, false);
        spinner = root.findViewById(R.id.countrySpinner);
        dateTV = root.findViewById(R.id.dateTextView);
        newCasesTV = root.findViewById(R.id.newCasesTextView);
        activeTV = root.findViewById(R.id.activeTextView);
        criticalTV = root.findViewById(R.id.criticalTextView);
        recoveredTV = root.findViewById(R.id.recoveredTextView);
        deathsTV = root.findViewById(R.id.deathsTextView);
        lastUpdatedTV = root.findViewById(R.id.lastUpdatedTextView);
        checkForUpdatesB = root.findViewById(R.id.checkForUpdatesButton);
        fAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userID = fAuth.getCurrentUser().getUid();


        // get last updated information from the db
        documentReference = db.collection(COLLECTION_COVID_INFORMATION).document(userID);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                newCasesTV.setText("Новозаразени:\n" + documentSnapshot.getString(FIREBASE_NEW_CASES));
                activeTV.setText("Активни:\n" + documentSnapshot.getString(FIREBASE_ACTIVE_CASES));
                criticalTV.setText("Критични:\n" + documentSnapshot.getString(FIREBASE_CRITICAL_CASES));
                recoveredTV.setText("Излекувани:\n" +documentSnapshot.getString(FIREBASE_RECOVERED));
                deathsTV.setText("Починали:\n" + documentSnapshot.getString(FIREBASE_DEATHS));
                lastUpdatedTV.setText("Last updated: " + documentSnapshot.getString(FIREBASE_LAST_UPDATED));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(RegisterActivity.TAG, "Error " + e.getMessage());
            }
        });


        // create dropdown with values from array in res/values/strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.country_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCountry = (String) parent.getItemAtPosition(position);
                Log.d("TAG", "selected country: " + selectedCountry);
                url = "https://covid-193.p.rapidapi.com//history?country=" + selectedCountry;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                parent.getItemAtPosition(0);
            }

        });

        dateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(getContext(),
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        dateSetListener,
                        year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable((Color.TRANSPARENT)));
                dialog.show();
            }
        });

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month + 1;
                String date;
                if(month < 10 && dayOfMonth < 10){
                    date = year + "-0" + month + "-0" + dayOfMonth;
                }else if(month < 10){
                    date = year + "-0" + month + "-" + dayOfMonth;
                }else if (dayOfMonth < 10){
                    date = year + "-" + month + "-0" + dayOfMonth;
                }else {
                    date = year + "-" + month + "-" + dayOfMonth;
                }
                url = url + "&day=" + date;
                dateTV.setText(date);
            }
        };

        myQueue = Volley.newRequestQueue(getContext());
        checkForUpdatesB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsonParse();
            }
        });

        return root;
    }

    private void jsonParse() {

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("TAG", "onResponse:" + response.toString());
                        try {
                            JSONArray mResponse = response.getJSONArray("response");
                            JSONObject jsonObject = mResponse.getJSONObject(0);
                            JSONObject cases = jsonObject.getJSONObject("cases");
                            JSONObject deaths = jsonObject.getJSONObject("deaths");

                            String newCases = cases.getString("new");
                            String activeCases = cases.getString("active");
                            String criticalCases = cases.getString("critical");
                            String recovered = cases.getString("recovered");
                            String newDeaths = deaths.getString("new");

                            String oldNewCases = newCasesTV.getText().toString().substring(14);
                            String oldActiveCases = activeTV.getText().toString().substring(9);
                            String oldCritical = criticalTV.getText().toString().substring(10);
                            String oldRecovered = recoveredTV.getText().toString().substring(12);
                            String oldDeaths= deathsTV.getText().toString().substring(10);

                            if(!oldNewCases.equals(newCases) || !oldActiveCases.equals(activeCases)
                                || !oldCritical.equals(criticalCases) || !oldRecovered.equals(recovered)
                                || !oldDeaths.equals(newDeaths)){

                                customeDialog = new Dialog(getContext());
                                customeDialog.setContentView(R.layout.update_information_dialog);

                                Button dontDoAnythingB = customeDialog.findViewById(R.id.dontDoanythingButton);
                                Button updateInfoB = customeDialog.findViewById(R.id.updateInformationsButton);
                                Button updateAndSaveB = customeDialog.findViewById(R.id.updateAndSaveOldButton);

                                dontDoAnythingB.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        customeDialog.cancel();
                                    }
                                });

                                updateInfoB.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
//                                        updateInforationCollection();

                                        timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(new Date());
                                        newCasesTV.setText("Новозаразени:\n" + newCases);
                                        activeTV.setText("Активни:\n" + activeCases);
                                        criticalTV.setText("Критични:\n" + criticalCases);
                                        recoveredTV.setText("Излекувани:\n" + recovered);
                                        deathsTV.setText("Починали:\n" + newDeaths);
                                        lastUpdatedTV.setText("Last updated: " + timeStamp);

                                        documentReference = db.collection(COLLECTION_COVID_INFORMATION).document(userID);

                                        Map<String, Object> info = new HashMap<>();
                                        info.put(FIREBASE_NEW_CASES,newCases);
                                        info.put(FIREBASE_ACTIVE_CASES,activeCases);
                                        info.put(FIREBASE_CRITICAL_CASES,criticalCases);
                                        info.put(FIREBASE_RECOVERED,recovered);
                                        info.put(FIREBASE_DEATHS,newDeaths);
                                        info.put(FIREBASE_LAST_UPDATED,timeStamp);

                                        documentReference.set(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(RegisterActivity.TAG, "Successfully uploaded info ");
                                                customeDialog.hide();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(RegisterActivity.TAG, "Error: " + e.getMessage());
                                            }
                                        });
                                    }
                                });

                                updateAndSaveB.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        documentReference = db.collection(COLLECTION_HISTORY).document(userID);

                                        Map<String, Object> info = new HashMap<>();
                                        info.put(FIREBASE_NEW_CASES,newCasesTV.getText().toString().substring(14));
                                        info.put(FIREBASE_ACTIVE_CASES,activeTV.getText().toString().substring(9));
                                        info.put(FIREBASE_CRITICAL_CASES,criticalTV.getText().toString().substring(10));
                                        info.put(FIREBASE_RECOVERED,recoveredTV.getText().toString().substring(12));
                                        info.put(FIREBASE_DEATHS,deathsTV.getText().toString().substring(10));
                                        info.put(FIREBASE_LAST_UPDATED,lastUpdatedTV.getText().toString().substring(13));

                                        documentReference.set(info).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(RegisterActivity.TAG, "Successfully uploaded history ");
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(RegisterActivity.TAG, "Error: " + e.getMessage());
                                            }
                                        });

//                                        updateInforationCollection();
                                        timeStamp = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss").format(new Date());
                                        newCasesTV.setText("Новозаразени:\n" + newCases);
                                        activeTV.setText("Активни:\n" + activeCases);
                                        criticalTV.setText("Критични:\n" + criticalCases);
                                        recoveredTV.setText("Излекувани:\n" + recovered);
                                        deathsTV.setText("Починали:\n" + newDeaths);
                                        lastUpdatedTV.setText("Last updated: " + timeStamp);

                                        documentReference = db.collection(COLLECTION_COVID_INFORMATION).document(userID);

                                        Map<String, Object> history = new HashMap<>();
                                        history.put(FIREBASE_NEW_CASES,newCases);
                                        history.put(FIREBASE_ACTIVE_CASES,activeCases);
                                        history.put(FIREBASE_CRITICAL_CASES,criticalCases);
                                        history.put(FIREBASE_RECOVERED,recovered);
                                        history.put(FIREBASE_DEATHS,newDeaths);
                                        history.put(FIREBASE_LAST_UPDATED,timeStamp);

                                        documentReference.update(history).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(RegisterActivity.TAG, "Successfully uploaded info ");
                                                customeDialog.hide();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(RegisterActivity.TAG, "Error: " + e.getMessage());
                                            }
                                        });
                                    }
                                });

                                customeDialog.setTitle("What do u want to Do ?");
                                customeDialog.setCanceledOnTouchOutside(false);
                                customeDialog.show();
                            } else {
                                Toast.makeText(getContext(), "Everything is up to date", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String,String> headers = new HashMap<>();
                headers.put("x-rapidapi-key", "9b73cb45b8msh13466e11c26860ep19ac0ajsn061d10b748a1");
                headers.put("x-rapidapi-host", "covid-193.p.rapidapi.com");

                return headers;
            }
        };

        myQueue.add(request);
    }
}