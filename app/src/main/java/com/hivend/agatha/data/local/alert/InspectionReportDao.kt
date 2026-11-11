package com.hivend.agatha.data.local.alert

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionReportDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReport(report: InspectionReportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotos(photos: List<InspectionPhotoEntity>)

    @Query("SELECT * FROM inspection_reports ORDER BY recordedAt DESC")
    fun observeReports(): Flow<List<InspectionReportEntity>>

    @Query("SELECT * FROM inspection_photos")
    fun observePhotos(): Flow<List<InspectionPhotoEntity>>
}
