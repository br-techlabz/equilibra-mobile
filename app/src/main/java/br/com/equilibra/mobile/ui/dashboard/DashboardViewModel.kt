package br.com.equilibra.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.dashboard.DashboardRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.model.dashboard.Dashboard
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodSelection
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodType
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
 * Task 2.9: período mutável (mês atual, anterior, custom).
 * Task 2.10: sem cache Room — sempre consulta API.
 */
sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val dashboard: Dashboard,
        val periodLabel: String,
        val selectedPeriod: DashboardPeriodSelection
    ) : DashboardUiState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val selectedPeriod: DashboardPeriodSelection? = null
    ) : DashboardUiState()
}

/**
 * ViewModel do Dashboard.
 *
 * Carrega sempre da API (sem cache local).
 * Período padrão: mês atual. Task 2.9: filtro mutável com validação.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentPeriod: DashboardPeriodSelection = DashboardPeriodSelection.currentMonth()

    init {
        load(currentPeriod)
    }

    fun load() {
        load(currentPeriod)
    }

    fun load(period: DashboardPeriodSelection) {
        // Valida intervalo antes de chamar API
        if (!period.isValid()) {
            _uiState.value = DashboardUiState.Error(
                message = "Período inválido: a data inicial deve ser anterior à data final.",
                canRetry = false,
                selectedPeriod = period
            )
            return
        }

        currentPeriod = period
        val (from, to) = period.toIso()

        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            when (val result = repository.getDashboard(from, to)) {
                is ApiResult.Success -> {
                    val periodLabel = formatPeriodLabel(period)
                    _uiState.value = DashboardUiState.Success(
                        dashboard = result.data,
                        periodLabel = periodLabel,
                        selectedPeriod = period
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
                        canRetry = true,
                        selectedPeriod = period
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = DashboardUiState.Error(
                        message = "Sem conexão. Verifique sua internet e tente novamente.",
                        canRetry = true,
                        selectedPeriod = period
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = DashboardUiState.Error(
                        message = "Erro inesperado. Tente novamente.",
                        canRetry = true,
                        selectedPeriod = period
                    )
                }
            }
        }
    }

    fun retry() = load(currentPeriod)

    fun selectPeriod(type: DashboardPeriodType) {
        val period = when (type) {
            DashboardPeriodType.CURRENT_MONTH -> DashboardPeriodSelection.currentMonth()
            DashboardPeriodType.PREVIOUS_MONTH -> DashboardPeriodSelection.previousMonth()
            DashboardPeriodType.CUSTOM -> return // custom requer datas explícitas
        }
        load(period)
    }

    fun selectCustomPeriod(from: LocalDate, to: LocalDate) {
        load(DashboardPeriodSelection.custom(from, to))
    }

    /**
     * Formata o período para exibição (ex.: "01/10/2026 — 31/10/2026").
     */
    private fun formatPeriodLabel(period: DashboardPeriodSelection): String {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return "${period.from.format(fmt)} — ${period.to.format(fmt)}"
    }

    // Legacy helpers (mantidos para compatibilidade com tests antigos)
    fun load(from: String, to: String) {
        // Converte ISO strings para LocalDate
        // "to" no dashboard é exclusivo (próximo dia 00:00), então subtrai 1 dia para o LocalDate
        val fromDate = try { Instant.parse(from).atZone(ZoneId.systemDefault()).toLocalDate() } catch (_: Exception) { LocalDate.now() }
        val toDate = try { Instant.parse(to).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1) } catch (_: Exception) { LocalDate.now() }
        load(DashboardPeriodSelection.custom(fromDate, toDate))
    }

    companion object {
        fun monthStartIso(): String {
            val period = DashboardPeriodSelection.currentMonth()
            return period.toIso().first
        }

        fun monthEndIso(): String {
            val period = DashboardPeriodSelection.currentMonth()
            return period.toIso().second
        }
    }
}
