package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryHistoryRepository @Inject constructor() : HistoryRepository {

    override fun observeHistory(deviceId: String) =
        flowOf(SampleAgathaData.historyFor(deviceId))
}
