package com.example.lab5_starter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // addDummyData();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });



        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // modified
        citiesRef.addSnapshotListener(((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (value == null) return;

            cityArrayList.clear();
            for (QueryDocumentSnapshot snapshot : value) {
                String name = snapshot.getString("name");
                String province = snapshot.getString("province");

                cityArrayList.add(new City(name, province));
            }
            cityArrayAdapter.notifyDataSetChanged();
        }));

        cityListView.setOnTouchListener(new View.OnTouchListener() {
            float startX;
            float startY;
            boolean swiping = false;

            @Override
            public  boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    // record start
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        swiping = false;
                        return false;

                    // track movement
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - startX;
                        float dy = event.getY() - startY;

                        // check if swipe
                        if (Math.abs(dx) > 200 && Math.abs(dx) > Math.abs(dy)) {
                            swiping = true;
                            return true; // prevent edit start
                        }
                        return false;

                    // check action
                    case MotionEvent.ACTION_UP:
                        if (swiping) {
                            int pos = cityListView.pointToPosition((int) event.getX(), (int) event.getY()); // locate end pos
                            if (pos != ListView.INVALID_POSITION) {
                                City city = cityArrayAdapter.getItem(pos);
                                if (city != null) confirmDelete(city);
                            }
                            return true; // prevent edit start
                        }
                        return false;
                }
                return false;
            }
        });
    }

    private void confirmDelete(City city) {
        new AlertDialog.Builder(this)
                .setTitle("Delete city")
                .setMessage("Delete " + city.getName() + " " + city.getProvince() + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    citiesRef.document(city.getName()).delete();
                })
                .show();
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();

        city.setName(title);
        city.setProvince(year);

        // Updating the database using delete + addition
        if (oldName.equals(title)) {
            citiesRef.document(oldName).set(city);
        } else {
            citiesRef.document(oldName).delete();
            citiesRef.document(title).set(city);
        }

        cityArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    /**
     * public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
     */
}