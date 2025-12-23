package com.example.vacationapp.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vacationapp.data.alerts.AlertReceiver;
import com.example.vacationapp.R;
import com.example.vacationapp.data.database.AppDatabase;
import com.example.vacationapp.data.entity.ExcursionEntity;
import com.example.vacationapp.data.entity.VacationEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcursionDetailActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText dateEditText;

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int excursionId = -1;
    private int vacationId = -1;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int REQ_NOTIF_PERMISSION = 601;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excursion_detail);

        titleEditText = findViewById(R.id.excursionTitleEditText);
        dateEditText = findViewById(R.id.excursionDateEditText);

        Button saveButton = findViewById(R.id.saveExcursionButton);
        Button deleteButton = findViewById(R.id.deleteExcursionButton);
        Button setAlertButton = findViewById(R.id.setExcursionAlertButton);

        db = AppDatabase.getInstance(getApplicationContext());

        vacationId = getIntent().getIntExtra("vacationId", -1);
        excursionId = getIntent().getIntExtra("excursionId", -1);

        dateEditText.setOnClickListener(v -> showDatePicker(dateEditText));

        if (excursionId != -1) {
            loadExcursion();
        }

        saveButton.setOnClickListener(v -> saveExcursion());
        deleteButton.setOnClickListener(v -> deleteExcursion());
        setAlertButton.setOnClickListener(v -> setExcursionAlert());
    }

    private void loadExcursion() {
        executor.execute(() -> {
            ExcursionEntity e = db.excursionDao().getExcursionById(excursionId);
            if (e != null) {
                runOnUiThread(() -> {
                    titleEditText.setText(safe(e.getTitle()));
                    dateEditText.setText(safe(e.getDate()));
                });
            }
        });
    }

    private void saveExcursion() {
        String title = titleEditText.getText().toString().trim();
        String dateText = dateEditText.getText().toString().trim();

        if (vacationId == -1) {
            Toast.makeText(this, "Missing vacation id", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty()) {
            Toast.makeText(this, "Excursion title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dateText.isEmpty()) {
            Toast.makeText(this, "Excursion date is required", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDate excursionDate = parseDateOrNull(dateText);
        if (excursionDate == null) {
            Toast.makeText(this, "Date must be in YYYY-MM-DD format", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            VacationEntity v = db.vacationDao().getVacationById(vacationId);
            if (v == null) {
                runOnUiThread(() -> Toast.makeText(this, "Vacation not found", Toast.LENGTH_SHORT).show());
                return;
            }

            LocalDate start = parseDateOrNull(v.getStartDate());
            LocalDate end = parseDateOrNull(v.getEndDate());
            if (start == null || end == null) {
                runOnUiThread(() -> Toast.makeText(this, "Vacation dates are invalid", Toast.LENGTH_SHORT).show());
                return;
            }

            boolean inside = !excursionDate.isBefore(start) && !excursionDate.isAfter(end);
            if (!inside) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Excursion date must be during the vacation (" + v.getStartDate() + " to " + v.getEndDate() + ")",
                        Toast.LENGTH_LONG
                ).show());
                return;
            }

            if (excursionId == -1) {
                db.excursionDao().insert(new ExcursionEntity(vacationId, title, dateText));
            } else {
                ExcursionEntity updated = new ExcursionEntity(vacationId, title, dateText);
                updated.setExcursionId(excursionId);
                db.excursionDao().update(updated);
            }

            runOnUiThread(this::finish);
        });
    }
    private void deleteExcursion() {
        if (excursionId == -1) {
            finish();
            return;
        }

        executor.execute(() -> {
            ExcursionEntity toDelete = new ExcursionEntity(vacationId, "", "");
            toDelete.setExcursionId(excursionId);
            db.excursionDao().delete(toDelete);
            runOnUiThread(this::finish);
        });
    }
    private void setExcursionAlert() {
        String title = titleEditText.getText().toString().trim();
        String dateText = dateEditText.getText().toString().trim();

        if (title.isEmpty() || dateText.isEmpty()) {
            Toast.makeText(this, "Enter title and date first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ensureNotificationPermission()) {
            return; // tap again after granting
        }

        LocalDate excursionDate = parseDateOrNull(dateText);
        if (excursionDate == null) {
            Toast.makeText(this, "Date must be in YYYY-MM-DD format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fire at 9:00 AM local time on excursion date
        long triggerMillis = ZonedDateTime.of(excursionDate, LocalTime.of(9, 0), ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        String message = title;

        // requestCode: stable-ish; if not saved yet, use time-based fallback
        int requestCode = (excursionId != -1)
                ? (excursionId * 10) + 7
                : (int) (System.currentTimeMillis() & 0x7fffffff);

        scheduleAlarm(triggerMillis, requestCode, "Excursion Alert", message);

        Toast.makeText(this, "Excursion alert set", Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(long triggerAtMillis, int requestCode, String notifTitle, String notifMessage) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlertReceiver.class);
        intent.putExtra(AlertReceiver.EXTRA_TITLE, notifTitle);
        intent.putExtra(AlertReceiver.EXTRA_MESSAGE, notifMessage);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException se) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private boolean ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF_PERMISSION
                );
                return false;
            }
        }
        return true;
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    String date = String.format("%04d-%02d-%02d", year, month + 1, day);
                    target.setText(date);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dlg.show();
    }

    private LocalDate parseDateOrNull(String text) {
        try {
            return LocalDate.parse(text, DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String safe(String s) { return s == null ? "" : s; }
}
