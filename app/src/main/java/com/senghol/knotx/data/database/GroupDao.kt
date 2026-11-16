package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.senghol.knotx.utils.data.GroupMessageMeta
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM group_sandesh WHERE groupId = :groupId ORDER BY time DESC")
    fun getMessagesByUser(groupId: String): Flow<List<GroupSandesh>>
    @Query("SELECT * FROM group_sandesh")
    suspend fun getAllGroupSandesh(): List<GroupSandesh>

    // Keyset paging for older messages
    @Query("""
        SELECT * FROM group_sandesh 
        WHERE groupId = :groupId AND time < :beforeTime 
        ORDER BY time DESC 
        LIMIT :limit
    """)


    suspend fun getOlderMessagesByGroup(
        groupId: String,
        beforeTime: Long,
        limit: Int
    ): List<GroupSandesh>
    @Query("SELECT * FROM group_sandesh WHERE sandeshId = :sandeshId AND groupId = :groupId AND userNumber = :userNumber LIMIT 1")
    suspend fun getGroupSandeshByIdAndUser(
        sandeshId: String,
        groupId: String,
        userNumber: String
    ): GroupSandesh?

    @Query("SELECT * FROM group_sandesh WHERE sandeshId = :sandeshId AND groupId = :groupId LIMIT 1")
    suspend fun getGroupSandeshByIdAndGroupId(
        sandeshId: String,
        groupId: String,
    ): GroupSandesh?

        @Query("""SELECT * FROM group_sandesh WHERE groupId = :groupId AND sandeshStithi = 0 """)
    suspend fun getLikhitGroupMessages(groupId: String): List<GroupSandesh>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(sandesh: GroupSandesh)


    @Query("SELECT * FROM group_sandesh WHERE userNumber = :userNumber AND sandeshType = :type")
    fun getGroupSandeshByUserAndType(userNumber: String, type: Int): Flow<List<GroupSandesh>>

    @Query("""
    UPDATE group_sandesh 
    SET sandeshStithi = :newStithi 
    WHERE sandeshId = :messageId 
      AND groupId = :groupId 
      AND sandeshStithi < :newStithi
""")
    fun updateStithiGroupSandeshIfForwardProgress(
        messageId: String,
        groupId: String,
        newStithi: Int  // use `stithi.value`
    )

    @Query(
        """
    UPDATE group_sandesh
    SET sandeshStithi = :status
    """
    )
    suspend fun markAllGroupMessagesAsViewed(
        status: SandeshStithi = SandeshStithi.DHRIST
    )

    @Query("UPDATE group_entity SET totalUnread = 0 WHERE groupId = :userNumber")
    suspend fun resetUnread(userNumber: String)
    @Query("UPDATE group_sandesh SET time = :newTime WHERE groupId = :userNumber AND sandeshId = :sandeshId")
    suspend fun updateTimeByUserAndId(userNumber: String, sandeshId: String, newTime: Long)


    @Query(
        """
    DELETE FROM group_sandesh 
    WHERE sandeshId IN (:messageIds)
    AND groupId = :groupId
    AND (:skipOwnerCheck = 1 OR userNumber = :ownerNumber)
"""
    )
    suspend fun deleteMessages(
        messageIds: List<String>,
        groupId: String,
        ownerNumber: String,
        skipOwnerCheck: Boolean
    )
    @Query("UPDATE group_sandesh SET samagri = :samagri WHERE sandeshId = :sandeshId AND groupId = :groupId")
    suspend fun updateSamagri(sandeshId: String, samagri: String,groupId: String)

    @Delete
    suspend fun deleteMessage(sandesh: GroupSandesh)

    @Query("DELETE FROM group_sandesh WHERE sandeshId = :sandeshId AND userNumber = :user")
    suspend fun deleteMessageById(sandeshId: String, user: String)

    @Query("DELETE FROM group_sandesh WHERE groupId = :id")
    suspend fun clearGroupChat(id: String)
    // ✅ 2. OPTIONAL: Get unread messages for a specific USER inside a GROUP
    // (Useful if you track per-member read status)
    @Query("""
    SELECT sandeshId, userNumber
    FROM group_sandesh
    WHERE groupId = :groupId
      AND isSelf = 0
      AND sandeshStithi != :dhrist
""")
    suspend fun getUnreadGroupMessagesForUser(
        groupId: String,
        dhrist: SandeshStithi = SandeshStithi.DHRIST
    ): List<GroupMessageMeta>




//region GROUPS

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupEntity(sandesh: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGroups(groups: List<GroupEntity>)
    @Query("SELECT * FROM group_entity WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): GroupEntity?

    @Query("UPDATE group_entity SET draftMessage = :draft WHERE groupId = :userNumber")
    suspend fun updateDraft(userNumber: String, draft: String)

    @Query("""
    UPDATE group_entity
    SET currentEpoch = :currentEpoch,
        currentCommitHash = :currentCommitHash,
        rosterVersion = :rosterVersion,
        groupCryptoProtocolVersion = :protocolVersion
    WHERE groupId = :groupId
""")
    suspend fun updateGroupCryptoState(
        groupId: String,
        currentEpoch: Long,
        currentCommitHash: ByteArray,
        rosterVersion: Long,
        protocolVersion: Int
    )

    @Query("""
    SELECT * FROM group_members
    WHERE groupId = :groupId
      AND memberInstanceId = :memberInstanceId
      AND deviceId = :deviceId
    LIMIT 1
""")
    suspend fun getGroupMemberByInstanceAndDevice(
        groupId: String,
        memberInstanceId: String,
        deviceId: String
    ): GroupMemberEntity?

    // 🔹 Delete group
    @Query("DELETE FROM group_entity WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("""
    UPDATE group_entity
    SET wallpaperImage = :wallpaperImage
    WHERE groupId = :groupId
""")
    suspend fun updateWallpaper(
        groupId: String,
        wallpaperImage: String?
    )
@Query("UPDATE group_entity SET currentEpoch = :newEpoch WHERE groupId = :groupId")
    suspend fun updateGroupEpoch(groupId: String, newEpoch: Long)

    @Query("SELECT currentEpoch FROM group_entity WHERE groupId = :groupId")
    suspend fun getCurrentEpoch(groupId: String): Long?
    @Query("SELECT * FROM group_entity WHERE groupId = :groupId LIMIT 1")
    fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?>
    @Query("SELECT COUNT(*) FROM group_entity WHERE groupId = :groupId")
    suspend fun doesGroupExist(groupId: String): Int  // ✅ Returns count (0 = not exists, >0 =// exists)

    @Query("""UPDATE group_entity SET isGroupActive = :isActive WHERE groupId = :groupId""")
    suspend fun updateGroupActive(groupId: String, isActive: Boolean)

    @Query("""
    SELECT revealSender
    FROM group_entity
    WHERE groupId = :groupId
    LIMIT 1
""")
    suspend fun getRevealSender(groupId: String): Boolean?

    @Query("""
    UPDATE group_entity
    SET revealSender = :revealSender
    WHERE groupId = :groupId
""")
    suspend fun updateRevealSender(
        groupId: String,
        revealSender: Boolean
    )
    // 🔹 Clear chat (only messages, not group itself)
    @Query("""
        UPDATE group_entity 
        SET lastMessage = NULL, 
            lastMessageTime = 0,
            totalUnread = 0,
            lastMessageId = ''
        WHERE groupId = :groupId
    """)
    suspend fun clearGroupChatEntity(groupId: String)
    // 🔹 Update Pin
    @Query("""
        UPDATE group_entity 
        SET isPinned = :pin 
        WHERE groupId = :groupId
    """)
    suspend fun updatePinnedStatus(groupId: String, pin: Boolean)

    // 🔹 Update Mute
    @Query("""
        UPDATE group_entity 
        SET isMuted = :isMuted 
        WHERE groupId = :groupId
    """)
    suspend fun updateMutedStatus(groupId: String, isMuted: Boolean)

    @Query("""
    UPDATE group_entity
    SET lastMessage = :lastMessage,
        lastMessageTime = :lastMessageTime,
        lastMessageId = :lastMessageId,
        sandeshStithi = :sandeshStithi
    WHERE groupId = :groupId
""")
    suspend fun updateLastMessageAndId(
        groupId: String,
        lastMessage: String,
        lastMessageTime: Long,
        lastMessageId: String,
        sandeshStithi: SandeshStithi
    )

        @Query("""
        UPDATE group_entity
        SET lastMessage = :deletedText,
            lastMessageId = '',
            sandeshStithi = :sandeshStithi,
            lastMessageTime = :lastMessageTime
        WHERE lastMessageId = :messageId AND groupId = :group
    """)
        suspend fun resetLastMessageByMessageId(
            messageId: String,
            deletedText: String = "",
            sandeshStithi: SandeshStithi = SandeshStithi.LIKHIT,
            lastMessageTime: Long = System.currentTimeMillis(),
            group: String
        )


    @Query("""
            UPDATE group_entity 
            SET sandeshStithi = :newStithi 
            WHERE lastMessageId = :messageId 
              AND groupId = :group
              AND sandeshStithi < :newStithi
        """)
    fun updateStithiGroupEntityIfForwardProgress(
        messageId: String,
        group: String,
        newStithi: Int  // use `stithi.value`
    )

    @Query("SELECT * FROM group_entity ORDER BY lastMessageTime DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>
    @Query("""
    UPDATE group_entity
    SET currentEpoch = 0,
        currentCommitHash = NULL,
        rosterVersion = 0
""")
    suspend fun clearAllGroupCryptoState()
    @Query("DELETE FROM group_epoch_keys")
    suspend fun clearAllAesKeys()
    @Query("UPDATE group_entity SET totalUnread = totalUnread + 1 WHERE groupId = :groupId")
    suspend fun incrementUnreadByUserNumber(groupId: String)

    @Query("UPDATE group_entity SET groupImageUrl = :newUrl WHERE groupId = :groupId")
    suspend fun updateImageUrlByGroupId(newUrl: String, groupId: String)

    @Query("UPDATE group_entity SET imageTime = :newImageTime WHERE groupId = :userNumber")
    fun updateImageTimeByNumber(userNumber: String, newImageTime: Long)

    @Query("SELECT groupImageUrl FROM group_entity WHERE groupId = :groupId")
    suspend fun getImageUrlByGroupId(groupId: String): String?

    @Query("SELECT groupName FROM group_entity WHERE groupId = :groupId")
    suspend fun getGroupNameByGroupId(groupId: String): String?

    @Query("SELECT isMuted FROM group_entity WHERE groupId = :groupId LIMIT 1")
    suspend fun isGroupMuted(groupId: String): Boolean?

    // or this version if you prefer non-null:
    // @Query("SELECT COALESCE(isMuted, 0) FROM group_entity WHERE groupId = :groupId LIMIT 1")
    // suspend fun isGroupMuted(groupId: String): Boolean

    @Query("UPDATE group_entity SET isMuted = :muted WHERE groupId = :groupId")
    suspend fun setGroupMuted(groupId: String, muted: Boolean)


    //endregion

    //region MEMBERS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getAllMembersInGroup(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members")
    fun getAllGroupMembersFlow(): Flow<List<GroupMemberEntity>>
    @Query("SELECT userNumber FROM group_members WHERE groupId = :groupId")
    suspend fun getAllMemberNumbersInGroup(groupId: String): List<String>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getAllMembersInGroupFlow(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT profileUrl FROM group_members WHERE userNumber = :number")
    suspend fun getImageUrlByNumber(number: String): String?

    @Query("SELECT onlineName FROM group_members WHERE userNumber = :number")
    suspend fun getUserOnlineNameByNumber(number: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Delete
    suspend fun deleteMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userNumber = :userNumber")
    suspend fun deleteMemberById(groupId: String, userNumber: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userNumber IN (:userNumbers)")
    suspend fun deleteMembersFromGroup(groupId: String, userNumbers: List<String>)


    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteAllMembersInGroup(groupId: String)
    @Query("SELECT publicKey FROM group_members WHERE userNumber = :userNumber LIMIT 1")
    suspend fun getPublicKeyFromGroupMembers(userNumber: String): String?
    @Query("UPDATE group_members SET publicKey = :publicKey WHERE userNumber = :userNumber")
    suspend fun updatePublicKeyInGroupMembers(publicKey: String, userNumber: String)
    @Query("UPDATE group_members SET canMessage = :newValue WHERE groupId = :groupId AND userNumber = :userNumber")
    suspend fun updateCanMessage(groupId: String, userNumber: String, newValue: Boolean)
    @Query("UPDATE group_members SET canUpdateProfile = :newValue WHERE groupId = :groupId AND userNumber = :userNumber")
    suspend fun updateCanUpdateProfile(groupId: String, userNumber: String, newValue: Boolean)
    @Query("UPDATE group_members SET canModifyMembers = :newValue WHERE groupId = :groupId AND userNumber = :userNumber")
    suspend fun updateCanModifyMembers(groupId: String, userNumber: String, newValue: Boolean)
    @Query("UPDATE group_members SET canStartCall = :newValue WHERE groupId = :groupId AND userNumber = :userNumber")
    suspend fun updateCanStartCall(groupId: String, userNumber: String, newValue: Boolean)
    @Query("SELECT COUNT(*) FROM group_members WHERE userNumber = :userNumber")
    suspend fun doesUserExistInMembers(userNumber: String): Int
    @Query("UPDATE group_members SET profileUrl = :newUrl WHERE userNumber = :number")
    suspend fun updatePhotoUrlByNumber(newUrl: String, number: String)
    // ✅ Check if a userNumber exists in *any* group
    @Query("SELECT COUNT(*) FROM group_members WHERE userNumber = :userNumber")
    suspend fun isUserExists(userNumber: String): Int

    @Query("SELECT * FROM group_members WHERE userNumber = :userNumber LIMIT 1")
    suspend fun getAnyGroupMemberByNumber(userNumber: String): GroupMemberEntity?


    @Query("SELECT * FROM group_members WHERE groupId= :groupId And userNumber = :userNumber LIMIT 1")
    suspend fun getAGroupMemberByNumber(groupId: String ,userNumber: String): GroupMemberEntity?


    @Query("UPDATE group_members SET profileTime = :newProfileTime WHERE userNumber = :userNumber")
    suspend fun updateProfileTimeForUser(userNumber: String, newProfileTime: Long)

    @Query("UPDATE group_members SET onlineName = :onlineName WHERE userNumber = :userNumber")
    suspend fun updateGroupMemberOnlineName(onlineName: String, userNumber: String)

    @Query("""
    UPDATE group_members
    SET x25519PublicKey = :x25519PublicKey,
        mlKemPublicKey = :mlKemPublicKey,
        ed25519PublicKey = :ed25519PublicKey
    WHERE groupId = :groupId
      AND userNumber = :userNumber
""")
    suspend fun updateMemberCryptoKeys(
        groupId: String,
        userNumber: String,
        x25519PublicKey: ByteArray,
        mlKemPublicKey: ByteArray,
        ed25519PublicKey: ByteArray
    )
    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userNumber NOT IN (:keepList)")
    fun deleteMembersNotIn(groupId: String, keepList: Set<String>)
    //endregion


    //region SandeshStatus
    @Query("SELECT statusJson FROM sandeshStatus WHERE userNumber = :userNumber AND sandeshId = :sandeshId")
    fun getStatusJson(userNumber: String, sandeshId: String): String?
    //endregion

    //region EpochKey
    @Query("""
    SELECT * FROM group_epoch_keys
    WHERE groupId = :groupId AND epoch = :epoch
    LIMIT 1
""")
    suspend fun getEpochKey(
        groupId: String,
        epoch: Long
    ): GroupEpochKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpochKey(epochKey: GroupEpochKeyEntity)

    /**
     * Deletes historical keys beyond the safety threshold to maintain forward secrecy.
     */
    @Query("""
    DELETE FROM group_epoch_keys
    WHERE groupId = :groupId AND epoch < :minimumEpoch
""")
    suspend fun pruneOldEpochKeys(
        groupId: String,
        minimumEpoch: Long
    )

    @Query("DELETE FROM group_epoch_keys WHERE groupId = :groupId")
    suspend fun deleteEpochByGroupId(groupId: String)
    //endregion


    //region GroupPrivateKeyEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPrivateKeys(
        entity: GroupPrivateKeyEntity
    )

    @Query("""
    SELECT * FROM group_private_keys
    WHERE groupId = :groupId
    AND memberInstanceId = :memberInstanceId
    LIMIT 1
""")
    suspend fun getGroupPrivateKeys(
        groupId: String,
        memberInstanceId: String
    ): GroupPrivateKeyEntity?

    @Query("""
    DELETE FROM group_private_keys
    WHERE groupId = :groupId
    AND memberInstanceId = :memberInstanceId
""")
    suspend fun deleteGroupPrivateKeys(
        groupId: String,
        memberInstanceId: String
    )
    @Query("DELETE FROM group_private_keys WHERE groupId = :groupId")
    suspend fun deletePrivateKeyByGroupId(groupId: String)
    @Query("DELETE FROM group_private_keys")
    suspend fun clearAllGroupPrivateKeys()
    //endregion

    //region Pending
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingGroupKeyPackage(
        pending: PendingGroupKeyPackageEntity
    )

    @Query("""
    SELECT * FROM pending_group_key_packages
    WHERE groupId = :groupId
    ORDER BY createdAt ASC
""")
    suspend fun getPendingGroupKeyPackages(
        groupId: String
    ): List<PendingGroupKeyPackageEntity>


    @Query("""
    DELETE FROM pending_group_key_packages
    WHERE groupId = :groupId
    AND userNumber = :userNumber
""")
    suspend fun deletePendingGroupKeyPackage(
        groupId: String,
        userNumber: String
    )


    @Query("""
    SELECT * FROM pending_group_key_packages
    WHERE groupId = :groupId
    AND userNumber = :userNumber
    LIMIT 1
""")
    suspend fun getPendingGroupKeyPackage(
        groupId: String,
        userNumber: String
    ): PendingGroupKeyPackageEntity?
    //endregion



}
