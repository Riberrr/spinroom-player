package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.github.riberrr.spinroom.core.sendspin.session.PairingRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SendspinCredentialStoreTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test fun identityAndPskSurviveReload() = runBlocking {
        val prefs = Prefs()
        val first = SendspinCredentialStore.load(prefs, scope)
        val second = SendspinCredentialStore.load(prefs, scope)
        assertEquals(first.identity.id, second.identity.id)
        assertArrayEquals(first.pairingPsk, second.pairingPsk)
        assertEquals(32, first.pairingPsk.size)
        scope.cancel()
    }

    @Test fun recordsPersistInLruOrder() = runBlocking {
        val prefs = Prefs()
        val store = SendspinCredentialStore.load(prefs, scope)
        store.save(PairingRecord("a", ByteArray(32) { 1 }))
        store.save(PairingRecord("b", ByteArray(32) { 2 }))
        store.save(PairingRecord("c", ByteArray(32) { 3 }))
        store.remove("b")
        store.touch("a")
        // Written in the background: wait for the latest snapshot to land.
        withTimeout(5_000) {
            while (SendspinCredentialStore.load(prefs, scope).records().map { it.serverId } != listOf("c", "a")) delay(10)
        }
        assertArrayEquals(ByteArray(32) { 1 }, SendspinCredentialStore.load(prefs, scope).records().last().longTermPsk)
        scope.cancel()
    }

    @Test fun capacityEvictsLeastRecentlyUsed() = runBlocking {
        val store = SendspinCredentialStore.load(Prefs(), scope)
        repeat(SendspinCredentialStore.CAPACITY + 2) { store.save(PairingRecord("s$it", ByteArray(32))) }
        assertEquals(SendspinCredentialStore.CAPACITY, store.records().size)
        assertEquals("s2", store.records().first().serverId)
        scope.cancel()
    }

    @Test fun decodeSkipsBrokenLines() {
        val good = SendspinCredentialStore.encode(listOf(PairingRecord("x", ByteArray(32) { 7 })))
        val parsed = SendspinCredentialStore.decode("garbage\n$good\n\tabc\n")
        assertEquals(listOf("x"), parsed.map { it.serverId })
    }
}

/** In-memory stand-in: the real file-backed DataStore fails its atomic rename on Windows JVMs. */
private class Prefs : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val lock = Mutex()
    override val data = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        lock.withLock { transform(state.value).also { state.value = it } }
}
