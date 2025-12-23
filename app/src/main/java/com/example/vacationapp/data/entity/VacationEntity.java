package com.example.vacationapp.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "vacations")
public class VacationEntity {

    @PrimaryKey(autoGenerate = true)
    private int vacationId;

    private String title;
    private String hotel;
    private String startDate;
    private String endDate;

    private boolean alertsEnabled;
    private int alertHour;
    private int alertMinute;

    // Room will use this
    public VacationEntity(String title, String hotel, String startDate, String endDate,
                          boolean alertsEnabled, int alertHour, int alertMinute) {
        this.title = title;
        this.hotel = hotel;
        this.startDate = startDate;
        this.endDate = endDate;
        this.alertsEnabled = alertsEnabled;
        this.alertHour = alertHour;
        this.alertMinute = alertMinute;
    }

    // Convenience constructor (older code)
    @Ignore
    public VacationEntity(String title, String hotel, String startDate, String endDate) {
        this(title, hotel, startDate, endDate, false, 9, 0);
    }

    @Ignore
    public VacationEntity(String title) {
        this(title, "", "", "", false, 9, 0);
    }

    public int getVacationId() { return vacationId; }
    public void setVacationId(int vacationId) { this.vacationId = vacationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHotel() { return hotel; }
    public void setHotel(String hotel) { this.hotel = hotel; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }

    public int getAlertHour() { return alertHour; }
    public void setAlertHour(int alertHour) { this.alertHour = alertHour; }

    public int getAlertMinute() { return alertMinute; }
    public void setAlertMinute(int alertMinute) { this.alertMinute = alertMinute; }
}
