package com.appspy.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appspy.data.repository.UsageRepository
import com.appspy.di.scheduleSnapshotWork
import com.appspy.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.work.WorkManager
import javax.inject.Inject

enum class SortMode { BY_DURATION, BY_COUNT }

data class TodayUiState(
    val isLoading: Boolean = false,
    val appList: List<UsageStatsHelper.AppUsageData> = emptyList(),
    val sortMode: SortMode = SortMode.BY_DURATION,
    val dateLabel: String = "",
    val totalDurationMs: Long = 0L,
    val totalOpenCount: Int = 0,
    val activeApps: Int = 0
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: UsageRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        // 确保后台快照任务已注册
        scheduleSnapshotWork(workManager)
        loadTodayData()
    }

    fun loadTodayData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val rawData = repository.getTodayUsageLive()

                // 同时触发落库 + 清理旧版误入库的系统组件（不阻塞 UI，异步执行）
                launch {
                    try {
                        repository.snapshotDate(LocalDate.now())
                        repository.purgeNonLaunchablePackages()
                    } catch (_: Exception) {}
                }

                val sorted = sortList(rawData, _uiState.value.sortMode)
                _uiState.value = TodayUiState(
                    isLoading = false,
                    appList = sorted,
                    sortMode = _uiState.value.sortMode,
                    dateLabel = buildDateLabel(),
                    totalDurationMs = rawData.sumOf { it.totalForegroundMs },
                    totalOpenCount = rawData.sumOf { it.openCount },
                    activeApps = rawData.size
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setSortMode(mode: SortMode) {
        val sorted = sortList(_uiState.value.appList, mode)
        _uiState.value = _uiState.value.copy(sortMode = mode, appList = sorted)
    }

    private fun sortList(
        list: List<UsageStatsHelper.AppUsageData>,
        mode: SortMode
    ): List<UsageStatsHelper.AppUsageData> = when (mode) {
        SortMode.BY_DURATION -> list.sortedByDescending { it.totalForegroundMs }
        SortMode.BY_COUNT    -> list.sortedByDescending { it.openCount }
    }

    private fun buildDateLabel(): String {
        val now = LocalDateTime.now()
        val dayOfWeek = when (now.dayOfWeek.value) {
            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
            5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
        }
        return "${now.monthValue} 月 ${now.dayOfMonth} 日 $dayOfWeek"
    }
}
