package com.example.vacationapp.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vacationapp.data.alerts.AlertReceiver;
import com.example.vacationapp.R;
import com.example.vacationapp.data.database.AppDatabase;
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

public class VacationDetailActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText hotelEditText;
    private EditText startDateEditText;
    private EditText endDateEditText;

    private CheckBox alertsEnabledCheckBox;
    private EditText alertTimeEditText;

    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int vacationId = -1;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int REQ_NOTIF_PERMISSION = 501;

    // persisted alert time
    private int alertHour = 9;
    private int alertMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vacation_detail);

        titleEditText = findViewById(R.id.titleEditText);
        hotelEditText = findViewById(R.id.hotelEditText);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);

        alertsEnabledCheckBox = findViewById(R.id.alertsEnabledCheckBox);
        alertTimeEditText = findViewById(R.id.alertTimeEditText);

        db = AppDatabase.getInstance(getApplicationContext());

        // Date pickers
        startDateEditText.setOnClickListener(v -> showDatePicker(startDateEditText));
        endDateEditText.setOnClickListener(v -> showDatePicker(endDateEditText));

        // Time picker
        setAlertTimeText();
        alertTimeEditText.setOnClickListener(v -> showTimePicker());

        // buttons
        findViewById(R.id.saveButton).setOnClickListener(v -> saveVacation());
        findViewById(R.id.deleteButton).setOnClickListener(v -> deleteVacation());

        // Edit mode
        if (getIntent() != null && getIntent().hasExtra("vacationId")) {
            vacationId = getIntent().getIntExtra("vacationId", -1);
            if (vacationId != -1) loadVacation();
        }
    }

    private void loadVacation() {
        executor.execute(() -> {
            VacationEntity v = db.vacationDao().getVacationById(vacationId);
            if (v != null) {
                runOnUiThread(() -> {
                    titleEditText.setText(nullToEmpty(v.getTitle()));
                    hotelEditText.setText(nullToEmpty(v.getHotel()));
                    startDateEditText.setText(nullToEmpty(v.getStartDate()));
                    endDateEditText.setText(nullToEmpty(v.getEndDate()));

                    alertsEnabledCheckBox.setChecked(v.isAlertsEnabled());
                    alertHour = v.getAlertHour();
                    alertMinute = v.getAlertMinute();
                    setAlertTimeText();
                });
            }
        });
    }

    private void saveVacation() {
        String title = titleEditText.getText().toString().trim();
        String hotel = hotelEditText.getText().toString().trim();
        String startText = startDateEditText.getText().toString().trim();
        String endText = endDateEditText.getText().toString().trim();

        boolean alertsEnabled = alertsEnabledCheckBox.isChecked();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDate start = parseDateOrNull(startText);
        LocalDate end = parseDateOrNull(endText);

        if (start == null || end == null) {
            Toast.makeText(this, "Dates must be in YYYY-MM-DD format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (end.isBefore(start)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (alertsEnabled && !ensureNotificationPermission()) {
            return; // prompt shows; user taps save again
        }

        executor.execute(() -> {
            if (vacationId == -1) {
                VacationEntity entity = new VacationEntity(
                        title, hotel, startText, endText,
                        alertsEnabled, alertHour, alertMinute
                );
                long newId = db.vacationDao().insert(entity);
                vacationId = (int) newId;
            } else {
                VacationEntity updated = new VacationEntity(
                        title, hotel, startText, endText,
                        alertsEnabled, alertHour, alertMinute
                );
                updated.setVacationId(vacationId);
                db.vacationDao().update(updated);
            }

            if (alertsEnabled) {
                scheduleBothAlerts(title, start, end);
            }

            runOnUiThread(() -> Toast.makeText(this, "Vacation saved", Toast.LENGTH_SHORT).show());
        });
    }

    private void deleteVacation() {
        if (vacationId == -1) {
            finish();
            return;
        }

        executor.execute(() -> {
            int excursionCount = db.excursionDao().countForVacation(vacationId);

            if (excursionCount > 0) {
                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Cannot delete vacation with excursions",
                                Toast.LENGTH_LONG
                        ).show()
                );
            } else {
                VacationEntity toDelete = new VacationEntity("", "", "", "", false, 9, 0);
                toDelete.setVacationId(vacationId);
                db.vacationDao().delete(toDelete);
                runOnUiThread(this::finish);
            }
        });
    }

    private void openExcursions() {
        if (vacationId == -1) {
            Toast.makeText(this, "Save the vacation first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ExcursionListActivity.class);
        intent.putExtra("vacationId", vacationId);
        startActivity(intent);
    }

    private void shareVacationDetails() {
        String title = titleEditText.getText().toString().trim();
        String hotel = hotelEditText.getText().toString().trim();
        String startText = startDateEditText.getText().toString().trim();
        String endText = endDateEditText.getText().toString().trim();

        String shareText =
                "Vacation: " + title + "\n" +
                        "Hotel: " + hotel + "\n" +
                        "Start: " + startText + "\n" +
                        "End: " + endText;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Vacation Details");
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(sendIntent, "Share Vacation"));
    }

    private void scheduleBothAlerts(String title, LocalDate start, LocalDate end) {
        long startTrigger = toTimeMillis(start, alertHour, alertMinute);
        long endTrigger = toTimeMillis(end, alertHour, alertMinute);

        int startRequestCode = (vacationId * 10) + 1;
        int endRequestCode = (vacationId * 10) + 2;

        scheduleAlarm(startTrigger, startRequestCode, title, title + " is starting");
        scheduleAlarm(endTrigger, endRequestCode, title, title + " is ending");
    }

    private long toTimeMillis(LocalDate date, int hour, int minute) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute), ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
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

    // ===== Pickers / helpers =====
    private void showTimePicker() {
        TimePickerDialog dlg = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    alertHour = hourOfDay;
                    alertMinute = minute;
                    setAlertTimeText();
                },
                alertHour,
                alertMinute,
                true
        );
        dlg.show();
    }

    private void setAlertTimeText() {
        alertTimeEditText.setText(String.format("%02d:%02d", alertHour, alertMinute));
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

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
