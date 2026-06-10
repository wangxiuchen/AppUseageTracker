package com.appspy.ui.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appspy.data.db.entity.AppMeta
import com.appspy.data.db.entity.DailyUsage
import com.appspy.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TrendMetric { DURATION, COUNT }
enum class TrendRange(val days: Int, val label: String) {
    SEVEN(7, "最近 7 天"),
    THIRTY(30, "30 天")
}

/** 单个 app 在时间轴上的系列数据 */
data class AppSeries(
    val packageName: String,
    val appLabel: String,
    /** 按日期升序排列的值（时长 ms 或次数） */
    val values: List<Float>,
    val totalMs: Long,
    val totalCount: Int
)

data class TrendUiState(
    val isLoading: Boolean = false,
    // 所有有历史数据的 app（用于选择器）
    val availableApps: List<AppMeta> = emptyList(),
    // 当前已选中的包名
    val selectedPackages: Set<String> = emptySet(),
    val metric: TrendMetric = TrendMetric.DURATION,
    val range: TrendRange = TrendRange.SEVEN,
    // 折线图数据
    val series: List<AppSeries> = emptyList(),
    // X 轴日期标签（简短，如"周一"）
    val xLabels: List<String> = emptyList(),
    // 选择器是否展开
    val selectorOpen: Boolean = false
)

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendUiState(isLoading = true))
    val uiState: StateFlow<TrendUiState> = _uiState.asStateFlow()

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadAvailableApps()
    }

    private fun loadAvailableApps() {
        viewModelScope.launch {
            repository.getDistinctPackagesFlow()
                .onEach { packages ->
                    val metas = packages.map { pkg ->
                        val label = repository.getAppLabel(pkg)
                        AppMeta(pkg, label)
                    }.sortedBy { it.appLabel }
                    _uiState.value = _uiState.value.copy(availableApps = metas, isLoading = false)
                }
                .launchIn(this)
        }
    }

    fun toggleApp(pkg: String) {
        val current = _uiState.value.selectedPackages.toMutableSet()
        if (pkg in current) current.remove(pkg) else current.add(pkg)
        _uiState.value = _uiState.value.copy(selectedPackages = current)
        refreshChart()
    }

    fun setMetric(metric: TrendMetric) {
        _uiState.value = _uiState.value.copy(metric = metric)
        refreshChart()
    }

    fun setRange(range: TrendRange) {
        _uiState.value = _uiState.value.copy(range = range)
        refreshChart()
    }

    fun openSelector() {
        _uiState.value = _uiState.value.copy(selectorOpen = true)
    }

    fun closeSelector() {
        _uiState.value = _uiState.value.copy(selectorOpen = false)
    }

    private fun refreshChart() {
        val state = _uiState.value
        if (state.selectedPackages.isEmpty()) {
            _uiState.value = state.copy(series = emptyList(), xLabels = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val today = LocalDate.now()
                val rangeDay = _uiState.value.range.days
                val dates = (rangeDay - 1 downTo 0).map { today.minusDays(it.toLong()) }
                val dateStrs = dates.map { it.format(dateFmt) }

                val dbRows: List<DailyUsage> = repository.getDailyUsageForApps(
                    packages = _uiState.value.selectedPackages.toList(),
                    startDate = dateStrs.first(),
                    endDate = dateStrs.last()
                )

                // 按包名 → 日期 → 数据 的查找表
                val byPkg = dbRows.groupBy { it.packageName }
                    .mapValues { (_, rows) -> rows.associateBy { it.date } }

                val seriesList = _uiState.value.selectedPackages.map { pkg ->
                    val label = _uiState.value.availableApps.find { it.packageName == pkg }?.appLabel ?: pkg
                    val dailyMap = byPkg[pkg] ?: emptyMap()
                    val values = dateStrs.map { d ->
                        val row = dailyMap[d]
                        when (_uiState.value.metric) {
                            TrendMetric.DURATION -> (row?.totalForegroundMs ?: 0L).toFloat()
                            TrendMetric.COUNT    -> (row?.openCount ?: 0).toFloat()
                        }
                    }
                    AppSeries(
                        packageName = pkg,
                        appLabel = label,
                        values = values,
                        totalMs = dailyMap.values.sumOf { it.totalForegroundMs },
                        totalCount = dailyMap.values.sumOf { it.openCount }
                    )
                }

                val xLabels = dates.map { shortDayLabel(it) }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    series = seriesList,
                    xLabels = xLabels
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun shortDayLabel(date: LocalDate): String = when {
        date == LocalDate.now() -> "今"
        else -> when (date.dayOfWeek.value) {
            1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"
            5 -> "五"; 6 -> "六"; 7 -> "日"; else -> ""
        }
    }
}
