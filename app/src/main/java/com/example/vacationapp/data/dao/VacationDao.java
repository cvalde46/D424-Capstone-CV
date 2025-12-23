package com.example.vacationapp.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.vacationapp.data.entity.VacationEntity;

import java.util.List;

@Dao
public interface VacationDao {

    @Query("SELECT * FROM vacations ORDER BY vacationId DESC")
    List<VacationEntity> getAllVacations();

    @Query("SELECT * FROM vacations WHERE vacationId = :id LIMIT 1")
    VacationEntity getVacationById(int id);

    @Insert
    long insert(VacationEntity vacation);

    @Update
    int update(VacationEntity vacation);

    @Delete
    int delete(VacationEntity vacation);

    @Query("SELECT * FROM vacations WHERE title LIKE :query OR hotel LIKE :query ORDER BY startDate ASC")
    List<VacationEntity> searchVacations(String query);

}
