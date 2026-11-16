package com.senghol.knotx.utils;

import androidx.annotation.NonNull;

import java.util.Objects;

public class MessageManager {
    // Class to represent the composite key of username and messageId
    public static class UserMessageKey {
        String username;
        String messageId;

        public UserMessageKey(String username, String messageId) {
            this.username = username;
            this.messageId = messageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserMessageKey that = (UserMessageKey) o;
            return Objects.equals(username, that.username) && Objects.equals(messageId, that.messageId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, messageId);
        }
        @NonNull
        @Override
        public String toString() {
            return "UserMessageKey{" +
                    "username='" + username + '\'' +
                    ", messageId='" + messageId + '\'' +
                    '}';
        }

        public String getUsername() {
            return username;
        }

        public String getMessageId() {
            return messageId;
        }
    }
}
