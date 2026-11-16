package com.senghol.knotx.connection


import com.google.gson.Gson
import com.google.gson.JsonObject
import com.senghol.knotx.data.database.CallSessionDao
import com.senghol.knotx.data.database.ContactDao
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.GroupDao
import com.senghol.knotx.data.database.IdentityEntity
import com.senghol.knotx.data.database.KeyStateEntity
import com.senghol.knotx.data.database.KyberPreKeyEntity
import com.senghol.knotx.data.database.PreKeyEntity
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.data.database.SandeshStatusDao
import com.senghol.knotx.data.database.SessionEntity
import com.senghol.knotx.data.database.SignalDao
import com.senghol.knotx.data.database.SignedPreKeyEntity
import com.senghol.knotx.data.database.TrustedIdentityEntity
import com.senghol.knotx.data.database.UpbhogtaDao
import com.senghol.knotx.utils.data.KyberPreKey
import com.senghol.knotx.utils.data.PreKey
import com.senghol.knotx.utils.data.PreKeyBundleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class RoomSignalStore(
    private val dootawasDatabase: DootawasDatabase
) : SignalProtocolStore {
    private val TAG = "SIGNAL_PROTOCOL_DEBUG"
    private val sessionCache = ConcurrentHashMap<String, SessionRecord>()
    private val sessionIdCache = ConcurrentHashMap<String, String>()
    private val triggerCache = ConcurrentHashMap<String, Boolean>()
    private val identityMutex = Mutex()
    private val upbhogtaDao: UpbhogtaDao by lazy { dootawasDatabase.upbhogtaDao() }
    private val signalDao: SignalDao by lazy { dootawasDatabase.signalDao() }
    private val sandeshStatusDao: SandeshStatusDao by lazy { dootawasDatabase.sandeshStatusDao() }
    private val callSessionDao: CallSessionDao by lazy { dootawasDatabase.callSessionDao() }
    private val contactDao: ContactDao by lazy { dootawasDatabase.contactDao() }
    private val sandeshDao: SandeshDao by lazy { dootawasDatabase.sandeshDao() }
    private val gson = Gson()
    private val preKeyGenMutex = Mutex()
    private val kyberGenMutex = Mutex()

    private val encodeBase64: (ByteArray) -> String = {
        android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
    }

    private suspend fun ensureKeyState() {
        if (signalDao.getKeyState() == null) {
            signalDao.insertKeyState(KeyStateEntity())
        }
    }
    private suspend fun ensureIdentity(): IdentityEntity = identityMutex.withLock {
        signalDao.getIdentity() ?: run {
            val identityKeyPair = IdentityKeyPair.generate()
            val registrationId = KeyHelper.generateRegistrationId(false)
            val entity = IdentityEntity(
                identityKeyPair = identityKeyPair.serialize(),
                registrationId = registrationId
            )
            signalDao.saveIdentity(entity)
            entity
        }
    }

    // ==================== IDENTITY ====================
    override fun getIdentityKeyPair(): IdentityKeyPair = runBlocking {
        IdentityKeyPair(ensureIdentity().identityKeyPair)
    }

    override fun getLocalRegistrationId(): Int = runBlocking {
        ensureIdentity().registrationId
    }

    override fun saveIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey?
    ): IdentityKeyStore.IdentityChange = runBlocking {
        if (address.name.isBlank() || identityKey == null) {
            return@runBlocking IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
        }

        val newBytes = identityKey.serialize()
        val existing = signalDao.getTrustedIdentity(address.toString())

        if (existing == null) {
            signalDao.saveTrustedIdentity(TrustedIdentityEntity(address.toString(), newBytes))
            return@runBlocking IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
        }

        if (!existing.identityKey.contentEquals(newBytes)) {
            signalDao.saveTrustedIdentity(TrustedIdentityEntity(address.toString(), newBytes))
            return@runBlocking IdentityKeyStore.IdentityChange.REPLACED_EXISTING
        }

        IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey?,
        direction: IdentityKeyStore.Direction
    ): Boolean = runBlocking {
        if (identityKey == null) return@runBlocking true

        val stored = signalDao.getTrustedIdentity(address.toString()) ?: return@runBlocking true


        val match = stored.identityKey.contentEquals(identityKey.serialize())
        match
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey? = runBlocking {
        signalDao.getTrustedIdentity(address.toString())?.let { IdentityKey(it.identityKey, 0) }
    }

    // ==================== PRE KEYS ====================
    override fun loadPreKey(preKeyId: Int): PreKeyRecord = runBlocking {
        signalDao.getPreKey(preKeyId)?.let { PreKeyRecord(it.record) }
            ?: throw InvalidKeyIdException("No PreKey for id $preKeyId")
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) = runBlocking {
        signalDao.savePreKey(PreKeyEntity(preKeyId, record.serialize()))
    }

    override fun containsPreKey(preKeyId: Int): Boolean = runBlocking {
        signalDao.getPreKey(preKeyId) != null
    }

    override fun removePreKey(preKeyId: Int) {
        runBlocking {
            signalDao.getPreKey(preKeyId)?.let { signalDao.deletePreKey(it) }
        }
    }

    // ==================== SIGNED PRE KEYS ====================
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord = runBlocking {
        SignedPreKeyRecord(
            signalDao.getSignedPreKey(signedPreKeyId)?.record
                ?: throw InvalidKeyIdException("No SignedPreKey $signedPreKeyId")
        )
    }

    override fun loadSignedPreKeys(): List<SignedPreKeyRecord> = runBlocking {
        signalDao.getAllSignedPreKeys().map { SignedPreKeyRecord(it.record) }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) = runBlocking {
        signalDao.saveSignedPreKey(SignedPreKeyEntity(signedPreKeyId, record.serialize()))
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean = runBlocking {
        signalDao.getSignedPreKey(signedPreKeyId) != null
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        runBlocking { signalDao.deleteSignedPreKey(signedPreKeyId) }
    }

    // ==================== SESSIONS ====================
    override fun loadSession(address: SignalProtocolAddress): SessionRecord = runBlocking {
        val key = address.toString()
        sessionCache[key] ?: signalDao.getSession(key)?.let {
            SessionRecord(it.record).also { rec -> sessionCache[key] = rec }
        } ?: SessionRecord()
    }

    override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> = runBlocking {
        addresses.mapNotNull { loadSession(it).takeIf { s -> s.hasSenderChain() } }
    }

    override fun getSubDeviceSessions(name: String): List<Int> = emptyList()

    private fun generateSessionId(record: SessionRecord): String {
        return try {
            val remoteIdentity = record.remoteIdentityKey?.serialize() ?: byteArrayOf()
            val remoteRegId = record.remoteRegistrationId.toString().toByteArray()
            val version = record.sessionVersion.toString().toByteArray()
            val localIdentity = record.localIdentityKey?.serialize() ?: byteArrayOf()
            val combined = remoteIdentity + remoteRegId + version + localIdentity
            val digest = MessageDigest.getInstance("SHA-256")
            digest.digest(combined).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) = runBlocking {
        val addressKey = address.toString()
        val sid = generateSessionId(record)
        val serialized = record.serialize()


        signalDao.saveSession(SessionEntity(addressKey, serialized, sid))
        sessionCache[addressKey] = record
        sessionIdCache[addressKey] = sid
        triggerCache[address.name] = false
    }

    override fun containsSession(address: SignalProtocolAddress): Boolean = runBlocking {
        val key = address.toString()
        sessionCache.containsKey(key) || signalDao.getSession(key) != null
    }

    // FIX #2: deleteSession no longer wipes the trusted identity. Deleting a session
    // (e.g. during UntrustedIdentityException recovery) should NOT silently reset
    // identity pinning back to trust-on-first-use — that's a separate, deliberate
    // decision, not a side effect of clearing a session. If you genuinely want identity
    // forgotten too in some specific flow, call dao.deleteTrustedIdentity(...) explicitly
    // at that call site instead of always doing it here.
    override fun deleteSession(address: SignalProtocolAddress) {
        runBlocking {
            val key = address.toString()
            signalDao.deleteSessionByAddress(key)
            sessionCache.remove(key)
        }
    }

    // FIX #3: only clear this user's cache entries, not the whole cache.
    override fun deleteAllSessions(name: String) {
        runBlocking {
            signalDao.deleteAllSessionsForUser(name)
            val prefix = "$name."
            sessionCache.keys.filter { it.startsWith(prefix) }.forEach { sessionCache.remove(it) }
        }
    }

    // ==================== GROUPS (optional) ====================
    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {}
    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? = null

    // ==================== KYBER (PQXDH) ====================
    override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord = runBlocking {
        val entity = signalDao.getKyberPreKey(kyberPreKeyId)
            ?: throw InvalidKeyIdException(
                "No KyberPreKey $kyberPreKeyId. Available: ${signalDao.getAllKyberPreKeys().map { it.id }}"
            )
        KyberPreKeyRecord(entity.record)
    }

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) = runBlocking {
        signalDao.saveKyberPreKey(KyberPreKeyEntity(kyberPreKeyId, record.serialize()))
    }

    override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean = runBlocking {
        signalDao.getKyberPreKey(kyberPreKeyId) != null
    }

    override fun loadKyberPreKeys(): MutableList<KyberPreKeyRecord> = runBlocking {
        signalDao.getAllKyberPreKeys().map { KyberPreKeyRecord(it.record) }.toMutableList()
    }

    // NOTE: still unconditionally deletes on use. Fine if every kyber prekey is strictly
    // one-time. If you maintain a last-resort/reusable kyber key at a fixed ID, exclude
    // that ID from deletion here — confirm which scheme you're using before relying on this.
    override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey?) {
        runBlocking { signalDao.deleteKyberPreKey(kyberPreKeyId) }
    }


    fun acceptNewIdentityAndResetSession(
        address: SignalProtocolAddress,
        identityKey: IdentityKey
    ): IdentityKeyStore.IdentityChange {

        val identityChange =
            saveIdentity(
                address,
                identityKey
            )

        deleteSession(
            address
        )

        return identityChange
    }

    suspend fun generateInitialBundle(phoneNumber: String): String = withContext(Dispatchers.IO) {
        ensureKeyState()

        val regId = getLocalRegistrationId()
        val identityKeyPair = getIdentityKeyPair()

        val signedPreKeyId = 1
        val existingSignedPreKey = if (containsSignedPreKey(signedPreKeyId)) {
            loadSignedPreKey(signedPreKeyId)
        } else {
            null
        }

        val signedPreKeyRecord = existingSignedPreKey ?: run {
            val signedKeyPair = ECKeyPair.generate()
            val signature = identityKeyPair.privateKey.calculateSignature(signedKeyPair.publicKey.serialize())
            SignedPreKeyRecord(
                signedPreKeyId,
                System.currentTimeMillis(),
                signedKeyPair,
                signature
            ).also { storeSignedPreKey(signedPreKeyId, it) }
        }

        val bundle = PreKeyBundleDto(
            phoneNumber = phoneNumber,
            registrationId = regId,
            identityKey = encodeBase64(identityKeyPair.publicKey.serialize()),
            signedPreKeyId = signedPreKeyId,
            signedPreKeyPublic = encodeBase64(signedPreKeyRecord.keyPair.publicKey.serialize()),
            signedPreKeySignature = encodeBase64(signedPreKeyRecord.signature)
        )

        gson.toJson(bundle)
    }
    suspend fun generateNextPreKeysBatch(): JsonObject = withContext(Dispatchers.IO) {
        ensureKeyState()
        preKeyGenMutex.withLock {
            val lastId = signalDao.getLastPreKeyId() ?: 1000
            val startId = lastId + 1
            val batchSize = 15

            val preKeys = mutableListOf<PreKey>()

            repeat(batchSize) { offset ->
                val id = startId + offset
                val keyPair = ECKeyPair.generate()
                val record = PreKeyRecord(id, keyPair)
                storePreKey(id, record)

                preKeys += PreKey(
                    preKeyId = id,
                    preKeyPublic = encodeBase64(keyPair.publicKey.serialize())
                )
            }
            signalDao.saveLastPreKeyId(startId + batchSize - 1)

            JsonObject().apply {
                add("preKeys", gson.toJsonTree(preKeys))
            }
        }
    }

    suspend fun generateNextKyberKeysBatch(): JsonObject = withContext(Dispatchers.IO) {
        ensureKeyState()
        kyberGenMutex.withLock {
            val lastId = signalDao.getLastKyberKeyId() ?: 0
            val startId = lastId + 1
            val batchSize = 15

            // FIX: read identity once for the whole batch, not once per key — avoids
            // re-hitting ensureIdentity()'s mutex 15 times and keeps the batch consistent
            // even if identity somehow changed mid-loop.
            val identityKeyPair = getIdentityKeyPair()

            val kyberKeys = mutableListOf<KyberPreKey>()

            repeat(batchSize) { offset ->
                val id = startId + offset
                val kyberPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
                val signature = identityKeyPair.privateKey.calculateSignature(kyberPair.publicKey.serialize())

                val record = KyberPreKeyRecord(
                    id,
                    System.currentTimeMillis(),
                    kyberPair,
                    signature
                )
                storeKyberPreKey(id, record)

                kyberKeys += KyberPreKey(
                    kyberPreKeyId = id,
                    kyberPreKeyPublic = encodeBase64(kyberPair.publicKey.serialize()),
                    kyberPreKeySignature = encodeBase64(signature)
                )
            }
            signalDao.saveLastKyberKeyId(startId + batchSize - 1)

            JsonObject().apply {
                add("kyberPreKeys", gson.toJsonTree(kyberKeys))
            }
        }
    }

    suspend fun updateUser(oldUser: String, newUser: String) {
        if (oldUser == newUser) return

        val oldAddress = SignalProtocolAddress(oldUser, 1).toString()
        val newAddress = SignalProtocolAddress(newUser, 1).toString()

        if (signalDao.getSession(oldAddress) != null) {
            upbhogtaDao.deleteUpbhogta(newUser)
            upbhogtaDao.updateUserNumber(oldUser, newUser)
            sandeshDao.updateSandeshUserNumber(oldUser, newUser)
            sandeshStatusDao.updateSandeshStatusUserNumber(oldUser, newUser)
            callSessionDao.updateCallReceiver(oldUser, newUser)
            contactDao.setInstalled(newUser)

            signalDao.deleteSessionByAddress(newAddress)
            sessionCache.remove(newAddress)

            signalDao.updateSessionAddress(oldAddress, newAddress)
            sessionCache.remove(oldAddress)?.let {
                sessionCache[newAddress] = it
            }
        }
    }
}