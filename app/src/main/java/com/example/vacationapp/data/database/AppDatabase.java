package com.example.vacationapp.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.vacationapp.data.dao.ExcursionDao;
import com.example.vacationapp.data.dao.VacationDao;
import com.example.vacationapp.data.entity.ExcursionEntity;
import com.example.vacationapp.data.entity.VacationEntity;

@Database(entities = {VacationEntity.class, ExcursionEntity.class}, version = 4 , exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract VacationDao vacationDao();
    public abstract ExcursionDao excursionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "vacation_app_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
