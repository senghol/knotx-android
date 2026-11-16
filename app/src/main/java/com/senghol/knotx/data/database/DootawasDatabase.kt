package com.senghol.knotx.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room


import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.senghol.knotx.calling.data.model.CallConverters

import com.senghol.knotx.utils.CallDataTypeConverter
import com.senghol.knotx.utils.CallOutcomeConverter
import com.senghol.knotx.utils.converters.EncryptedKeyBlobConverter
import com.senghol.knotx.utils.converters.SandeshStithiConverter
import java.util.concurrent.Executors

@Database(
    entities = [
        Sandesh::class, UnsentFiles::class, UnsentSandesh::class, Contact::class,
        CallSession::class, Upbhogta::class, DakiyaSetting::class, GroupSandesh::class,
        GroupEntity::class, GroupMemberEntity::class, SandeshStatus::class,
        IdentityEntity::class, PreKeyEntity::class, SignedPreKeyEntity::class,
        SessionEntity::class, TrustedIdentityEntity::class, KyberPreKeyEntity::class,
        KeyStateEntity::class, DebugLogEntity::class, TransactionEntity::class, GroupEpochKeyEntity::class,
        GroupPrivateKeyEntity::class, PendingGroupKeyPackageEntity::class, StoryEntity::class,
        StoryViewEntity::class, StoryLikeEntity::class, StoryCommentEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    CallOutcomeConverter::class, CallDataTypeConverter::class, SandeshStithiConverter::class,
    EncryptedKeyBlobConverter::class, StoryTypeConverters::class
)
abstract class DootawasDatabase : RoomDatabase() {
    abstract fun sandeshDao(): SandeshDao
    abstract fun groupDao(): GroupDao
    abstract fun signalDao(): SignalDao
    abstract fun upbhogtaDao(): UpbhogtaDao
    abstract fun unsentSandeshDao(): UnsentSandeshDao
    abstract fun unsentFilesDao(): UnsentFilesDao
    abstract fun contactDao(): ContactDao
    abstract fun callSessionDao(): CallSessionDao
    abstract fun dakiyaSettingDao(): DakiyaSettingDao
    abstract fun sandeshStatusDao(): SandeshStatusDao
    abstract fun debugLogDao(): DebugLogDao
    abstract fun transactionDao(): TransactionDao
    abstract fun storyDao(): StoryDao
    abstract fun storyViewDao(): StoryViewDao
    abstract fun storyLikeDao(): StoryLikeDao
    abstract fun storyCommentDao(): StoryCommentDao

    companion object {
        @Volatile
        private var INSTANCE: DootawasDatabase? = null

        fun getDatabase(context: Context): DootawasDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DootawasDatabase::class.java,
                    "sengholroom.db"
                )
                    // 🛠️ Added the new migration here
//                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                        }
                    })
                    .setQueryExecutor(Executors.newFixedThreadPool(4))
                    .build()
                    .also { INSTANCE = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE upbhogta
                    ADD COLUMN customColors TEXT
                """.trimIndent()
                )
            }
        }


        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE upbhogta 
                    ADD COLUMN bakaya REAL NOT NULL DEFAULT 0.0
                """.trimIndent()
                )
            }
        }


        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ============================================================
                // UPBHOGTA
                // ============================================================

                db.execSQL("ALTER TABLE upbhogta ADD COLUMN about TEXT")
                db.execSQL("ALTER TABLE upbhogta ADD COLUMN revealSender INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE upbhogta ADD COLUMN autoDisappearMsg INTEGER NOT NULL DEFAULT 0")


                // ============================================================
                // GROUP ENTITY
                // Remove: aesKeys
                // Add: final group crypto fields
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE group_entity_new (
                groupId TEXT NOT NULL,
                groupName TEXT NOT NULL,
                groupDescription TEXT,
                groupImageUrl TEXT,
                lastMessage TEXT,
                totalUnread INTEGER NOT NULL,
                createdBy TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                imageTime INTEGER NOT NULL,
                isMuted INTEGER NOT NULL,
                isPinned INTEGER,
                lastMessageTime INTEGER NOT NULL,
                wallpaperImage TEXT,
                sandeshStithi INTEGER NOT NULL,
                lastMessageId TEXT NOT NULL,
                isGroupSuspended INTEGER NOT NULL,
                backgroundColor INTEGER NOT NULL,
                draftMessage TEXT,
                isGroupActive INTEGER,
                revealSender INTEGER NOT NULL,
                autoDisappearMsg INTEGER NOT NULL,
                currentEpoch INTEGER NOT NULL,
                currentCommitHash BLOB,
                rosterVersion INTEGER NOT NULL,
                groupCryptoProtocolVersion INTEGER NOT NULL,
                PRIMARY KEY(groupId)
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO group_entity_new (
                groupId,
                groupName,
                groupDescription,
                groupImageUrl,
                lastMessage,
                totalUnread,
                createdBy,
                createdAt,
                imageTime,
                isMuted,
                isPinned,
                lastMessageTime,
                wallpaperImage,
                sandeshStithi,
                lastMessageId,
                isGroupSuspended,
                backgroundColor,
                draftMessage,
                isGroupActive,
                revealSender,
                autoDisappearMsg,
                currentEpoch,
                currentCommitHash,
                rosterVersion,
                groupCryptoProtocolVersion
            )
            SELECT
                groupId,
                groupName,
                groupDescription,
                groupImageUrl,
                lastMessage,
                totalUnread,
                createdBy,
                createdAt,
                imageTime,
                isMuted,
                isPinned,
                lastMessageTime,
                wallpaperImage,
                sandeshStithi,
                lastMessageId,
                isGroupSuspended,
                backgroundColor,
                draftMessage,
                isGroupActive,
                0,
                0,
                0,
                NULL,
                0,
                1
            FROM group_entity
        """.trimIndent()
                )

                db.execSQL("DROP TABLE group_entity")
                db.execSQL("ALTER TABLE group_entity_new RENAME TO group_entity")

                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS index_group_entity_groupName
            ON group_entity(groupName)
        """.trimIndent()
                )


                // ============================================================
                // GROUP MEMBERS
                // Add final crypto/member-instance fields + FK
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE group_members_new (
                groupId TEXT NOT NULL,
                userNumber TEXT NOT NULL,
                contactName TEXT NOT NULL,
                onlineName TEXT,
                userDarkColor INTEGER NOT NULL,
                userLightColor INTEGER NOT NULL,
                joinedAt INTEGER NOT NULL,
                isSwami INTEGER NOT NULL,
                canMessage INTEGER NOT NULL,
                canUpdateProfile INTEGER NOT NULL,
                canModifyMembers INTEGER NOT NULL,
                canStartCall INTEGER NOT NULL,
                profileTime INTEGER NOT NULL,
                profileUrl TEXT NOT NULL,
                publicKey TEXT NOT NULL,
                memberInstanceId TEXT NOT NULL,
                deviceId TEXT NOT NULL,
                leafIndex INTEGER NOT NULL,
                keyGeneration INTEGER NOT NULL,
                x25519PublicKey BLOB NOT NULL,
                mlKemPublicKey BLOB NOT NULL,
                ed25519PublicKey BLOB NOT NULL,
                isCryptoActive INTEGER NOT NULL,
                PRIMARY KEY(groupId, userNumber),
                FOREIGN KEY(groupId)
                    REFERENCES group_entity(groupId)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO group_members_new (
                groupId,
                userNumber,
                contactName,
                onlineName,
                userDarkColor,
                userLightColor,
                joinedAt,
                isSwami,
                canMessage,
                canUpdateProfile,
                canModifyMembers,
                canStartCall,
                profileTime,
                profileUrl,
                publicKey,
                memberInstanceId,
                deviceId,
                leafIndex,
                keyGeneration,
                x25519PublicKey,
                mlKemPublicKey,
                ed25519PublicKey,
                isCryptoActive
            )
            SELECT
                groupId,
                userNumber,
                contactName,
                onlineName,
                userDarkColor,
                userLightColor,
                joinedAt,
                isSwami,
                canMessage,
                canUpdateProfile,
                canModifyMembers,
                canStartCall,
                profileTime,
                profileUrl,
                publicKey,
                '',
                '',
                -1,
                0,
                X'',
                X'',
                X'',
                1
            FROM group_members
        """.trimIndent()
                )

                db.execSQL("DROP TABLE group_members")
                db.execSQL("ALTER TABLE group_members_new RENAME TO group_members")

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_group_members_groupId
            ON group_members(groupId)
        """.trimIndent()
                )


                // ============================================================
                // OLD DEVELOPMENT/UNUSED TABLES
                // ============================================================

                db.execSQL("DROP TABLE IF EXISTS message_session_map")
                db.execSQL("DROP TABLE IF EXISTS session_trigger_status")


                // ============================================================
                // GROUP EPOCH KEYS
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS group_epoch_keys (
                groupId TEXT NOT NULL,
                epoch INTEGER NOT NULL,
                encryptedEpochSecret BLOB NOT NULL,
                nonce BLOB NOT NULL,
                keyVersion INTEGER NOT NULL,
                commitHash BLOB NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(groupId, epoch),
                FOREIGN KEY(groupId)
                    REFERENCES group_entity(groupId)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_group_epoch_keys_groupId
            ON group_epoch_keys(groupId)
        """.trimIndent()
                )


                // ============================================================
                // GROUP PRIVATE KEYS
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS group_private_keys (
                groupId TEXT NOT NULL,
                memberInstanceId TEXT NOT NULL,
                encryptedX25519PrivateKey BLOB NOT NULL,
                x25519Nonce BLOB NOT NULL,
                x25519KeyVersion INTEGER NOT NULL,
                encryptedMlKemPrivateKey BLOB NOT NULL,
                mlKemNonce BLOB NOT NULL,
                mlKemKeyVersion INTEGER NOT NULL,
                encryptedEd25519PrivateKey BLOB NOT NULL,
                ed25519Nonce BLOB NOT NULL,
                ed25519KeyVersion INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(groupId, memberInstanceId),
                FOREIGN KEY(groupId)
                    REFERENCES group_entity(groupId)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_group_private_keys_groupId
            ON group_private_keys(groupId)
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_group_private_keys_memberInstanceId
            ON group_private_keys(memberInstanceId)
        """.trimIndent()
                )


                // ============================================================
                // PENDING GROUP KEY PACKAGES
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS pending_group_key_packages (
                groupId TEXT NOT NULL,
                userNumber TEXT NOT NULL,
                messageJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(groupId, userNumber)
            )
        """.trimIndent()
                )


                // ============================================================
                // STORIES
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS stories (
                id TEXT NOT NULL,
                ownerId TEXT NOT NULL,
                ownerName TEXT,
                ownerProfile TEXT,
                mediaUrl TEXT,
                thumbnailUrl TEXT,
                localPath TEXT,
                mediaType TEXT NOT NULL,
                visibility TEXT NOT NULL,
                allowedContactIds TEXT NOT NULL,
                durationMs INTEGER,
                createdAt INTEGER NOT NULL,
                expiresAt INTEGER NOT NULL,
                viewCount INTEGER NOT NULL DEFAULT 0,
                likeCount INTEGER NOT NULL DEFAULT 0,
                commentCount INTEGER NOT NULL DEFAULT 0,
                isLikedByMe INTEGER NOT NULL DEFAULT 0,
                isStoryDownloaded INTEGER NOT NULL DEFAULT 0,
                commentsPrivate INTEGER NOT NULL DEFAULT 0,
                storyPass TEXT,
                syncStatus TEXT NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent()
                )


                // ============================================================
                // STORY VIEWS
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS story_views (
                storyId TEXT NOT NULL,
                viewerId TEXT NOT NULL,
                viewedAt INTEGER NOT NULL,
                PRIMARY KEY(storyId, viewerId)
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_story_views_storyId
            ON story_views(storyId)
        """.trimIndent()
                )


                // ============================================================
                // STORY LIKES
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS story_likes (
                storyId TEXT NOT NULL,
                userId TEXT NOT NULL,
                likedAt INTEGER NOT NULL,
                PRIMARY KEY(storyId, userId)
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_story_likes_storyId
            ON story_likes(storyId)
        """.trimIndent()
                )


                // ============================================================
                // STORY COMMENTS
                // ============================================================

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS story_comments (
                id TEXT NOT NULL,
                storyId TEXT NOT NULL,
                userId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                pendingSync INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id)
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS index_story_comments_storyId
            ON story_comments(storyId)
        """.trimIndent()
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE upbhogta ADD COLUMN hideKey TEXT DEFAULT NULL")
            }
        }

    }
}