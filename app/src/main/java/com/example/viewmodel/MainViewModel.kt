package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.ai.DeepSeekService
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GoodItem
import com.example.data.TimeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "campus_ai_db"
    ).build()

    private val repository = AppRepository(db.appDao())

    val allTimeRecords: StateFlow<List<TimeRecord>> = repository.allTimeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoods: StateFlow<List<GoodItem>> = repository.allGoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiAnalysisState = MutableStateFlow("点击上方‘一键生成报告’按钮，AI将根据您的柳比歇夫时间记录生成极富洞察力的成长建议。")

    fun addTimeRecord(category: String, description: String, durationMins: Int) {
        viewModelScope.launch {
            repository.insertTimeRecord(
                TimeRecord(
                    category = category,
                    description = description,
                    durationMinutes = durationMins
                )
            )
        }
    }

    fun addGoodItem(title: String, description: String, tags: String) {
        viewModelScope.launch {
            repository.insertGood(
                GoodItem(
                    title = title,
                    description = description,
                    tags = tags
                )
            )
        }
    }

    fun generateAiAnalysis() {
        viewModelScope.launch {
            aiAnalysisState.value = "AI 正在深度分析您的日常习惯与心流时段..."
            // Get records from last 7 days usually, but for demo just get all from state
            val records = allTimeRecords.value
            if (records.isEmpty()) {
                aiAnalysisState.value = "您目前还没有记录任何专注时间。请在‘时间统计’中添加一些专注记录后重试！"
                return@launch
            }
            
            val stats = records.groupBy { it.category }.map { (cat, list) -> 
                 "$cat: ${list.sumOf { it.durationMinutes }} 分钟" 
            }.joinToString("\n")
            
            val details = records.take(15).joinToString("\n") { 
                "- ${it.category}: ${it.durationMinutes}分钟 (${it.description})" 
            }
            
            val promptText = "Summary:\n$stats\n\nRecent Details:\n$details"
            
            val result = DeepSeekService.generateAnalysis(promptText)
            aiAnalysisState.value = result
        }
    }
}
