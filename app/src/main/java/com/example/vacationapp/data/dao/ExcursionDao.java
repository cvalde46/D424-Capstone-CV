package com.example.vacationapp.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.vacationapp.data.entity.ExcursionEntity;

import java.util.List;

@Dao
public interface ExcursionDao {

    @Insert
    long insert(ExcursionEntity excursion);

    @Update
    void update(ExcursionEntity excursion);

    @Delete
    void delete(ExcursionEntity excursion);

    @Query("SELECT * FROM excursions WHERE excursionId = :excursionId LIMIT 1")
    ExcursionEntity getExcursionById(int excursionId);

    @Query("SELECT * FROM excursions WHERE vacationId = :vacationId ORDER BY date ASC")
    List<ExcursionEntity> getExcursionsForVacation(int vacationId);

    @Query("SELECT COUNT(*) FROM excursions WHERE vacationId = :vacationId")
    int countForVacation(int vacationId);
}


