package com.example.vacationapp.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vacationapp.R;
import com.example.vacationapp.adapter.VacationAdapter;
import com.example.vacationapp.data.database.AppDatabase;
import com.example.vacationapp.data.entity.VacationEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.appcompat.widget.SearchView;

public class VacationListActivity extends AppCompatActivity {

    private AppDatabase db;
    private VacationAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vacation_list);

        db = AppDatabase.getInstance(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.vacationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchView = findViewById(R.id.vacationSearchView);
        setupSearch();

        DividerItemDecoration divider =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(divider);

        adapter = new VacationAdapter(
                // Row click -> edit vacation
                vacation -> {
                    Intent intent = new Intent(VacationListActivity.this, VacationDetailActivity.class);
                    intent.putExtra("vacationId", vacation.getVacationId());
                    startActivity(intent);
                },
                // Share click -> share vacation details
                vacation -> shareVacation(vacation),
                // Excursions click -> open ExcursionListActivity
                vacation -> {
                    Intent intent = new Intent(VacationListActivity.this, ExcursionListActivity.class);
                    intent.putExtra("vacationId", vacation.getVacationId());
                    startActivity(intent);
                }
        );


        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.addVacationFab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(VacationListActivity.this, VacationDetailActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVacations();
    }

    private void loadVacations() {
        executor.execute(() -> {
            List<VacationEntity> vacations = db.vacationDao().getAllVacations();
            runOnUiThread(() -> adapter.setVacations(vacations));
        });
    }

    private void shareVacation(VacationEntity v) {
        // Use the entity fields that are already loaded (title/hotel/start/end)
        String shareText =
                "Vacation: " + safe(v.getTitle()) + "\n" +
                        "Hotel: " + safe(v.getHotel()) + "\n" +
                        "Start: " + safe(v.getStartDate()) + "\n" +
                        "End: " + safe(v.getEndDate());

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Vacation Details");
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(sendIntent, "Share Vacation"));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                runSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                runSearch(newText);
                return true;
            }
        });
    }

    private void runSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadVacations(); // reuse existing behavior
            return;
        }

        executor.execute(() -> {
            String q = "%" + query.trim() + "%";
            List<VacationEntity> results =
                    db.vacationDao().searchVacations(q);

            runOnUiThread(() -> adapter.setVacations(results));
        });
    }

}
