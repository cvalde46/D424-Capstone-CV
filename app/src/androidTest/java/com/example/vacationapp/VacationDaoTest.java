package com.example.vacationapp;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.vacationapp.data.database.AppDatabase;
import com.example.vacationapp.data.dao.VacationDao;
import com.example.vacationapp.data.entity.VacationEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class VacationDaoTest {

    private AppDatabase db;
    private VacationDao vacationDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        vacationDao = db.vacationDao();
    }

    @After
    public void teardown() {
        db.close();
    }

    @Test
    public void insertAndSearchVacations_returnsMatchingRows() {
        VacationEntity v1 = new VacationEntity("Hawaii Trip","Hilton","2025-01-01","2025-01-05");
        VacationEntity v2 = new VacationEntity("Hawaii Weekend","Marriott","2025-02-01","2025-02-03");
        VacationEntity v3 = new VacationEntity("Seattle","Hyatt","2025-03-01","2025-03-04");

        // Insert
        vacationDao.insert(v1);
        vacationDao.insert(v2);
        vacationDao.insert(v3);

        // Search
        List<VacationEntity> results = vacationDao.searchVacations("%Hawaii%");

        assertNotNull(results);
        assertEquals(2, results.size());

        // Confirm the two Hawaii rows are present
        boolean found1 = false;
        boolean found2 = false;

        for (VacationEntity v : results) {
            if ("Hawaii Trip".equals(v.getTitle())) found1 = true;
            if ("Hawaii Weekend".equals(v.getTitle())) found2 = true;
        }

        assertTrue(found1);
        assertTrue(found2);
    }
}
