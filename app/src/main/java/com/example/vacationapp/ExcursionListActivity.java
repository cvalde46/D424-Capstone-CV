package com.example.vacationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vacationapp.data.database.AppDatabase;
import com.example.vacationapp.data.entity.ExcursionEntity;
import com.example.vacationapp.data.entity.VacationEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcursionListActivity extends AppCompatActivity {

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int vacationId = -1;

    private ExcursionAdapter adapter;
    private TextView header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excursion_list);

        db = AppDatabase.getInstance(getApplicationContext());

        header = findViewById(R.id.excursionListHeader);

        vacationId = getIntent().getIntExtra("vacationId", -1);
        if (vacationId == -1) {
            Toast.makeText(this, "Missing vacation id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RecyclerView rv = findViewById(R.id.excursionListRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new ExcursionAdapter(excursion -> {
            Intent intent = new Intent(ExcursionListActivity.this, ExcursionDetailActivity.class);
            intent.putExtra("vacationId", vacationId);
            intent.putExtra("excursionId", excursion.getExcursionId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        findViewById(R.id.addExcursionFromListButton).setOnClickListener(v -> {
            Intent intent = new Intent(ExcursionListActivity.this, ExcursionDetailActivity.class);
            intent.putExtra("vacationId", vacationId);
            startActivity(intent);
        });

        loadHeaderTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExcursions();
    }

    private void loadHeaderTitle() {
        executor.execute(() -> {
            VacationEntity v = db.vacationDao().getVacationById(vacationId);
            String title = (v == null || v.getTitle() == null) ? "Excursions" : ("Excursions: " + v.getTitle());
            runOnUiThread(() -> header.setText(title));
        });
    }

    private void loadExcursions() {
        executor.execute(() -> {
            List<ExcursionEntity> list = db.excursionDao().getExcursionsForVacation(vacationId);
            runOnUiThread(() -> adapter.setExcursions(list));
        });
    }
}

