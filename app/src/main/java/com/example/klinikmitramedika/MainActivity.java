package com.example.klinikmitramedika;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    EditText etNama, etKeluhan;
    Spinner spinnerPoli;
    Button btnAmbil, btnAdmin;
    TextView tvNomor, tvCurrent;

    DatabaseReference db;

    String[] poliList = {"Umum", "Gigi"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNama = findViewById(R.id.etNama);
        etKeluhan = findViewById(R.id.etKeluhan);
        spinnerPoli = findViewById(R.id.spinnerPoli);
        btnAmbil = findViewById(R.id.btnAmbil);
        btnAdmin = findViewById(R.id.btnAdmin);
        tvNomor = findViewById(R.id.tvNomor);
        tvCurrent = findViewById(R.id.tvCurrent);

        db = FirebaseDatabase.getInstance().getReference();

        // Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, poliList);
        spinnerPoli.setAdapter(adapter);

        // Realtime antrian sekarang
        spinnerPoli.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String poli = spinnerPoli.getSelectedItem().toString();
                lihatAntrianSekarang(poli);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Button ke admin
        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AdminLoginActivity.class))
        );

        // Button ambil antrian
        btnAmbil.setOnClickListener(v -> {

            String nama = etNama.getText().toString().trim();
            String keluhan = etKeluhan.getText().toString().trim();
            String poli = spinnerPoli.getSelectedItem().toString();

            if(nama.isEmpty() || keluhan.isEmpty()){
                Toast.makeText(MainActivity.this, "Isi semua data!", Toast.LENGTH_SHORT).show();
                return;
            }

            ambilAntrian(nama, keluhan, poli);
        });
    }

    // 🔥 AMBIL ANTRIAN (VERSI FIX)
    private void ambilAntrian(String nama, String keluhan, String poli){

        String poliKey = poli.toLowerCase();

        DatabaseReference counterRef = db.child("counter").child(poliKey);

        counterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                int nomor = 1;

                if(snapshot.exists()){
                    Integer val = snapshot.getValue(Integer.class);
                    if(val != null) nomor = val + 1;
                }

                // update counter
                counterRef.setValue(nomor);

                String kode = poliKey.equals("umum") ? "A" : "G";
                String nomorAntrian = kode + nomor;

                // 🔥 SIMPAN DATA (PAKAI MAP BIAR AMAN)
                DatabaseReference antrianRef = db.child("antrian")
                        .child(poliKey)
                        .child(nomorAntrian);

                HashMap<String, Object> data = new HashMap<>();
                data.put("nama", nama);
                data.put("keluhan", keluhan);
                data.put("status", "menunggu");

                antrianRef.setValue(data);

                tvNomor.setText("Nomor Antrian: " + nomorAntrian);

                // 🔥 CLEAR INPUT BIAR ENAK
                etNama.setText("");
                etKeluhan.setText("");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 REALTIME CURRENT QUEUE
    private void lihatAntrianSekarang(String poli){

        String poliKey = poli.toLowerCase();

        DatabaseReference currentRef = db.child("current_queue").child(poliKey);

        currentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if(snapshot.exists()){
                    String data = snapshot.getValue(String.class);
                    tvCurrent.setText("Sedang dipanggil: " + data);
                } else {
                    tvCurrent.setText("Sedang dipanggil: -");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
}