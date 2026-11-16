package com.senghol.knotx.utils.enums;

public enum StoryAction {

    DELETE_STORY(1),
    SET_COMMENT_PRIVATE(2),

    ADD_COMMENT(3),
    DELETE_COMMENT(4),

    LIKE_STORY(5),
    UNLIKE_STORY(6),

    VIEW_STORY(7),

    GET_STORY_DETAILS(8),
    SET_VISIBILITY(9);

    private final int value;

    StoryAction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static StoryAction fromValue(int value) {
        for (StoryAction action : values()) {
            if (action.value == value) {
                return action;
            }
        }

        throw new IllegalArgumentException(
                "Unknown story action: " + value
        );
    }
}