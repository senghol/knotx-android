package com.senghol.knotx.utils.enums;

public enum DataType {
    MESSAGE(0), //0
    DELIVERY_CONFIRMATION(1), //1
    MESSAGE_ON_HOLD(2), //2
    DELIVERY_DELIVERED(3), //3
    INVALID_LOGIN_TOKEN(4),
    SEEN_STATUS(5), //5
    AI_MESSAGE(6),
    SEEN_DELIVERED(7), //7
    AI_ACK(8), //8
    PROFILE_UPDATE(9), //9
    FETCH_AI_TOKENS(10),//10
    PROFILE_UPDATE_NOTIFY(11), //11
//    LOCATION(12),//12
    FORWARD_MESSAGE(13), //13
//    R_MESSAGE(14),
    HEART_BEAT(15),
//    R_IMAGE(16),
    AUTH_RESPONSE(17),
//    R_VIDEO(18),
    CALL_DATA(19),
//    R_DOCUMENTS(20),
    DATA_VERIFIED(21),
//    R_AUDIO(22),
    FILE_TOKEN(23),
//    R_CONTACT(24),
    FORWARD_IMAGE(25), //15
    GET_PROFILE_PIC(26), //16
    CONTACT_PROFILE_STATUS(27),//17
    BLOCK(28),//18
    FCM_TOKEN(29), //19
    GET_FILE_TOKEN(30),//20
    LOGIN(31),
    TYPING(32),
    JUST_DELIVERY(33),
    ASK_FOR_TOKEN(34),
    DELETE_DATA(35),
    ASK_REGISTER_KEY(36),
    ASK_SINGLE_PROFILE_STATUS(37),
    REGISTRATION_RESPONSE(38),
    SEND_OTP(39),
    SUBMIT_OTP(40),
    CHECK_USER_EXISTENCE(41),
    INVALID_DATA(42),
    MALFUNCTION_JSON(43),
    CHECK_USER_EXISTENCE_QR(44),
    PUBLIC_KEY(45),
    ASK_KEY(46),
    ASK_AGAIN(47),
    HOME_PROFILE_STATUS(48),
    CREATE_GROUP(49),
    ADD_MEMBER_GROUP(50),
    MEMBER_RESPONSE(51),
    REQUEST_MEMBER(52),
    PERMISSION_UPDATE(53),
    QUERY_MY_GROUPS(54),
    REMOVE_MEMBER_GROUP(55),
    SAY_HELLO(56),
    GROUP_HOME_PROFILE(57),
    GROUP_MEMBER_PROFILE(58),
    DELETE_GROUP_PROFILE(59),
    DELETE_PROFILE(60),
    GROUP_ACTION(61),
    INFORM_GROUP_MEMBER_UPDATE(62),
    SESSION_KEY_REGISTERED(63),
    GET_DELIVERY_TOKEN(64),
    UPDATE_SERVER_CONTACTS(65),
    GET_SERVER_CONTACTS(66),
    UPDATE_PRIVACY_SETTING(67),
    GET_PRIVACY_SETTINGS(68),
    GET_SERVER_BLOCKLIST(69),
    DELETE_ACCOUNT(70),
    MORE_KEYS_NEEDED(71),
    CREATE_ORDER_ID(72),
    GET_MY_CONTRIBUTIONS(73),
    IS_MESSAGE_PENDING(74),
    LIVE_LOCATION_ROUTE(75),
    LEDGER(76),
    FETCH_USER_SETTINGS(77),
    FILE_PROCESS(78),
    CHECK_USERNAME_ID(79),
    UPDATE_USERNAME_ID(80),
    STORY_MP3_LINK_GENERATE(81),
    STORY_ACTION(82),
    UPDATE_USER_SETTINGS(83),
    GET_USER_SETTINGS(84),
    UPDATE_USERNAME(85),
    UPDATE_USER_BIO(86),
    GET_QUICK_TIPS_DATA(87);

    private final int value;
    DataType(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    // Static method to get enum by int value
    public static DataType fromInt(int i) {
        for (DataType type : DataType.values()) {
            if (type.getValue() == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unexpected value: " + i);
    }
}

