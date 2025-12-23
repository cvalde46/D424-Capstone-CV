package com.example.vacationapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vacationapp.R;

import com.example.vacationapp.data.database.AppDatabase;
import com.example.vacationapp.data.entity.VacationEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HomeActivity extends AppCompatActivity {
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = AppDatabase.getInstance(getApplicationContext());

        Button snapshotBtn = findViewById(R.id.btnSnapshotVacations);
        snapshotBtn.setOnClickListener(v -> snapshotVacations());


        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, VacationListActivity.class);
            startActivity(intent);
        });
    }
    private void snapshotVacations() {
        executor.execute(() -> {
            List<VacationEntity> vacations = db.vacationDao().getAllVacations();
            String report = buildVacationReport(vacations);

            runOnUiThread(() -> shareText("Vacation Snapshot Report", report));
        });
    }

    private String buildVacationReport(List<VacationEntity> vacations) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append("Vacation Snapshot Report\n");
        sb.append("Generated: ").append(timestamp).append("\n\n");
        sb.append("Title,Hotel,StartDate,EndDate\n");

        if (vacations != null) {
            for (VacationEntity v : vacations) {
                sb.append(csv(safe(v.getTitle()))).append(",");
                sb.append(csv(safe(v.getHotel()))).append(",");
                sb.append(csv(safe(v.getStartDate()))).append(",");
                sb.append(csv(safe(v.getEndDate()))).append("\n");
            }
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    private void shareText(String subject, String body) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sendIntent, "Share Report"));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

}
