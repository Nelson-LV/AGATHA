package com.hivend.agatha.ui.history

import com.hivend.agatha.domain.model.HistoryEvent

/** Returns the associated alert id for rows that can open the shared alert detail screen. */
internal fun HistoryEvent.alertIdForNavigation(): String? = alertId?.takeIf(String::isNotBlank)
