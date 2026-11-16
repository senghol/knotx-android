package com.senghol.knotx.utils

import com.google.gson.JsonObject
import com.senghol.knotx.ui.components.ListMessage
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.SandeshType

object SandeshValidator {

    data class SandeshSchema(
        val required: Set<String>,
        val optional: Set<String> = emptySet()
    )

    private val expectedKeysByType: Map<SandeshType, SandeshSchema> = mapOf(
        SandeshType.MESSAGE to SandeshSchema(
            required = setOf(DootawasMessage.PAATH, JsonKeys.TIME_STAMP, DootawasMessage.SANDESH_TYPE, DootawasMessage.IS_LINK,
                DootawasMessage.DISAPPEAR_DURATION),
            optional = setOf(JsonKeys.GROUP_ID)
        ),

        SandeshType.DELETE to SandeshSchema(
            required = setOf(DootawasMessage.PAATH, DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.IMAGE to SandeshSchema(
            required = setOf( DootawasMessage.SANDESH_TYPE, DootawasMessage.IMAGE_PREVIEW, JsonKeys.TIME_STAMP,
                DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.FILE_ID, DootawasMessage.FILE_PASS,DootawasMessage.PAATH,
                DootawasMessage.ALL_DATA, DootawasMessage.DATA_COUNT,DootawasMessage.DISAPPEAR_DURATION),
                    optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.CREATE_GROUP to SandeshSchema(
            required = setOf(
                JsonKeys.GROUP_ID,
                DootawasMessage.SANDESH_TYPE,
                JsonKeys.GROUP_NAME,
                JsonKeys.GROUP_DEC,
                JsonKeys.GROUP_ADDED_BY,
                JsonKeys.GROUP_CREATOR,
                JsonKeys.TIME_STAMP,
                JsonKeys.GROUP_CREATED_AT,
                DootawasMessage.GROUP_CURRENT_EPOCH,
                DootawasMessage.GROUP_ROSTER_VERSION,
                DootawasMessage.GROUP_CURRENT_COMMIT_HASH,
                DootawasMessage.GROUP_BOOTSTRAP_EPOCH_SECRET
            )
        ),

        SandeshType.GROUP_KEY_SETUP to SandeshSchema(
            required = setOf(
                JsonKeys.GROUP_ID,
                DootawasMessage.SANDESH_TYPE,
                JsonKeys.TIME_STAMP, JsonKeys.USER_NUMBER, DootawasMessage.SETUP_ID
            )
        ),

        SandeshType.GROUP_KEY_REQUEST to SandeshSchema(
            required = setOf(
                JsonKeys.GROUP_ID,
                DootawasMessage.SANDESH_TYPE,
                JsonKeys.USER_NUMBER,
                DootawasMessage.GROUP_MEMBER_INSTANCE_ID,
                DootawasMessage.GROUP_DEVICE_ID,
                DootawasMessage.GROUP_KEY_GENERATION,
                DootawasMessage.GROUP_X25519_PUBLIC_KEY,
                DootawasMessage.GROUP_ML_KEM_PUBLIC_KEY,
                DootawasMessage.GROUP_ED25519_PUBLIC_KEY,
                JsonKeys.TIME_STAMP, DootawasMessage.SETUP_ID
            )
        ),

        SandeshType.GROUP_KEY_RESPONSE to SandeshSchema(
            required = setOf(
                JsonKeys.GROUP_ID,
                DootawasMessage.SANDESH_TYPE,
                JsonKeys.USER_NUMBER,
                DootawasMessage.GROUP_MEMBER_INSTANCE_ID,
                DootawasMessage.GROUP_DEVICE_ID,
                DootawasMessage.GROUP_KEY_GENERATION,
                DootawasMessage.GROUP_X25519_PUBLIC_KEY,
                DootawasMessage.GROUP_ML_KEM_PUBLIC_KEY,
                DootawasMessage.GROUP_ED25519_PUBLIC_KEY,
                JsonKeys.TIME_STAMP
            )
        ),

        SandeshType.EPOCH_COMMIT to SandeshSchema(
            required = setOf(
                JsonKeys.GROUP_ID,
                DootawasMessage.SANDESH_TYPE,
                DootawasMessage.GROUP_CRYPTO_PROTOCOL_VERSION,
                DootawasMessage.GROUP_PARENT_EPOCH,
                DootawasMessage.GROUP_NEW_EPOCH,
                DootawasMessage.GROUP_ROSTER_VERSION,
                DootawasMessage.GROUP_COMMIT_ID,
                DootawasMessage.GROUP_PARENT_COMMIT_HASH,
                DootawasMessage.GROUP_COMMITTER_MEMBER_ID,
                DootawasMessage.GROUP_COMMITTER_DEVICE_ID,
                DootawasMessage.GROUP_PATH_HASHES,
                DootawasMessage.GROUP_ENCRYPTED_PATH,
                DootawasMessage.GROUP_COMMIT_SIGNATURE,
                JsonKeys.TIME_STAMP
            )
        ),

        SandeshType.DOCUMENT to SandeshSchema(
            required = setOf(DootawasMessage.MIME_TYPE, DootawasMessage.SANDESH_TYPE,
                DootawasMessage.DUPLICATE_URI,DootawasMessage.DISAPPEAR_DURATION,
                JsonKeys.TIME_STAMP, DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.ORIGINAL_URI,
                DootawasMessage.DOWNLOAD_SIZE,DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID, DootawasMessage.FILE_NAME),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.VIDEO to SandeshSchema(
            required = setOf( DootawasMessage.DATA_COUNT, DootawasMessage.ALL_DATA,
                DootawasMessage.SANDESH_TYPE,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.IMAGE_PREVIEW, JsonKeys.TIME_STAMP,
                DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID, DootawasMessage.PAATH),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.AUDIO to SandeshSchema(
            required = setOf(DootawasMessage.MIME_TYPE,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.SANDESH_TYPE, DootawasMessage.ORIGINAL_URI,
                DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.DURATION, JsonKeys.TIME_STAMP,
                DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID),
                    optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.CONTACT to SandeshSchema(
            required = setOf("count", "contacts", DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,DootawasMessage.DISAPPEAR_DURATION),
            optional = setOf(JsonKeys.GROUP_ID)
        ),

        SandeshType.UNIVERSAL_MESSAGE to SandeshSchema(
            required = setOf(DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,DootawasMessage.UNIVERSAL_TYPE,DootawasMessage.PAATH),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.LOCATION to SandeshSchema(
            required = setOf(DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.IMAGE_PREVIEW,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.SANDESH_TYPE,DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID,
                JsonKeys.TIME_STAMP, DootawasMessage.LATITUDE, DootawasMessage.LONGITUDE,
                DootawasMessage.MIME_TYPE, DootawasMessage.IS_LIVE, DootawasMessage.LIVE_UNTIL),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_MESSAGE to SandeshSchema(
            required = setOf(DootawasMessage.PAATH, DootawasMessage.ORIGINAL_MESSAGE,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,DootawasMessage.IS_LINK),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_IMAGE to SandeshSchema(
            required = setOf(DootawasMessage.MIME_TYPE, DootawasMessage.ORIGINAL_MESSAGE,
                DootawasMessage.SANDESH_TYPE, DootawasMessage.DUPLICATE_URI,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.IMAGE_PREVIEW,DootawasMessage.PAATH,
                DootawasMessage.FILE_ID, DootawasMessage.FILE_PASS, JsonKeys.TIME_STAMP),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_VIDEO to SandeshSchema(
            required = setOf(DootawasMessage.ORIGINAL_MESSAGE, DootawasMessage.SANDESH_TYPE,
                DootawasMessage.DOWNLOAD_SIZE,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.IMAGE_PREVIEW,DootawasMessage.FILE_ID, DootawasMessage.FILE_PASS,
                JsonKeys.TIME_STAMP,DootawasMessage.DATA_COUNT, DootawasMessage.ALL_DATA,
                DootawasMessage.PAATH),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_AUDIO to SandeshSchema(
            required = setOf("file_name", "download_url", "duration", "reply_to", "sandesh_type",DootawasMessage.DISAPPEAR_DURATION),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_DOCUMENT to SandeshSchema(
            required = setOf(DootawasMessage.MIME_TYPE, DootawasMessage.SANDESH_TYPE, DootawasMessage.DUPLICATE_URI,
                JsonKeys.TIME_STAMP, DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.ORIGINAL_URI,DootawasMessage.DISAPPEAR_DURATION,
                DootawasMessage.ORIGINAL_MESSAGE,
                DootawasMessage.DOWNLOAD_SIZE,DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID, DootawasMessage.FILE_NAME),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_CONTACT to SandeshSchema(
            required = setOf("count", "contacts", DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,DootawasMessage.ORIGINAL_MESSAGE,DootawasMessage.DISAPPEAR_DURATION),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REACTION to SandeshSchema(
            required = setOf(DootawasMessage.REACTION, DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,DootawasMessage.ORIGINAL_MESSAGE_ID),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REPLY_LOCATION to SandeshSchema(
            required = setOf( DootawasMessage.DOWNLOAD_SIZE, DootawasMessage.IMAGE_PREVIEW,
                DootawasMessage.SANDESH_TYPE,DootawasMessage.FILE_PASS, DootawasMessage.FILE_ID,
                DootawasMessage.ORIGINAL_MESSAGE,DootawasMessage.DISAPPEAR_DURATION,
                JsonKeys.TIME_STAMP, DootawasMessage.LATITUDE, DootawasMessage.LONGITUDE,
                DootawasMessage.MIME_TYPE,"isLive"),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.REQUEST_DISAPPEARING to SandeshSchema(
            required = setOf(DootawasMessage.DURATION,JsonKeys.TIME_STAMP, DootawasMessage.SANDESH_TYPE),
        ),

        SandeshType.RESPONSE_DISAPPEARING to SandeshSchema(
            required = setOf(JsonKeys.DATA_BOOLEAN,JsonKeys.TIME_STAMP, DootawasMessage.ORIGINAL_MESSAGE_ID,
                DootawasMessage.SANDESH_TYPE, DootawasMessage.DURATION),
        ),
        SandeshType.GROUP_UPDATED to SandeshSchema(
            required = setOf(JsonKeys.GROUP_ID, DootawasMessage.SANDESH_TYPE,JsonKeys.USER_NUMBER),
        ),
        SandeshType.MEMBERS_UPDATED to SandeshSchema(
            required = setOf(JsonKeys.GROUP_ID,JsonKeys.USER_NUMBER,  DootawasMessage.SANDESH_TYPE),
        ),

        SandeshType.LIVE_LOCATION_UPDATE to SandeshSchema(
            required = setOf(DootawasMessage.ORIGINAL_MESSAGE_ID, DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP,
                DootawasMessage.LATITUDE, DootawasMessage.LONGITUDE, DootawasMessage.IS_STOPPED),
            optional = setOf(JsonKeys.GROUP_ID,DootawasMessage.DISAPPEAR_DURATION)
        ),

        SandeshType.SCREEN_COLORS to SandeshSchema(
            required = setOf( DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP, DootawasMessage.ALL_DATA),
            optional = setOf(JsonKeys.GROUP_ID)
        ),
        SandeshType.LEDGER to SandeshSchema(
            required = setOf( DootawasMessage.SANDESH_TYPE, JsonKeys.TIME_STAMP),
            optional = setOf(JsonKeys.GROUP_ID,DootawasMessage.LEDGER_TYPE, DootawasMessage.LEDGER_STATUS, DootawasMessage.LEDGER_AMOUNT,
                DootawasMessage.LEDGER_NEW_BALANCE, DootawasMessage.LEDGER_CONTACT_ID,
                DootawasMessage.LEDGER_CONTACT_NAME, DootawasMessage.LEDGER_PREVIOUS_BALANCE,
                DootawasMessage.LEDGER_NOTE, DootawasMessage.LEDGER_DATA, DootawasMessage.LEDGER_CREATED_AT,
                DootawasMessage.ORIGINAL_MESSAGE_ID)
        ),
        SandeshType.LIST to SandeshSchema(
            required = setOf(
                DootawasMessage.SANDESH_TYPE,DootawasMessage.DISAPPEAR_DURATION,
                JsonKeys.TIME_STAMP,
                ListMessage::messageType.name, // Resolves safely to "messageType"
                ListMessage::current.name      // Resolves safely to "current"
            ),
            optional = setOf(
                JsonKeys.GROUP_ID,
                ListMessage::baseVersion.name, // Resolves safely to "baseVersion"
                ListMessage::updated.name,     // Resolves safely to "updated"
                ListMessage::listId.name,       // Resolves safely to "listId"
                ListMessage::currentVersion.name       // Resolves safely to "listId"
            )
        )
    )

    /**
     * Returns true if the JsonObject has all required keys and no disallowed extras.
     */
    fun isValid(type: SandeshType, json: JsonObject): Boolean {
        val schema = expectedKeysByType[type] ?: return false
        val actualKeys = json.keySet()

        val hasAllRequired = actualKeys.containsAll(schema.required)
        val allKeysAllowed = actualKeys.all { it in schema.required || it in schema.optional }

        return hasAllRequired && allKeysAllowed
    }

    /**
     * Provides a debug message showing which keys are missing or unexpected.
     */
    fun debugInvalidKeys(type: SandeshType, json: JsonObject): String {
        val schema = expectedKeysByType[type] ?: return "No schema for type $type"
        val actual = json.keySet()

        val missing = schema.required - actual
        val extra = actual - schema.required - schema.optional

        return buildString {
            if (missing.isNotEmpty()) append("Message Should Have : $missing. ")
            if (extra.isNotEmpty()) append("Message Should Not Have: $extra.")
            if (isEmpty()) append("No schema mismatch.")
        }
    }
}
