package com.hivend.agatha.data.sync

import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiErrorBody
import com.hivend.agatha.domain.model.SyncError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El motivo de un fallo se guarda como texto en la cola y se lee de vuelta para la pantalla. */
class SyncErrorTextTest {

    @Test
    fun `each api error round-trips to the error the screen shows`() {
        assertEquals(SyncError.NoConnection, SyncErrorText.decode(SyncErrorText.encode(ApiError.NoConnection)))
        assertEquals(SyncError.InvalidResponse, SyncErrorText.decode(SyncErrorText.encode(ApiError.InvalidResponse)))
        assertEquals(SyncError.Server(500), SyncErrorText.decode(SyncErrorText.encode(ApiError.Http(500, null))))
    }

    @Test
    fun `the backend reason is kept in the stored text`() {
        val text = SyncErrorText.encode(ApiError.Http(400, ApiErrorBody("INVALID_INPUT", "label")))
        assertEquals("HTTP 400 INVALID_INPUT: label", text)
        assertEquals(SyncError.Server(400), SyncErrorText.decode(text))
    }

    @Test
    fun `no stored reason means no error`() {
        assertNull(SyncErrorText.decode(null))
    }
}
