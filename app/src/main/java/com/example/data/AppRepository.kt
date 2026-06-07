package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allTimeRecords: Flow<List<TimeRecord>> = appDao.getAllTimeRecords()
    val allGoods: Flow<List<GoodItem>> = appDao.getAllGoods()

    suspend fun insertTimeRecord(record: TimeRecord) {
        appDao.insertTimeRecord(record)
    }

    suspend fun getRecentRecords(since: Long): List<TimeRecord> {
        return appDao.getRecentRecords(since)
    }

    suspend fun insertGood(good: GoodItem) {
        appDao.insertGood(good)
    }
}
