package io.github.riberrr.spinroom.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.riberrr.spinroom.core.sendspin.noise.Base64Url
import io.github.riberrr.spinroom.core.sendspin.noise.Psk
import io.github.riberrr.spinroom.core.sendspin.noise.StaticKeyPair
import io.github.riberrr.spinroom.core.sendspin.session.CredentialStore
import io.github.riberrr.spinroom.core.sendspin.session.InMemoryCredentialStore
import io.github.riberrr.spinroom.core.sendspin.session.PairingRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sendspin client secrets (identity key, pairing PSK, pairing records) in their own DataStore file.
 * App-private and excluded from backup (`allowBackup=false`). Never log the values.
 *
 * [CredentialStore] is synchronous: reads come from memory, writes are persisted in the background
 * (latest snapshot wins). Create it with [load] before starting the Sendspin session.
 */
class SendspinCredentialStore private constructor(
    private val store: DataStore<Preferences>,
    scope: CoroutineScope,
    override val identity: StaticKeyPair,
    override val pairingPsk: ByteArray,
    initial: List<PairingRecord>,
) : CredentialStore {
    private val cache = InMemoryCredentialStore(identity, pairingPsk, CAPACITY).apply { initial.forEach(::save) }
    private val pending = MutableStateFlow<List<PairingRecord>?>(null)

    init {
        scope.launch {
            pending.filterNotNull().collect { records -> store.edit { it[RECORDS] = encode(records) } }
        }
    }

    override fun records(): List<PairingRecord> = cache.records()

    override fun save(record: PairingRecord) {
        cache.save(record)
        persist()
    }

    override fun remove(serverId: String) {
        cache.remove(serverId)
        persist()
    }

    override fun touch(serverId: String) {
        cache.touch(serverId)
        persist()
    }

    private fun persist() {
        pending.value = cache.records()
    }

    companion object {
        /** Pairing records kept (spec: at least 5), least recently used evicted first. */
        const val CAPACITY = 8

        private val IDENTITY = stringPreferencesKey("identity_private_key")
        private val PAIRING_PSK = stringPreferencesKey("pairing_psk")
        private val RECORDS = stringPreferencesKey("pairing_records")
        private val loadLock = Mutex()

        /** Reads the secrets, generating the identity and pairing PSK on first run. */
        suspend fun load(store: DataStore<Preferences>, scope: CoroutineScope): SendspinCredentialStore =
            loadLock.withLock {
                var prefs = store.data.first()
                if (prefs[IDENTITY] == null || prefs[PAIRING_PSK] == null) {
                    prefs = store.edit {
                        if (it[IDENTITY] == null) it[IDENTITY] = Base64Url.encode(StaticKeyPair.generate().privateKey)
                        if (it[PAIRING_PSK] == null) it[PAIRING_PSK] = Base64Url.encode(Psk.generate())
                    }
                }
                SendspinCredentialStore(
                    store = store,
                    scope = scope,
                    identity = StaticKeyPair(Base64Url.decode(prefs[IDENTITY]!!)),
                    pairingPsk = Base64Url.decode(prefs[PAIRING_PSK]!!),
                    initial = decode(prefs[RECORDS].orEmpty()),
                )
            }

        /** One record per line, least recently used first: `serverId<TAB>base64url(psk)`. */
        internal fun encode(records: List<PairingRecord>): String =
            records.joinToString("\n") { "${it.serverId}\t${Base64Url.encode(it.longTermPsk)}" }

        internal fun decode(text: String): List<PairingRecord> = text.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 2 || parts[0].isEmpty()) return@mapNotNull null
            runCatching { PairingRecord(parts[0], Base64Url.decode(parts[1])) }.getOrNull()
        }.toList()
    }
}
