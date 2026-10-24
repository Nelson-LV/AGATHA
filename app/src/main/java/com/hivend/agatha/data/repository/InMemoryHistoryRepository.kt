package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Singleton
class InMemoryHistoryRepository @Inject constructor() : HistoryRepository {

    override fun observeHistoryForNode(nodeId: String) =
        flowOf(SampleAgathaData.historyFor(nodeId))
}
