package com.senghol.knotx.utils.enums;

import org.jetbrains.annotations.NotNull;

public class JsonKeys {
    public static final String SENDER = "sender";
    public static final String RECEIVER = "receiver";
    public static final String RECEIVERS = "receivers";
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE_IDS = "messageIds";
    public static final String DATA_TYPE = "dataType";
    public static final String PROFILE_ID = "profileId";
    public static final String TIME_STAMP = "timeStamp";
    public static final String LAST_ONLINE = "lastOnline";
    public static final String USER_NAME = "userName";
    public static final String USER_NUMBER = "userNumber";
    public static final String ORDER_TYPE = "orderType";   // 0 = other (default), 1 = AI credits
    public static final String IS_LOGIN = "isLogin";
    public static final String PASSWORD = "password";
    public static final String DATA_BOOLEAN = "dataBoolean";
    public static final String REASON = "reason";
    public static final String TOKEN = "token";
    public static final String SAMABHASH_TOKEN = "samabhashToken";
    public static final String DELIVERY_TOKEN = "deliveryToken";
    public static final String SANDESH = "sandesh";
    public static final String ACK_ID = "ackId";
    public static final String OTP_SUBMIT = "otpSubmit";
    public static final String CALL_DATA_TYPE = "callDataType";
    public static final String LIST = "list";
    public static final String SENT_DATA_TYPE = "sentDataType";
    public static final String ENCRYPTED_AES_KEY = "encryptedAesKey";
    public static final String ENCRYPTION_IV = "encryptionIV";
    public static final String ENCRYPTED_MESSAGE = "ciphertext";
    public static final String VERSION = "version";
    public static final String DATA = "data";
    public static final String STORAGE_TYPE = "storageType";
    public static final String ERROR_OPERATION = "errorOperation";
    public static final String GROUPS = "groups";
    public static final String GROUP_ID = "groupId";

    public static final String GROUP_NAME = "groupName";
    public static final String GROUP_DEC = "groupDec";
    public static final String GROUP_IS_ACTIVE = "groupIsActive";
    public static final String GROUP_JOINED_AT = "groupJoinedAt";
    public static final String GROUP_IS_MUTED = "groupISMuted";
    public static final String GROUP_PRIVATE_KEY = "groupPrivateKey";
    public static final String GROUP_PUBLIC_KEY = "groupPublicKey";
    public static final String GROUP_CREATOR = "groupCreator";
    public static final String GROUP_CREATED_AT = "groupCreatedAt";
    public static final String GROUP_ADDED_BY = "groupAddedBy";
    public static final String GROUP_PROFILE_TIME = "groupProfileTime";
    public static final String GROUP_PROFILE_PASS = "groupProfilePass";
    public static final String IS_GROUP = "isGroup";
    public static final String REVEAL_SENDER_NUMBER = "rSN";
    public static final String REVEAL_RECEIVER_NUMBER = "rRN";
    public static final String IS_SWAMI = "isSwami";
    public static final String CAN_MESSAGE = "canMessage";
    public static final String CAN_DELETE = "canDelete";
    public static final String CAN_ADD_MEMBERS = "canAddMembers";
    public static final String CAN_START_CALL = "canStartCall";
    public static final String GROUP_MEMBERS = "groupMembers";
    public static final String SIGNAL_DATA = "signalData";
    public static final String GROUP_LAST_ACTIVITY_AT = "groupLastActivity";
    public static final String CAN_UPDATE_PROFILE = "canUpdateProfile";
    public static final String CAN_MODIFY_MEMBERS = "canModifyMembers";
    public static final String MEMBERS = "members";
    public static final String AMOUNT = "amount";
    public static final String CURRENCY = "currency";
    public static final String ORDER_ID = "order_id";
    public static final String RECEIPT = "receipt";

    public static final String ORIGIN_LATITUDE = "origin_latitude";
    public static final String ORIGIN_LONGITUDE = "origin_longitude";
    public static final String DESTINATION_LATITUDE = "destination_latitude";
    public static final String DESTINATION_LONGITUDE = "destination_longitude";
    public static final String FILE_TOKEN_TYPE = "fileTokenType";
    public static final String UPLOAD_URL = "uploadUrl";
    public static final String DOWNLOAD_URL = "downloadUrl";

    // ── Prekey bundle fields ──
    public static final String IDENTITY_BUNDLE = "identityBundle";
    public static final String REGISTRATION_ID = "registrationId";
    public static final String IDENTITY_KEY = "identityKey";
    public static final String SIGNED_PREKEY_ID = "signedPreKeyId";
    public static final String SIGNED_PREKEY_PUBLIC = "signedPreKeyPublic";
    public static final String SIGNED_PREKEY_SIGNATURE = "signedPreKeySignature";
    public static final String EC_PREKEY = "ecPreKey";
    public static final String PREKEY_ID = "preKeyId";
    public static final String PREKEY_PUBLIC = "preKeyPublic";
    public static final String KYBER_PREKEY = "kyberPreKey";
    public static final String KYBER_PREKEY_ID = "kyberPreKeyId";
    public static final String KYBER_PREKEY_PUBLIC = "kyberPreKeyPublic";
    public static final String KYBER_PREKEY_SIGNATURE = "kyberPreKeySignature";
    public static final String NEED_MORE_KEYS = "needMoreKeys";

    public static final String USER_NAME_ID = "userNameId";
    public static final String USERNAME_UPDATE_TIME = "usernameUpdateTime";
    public static final String PROFILE_TIME = "profileTime";
    public static final String PROFILE_PHOTO = "profilePhoto";
    public static final String ABOUT = "about";
    public static final String PRIVACY_TYPE = "privacyType";

    // ==========================================
    // PIXABAY API RESPONSE JSON KEYS
    // ==========================================
    public static final String HITS = "hits";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String USER = "user";
    public static final String AUDIO = "audio";
    public static final String DOWNLOAD = "download";
    public static final String DURATION = "duration";
    public static final String CATEGORY = "category";
    public static final String TRACKS = "tracks";
    public static final String ARTIST = "artist";
    public static final String STREAM_URL = "streamUrl";
    public static final String DURATION_MS = "durationMs";
    public static final String STORIES = "stories";
    public static final String COMMENT = "comment";
    public static final String COMMENT_ID = "commentId";
    public static final String STORY_ACTION = "story_action";
    public static final String COMMENT_IS_PRIVATE = "commentIsPrivate";

    public static final String COMMENTS = "comments";
    public static final String LIKES = "likes";
    public static final String VIEWS = "views";

    public static final String LIKE_COUNT = "likeCount";
    public static final String COMMENT_COUNT = "commentCount";
    public static final String VIEW_COUNT = "viewCount";

    public static final String COMMENT_PRIVATE = "commentPrivate";
    public static final String USER_SETTING = "userSetting";

    // 🔥 new — for CALL_DATA / CALL_INVITE payloads
    public static final String ACTION = "action";
    public static final String CALL_ID = "callId";
    public static final String TARGET = "target";
    public static final String PAYLOAD = "payload"; // add if the nested {callType:...} object is required too
    public static final @NotNull String REPLY_TO = "replyTo";
}
