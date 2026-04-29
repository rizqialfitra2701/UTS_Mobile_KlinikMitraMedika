package com.example.klinikmitramedika;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

public class AdminDashboardActivity extends AppCompatActivity {

    TextView tvNow;
    Spinner spinnerPoliAdmin;
    Button btnNext;

    DatabaseReference db;

    String[] poliList = {"Umum", "Gigi"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvNow = findViewById(R.id.tvNow);
        spinnerPoliAdmin = findViewById(R.id.spinnerPoliAdmin);
        btnNext = findViewById(R.id.btnNext);

        db = FirebaseDatabase.getInstance().getReference();

        // 🔹 Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, poliList);
        spinnerPoliAdmin.setAdapter(adapter);

        // 🔥 REALTIME TAMPIL CURRENT QUEUE
        spinnerPoliAdmin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String poli = spinnerPoliAdmin.getSelectedItem().toString().toLowerCase();

                db.child("current_queue").child(poli)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String data = snapshot.getValue(String.class);
                                    tvNow.setText("Sedang dipanggil: " + data);
                                } else {
                                    tvNow.setText("Sedang dipanggil: -");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {

                            }
                        });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 🔹 BUTTON NEXT
        btnNext.setOnClickListener(v -> nextAntrian());
    }

    // 🔥 FUNCTION NEXT ANTRIAN (SUDAH FIX + NAMA PASIEN)
    private void nextAntrian() {

        String poli = spinnerPoliAdmin.getSelectedItem().toString().toLowerCase();

        DatabaseReference antrianRef = db.child("antrian").child(poli);

        antrianRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot data : snapshot.getChildren()) {

                    String status = data.child("status").getValue(String.class);

                    // 🔥 CEK BIAR GAK CRASH
                    if (status != null && status.equals("menunggu")) {

                        String nomor = data.getKey();
                        String nama = data.child("nama").getValue(String.class);

                        if (nama == null) nama = "Tanpa Nama";

                        String hasil = nomor + " - " + nama;

                        // 🔥 SIMPAN KE CURRENT QUEUE
                        db.child("current_queue")
                                .child(poli)
                                .setValue(hasil);

                        // 🔥 UPDATE STATUS
                        data.getRef().child("status").setValue("dipanggil");

                        // 🔥 UPDATE TEXT ADMIN
                        tvNow.setText("Sedang dipanggil: " + hasil);

                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}