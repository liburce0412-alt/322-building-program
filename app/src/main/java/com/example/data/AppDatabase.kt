package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "time_records")
data class TimeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // Study, Sports, Entertainment, etc.
    val description: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "goods")
data class GoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val tags: String, // comma separated
    val ownerName: String = "Campus User"
)

@Dao
interface AppDao {
    @Query("SELECT * FROM time_records ORDER BY timestamp DESC")
    fun getAllTimeRecords(): Flow<List<TimeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeRecord(record: TimeRecord)

    // For simplicity, just get one week of stats
    @Query("SELECT * FROM time_records WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getRecentRecords(since: Long): List<TimeRecord>

    @Query("SELECT * FROM goods ORDER BY id DESC")
    fun getAllGoods(): Flow<List<GoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGood(good: GoodItem)
}

@Database(entities = [TimeRecord::class, GoodItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
