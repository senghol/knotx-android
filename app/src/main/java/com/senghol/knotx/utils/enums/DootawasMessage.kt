package com.senghol.knotx.utils.enums

object DootawasMessage {
    const val ORIGINAL_URI: String = "originalUri"
    const val DUPLICATE_URI: String = "duplicateUri"
    const val CIPHER_TYPE_PREKEY: String = "cipherTypePrekey"
    const val CIPHER_TYPE_WHISPER: String = "cipherTypeWhisper"
    const val CIPHER_TYPE_UNKNOWN: String = "cipherTypeUnknown"
    const val CIPHER_MESSAGE: String = "cipherMessage"
    const val CIPHER_MESSAGE_TYPE: String = "cipherMessageType"

    const val BLUR_URI: String = "blurUri"
    const val CONTACT_NAME: String = "contactName"
    const val CONTACTS: String = "contacts"
    const val PAATH: String = "paath"
    const val IS_LINK: String = "link"
    const val LINK_TITLE: String = "linkTitle"
    const val LINK_URI: String = "linkUri"

    const val THUMBNAIL_URI: String = "thumbnailUri"

    const val SANDESH_TYPE: String = "sandeshType"
    const val REACTION: String = "reaction"
    const val SANDESH_TIME: String = "sandeshTime"

    const val IMAGE_PREVIEW: String = "imagePreview"
    const val MIME_TYPE: String = "mimeType"
    const val FILE_NAME: String = "fileName"

    const val DOWNLOAD_PERCENT: String = "downloadPercent"
    const val DOWNLOAD_SIZE: String = "downloadSize"
    const val DATA_COUNT: String = "dataCount"
    const val ALL_DATA: String = "allData"
    const val DURATION: String = "duration"

    const val ASK_AGAIN: String = "askAgain"
    const val REPLY_MESSAGE: String = "replyMessage"
    const val ORIGINAL_MESSAGE: String = "originalMessage"
    const val IS_LIVE: String = "isLive"
    const val LIVE_UNTIL: String = "liveUntil"
    const val IS_STOPPED: String = "isStopped"
    const val ORIGINAL_SAMAGRI: String = "originalSamagri"
    const val ORIGINAL_MESSAGE_ID: String = "originalMessageId"
    const val FILE_ID: String = "fileId"
    const val FILE_PASS: String = "filePass"
    const val ORIGINAL_MESSAGE_TYPE: String = "originalMessageType"
    const val COORDINATION: String = "coordination"
    const val LONGITUDE: String = "longitude"
    const val LATITUDE: String = "latitude"
    const val NUMBER: String = "number"

    const val PROFILE_ID: String = "profileId"
    const val DATA_LIST: String = "listData"
    const val SETUP_ID: String = "setupId"
    const val IS_KEY_REQUEST: String = "isKeyRequest"
    const val GROUP_MEMBERS: String = "groupMembers"



    // Ledger flat keys
    const val LEDGER_DATA        = "ledgerData"
    const val LEDGER_TYPE        = "ledgerType"
    const val LEDGER_STATUS      = "ledgerStatus"
    const val LEDGER_AMOUNT      = "ledgerAmount"
    const val LEDGER_NEW_BALANCE = "ledgerNewBalance"
    const val LEDGER_RESOLVED_BALANCE = "resolvedBalance"

    // Ledger common fields
    const val LEDGER_CONTACT_ID       = "contactId"
    const val LEDGER_CONTACT_NAME     = "contactName"
    const val LEDGER_PREVIOUS_BALANCE = "previousBalance"
    const val LEDGER_NOTE             = "note"
    const val LEDGER_CREATED_AT       = "createdAt"

    // Bill data fields
    const val BILL_ITEMS        = "items"
    const val BILL_ITEM_NAME    = "name"
    const val BILL_ITEM_PRICE   = "price"
    const val BILL_ITEM_QTY     = "quantity"
    const val BILL_ITEM_TOTAL   = "total"
    const val BILL_TAX_PERCENT  = "taxPercent"
    const val BILL_TAX_AMOUNT   = "taxAmount"
    const val BILL_SUBTOTAL     = "subtotal"
    const val BILL_AMOUNT_PAID  = "amountPaid"
    const val BILL_TOTAL        = "billTotal"

    // Settlement data fields
    const val SETTLEMENT_TYPE   = "settlementType"

    const val BILL_SELLER_NAME = "sellerName"
    const val BILL_SELLER_GST  = "sellerGst"
    const val BILL_BUYER_NAME  = "buyerName"
    const val BILL_BUYER_GST   = "buyerGst"
    //
   const val BILL_INVOICE_NUMBER = "invoiceNumber"
   const val BILL_DATE           = "billDate"          // epoch millis, mandatory
   const val BILL_SELLER_PHONE   = "sellerPhone"
   const val BILL_SELLER_EMAIL   = "sellerEmail"
   const val BILL_SELLER_ADDRESS = "sellerAddress"
   const val BILL_BUYER_PHONE    = "buyerPhone"
   const val BILL_BUYER_EMAIL    = "buyerEmail"
   const val BILL_BUYER_ADDRESS  = "buyerAddress"
   const val BILL_ITEM_HSN       = "hsnCode"
   const val DISAPPEAR_DURATION = "diD"


    // ===== Group E2EE =====
    const val GROUP_CRYPTO_PROTOCOL_VERSION = "groupCryptoProtocolVersion"
    const val GROUP_EPOCH = "groupEpoch"
    const val GROUP_SENDER_ID = "groupSenderId"
    const val GROUP_SENDER_DEVICE_ID = "groupSenderDeviceId"
    const val GROUP_MESSAGE_NUMBER = "groupMessageNumber"
    const val GROUP_NONCE = "groupNonce"

    // ===== Group Member Key Package =====
    const val GROUP_MEMBER_INSTANCE_ID = "groupMemberInstanceId"
    const val GROUP_DEVICE_ID = "groupDeviceId"
    const val GROUP_KEY_GENERATION = "groupKeyGeneration"
    const val GROUP_X25519_PUBLIC_KEY = "groupX25519PublicKey"
    const val GROUP_ML_KEM_PUBLIC_KEY = "groupMlKemPublicKey"
    const val GROUP_ED25519_PUBLIC_KEY = "groupEd25519PublicKey"

    // ===== Group E2EE : Epoch Commit =====
    const val GROUP_PARENT_EPOCH = "groupParentEpoch"
    const val GROUP_NEW_EPOCH = "groupNewEpoch"
    const val GROUP_ROSTER_VERSION = "groupRosterVersion"
    const val GROUP_COMMIT_ID = "groupCommitId"
    const val GROUP_PARENT_COMMIT_HASH = "groupParentCommitHash"
    const val GROUP_COMMITTER_MEMBER_ID = "groupCommitterMemberId"
    const val GROUP_COMMITTER_DEVICE_ID = "groupCommitterDeviceId"
    const val GROUP_COMMIT_SIGNATURE = "groupCommitSignature"
    // ===== Group E2EE : Current Group State =====
    const val GROUP_CURRENT_EPOCH = "groupCurrentEpoch"
    const val GROUP_CURRENT_COMMIT_HASH = "groupCurrentCommitHash"
    const val GROUP_BOOTSTRAP_EPOCH_SECRET = "groupBootstrapEpochSecret"

    // ===== Group E2EE : Recipient Encrypted Path =====
    const val GROUP_ENCRYPTED_PATH = "groupEncryptedPath"
    const val GROUP_RECIPIENT_LEAF_ID = "groupRecipientLeafId"
    const val GROUP_EPHEMERAL_X25519_PUBLIC = "groupEphemeralX25519Public"
    const val GROUP_ML_KEM_CIPHERTEXT = "groupMlKemCiphertext"
    const val GROUP_ENCRYPTED_PATH_SEED = "groupEncryptedPathSeed"
    const val GROUP_PATH_NONCE = "groupPathNonce"

    const val GROUP_PATH_HASHES = "groupPathHashes"
    const val GROUP_PATH_HASH = "groupPathHash"


    const val STORY_ID: String = "storyId"
    const val STORY_PASS: String = "storyPass"
    const val MEDIA_TYPE: String = "mediaType"
    const val VISIBILITY: String = "visibility"
    const val UNIVERSAL_TYPE: String = " universalType"
}
