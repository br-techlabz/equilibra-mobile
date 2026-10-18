package br.com.equilibra.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.dashboard.DashboardRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.model.dashboard.Dashboard
import br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Estado da UI do Dashboard.
 *
 * Task 2.3: estados completos (Loading, Success, Error) com período aplicado.
 * Task 2.9: período será mutável (mês atual, anterior, custom).
 * Task 2.10: sem cache Room — sempre consulta API.
 */
sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val dashboard: Dashboard,
        val periodLabel: String
    ) : DashboardUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : DashboardUiState()
}

/**
 * ViewModel do Dashboard.
 *
 * Carrega sempre da API (sem cache local).
 * Período padrão: mês atual (Task 2.9 expandirá).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentFrom: String = monthStartIso()
    private var currentTo: String = monthEndIso()

    init {
        load()
    }

    fun load() {
        load(currentFrom, currentTo)
    }

    fun load(from: String, to: String) {
        currentFrom = from
        currentTo = to

        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            when (val result = repository.getDashboard(from, to)) {
                is ApiResult.Success -> {
                    val periodLabel = formatPeriodLabel(from, to)
                    _uiState.value = DashboardUiState.Success(
                        dashboard = result.data,
                        periodLabel = periodLabel
                    )
                }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> "Período inválido. Tente novamente."
                        401 -> "Sessão expirada. Faça login novamente."
                        else -> "Erro ao carregar dashboard (${result.code})."
                    }
                    _uiState.value = DashboardUiState.Error(
                        message = msg,
                        canRetry = true
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = DashboardUiState.Error(
                        message = "Sem conexão. Verifique sua internet e tente novamente.",
                        canRetry = true
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = DashboardUiState.Error(
                        message = "Erro inesperado. Tente novamente.",
                        canRetry = true
                    )
                }
            }
        }
    }

    fun retry() = load()

    /**
     * Formata o período para exibição (ex.: "01/10/2026 — 31/10/2026").
     */
    private fun formatPeriodLabel(from: String, to: String): String {
        return try {
            val fromDate = Instant.parse(from).atZone(ZoneId.systemDefault()).toLocalDate()
            val toDate = Instant.parse(to).atZone(ZoneId.systemDefault()).toLocalDate()
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            "${fromDate.format(fmt)} — ${toDate.format(fmt)}"
        } catch (_: Exception) {
            "$from — $to"
        }
    }

    companion object {
        /**
         * Mês atual: 1º dia 00:00 UTC até último dia 23:59:59 UTC.
         */
        fun monthStartIso(): String {
            val now = LocalDate.now(ZoneId.systemDefault())
            return now.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString()
        }

        fun monthEndIso(): String {
            val now = LocalDate.now(ZoneId.systemDefault())
            val lastDay = now.withDayOfMonth(now.lengthOfMonth())
            return lastDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString()
        }
    }
}
