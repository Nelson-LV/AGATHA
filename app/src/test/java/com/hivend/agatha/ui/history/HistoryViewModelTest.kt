package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import com.hivend.agatha.core.connectivity.ConnectivityMonitor
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.HISTORY_LAST_SELECTED_DEVICE_ID
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun rootTabLoadsTheSavedSensorAndChangingSensorRefreshesItsHistory() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeHistoryRepository().apply {
                setHistory("MP-11556", alert("ALR-000007", 11556))
                setHistory("MP-1189", alert("ALR-000008", 1189))
            }
            val sensorNodes = FakeSensorNodeRepository(
                listOf(
                    SensorNode("MP-11556", name = "Sensor MP-11556", pk = "PK34-470", municipality = "San José de Pare"),
                    SensorNode("MP-1189", name = "Sensor MP-1189", pk = "PK22-300", municipality = "San José de Pare"),
                ),
            )
            val selectionStore = HistorySelectionStore()
            val viewModel = HistoryViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(BottomTab.History.ARG_DEVICE_ID to HISTORY_LAST_SELECTED_DEVICE_ID),
                ),
                historyRepository = repository,
                sensorNodeRepository = sensorNodes,
                connectivityMonitor = FakeConnectivityMonitor(connected = true),
                selectionStore = selectionStore,
            )
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            advanceUntilIdle()
            assertEquals("MP-11556", viewModel.uiState.value.selectedSensor?.id)
            assertEquals(listOf("ALR-000007:started"), viewModel.uiState.value.events.map(HistoryEvent::id))
            assertTrue(repository.refreshCalls.contains("MP-11556"))

            viewModel.selectSensor("MP-1189")
            advanceUntilIdle()

            assertEquals("MP-1189", viewModel.uiState.value.selectedSensor?.id)
            assertEquals(listOf("ALR-000008:started"), viewModel.uiState.value.events.map(HistoryEvent::id))
            assertTrue(repository.refreshCalls.contains("MP-1189"))
            assertEquals("MP-1189", selectionStore.selection.value.sensorId)
            collection.cancel()

            val reopenedViewModel = HistoryViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(BottomTab.History.ARG_DEVICE_ID to HISTORY_LAST_SELECTED_DEVICE_ID),
                ),
                historyRepository = repository,
                sensorNodeRepository = sensorNodes,
                connectivityMonitor = FakeConnectivityMonitor(connected = true),
                selectionStore = selectionStore,
            )
            val reopenedCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                reopenedViewModel.uiState.collect()
            }
            advanceUntilIdle()

            assertEquals("MP-1189", reopenedViewModel.uiState.value.selectedSensor?.id)
            reopenedCollection.cancel()
        }

    @Test
    fun detailRoutePrefiltersTheRequestedSensor() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeHistoryRepository().apply {
            setHistory("MP-1189", alert("ALR-000008", 1189))
        }
        val viewModel = HistoryViewModel(
            savedStateHandle = SavedStateHandle(mapOf(BottomTab.History.ARG_DEVICE_ID to "MP-1189")),
            historyRepository = repository,
            sensorNodeRepository = FakeSensorNodeRepository(emptyList()),
            connectivityMonitor = FakeConnectivityMonitor(connected = true),
            selectionStore = HistorySelectionStore(),
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        assertEquals("MP-1189", viewModel.uiState.value.selectedSensor?.id)
        assertEquals(HistorySiteCatalog.GUEPSA.id, viewModel.uiState.value.selectedSite?.id)
        assertTrue(repository.refreshCalls.contains("MP-1189"))
        collection.cancel()
    }

    @Test
    fun offlineKeepsCachedAlertsAndShowsTheLastSuccessfulSyncTime() = runTest(mainDispatcherRule.dispatcher) {
        val lastSync = Instant.parse("2026-10-30T15:00:00Z")
        val repository = FakeHistoryRepository().apply {
            setHistory("MP-11556", alert("ALR-000007", 11556))
            refreshResult = AlertResult.Failure(com.hivend.agatha.domain.model.AlertFailure.NoConnection)
        }
        val selectionStore = HistorySelectionStore().apply {
            recordSync("MP-11556", lastSync)
        }
        val viewModel = HistoryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(BottomTab.History.ARG_DEVICE_ID to HISTORY_LAST_SELECTED_DEVICE_ID),
            ),
            historyRepository = repository,
            sensorNodeRepository = FakeSensorNodeRepository(emptyList()),
            connectivityMonitor = FakeConnectivityMonitor(connected = false),
            selectionStore = selectionStore,
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.isConnected)
        assertEquals(lastSync, viewModel.uiState.value.lastSyncedAt)
        assertEquals(listOf("ALR-000007:started"), viewModel.uiState.value.events.map(HistoryEvent::id))
        assertEquals(com.hivend.agatha.domain.model.AlertFailure.NoConnection, viewModel.uiState.value.failure)
        collection.cancel()
    }

    @Test
    fun changingEventAndDateFiltersIsLocalAndClearKeepsTheSensorSelection() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeHistoryRepository().apply {
                setHistory("MP-11556", alert("ALR-000007", 11556))
            }
            val selectionStore = HistorySelectionStore()
            val viewModel = HistoryViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(BottomTab.History.ARG_DEVICE_ID to HISTORY_LAST_SELECTED_DEVICE_ID),
                ),
                historyRepository = repository,
                sensorNodeRepository = FakeSensorNodeRepository(emptyList()),
                connectivityMonitor = FakeConnectivityMonitor(connected = true),
                selectionStore = selectionStore,
            )
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }
            advanceUntilIdle()
            val refreshCount = repository.refreshCalls.size

            viewModel.setEventTypeFilter(EventType.INSPECTION)
            viewModel.setDateRange(LocalDate.now(), LocalDate.now())
            advanceUntilIdle()

            assertEquals(refreshCount, repository.refreshCalls.size)
            assertTrue(viewModel.uiState.value.events.isEmpty())
            assertTrue(viewModel.uiState.value.hasAdditionalFilters)

            viewModel.clearFilters()
            advanceUntilIdle()

            assertEquals("MP-11556", viewModel.uiState.value.selectedSensor?.id)
            assertEquals(listOf("ALR-000007:started"), viewModel.uiState.value.events.map(HistoryEvent::id))
            assertTrue(!viewModel.uiState.value.hasAdditionalFilters)
            assertEquals(refreshCount, repository.refreshCalls.size)
            collection.cancel()
        }

    private fun alert(id: String, deviceId: Int) = Alert(
        id = id,
        deviceId = deviceId,
        pk = "PK34-470",
        event = AlertEvent.LEAK,
        level = AlertLevel.YELLOW,
        maxLevel = AlertLevel.YELLOW,
        startedAt = Instant.parse("2026-09-26T23:00:00Z"),
    )

    private class FakeHistoryRepository : HistoryRepository {
        private val histories = mutableMapOf<String, MutableStateFlow<DeviceHistory>>()
        val refreshCalls = mutableListOf<String>()
        var refreshResult: AlertResult<Unit> = AlertResult.Success(Unit)

        fun setHistory(deviceId: String, alert: Alert) {
            histories.getOrPut(deviceId) { MutableStateFlow(DeviceHistory()) }
                .value = DeviceHistory(alerts = listOf(alert))
        }

        override fun observeHistory(deviceId: String): Flow<DeviceHistory> =
            histories.getOrPut(deviceId) { MutableStateFlow(DeviceHistory()) }

        override suspend fun refresh(deviceId: String): AlertResult<Unit> {
            refreshCalls += deviceId
            return refreshResult
        }
    }

    private class FakeSensorNodeRepository(nodes: List<SensorNode>) : SensorNodeRepository {
        private val values = MutableStateFlow(nodes)

        override fun observeSensorNodes(): StateFlow<List<SensorNode>> = values.asStateFlow()

        override fun getSensorNodeById(nodeId: String): Flow<SensorNode?> =
            values.map { allNodes -> allNodes.firstOrNull { it.id == nodeId } }
    }

    private class FakeConnectivityMonitor(connected: Boolean) : ConnectivityMonitor {
        private val values = MutableStateFlow(connected)

        override fun observeConnected(): Flow<Boolean> = values.asStateFlow()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
