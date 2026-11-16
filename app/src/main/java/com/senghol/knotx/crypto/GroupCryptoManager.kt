package com.senghol.knotx.crypto

import com.senghol.knotx.data.database.GroupDao

import android.util.Base64
import android.util.Log
import com.google.gson.JsonObject
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.GroupEpochKeyEntity
import com.senghol.knotx.data.database.GroupMemberEntity
import com.senghol.knotx.data.database.GroupPrivateKeyEntity
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jcajce.spec.KEMExtractSpec
import org.bouncycastle.jcajce.spec.KEMGenerateSpec
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec
import org.bouncycastle.jcajce.spec.MLKEMParameterSpec.ml_kem_768
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.sortedBy

class GroupCryptoManager(
    private val groupDao: GroupDao,
    private val keyStoreManager: KeyStoreManager,
    private val userNumberKhud: Flow<String?>,
    private val userNameIdKhud: Flow<String?>
) {

    companion object {
        private const val CURRENT_DEVICE_ID = "1"
        private const val BC_PROVIDER = "BC"
        private const val X25519_ALGORITHM = "X25519"
        private const val ML_KEM_ALGORITHM = "ML-KEM-768"
        private const val ED25519_ALGORITHM = "Ed25519"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE_BYTES = 32
        private const val GCM_NONCE_SIZE_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val MAX_RETAINED_EPOCHS = 50
        private const val HKDF_HASH = "HmacSHA256"
        private val SECURE_RANDOM = SecureRandom()
        private const val DOMAIN_EPOCH_SECRET = "DOOTAWAS_GROUP_EPOCH_SECRET_V1"
        private const val DOMAIN_HYBRID_WRAP = "DOOTAWAS_GROUP_HYBRID_WRAP_V1"
        private const val DOMAIN_COMMIT_HASH = "DOOTAWAS_GROUP_COMMIT_HASH_V1"
        private const val DOMAIN_COMMIT_SIGNATURE = "DOOTAWAS_GROUP_COMMIT_SIGNATURE_V1"
        private const val DOMAIN_MESSAGE_AAD = "DOOTAWAS_GROUP_MESSAGE_AAD_V1"
        private const val DOMAIN_EPOCH_STORAGE_AAD = "DOOTAWAS_EPOCH_STORAGE_V1"

            init {
                Security.removeProvider(BC_PROVIDER)
                Security.insertProviderAt(BouncyCastleProvider(), 1)
            }
    }


    private suspend fun getMyMemberId(groupId: String): String {
        val myNumber = userNumberKhud.first()
            ?: throw IllegalStateException("Local user number missing.")

        val myUsername = userNameIdKhud.first()

        val members = groupDao.getAllMembersInGroup(groupId)

        return members.firstOrNull {
            it.userNumber == myNumber || it.userNumber == myUsername
        }?.memberInstanceId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Local member not found in group $groupId.")
    }

    suspend fun createInitialEpoch(
        groupId: String
    ) {
        val group = groupDao.getGroupById(groupId)
            ?: throw IllegalStateException(
                "Group $groupId not found."
            )

        if (group.currentEpoch > 0L) {
            throw IllegalStateException(
                "Group $groupId already has an active epoch."
            )
        }

        val initialEpoch = 1L
        val initialEpochSecret = randomBytes(AES_KEY_SIZE_BYTES)

        val initialCommitHash = sha256(
            buildInitialCommitHashInput(
                groupId = groupId,
                epoch = initialEpoch,
                protocolVersion = group.groupCryptoProtocolVersion
            )
        )

        saveEpochSecret(
            groupId = groupId,
            epoch = initialEpoch,
            epochSecret = initialEpochSecret,
            commitHash = initialCommitHash
        )

        groupDao.updateGroupCryptoState(
            groupId = groupId,
            currentEpoch = initialEpoch,
            currentCommitHash = initialCommitHash,
            rosterVersion = group.rosterVersion,
            protocolVersion = group.groupCryptoProtocolVersion
        )

        DLog.i(
            "Initial group epoch created: " +
                    "groupId=$groupId epoch=$initialEpoch"
        )
    }
    private fun buildInitialCommitHashInput(
        groupId: String,
        epoch: Long,
        protocolVersion: Int
    ): ByteArray {
        return buildString {
            append("DOOTAWAS_GROUP_INITIAL_EPOCH|")
            append(protocolVersion)
            append("|")
            append(groupId)
            append("|")
            append(epoch)
        }.toByteArray(Charsets.UTF_8)
    }
    suspend fun encryptSendMessage(
        groupId: String,
        plaintext: ByteArray,
        messageId: String,
        messageNumber: Long
    ): GroupMessage {
        val group = groupDao.getGroupById(groupId)
            ?: throw IllegalStateException("Group $groupId not found.")

        if (group.currentEpoch <= 0L) {
            throw IllegalStateException("Group $groupId has no active epoch.")
        }

        val senderId = getMyMemberId(groupId)

        val senderDeviceId = CURRENT_DEVICE_ID

        val epochSecret = getEpochSecret(
            groupId = groupId,
            epoch = group.currentEpoch
        )

        val unsignedMessage = GroupMessage(
            protocolVersion = group.groupCryptoProtocolVersion,
            groupId = groupId,
            epoch = group.currentEpoch,
            senderId = senderId,
            senderDeviceId = senderDeviceId,
            messageId = messageId,
            messageNumber = messageNumber,
            nonce = ByteArray(0),
            ciphertext = ByteArray(0)
        )

        val aad = buildMessageAad(unsignedMessage)

        val encrypted = encryptAesGcm(
            key = epochSecret,
            plaintext = plaintext,
            aad = aad
        )

        return unsignedMessage.copy(
            nonce = encrypted.nonce,
            ciphertext = encrypted.ciphertext
        )
    }




    private fun writeBytes(
        out: DataOutputStream,
        value: ByteArray
    ) {
        out.writeInt(value.size)
        out.write(value)
    }



    suspend fun receiveMessage(
        groupId: String,
        message: GroupMessage
    ): String {
        require(message.groupId == groupId) {
            "Group ID mismatch."
        }

        val group = groupDao.getGroupById(groupId)
            ?: throw IllegalStateException(
                "Group $groupId not found."
            )

        require(
            message.protocolVersion ==
                    group.groupCryptoProtocolVersion
        ) {
            "Unsupported group crypto protocol version."
        }

        if (message.epoch <= 0L) {
            throw SecurityException(
                "Invalid message epoch."
            )
        }

        if (message.nonce.size != GCM_NONCE_SIZE_BYTES) {
            throw SecurityException(
                "Invalid AES-GCM nonce length."
            )
        }

        val epochSecret = getEpochSecret(
            groupId = groupId,
            epoch = message.epoch
        )

        val plaintext = decryptAesGcm(
            key = epochSecret,
            ciphertext = message.ciphertext,
            nonce = message.nonce,
            aad = buildMessageAad(message)
        )

        return String(
            plaintext,
            Charsets.UTF_8
        )
    }


    fun hashEncryptedPath(
        path: HybridEncapsulatedPayload
    ): ByteArray {
        val recipientLeafIdBytes =
            path.recipientLeafId.toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(
            4 + recipientLeafIdBytes.size +
                    4 + path.ephemeralX25519Public.size +
                    4 + path.mlKemCiphertext.size +
                    4 + path.encryptedPathSeed.size +
                    4 + path.nonce.size
        )

        buffer.putInt(recipientLeafIdBytes.size)
        buffer.put(recipientLeafIdBytes)

        buffer.putInt(path.ephemeralX25519Public.size)
        buffer.put(path.ephemeralX25519Public)

        buffer.putInt(path.mlKemCiphertext.size)
        buffer.put(path.mlKemCiphertext)

        buffer.putInt(path.encryptedPathSeed.size)
        buffer.put(path.encryptedPathSeed)

        buffer.putInt(path.nonce.size)
        buffer.put(path.nonce)

        return MessageDigest
            .getInstance("SHA-256")
            .digest(buffer.array())
    }
// =========================================================================
// 4. CREATE EPOCH COMMIT
// =========================================================================

    suspend fun createEpochCommit(
        groupId: String,
        commitId: String,
        rosterVersion: Long,
        privateX25519: PrivateKey,
        privateMlKem: PrivateKey,
        privateEd25519: PrivateKey
    ): EpochCommit {
        val group = groupDao.getGroupById(groupId)
            ?: throw IllegalStateException(
                "Group $groupId not found."
            )

        val members = groupDao.getAllMembersInGroup(groupId)

        val myMember = members.find {
            isMe(it)
        } ?: throw IllegalStateException(
            "Local member not found in group roster."
        )

        if (!myMember.isCryptoActive) {
            throw SecurityException(
                "Local member is not cryptographically active."
            )
        }

        if (!myMember.isSwami && !myMember.canModifyMembers) {
            throw SecurityException(
                "Local member is not authorized to commit a new epoch."
            )
        }

        val parentEpoch = group.currentEpoch

        if (parentEpoch <= 0L) {
            throw IllegalStateException(
                "Initial epoch must be created before rotation."
            )
        }

        val newEpoch = parentEpoch + 1L
        val pathSeed = randomBytes(AES_KEY_SIZE_BYTES)

        val newEpochSecret = deriveEpochSecret(
            pathSeed = pathSeed,
            groupId = groupId,
            newEpoch = newEpoch
        )

        val activeRecipients = members.filter {
            it.isCryptoActive &&
                    it.memberInstanceId.isNotBlank() &&
                    it.x25519PublicKey.isNotEmpty() &&
                    it.mlKemPublicKey.isNotEmpty()
        }

        if (activeRecipients.isEmpty()) {
            throw IllegalStateException(
                "No active recipients with valid encryption keys."
            )
        }

        // Create the actual encrypted path for every active member.
        val encryptedPaths = activeRecipients.map { recipient ->
            val recipientX25519 = parsePublicKey(
                bytes = recipient.x25519PublicKey,
                algorithm = X25519_ALGORITHM
            )

            val recipientMlKem = parsePublicKey(
                bytes = recipient.mlKemPublicKey,
                algorithm = ML_KEM_ALGORITHM
            )

            encapsulatePathSeed(
                groupId = groupId,
                newEpoch = newEpoch,
                commitId = commitId,
                recipientLeafId = recipient.memberInstanceId,
                pathSeed = pathSeed,
                recipientX25519Public = recipientX25519,
                recipientMlKemPublic = recipientMlKem
            )
        }

        // Create a signed hash entry for every encrypted path.
        // These hashes are shared with every recipient, while each recipient
        // receives only its own actual encrypted path.
        val pathHashes = encryptedPaths
            .map { path ->
                EpochPathHash(
                    recipientLeafId = path.recipientLeafId,
                    pathHash = hashEncryptedPath(path)
                )
            }
            .sortedBy {
                it.recipientLeafId
            }

        val parentCommitHash = group.currentCommitHash

        // IMPORTANT:
        // computeCommitHash() must use pathHashes.
        // It must NOT use encryptedPaths.
        val unsignedCommit = EpochCommit(
            protocolVersion = group.groupCryptoProtocolVersion,
            groupId = groupId,
            parentEpoch = parentEpoch,
            newEpoch = newEpoch,
            rosterVersion = rosterVersion,
            commitId = commitId,
            parentCommitHash = parentCommitHash,
            committerMemberId = myMember.memberInstanceId,
            committerDeviceId = CURRENT_DEVICE_ID,
            pathHashes = pathHashes,
            encryptedPaths = encryptedPaths,
            signature = ByteArray(0)
        )

        val commitHash = computeCommitHash(unsignedCommit)

        val signature = signCommit(
            commitHash = commitHash,
            signingPrivateKey = privateEd25519
        )

        val finalCommit = unsignedCommit.copy(
            signature = signature
        )

        // Save the new epoch locally before distributing it.
        saveEpochSecret(
            groupId = groupId,
            epoch = newEpoch,
            epochSecret = newEpochSecret,
            commitHash = commitHash
        )

        groupDao.updateGroupCryptoState(
            groupId = groupId,
            currentEpoch = newEpoch,
            currentCommitHash = commitHash,
            rosterVersion = rosterVersion,
            protocolVersion = finalCommit.protocolVersion
        )

        pruneOldEpochs(
            groupId = groupId,
            currentEpoch = newEpoch
        )

        return finalCommit
    }

    suspend fun receiveEpochCommit(
        commit: EpochCommit,
        myX25519Private: PrivateKey,
        myMlKemPrivate: PrivateKey
    ) {
        val group = groupDao.getGroupById(commit.groupId)
            ?: throw IllegalStateException(
                "Group ${commit.groupId} not found."
            )

        validateIncomingCommitStructure(
            group = group,
            commit = commit
        )

        val committerMember = groupDao.getGroupMemberByInstanceAndDevice(
            groupId = commit.groupId,
            memberInstanceId = commit.committerMemberId,
            deviceId = commit.committerDeviceId
        ) ?: throw SecurityException(
            "Committer member/device not found in local group roster."
        )

        if (!committerMember.isCryptoActive) {
            throw SecurityException(
                "Committer member/device is not cryptographically active."
            )
        }

        if (committerMember.ed25519PublicKey.isEmpty()) {
            throw SecurityException(
                "Committer Ed25519 public key is missing."
            )
        }

        val committerSigningPublicKey = parsePublicKey(
            bytes = committerMember.ed25519PublicKey,
            algorithm = ED25519_ALGORITHM
        )

        val unsignedCommit = commit.copy(
            signature = ByteArray(0)
        )

        val computedCommitHash = computeCommitHash(
            unsignedCommit
        )

        verifyCommitSignature(
            commitHash = computedCommitHash,
            signatureBytes = commit.signature,
            signingPublicKey = committerSigningPublicKey
        )

        val myMember = groupDao
            .getAllMembersInGroup(commit.groupId)
            .find { isMe(it) }
            ?: throw IllegalStateException(
                "Local member not found in group roster."
            )

        if (!myMember.isCryptoActive) {
            throw SecurityException(
                "Local member is not cryptographically active."
            )
        }

        val myEncryptedPath = commit.encryptedPaths.find {
            it.recipientLeafId == myMember.memberInstanceId
        } ?: throw SecurityException(
            "Epoch commit does not contain a path for this member."
        )

        val expectedPathHash = commit.pathHashes.find {
            it.recipientLeafId == myMember.memberInstanceId
        } ?: throw SecurityException(
            "Epoch commit does not contain a signed path hash for this member."
        )

        val actualPathHash = hashEncryptedPath(
            myEncryptedPath
        )

        if (!actualPathHash.contentEquals(expectedPathHash.pathHash)) {
            throw SecurityException(
                "Encrypted path does not match its signed path hash."
            )
        }



        val pathSeed = decapsulatePathSeed(
            groupId = commit.groupId,
            newEpoch = commit.newEpoch,
            commitId = commit.commitId,
            payload = myEncryptedPath,
            recipientX25519Private = myX25519Private,
            recipientMlKemPrivate = myMlKemPrivate
        )

        val newEpochSecret = deriveEpochSecret(
            pathSeed = pathSeed,
            groupId = commit.groupId,
            newEpoch = commit.newEpoch
        )

        saveEpochSecret(
            groupId = commit.groupId,
            epoch = commit.newEpoch,
            epochSecret = newEpochSecret,
            commitHash = computedCommitHash
        )

        groupDao.updateGroupCryptoState(
            groupId = commit.groupId,
            currentEpoch = commit.newEpoch,
            currentCommitHash = computedCommitHash,
            rosterVersion = commit.rosterVersion,
            protocolVersion = commit.protocolVersion
        )

        pruneOldEpochs(
            groupId = commit.groupId,
            currentEpoch = commit.newEpoch
        )
    }

    // =========================================================================
    // 6. HYBRID X25519 + ML-KEM PATH SEED ENCAPSULATION
    // =========================================================================

    private fun encapsulatePathSeed(
        groupId: String,
        newEpoch: Long,
        commitId: String,
        recipientLeafId: String,
        pathSeed: ByteArray,
        recipientX25519Public: PublicKey,
        recipientMlKemPublic: PublicKey
    ): HybridEncapsulatedPayload {
        val ephemeralPair = KeyPairGenerator.getInstance(
            X25519_ALGORITHM,
            BC_PROVIDER
        ).generateKeyPair()

        val ecdhSecret = performX25519(
            privateKey = ephemeralPair.private,
            publicKey = recipientX25519Public
        )

        val kemGenerator = KeyGenerator.getInstance(
            ML_KEM_ALGORITHM,
            BC_PROVIDER
        )

        kemGenerator.init(
            KEMGenerateSpec(
                recipientMlKemPublic,
                "AES"
            )
        )

        val encapsulatedSecret =
            kemGenerator.generateKey()
                    as SecretKeyWithEncapsulation

        val pqSecret = encapsulatedSecret.encoded

        val hybridKey = deriveHybridWrapKey(
            ecdhSecret = ecdhSecret,
            pqSecret = pqSecret,
            groupId = groupId,
            newEpoch = newEpoch,
            commitId = commitId,
            recipientLeafId = recipientLeafId
        )

        val encrypted = encryptAesGcm(
            key = hybridKey,
            plaintext = pathSeed,
            aad = buildPathSeedAad(
                groupId = groupId,
                newEpoch = newEpoch,
                commitId = commitId,
                recipientLeafId = recipientLeafId
            )
        )

        return HybridEncapsulatedPayload(
            recipientLeafId = recipientLeafId,
            ephemeralX25519Public = ephemeralPair.public.encoded,
            mlKemCiphertext = encapsulatedSecret.encapsulation,
            encryptedPathSeed = encrypted.ciphertext,
            nonce = encrypted.nonce
        )
    }

    private fun decapsulatePathSeed(
        groupId: String,
        newEpoch: Long,
        commitId: String,
        payload: HybridEncapsulatedPayload,
        recipientX25519Private: PrivateKey,
        recipientMlKemPrivate: PrivateKey
    ): ByteArray {
        val ephemeralPublic = parsePublicKey(
            bytes = payload.ephemeralX25519Public,
            algorithm = X25519_ALGORITHM
        )

        val ecdhSecret = performX25519(
            privateKey = recipientX25519Private,
            publicKey = ephemeralPublic
        )

        val kemExtractor = KeyGenerator.getInstance(
            ML_KEM_ALGORITHM,
            BC_PROVIDER
        )

        kemExtractor.init(
            KEMExtractSpec(
                recipientMlKemPrivate,
                payload.mlKemCiphertext,
                "AES"
            )
        )

        val pqSecret = kemExtractor
            .generateKey()
            .encoded

        val hybridKey = deriveHybridWrapKey(
            ecdhSecret = ecdhSecret,
            pqSecret = pqSecret,
            groupId = groupId,
            newEpoch = newEpoch,
            commitId = commitId,
            recipientLeafId = payload.recipientLeafId
        )

        return decryptAesGcm(
            key = hybridKey,
            ciphertext = payload.encryptedPathSeed,
            nonce = payload.nonce,
            aad = buildPathSeedAad(
                groupId = groupId,
                newEpoch = newEpoch,
                commitId = commitId,
                recipientLeafId = payload.recipientLeafId
            )
        )
    }

    // =========================================================================
    // 7. EPOCH SECRET DERIVATION
    // =========================================================================

    private fun deriveEpochSecret(
        pathSeed: ByteArray,
        groupId: String,
        newEpoch: Long
    ): ByteArray {
        require(pathSeed.size == AES_KEY_SIZE_BYTES) {
            "Invalid path seed length."
        }

        return hkdfSha256(
            ikm = pathSeed,
            salt = ByteArray(0),
            info = canonicalBytes(
                DOMAIN_EPOCH_SECRET,
                groupId,
                newEpoch.toString()
            ),
            outputLength = AES_KEY_SIZE_BYTES
        )
    }

    private fun deriveHybridWrapKey(
        ecdhSecret: ByteArray,
        pqSecret: ByteArray,
        groupId: String,
        newEpoch: Long,
        commitId: String,
        recipientLeafId: String
    ): ByteArray {
        return hkdfSha256(
            ikm = ecdhSecret + pqSecret,
            salt = sha256(
                canonicalBytes(
                    DOMAIN_HYBRID_WRAP,
                    groupId
                )
            ),
            info = canonicalBytes(
                DOMAIN_HYBRID_WRAP,
                groupId,
                newEpoch.toString(),
                commitId,
                recipientLeafId
            ),
            outputLength = AES_KEY_SIZE_BYTES
        )
    }

    // =========================================================================
    // 8. EPOCH SECRET LOCAL STORAGE
    // =========================================================================

    suspend fun saveEpochSecret(
        groupId: String,
        epoch: Long,
        epochSecret: ByteArray,
        commitHash: ByteArray
    ) {
        require(epochSecret.size == AES_KEY_SIZE_BYTES) {
            "Epoch secret must be exactly 32 bytes."
        }

        val encryptedPayload = keyStoreManager.wrap(
            plaintext = epochSecret,
            aad = buildEpochSecretStorageAad(
                groupId = groupId,
                epoch = epoch,
                commitHash = commitHash
            )
        )

        groupDao.insertEpochKey(
            GroupEpochKeyEntity(
                groupId = groupId,
                epoch = epoch,
                encryptedEpochSecret = encryptedPayload.ciphertext,
                nonce = encryptedPayload.nonce,
                keyVersion = encryptedPayload.keyVersion,
                commitHash = commitHash
            )
        )
    }

    suspend fun getEpochSecret(
        groupId: String,
        epoch: Long
    ): ByteArray {
        val entity = groupDao.getEpochKey(
            groupId = groupId,
            epoch = epoch
        ) ?: throw IllegalStateException(
            "Epoch secret for group $groupId epoch $epoch not found."
        )

        val epochSecret = keyStoreManager.unwrap(
            ciphertext = entity.encryptedEpochSecret,
            nonce = entity.nonce,
            keyVersion = entity.keyVersion,
            aad = buildEpochSecretStorageAad(
                groupId = entity.groupId,
                epoch = entity.epoch,
                commitHash = entity.commitHash
            )
        )

        require(epochSecret.size == AES_KEY_SIZE_BYTES) {
            "Invalid epoch secret length."
        }

        return epochSecret
    }

    private suspend fun pruneOldEpochs(
        groupId: String,
        currentEpoch: Long
    ) {
        if (currentEpoch > MAX_RETAINED_EPOCHS) {
            groupDao.pruneOldEpochKeys(
                groupId = groupId,
                minimumEpoch = currentEpoch - MAX_RETAINED_EPOCHS + 1L
            )
        }
    }

    // =========================================================================
    // 9. COMMIT VALIDATION
    // =========================================================================

    private fun validateIncomingCommitStructure(
        group: GroupEntity,
        commit: EpochCommit
    ) {
        DLog.i(
            "Epoch validation: " +
                    "groupId=${group.groupId} " +
                    "localCurrentEpoch=${group.currentEpoch} " +
                    "commitParentEpoch=${commit.parentEpoch} " +
                    "commitNewEpoch=${commit.newEpoch} " +
                    "localRosterVersion=${group.rosterVersion} " +
                    "commitRosterVersion=${commit.rosterVersion}"
        )

        require(
            commit.protocolVersion ==
                    group.groupCryptoProtocolVersion
        ) {
            "Unsupported group crypto protocol version."
        }

        require(commit.groupId == group.groupId) {
            "Commit group ID mismatch."
        }

        require(
            commit.newEpoch > group.currentEpoch
        ) {
            "Commit epoch is not newer than current epoch."
        }

        require(
            commit.rosterVersion > group.rosterVersion
        ) {
            "Commit roster version is not newer than current roster version."
        }

        require(
            commit.rosterVersion >= group.rosterVersion
        ) {
            "Stale roster version."
        }



        require(commit.commitId.isNotBlank()) {
            "Commit ID is required."
        }

        require(commit.committerMemberId.isNotBlank()) {
            "Committer member ID is required."
        }

        require(commit.committerDeviceId.isNotBlank()) {
            "Committer device ID is required."
        }

        require(commit.encryptedPaths.isNotEmpty()) {
            "Commit contains no encrypted paths."
        }

        require(commit.signature.isNotEmpty()) {
            "Commit signature is missing."
        }

        val uniqueRecipients = commit.encryptedPaths
            .map { it.recipientLeafId }
            .toSet()

        require(
            uniqueRecipients.size ==
                    commit.encryptedPaths.size
        ) {
            "Commit contains duplicate recipient paths."
        }
    }
// =========================================================================
// 10. COMMIT HASH + SIGNATURE
// =========================================================================

    private fun computeCommitHash(
        commit: EpochCommit
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")

        updateDigestString(
            digest,
            DOMAIN_COMMIT_HASH
        )

        updateDigestInt(
            digest,
            commit.protocolVersion
        )

        updateDigestString(
            digest,
            commit.groupId
        )

        updateDigestLong(
            digest,
            commit.parentEpoch
        )

        updateDigestLong(
            digest,
            commit.newEpoch
        )

        updateDigestLong(
            digest,
            commit.rosterVersion
        )

        updateDigestString(
            digest,
            commit.commitId
        )

        updateDigestBytes(
            digest,
            commit.parentCommitHash
        )

        updateDigestString(
            digest,
            commit.committerMemberId
        )

        updateDigestString(
            digest,
            commit.committerDeviceId
        )

        // IMPORTANT:
        // Hash the signed path hashes, not the actual encrypted paths.
        commit.pathHashes
            .sortedBy { it.recipientLeafId }
            .forEach { pathHash ->
                updateDigestString(
                    digest,
                    pathHash.recipientLeafId
                )

                updateDigestBytes(
                    digest,
                    pathHash.pathHash
                )
            }

        return digest.digest()
    }
    private fun signCommit(
        commitHash: ByteArray,
        signingPrivateKey: PrivateKey
    ): ByteArray {
        val signature = Signature.getInstance(
            ED25519_ALGORITHM,
            BC_PROVIDER
        )

        signature.initSign(
            signingPrivateKey,
            SECURE_RANDOM
        )

        signature.update(
            canonicalBytes(
                DOMAIN_COMMIT_SIGNATURE
            )
        )

        signature.update(commitHash)

        return signature.sign()
    }

    private fun verifyCommitSignature(
        commitHash: ByteArray,
        signatureBytes: ByteArray,
        signingPublicKey: PublicKey
    ) {
        val signature = Signature.getInstance(
            ED25519_ALGORITHM,
            BC_PROVIDER
        )

        signature.initVerify(signingPublicKey)

        signature.update(
            canonicalBytes(
                DOMAIN_COMMIT_SIGNATURE
            )
        )

        signature.update(commitHash)

        if (!signature.verify(signatureBytes)) {
            throw SecurityException(
                "Invalid epoch commit signature."
            )
        }
    }

    // =========================================================================
    // 11. AES-256-GCM
    // =========================================================================

    private fun encryptAesGcm(
        key: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray
    ): AesGcmPayload {
        require(key.size == AES_KEY_SIZE_BYTES) {
            "AES-256 requires a 32-byte key."
        }

        val nonce = randomBytes(
            GCM_NONCE_SIZE_BYTES
        )

        val cipher = Cipher.getInstance(
            AES_GCM_TRANSFORMATION
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                nonce
            )
        )

        cipher.updateAAD(aad)

        return AesGcmPayload(
            ciphertext = cipher.doFinal(plaintext),
            nonce = nonce
        )
    }

    private fun decryptAesGcm(
        key: ByteArray,
        ciphertext: ByteArray,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray {
        require(key.size == AES_KEY_SIZE_BYTES) {
            "AES-256 requires a 32-byte key."
        }

        require(nonce.size == GCM_NONCE_SIZE_BYTES) {
            "Invalid AES-GCM nonce."
        }

        val cipher = Cipher.getInstance(
            AES_GCM_TRANSFORMATION
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                nonce
            )
        )

        cipher.updateAAD(aad)

        return cipher.doFinal(ciphertext)
    }

    // =========================================================================
    // 12. MESSAGE AAD
    // =========================================================================

    private fun buildMessageAad(
        message: GroupMessage
    ): ByteArray {
        return canonicalBytes(
            DOMAIN_MESSAGE_AAD,
            message.protocolVersion.toString(),
            message.groupId,
            message.epoch.toString(),
            message.senderId,
            message.senderDeviceId,
            message.messageId,
            message.messageNumber.toString()
        )
    }

    private fun buildPathSeedAad(
        groupId: String,
        newEpoch: Long,
        commitId: String,
        recipientLeafId: String
    ): ByteArray {
        return canonicalBytes(
            DOMAIN_HYBRID_WRAP,
            groupId,
            newEpoch.toString(),
            commitId,
            recipientLeafId
        )
    }

    private fun buildEpochSecretStorageAad(
        groupId: String,
        epoch: Long,
        commitHash: ByteArray
    ): ByteArray {
        return canonicalBytes(
            DOMAIN_EPOCH_STORAGE_AAD,
            groupId,
            epoch.toString(),
            Base64.encodeToString(
                commitHash,
                Base64.NO_WRAP
            )
        )
    }

    private fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLength: Int
    ): ByteArray {
        val effectiveSalt =
            if (salt.isEmpty()) ByteArray(32)
            else salt

        val extractMac = Mac.getInstance(
            HKDF_HASH
        )

        extractMac.init(
            SecretKeySpec(
                effectiveSalt,
                HKDF_HASH
            )
        )

        val prk = extractMac.doFinal(ikm)

        val expandMac = Mac.getInstance(
            HKDF_HASH
        )

        expandMac.init(
            SecretKeySpec(
                prk,
                HKDF_HASH
            )
        )

        val output = ByteArray(outputLength)

        var previous = ByteArray(0)
        var outputOffset = 0
        var counter = 1

        while (outputOffset < outputLength) {
            expandMac.reset()

            expandMac.update(previous)
            expandMac.update(info)
            expandMac.update(counter.toByte())

            previous = expandMac.doFinal()

            val copyLength = minOf(
                previous.size,
                outputLength - outputOffset
            )

            System.arraycopy(
                previous,
                0,
                output,
                outputOffset,
                copyLength
            )

            outputOffset += copyLength
            counter++

            if (counter > 255) {
                throw IllegalArgumentException(
                    "HKDF output length is too large."
                )
            }
        }

        return output
    }

    // =========================================================================
    // 14. X25519 + ML-KEM HELPERS
    // =========================================================================

    private fun performX25519(
        privateKey: PrivateKey,
        publicKey: PublicKey
    ): ByteArray {
        val agreement = KeyAgreement.getInstance(
            X25519_ALGORITHM,
            BC_PROVIDER
        )

        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)

        return agreement.generateSecret()
    }
    private fun getKeyFactory(algorithm: String): KeyFactory {
        return KeyFactory.getInstance(algorithm, BC_PROVIDER)
    }
    private fun parsePublicKey(
        bytes: ByteArray,
        algorithm: String
    ): PublicKey {
        return getKeyFactory(algorithm).generatePublic(
            X509EncodedKeySpec(bytes)
        )
    }

    private fun parsePrivateKey(
        bytes: ByteArray,
        algorithm: String
    ): PrivateKey {
        return getKeyFactory(algorithm).generatePrivate(
            PKCS8EncodedKeySpec(bytes)
        )
    }
    // =========================================================================
    // 15. LOCAL MEMBER IDENTIFICATION
    // =========================================================================

    private suspend fun getMyMemberIdentity(): Pair<String, String?> {
        val userNumber = userNumberKhud.first()
            ?: throw IllegalStateException("User number is not available.")

        val userNameId = userNameIdKhud.first()

        return Pair(userNumber, userNameId)
    }

    private suspend fun isMe(member: GroupMemberEntity): Boolean {
        val (userNumber, userNameId) = getMyMemberIdentity()

        return member.userNumber == userNumber ||
                (!userNameId.isNullOrBlank() && member.userNumber == userNameId)
    }
    // =========================================================================
    // 16. LOW-LEVEL HELPERS
    // =========================================================================

    private fun randomBytes(
        size: Int
    ): ByteArray {
        return ByteArray(size).also {
            SECURE_RANDOM.nextBytes(it)
        }
    }

    private fun sha256(
        data: ByteArray
    ): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(data)
    }

    private fun canonicalBytes(
        vararg values: String
    ): ByteArray {
        val output = ByteArrayOutputStream()

        values.forEach { value ->
            val bytes = value.toByteArray(Charsets.UTF_8)

            output.write(
                ByteBuffer
                    .allocate(4)
                    .putInt(bytes.size)
                    .array()
            )

            output.write(bytes)
        }

        return output.toByteArray()
    }

    private fun updateDigestString(
        digest: MessageDigest,
        value: String
    ) {
        val bytes = value.toByteArray(Charsets.UTF_8)

        digest.update(
            ByteBuffer
                .allocate(4)
                .putInt(bytes.size)
                .array()
        )

        digest.update(bytes)
    }

    private fun updateDigestInt(
        digest: MessageDigest,
        value: Int
    ) {
        digest.update(
            ByteBuffer
                .allocate(4)
                .putInt(value)
                .array()
        )
    }

    private fun updateDigestLong(
        digest: MessageDigest,
        value: Long
    ) {
        digest.update(
            ByteBuffer
                .allocate(8)
                .putLong(value)
                .array()
        )
    }

    private fun updateDigestBytes(
        digest: MessageDigest,
        value: ByteArray?
    ) {
        if (value == null) {
            digest.update(
                ByteBuffer
                    .allocate(4)
                    .putInt(-1)
                    .array()
            )
            return
        }

        digest.update(
            ByteBuffer
                .allocate(4)
                .putInt(value.size)
                .array()
        )

        digest.update(value)
    }

    private fun ByteArray?.contentEqualsNullable(
        other: ByteArray?
    ): Boolean {
        return when {
            this === other -> true
            this == null || other == null -> false
            else -> this.contentEquals(other)
        }
    }





    private fun generateLocalGroupKeyPairs(): LocalGroupKeyPairs {

        val x25519KeyPair = KeyPairGenerator.getInstance(X25519_ALGORITHM, BC_PROVIDER).generateKeyPair()
        val mlKemGenerator = KeyPairGenerator.getInstance(
            ML_KEM_ALGORITHM,
            BC_PROVIDER
        )

        mlKemGenerator.initialize(
            MLKEMParameterSpec.ml_kem_768,
            SECURE_RANDOM
        )

        val mlKemKeyPair = mlKemGenerator.generateKeyPair()

        val ed25519KeyPair = KeyPairGenerator.getInstance(ED25519_ALGORITHM, BC_PROVIDER).generateKeyPair()

        return LocalGroupKeyPairs(
            x25519KeyPair = x25519KeyPair,
            mlKemKeyPair = mlKemKeyPair,
            ed25519KeyPair = ed25519KeyPair
        )
    }
    private fun buildLocalKeyAad(
        groupId: String,
        memberInstanceId: String,
        keyType: String
    ): ByteArray {
        return (
                "DOOTAWAS-GROUP-PRIVATE-KEY|$groupId|" +
                        "$memberInstanceId|$keyType"
                ).toByteArray(Charsets.UTF_8)
    }

    suspend fun getOrCreateLocalGroupKeys(
        groupId: String,
        memberInstanceId: String
    ): LocalGroupKeys {
        val myNumber = userNumberKhud.firstOrNull()
            ?: throw IllegalStateException("Local user number missing.")

        val myMember = groupDao.getAGroupMemberByNumber(
            groupId = groupId,
            userNumber = myNumber
        ) ?: throw IllegalStateException(
            "Local member not found in group=$groupId"
        )

        if (myMember.memberInstanceId != memberInstanceId) {
            throw SecurityException(
                "memberInstanceId does not belong to the local member."
            )
        }

        val existing = groupDao.getGroupPrivateKeys(
            groupId = groupId,
            memberInstanceId = memberInstanceId
        )

        if (existing != null) {
            val x25519Bytes = keyStoreManager.unwrap(
                ciphertext = existing.encryptedX25519PrivateKey,
                nonce = existing.x25519Nonce,
                keyVersion = existing.x25519KeyVersion,
                aad = buildLocalKeyAad(
                    groupId = groupId,
                    memberInstanceId = memberInstanceId,
                    keyType = "X25519"
                )
            )

            val mlKemBytes = keyStoreManager.unwrap(
                ciphertext = existing.encryptedMlKemPrivateKey,
                nonce = existing.mlKemNonce,
                keyVersion = existing.mlKemKeyVersion,
                aad = buildLocalKeyAad(
                    groupId = groupId,
                    memberInstanceId = memberInstanceId,
                    keyType = "ML-KEM-768"
                )
            )

            val ed25519Bytes = keyStoreManager.unwrap(
                ciphertext = existing.encryptedEd25519PrivateKey,
                nonce = existing.ed25519Nonce,
                keyVersion = existing.ed25519KeyVersion,
                aad = buildLocalKeyAad(
                    groupId = groupId,
                    memberInstanceId = memberInstanceId,
                    keyType = "Ed25519"
                )
            )

            if (
                myMember.x25519PublicKey.isEmpty() ||
                myMember.mlKemPublicKey.isEmpty() ||
                myMember.ed25519PublicKey.isEmpty()
            ) {
                throw IllegalStateException(
                    "Local private keys exist but matching public keys are missing."
                )
            }

            return LocalGroupKeys(
                x25519PrivateKey = parsePrivateKey(
                    bytes = x25519Bytes,
                    algorithm = X25519_ALGORITHM
                ),
                x25519PublicKey = parsePublicKey(
                    bytes = myMember.x25519PublicKey,
                    algorithm = X25519_ALGORITHM
                ),
                mlKemPrivateKey = parsePrivateKey(
                    bytes = mlKemBytes,
                    algorithm = ML_KEM_ALGORITHM
                ),
                mlKemPublicKey = parsePublicKey(
                    bytes = myMember.mlKemPublicKey,
                    algorithm = ML_KEM_ALGORITHM
                ),
                ed25519PrivateKey = parsePrivateKey(
                    bytes = ed25519Bytes,
                    algorithm = ED25519_ALGORITHM
                ),
                ed25519PublicKey = parsePublicKey(
                    bytes = myMember.ed25519PublicKey,
                    algorithm = ED25519_ALGORITHM
                )
            )
        }

        val keyPairs = generateLocalGroupKeyPairs()

        val encryptedX25519 = keyStoreManager.wrap(
            plaintext = keyPairs.x25519KeyPair.private.encoded,
            aad = buildLocalKeyAad(
                groupId = groupId,
                memberInstanceId = memberInstanceId,
                keyType = "X25519"
            )
        )

        val encryptedMlKem = keyStoreManager.wrap(
            plaintext = keyPairs.mlKemKeyPair.private.encoded,
            aad = buildLocalKeyAad(
                groupId = groupId,
                memberInstanceId = memberInstanceId,
                keyType = "ML-KEM-768"
            )
        )

        val encryptedEd25519 = keyStoreManager.wrap(
            plaintext = keyPairs.ed25519KeyPair.private.encoded,
            aad = buildLocalKeyAad(
                groupId = groupId,
                memberInstanceId = memberInstanceId,
                keyType = "Ed25519"
            )
        )

        groupDao.insertGroupPrivateKeys(
            GroupPrivateKeyEntity(
                groupId = groupId,
                memberInstanceId = memberInstanceId,
                encryptedX25519PrivateKey = encryptedX25519.ciphertext,
                x25519Nonce = encryptedX25519.nonce,
                x25519KeyVersion = encryptedX25519.keyVersion,
                encryptedMlKemPrivateKey = encryptedMlKem.ciphertext,
                mlKemNonce = encryptedMlKem.nonce,
                mlKemKeyVersion = encryptedMlKem.keyVersion,
                encryptedEd25519PrivateKey = encryptedEd25519.ciphertext,
                ed25519Nonce = encryptedEd25519.nonce,
                ed25519KeyVersion = encryptedEd25519.keyVersion
            )
        )

        groupDao.updateMemberCryptoKeys(
            groupId = groupId,
            userNumber = myNumber,
            x25519PublicKey = keyPairs.x25519KeyPair.public.encoded,
            mlKemPublicKey = keyPairs.mlKemKeyPair.public.encoded,
            ed25519PublicKey = keyPairs.ed25519KeyPair.public.encoded
        )

        DLog.i(
            "Created and saved local group key pairs: " +
                    "group=$groupId member=$memberInstanceId"
        )

        return LocalGroupKeys(
            x25519PrivateKey = keyPairs.x25519KeyPair.private,
            x25519PublicKey = keyPairs.x25519KeyPair.public,
            mlKemPrivateKey = keyPairs.mlKemKeyPair.private,
            mlKemPublicKey = keyPairs.mlKemKeyPair.public,
            ed25519PrivateKey = keyPairs.ed25519KeyPair.private,
            ed25519PublicKey = keyPairs.ed25519KeyPair.public
        )
    }

}