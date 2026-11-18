package com.hivend.agatha.ui.history

import androidx.lifecycle.SavedStateHandle
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.core.navigation.HISTORY_LAST_SELECTED_DEVICE_ID
import com.hivend.agatha.data.export.ExportFileStore
import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertExportFile
import com.hivend.agatha.domain.model.AlertFailure
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertResult
import com.hivend.agatha.domain.model.AlertSearchFilters
import com.hivend.agatha.domain.model.AlertSearchPage
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.DeviceTelemetry
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.AlertSearchRepository
import com.hivend.agatha.domain.repository.ConnectivityMonitor
import com.hivend.agatha.domain.repository.DeviceHistory
import com.hivend.agatha.domain.repository.HistoryRepository
import com.hivend.agatha.domain.repository.SensorNodeRepository
import com.hivend.agatha.domain.repository.TelemetryRepository
import java.io.File
import java.time.Duration
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val search = FakeAlertSearchRepository()
    private val connectivity = FakeConnectivityMonitor(connected = true)
    private val exportFiles = FakeExportFileStore()
    private val store = HistorySelectionStore()

    private fun viewModel(routeDeviceId: String = HISTORY_LAST_SELECTED_DEVICE_ID) = HistoryViewModel(
        savedStateHandle = SavedStateHandle(mapOf(BottomTab.History.ARG_DEVICE_ID to routeDeviceId)),
        historyRepository = history,
        searchRepository = search,
        telemetryRepository = FakeTelemetryRepository(
            listOf(
                TelemetryDevice(11556, "Sensor MP-11556 PK34-470", lastConnection = null, lastBatteryLevel = null),
                TelemetryDevice(1189, "Sensor MP-1189 PK22-300", lastConnection = null, lastBatteryLevel = null),
            ),
        ),
        sensorNodeRepository = FakeSensorNodeRepository(
            listOf(SensorNode("MP-11556", name = "Sensor MP-11556", pk = "PK34-470", municipality = "San José de Pare")),
        ),
        connectivityMonitor = connectivity,
        selectionStore = store,
        exportFiles = exportFiles,
    )

    private fun TestScope.collect(viewModel: HistoryViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    @Test
    fun `the tab opens the history of every device with no filters`() = runTest(mainDispatcherRule.dispatcher) {
        search.pages[1] = page(total = 2, alert("ALR-000002", 11556), alert("ALR-000001", 1189))
        val viewModel = viewModel()
        collect(viewModel)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedDevice)
        assertEquals(setOf("MP-11556", "MP-1189"), state.devices.map { it.id }.toSet())
        assertEquals(listOf(AlertSearchFilters() to 1), search.searches)
        assertEquals(listOf("ALR-000002", "ALR-000001"), state.alerts.map(Alert::id))
        assertEquals(2, state.totalAlerts)
        assertFalse(state.canLoadMore)
        assertTrue(state.canExport)
    }

    @Test
    fun `see history from a device searches only that device`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(routeDeviceId = "MP-11556")
        collect(viewModel)

        advanceUntilIdle()

        assertEquals("MP-11556", viewModel.uiState.value.selectedDevice?.id)
        assertEquals("Sensor MP-11556 PK34-470", viewModel.uiState.value.selectedDevice?.label)
        assertEquals(AlertSearchFilters(deviceId = 11556), search.searches.last().first)
    }

    @Test
    fun `each filter change searches again from page 1 and clearing keeps the device`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            collect(viewModel)
            advanceUntilIdle()

            viewModel.selectDevice("MP-1189")
            viewModel.toggleMaxLevel(AlertLevel.RED)
            viewModel.toggleMaxLevel(AlertLevel.ORANGE)
            viewModel.toggleAlertEvent(AlertEvent.LEAK)
            viewModel.toggleStatus(ManagementStatus.UNCLASSIFIED)
            viewModel.setDateRange(LocalDate.parse("2026-09-15"), LocalDate.parse("2026-09-01"))
            advanceUntilIdle()

            val expected = AlertSearchFilters(
                deviceId = 1189,
                from = LocalDate.parse("2026-09-01"),
                to = LocalDate.parse("2026-09-15"),
                maxLevels = setOf(AlertLevel.RED, AlertLevel.ORANGE),
                events = setOf(AlertEvent.LEAK),
                statuses = setOf(ManagementStatus.UNCLASSIFIED),
            )
            assertEquals(expected to 1, search.searches.last())
            assertTrue(viewModel.uiState.value.hasAdditionalFilters)

            viewModel.toggleMaxLevel(AlertLevel.RED)
            advanceUntilIdle()
            assertEquals(setOf(AlertLevel.ORANGE), search.searches.last().first.maxLevels)

            viewModel.clearFilters()
            advanceUntilIdle()
            assertEquals(AlertSearchFilters(deviceId = 1189) to 1, search.searches.last())
            assertEquals("MP-1189", viewModel.uiState.value.selectedDevice?.id)
            assertFalse(viewModel.uiState.value.hasAdditionalFilters)
        }

    @Test
    fun `reaching the end of the list loads the next page`() = runTest(mainDispatcherRule.dispatcher) {
        search.pages[1] = page(total = 12, *(1..10).map { alert("ALR-%06d".format(it), 11556) }.toTypedArray())
        search.pages[2] = page(total = 12, alert("ALR-000011", 11556), alert("ALR-000012", 11556), page = 2)
        val viewModel = viewModel()
        collect(viewModel)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canLoadMore)

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(12, viewModel.uiState.value.alerts.size)
        assertEquals(2, search.searches.last().second)
        assertFalse(viewModel.uiState.value.canLoadMore)

        viewModel.loadMore()
        advanceUntilIdle()
        assertEquals(2, search.searches.size)
    }

    @Test
    fun `offline the list shows the alerts already on the phone with the same filters`() =
        runTest(mainDispatcherRule.dispatcher) {
            connectivity.set(false)
            history.cached.value = listOf(
                alert("ALR-000001", 11556, level = AlertLevel.RED),
                alert("ALR-000002", 11556, level = AlertLevel.YELLOW),
                alert("ALR-000003", 1189, level = AlertLevel.RED),
            )
            val viewModel = viewModel(routeDeviceId = "MP-11556")
            collect(viewModel)
            viewModel.toggleMaxLevel(AlertLevel.RED)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(search.searches.isEmpty())
            assertTrue(state.showingLocalAlerts)
            assertEquals(listOf("ALR-000001"), state.alerts.map(Alert::id))
            assertEquals(1, state.totalAlerts)
            assertFalse(state.canExport)
            assertEquals(AlertFailure.NoConnection, state.failure)
        }

    @Test
    fun `export saves the excel of the current filters and then goes back to idle`() =
        runTest(mainDispatcherRule.dispatcher) {
            search.pages[1] = page(total = 1, alert("ALR-000001", 11556))
            val viewModel = viewModel(routeDeviceId = "MP-11556")
            collect(viewModel)
            advanceUntilIdle()

            viewModel.export()
            advanceUntilIdle()

            assertEquals(listOf(AlertSearchFilters(deviceId = 11556)), search.exports)
            val ready = viewModel.uiState.value.export as HistoryExportState.Ready
            assertEquals("alertas_20261118_1800.xlsx", ready.file.name)

            viewModel.onExportHandled()
            advanceUntilIdle()
            assertEquals(HistoryExportState.Idle, viewModel.uiState.value.export)
        }

    @Test
    fun `a rejected export shows the failure and an empty list cannot be exported`() =
        runTest(mainDispatcherRule.dispatcher) {
            search.pages[1] = page(total = 1, alert("ALR-000001", 11556))
            search.exportResult = AlertResult.Failure(AlertFailure.Rejected("INVALID_INPUT", "Demasiadas alertas"))
            val viewModel = viewModel()
            collect(viewModel)
            advanceUntilIdle()

            viewModel.export()
            advanceUntilIdle()
            assertEquals(
                HistoryExportState.Failed(AlertFailure.Rejected("INVALID_INPUT", "Demasiadas alertas")),
                viewModel.uiState.value.export,
            )

            search.pages[1] = page(total = 0)
            viewModel.toggleStatus(ManagementStatus.CLASSIFIED_WITH_TAG)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canExport)
        }

    @Test
    fun `filters and device survive the view model being recreated`() = runTest(mainDispatcherRule.dispatcher) {
        val first = viewModel()
        val firstCollection = collect(first)
        first.selectDevice("MP-1189")
        first.toggleAlertEvent(AlertEvent.LEAK)
        first.updateListScrollPosition(index = 3, offset = 27)
        advanceUntilIdle()
        firstCollection.cancel()

        val reopened = viewModel()
        collect(reopened)
        advanceUntilIdle()

        val state = reopened.uiState.value
        assertEquals("MP-1189", state.selectedDevice?.id)
        assertEquals(setOf(AlertEvent.LEAK), state.alertEvents)
        assertEquals(3, state.listScrollIndex)
        assertEquals(27, state.listScrollOffset)
    }

    private fun page(total: Int, vararg alerts: Alert, page: Int = 1) =
        AlertSearchPage(total = total, page = page, pageSize = 10, alerts = alerts.toList())

    private fun alert(
        id: String,
        deviceId: Int,
        level: AlertLevel = AlertLevel.YELLOW,
        classification: ClassificationRecord? = null,
    ) = Alert(
        id = id,
        deviceId = deviceId,
        pk = "PK34-470",
        event = AlertEvent.LEAK,
        level = level,
        maxLevel = level,
        startedAt = Instant.parse("2026-09-26T23:00:00Z"),
        classification = classification,
    )

    private class FakeAlertSearchRepository : AlertSearchRepository {
        val pages = mutableMapOf<Int, AlertSearchPage>()
        val searches = mutableListOf<Pair<AlertSearchFilters, Int>>()
        val exports = mutableListOf<AlertSearchFilters>()
        var exportResult: AlertResult<AlertExportFile> =
            AlertResult.Success(AlertExportFile("alertas_20261118_1800.xlsx", byteArrayOf(1)))

        override suspend fun search(filters: AlertSearchFilters, page: Int): AlertResult<AlertSearchPage> {
            searches += filters to page
            return AlertResult.Success(pages[page] ?: AlertSearchPage(total = 0, page = page, pageSize = 10, alerts = emptyList()))
        }

        override suspend fun export(filters: AlertSearchFilters): AlertResult<AlertExportFile> {
            exports += filters
            return exportResult
        }
    }

    private class FakeExportFileStore : ExportFileStore {
        override suspend fun save(file: AlertExportFile): File = File(file.fileName)
    }

    private class FakeHistoryRepository : HistoryRepository {
        private val histories = mutableMapOf<String, MutableStateFlow<DeviceHistory>>()
        val cached = MutableStateFlow<List<Alert>>(emptyList())
        val refreshCalls = mutableListOf<String>()

        fun setHistory(deviceId: String, alert: Alert) {
            histories.getOrPut(deviceId) { MutableStateFlow(DeviceHistory()) }.value = DeviceHistory(alerts = listOf(alert))
        }

        override fun observeHistory(deviceId: String): Flow<DeviceHistory> =
            histories.getOrPut(deviceId) { MutableStateFlow(DeviceHistory()) }

        override fun observeCachedAlerts(): Flow<List<Alert>> = cached

        override suspend fun refresh(deviceId: String): AlertResult<Unit> {
            refreshCalls += deviceId
            return AlertResult.Success(Unit)
        }
    }

    private class FakeTelemetryRepository(private val devices: List<TelemetryDevice>) : TelemetryRepository {
        override suspend fun devices(refresh: Boolean): TelemetryResult<List<TelemetryDevice>> = TelemetryResult.Success(devices)

        override suspend fun findDevice(nodeId: String): TelemetryResult<TelemetryDevice> =
            devices.firstOrNull { it.nodeCode == nodeId }?.let { TelemetryResult.Success(it) }
                ?: TelemetryResult.Failure(TelemetryError.DEVICE_NOT_FOUND)

        override suspend fun recentTelemetry(
            nodeId: String,
            keys: Set<String>,
            window: Duration,
        ): TelemetryResult<DeviceTelemetry> = TelemetryResult.Failure(TelemetryError.SERVER)
    }

    private class FakeSensorNodeRepository(nodes: List<SensorNode>) : SensorNodeRepository {
        private val values = MutableStateFlow(nodes)

        override fun observeSensorNodes(): StateFlow<List<SensorNode>> = values.asStateFlow()

        override fun getSensorNodeById(nodeId: String): Flow<SensorNode?> =
            values.map { allNodes -> allNodes.firstOrNull { it.id == nodeId } }
    }

    private class FakeConnectivityMonitor(connected: Boolean) : ConnectivityMonitor {
        private val values = MutableStateFlow(connected)

        fun set(connected: Boolean) {
            values.value = connected
        }

        override val isOnline: Flow<Boolean> = values.asStateFlow()
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
