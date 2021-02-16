package uni.fmi.masters.covid_19statistic.ui.covidStatistics;

import android.app.DatePickerDialog;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import uni.fmi.masters.covid_19statistic.R;

public class CovidStatisticsFragment extends Fragment {

    Spinner spinner;
    String selectedCountry, url;
    TextView dateTV, newCasesTV, activeTV, criticalTV, recoveredTV, deathsTV ;
    Button getStatisticsB;
    DatePickerDialog.OnDateSetListener dateSetListener;
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
        getStatisticsB = root.findViewById(R.id.checkForUpdatesButton);
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
        getStatisticsB.setOnClickListener(new View.OnClickListener() {
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
//                            JSONObject results = response.getJSONObject("results");
                            JSONArray mResponse = response.getJSONArray("response");
                            JSONObject jsonObject = mResponse.getJSONObject(0);
                            JSONObject cases = jsonObject.getJSONObject("cases");
                            JSONObject deaths = jsonObject.getJSONObject("deaths");

                            String newCases = cases.getString("new");
                            String activeCases = cases.getString("active");
                            String criticalCases = cases.getString("critical");
                            String recovered = cases.getString("recovered");
                            String newDeaths = deaths.getString("new");

                            newCasesTV.setText("Новозаразени:\n" + newCases);
                            activeTV.setText("Активни:\n" + activeCases);
                            criticalTV.setText("Критични:\n" + criticalCases);
                            recoveredTV.setText("Излекувани:\n" + recovered);
                            deathsTV.setText("Починали:\n" + newDeaths);

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