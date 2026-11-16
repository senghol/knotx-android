package com.senghol.knotx.notification // TODO: your package

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.senghol.knotx.MainActivity
import com.senghol.knotx.R
import com.senghol.knotx.data.database.GroupDao
import com.senghol.knotx.data.database.UpbhogtaDao
import com.senghol.knotx.notification.ReplyReceiver.Companion.KEY_TEXT_REPLY_REC
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit

/**
 * Notifications for 1:1 chats and group chats.
 *
 * Two notification slots per chat (key = sender number, or groupId for groups):
 *
 *   TEMP slot  (tempId)          placeholder / fallback. Short-lived, safe to remove.
 *   REAL slot  (conversationId)  the real conversation with message history.
 *
 * Temporary states never touch the REAL slot, so a failed message can never
 * dismiss or overwrite messages that were already shown successfully.
 *
 * Flows (same for 1:1 and groups):
 *   1. placeholder -> message    showPlaceholder()  then  showMessage()   (silent swap)
 *   2. placeholder -> failure    showPlaceholder()  then  showFallback()  (auto-dismisses)
 *   3. direct message            showMessage() only                       (alerts normally)
 *
 * Group equivalents: showGroupPlaceholder / showGroupMessage / showGroupFallback.
 */
class MessageNotifier(
    context: Context,
    private val upbhogtaDao: UpbhogtaDao,
    private val groupDao: GroupDao,
    /**
     * Optional: called with the new TOTAL unread count whenever it changes (0 = reset).
     * Use it to drive a vendor launcher badge, e.g. ShortcutBadger.applyCount(context, total).
     */
    private val onBadgeChanged: ((Int) -> Unit)? = null,
    /**
     * Direct messages (no placeholder) first show a lean heads-up without buttons, then the
     * Reply / Mark as read buttons are added silently after this many ms.
     * Negative = buttons immediately. Long.MAX_VALUE = effectively never.
     */
    private val actionsDelayMs: Long = DEFAULT_ACTIONS_DELAY_MS
) {

    private val context: Context = context.applicationContext
    private val manager = NotificationManagerCompat.from(this.context)

    /** Chat history per key (rebuilt from the active notification after process death). */
    private val conversations = ConcurrentHashMap<String, NotificationCompat.MessagingStyle>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val upgradeJobs = ConcurrentHashMap<String, Job>()
    private val seenIds: MutableSet<String> = Collections.synchronizedSet(
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 200
        })
    )
    private val badgeLock = Any()
    private val badgePrefs by lazy {
        this.context.getSharedPreferences(BADGE_PREFS, Context.MODE_PRIVATE)
    }

    /** Describes which chat a notification belongs to. */
    private data class Chat(
        val key: String,               // sender number or groupId
        val isGroup: Boolean = false,
        val groupName: String? = null
    )

    // ================================================================ 1:1 chats

    fun showPlaceholder(sender: String) =
        postPlaceholder(Chat(sender), title = sender)

    suspend fun showMessage(
        sender: String,
        message: String,
        messageId: String,
        timestamp: Long = System.currentTimeMillis(),
        wakeScreen: Boolean = false,
        countInBadge: Boolean = true
    ) {
        if (!manager.areNotificationsEnabled()) return
        if (messageId in seenIds) return          // duplicate or retry

        val chat = Chat(sender)
        val (displayName, profileBitmap) = withContext(Dispatchers.IO) {
            val name = upbhogtaDao.getUserNameByNumber(sender)
                ?.takeIf { it.isNotBlank() } ?: sender
            val bitmap = loadProfileBitmapFromUrl(upbhogtaDao.getImageUrlByNumber(sender))
                ?.let { makeBitmapRound(it) }
            name to bitmap
        }

        val icon = profileBitmap?.let { IconCompat.createWithBitmap(it) }
            ?: IconCompat.createWithResource(context, R.drawable.ic_person_thematic)

        val senderPerson = Person.Builder()
            .setName(displayName)
            .setKey(sender)
            .setIcon(icon)
            .setImportant(true)
            .build()

        pushConversationShortcut(sender, displayName, senderPerson, icon)

        val style = styleFor(sender) { NotificationCompat.MessagingStyle(youPerson()) }
        addToStyle(
            style,
            NotificationCompat.MessagingStyle.Message(message, timestamp, senderPerson)
        )


        seenIds.add(messageId)                    // 👈 right before postConversation

        postConversation(
            chat = chat,
            title = displayName,
            text = message,
            style = style,
            timestamp = timestamp,
            messageId = messageId,
            largeIcon = profileBitmap,
            shortcutId = sender,
            number = if (countInBadge) incrementUnread(sender) else null,
            silent = false,                 // placeholder already made noise
            wake = wakeScreen     // direct message: alert + wake
        )
    }

    fun showFallback(sender: String, text: String = FALLBACK_TEXT) =
        postFallback(Chat(sender), title = sender, text = text)

    /** Own reply typed in the notification: appends "You: ..." and refreshes the notification. */
    fun addReply(sender: String, replyText: String, messageId: String) =
        appendOwnReply(Chat(sender), replyText, messageId)

    // ================================================================== Groups

    fun showGroupPlaceholder(groupId: String, groupName: String) =
        postPlaceholder(Chat(groupId, isGroup = true, groupName = groupName), title = groupName)

    suspend fun showGroupMessage(
        groupId: String,
        groupName: String,
        sender: String,
        message: String,
        messageId: String,
        timestamp: Long = System.currentTimeMillis(),
        wakeScreen: Boolean = false,
        countInBadge: Boolean = true
    ) {
        if (!manager.areNotificationsEnabled()) return
        if (messageId in seenIds) return          // 👈 check at the top

        val chat = Chat(groupId, isGroup = true, groupName = groupName)
        val placeholderShown = isShowing(tempId(groupId))

        val (senderName, senderBitmap) = withContext(Dispatchers.IO) {
            val name = upbhogtaDao.getUserNameByNumber(sender)
                ?.takeIf { it.isNotBlank() } ?: sender
            val bitmap = loadProfileBitmapFromUrl(groupDao.getImageUrlByNumber(sender))
                ?.let { makeBitmapRound(it) }
            name to bitmap
        }

        val senderPerson = Person.Builder()
            .setName(senderName)
            .setKey(sender)
            .setIcon(
                senderBitmap?.let { IconCompat.createWithBitmap(it) }
                    ?: IconCompat.createWithResource(context, R.drawable.ic_person_thematic)
            )
            .build()

        val style = styleFor(groupId) {
            NotificationCompat.MessagingStyle(youPerson()).setGroupConversation(true)
        }
        style.setConversationTitle(groupName) // keeps title fresh if the group was renamed
        addToStyle(
            style,
            NotificationCompat.MessagingStyle.Message(message, timestamp, senderPerson)
        )

        seenIds.add(messageId)
        postConversation(
            chat = chat,
            title = groupName,
            text = "$senderName: $message",
            style = style,
            timestamp = timestamp,
            messageId = messageId,
            largeIcon = null,                          // group notifications: no large icon
            shortcutId = null,
            number = if (countInBadge) incrementUnread(groupId) else null,
            silent = false,
            wake =  wakeScreen
        )
    }

    fun showGroupFallback(groupId: String, groupName: String, text: String = FALLBACK_TEXT) =
        postFallback(Chat(groupId, isGroup = true, groupName = groupName), title = groupName, text = text)

    /** Own reply typed in a group notification (your old addGroupReplyToNotification). */
    fun addGroupReply(groupId: String, replyText: String, messageId: String) =
        appendOwnReply(Chat(groupId, isGroup = true), replyText, messageId)

    // ============================================================ Badge counting

    /** Total unread messages + missed calls across all chats. */
    fun getTotalUnread(): Int = synchronized(badgeLock) {
        badgePrefs.all.values.sumOf { (it as? Int) ?: 0 }
    }

    /** Unread count for one chat (sender number or groupId). */
    fun getUnread(key: String): Int = synchronized(badgeLock) { badgePrefs.getInt(key, 0) }

    /** Hard reset: every counter to 0 and the launcher badge cleared. */
    fun resetAllBadges() = synchronized(badgeLock) {
        badgePrefs.edit().clear().apply()
        publishBadge()
    }

    private fun incrementUnread(key: String): Int = synchronized(badgeLock) {
        val next = badgePrefs.getInt(key, 0) + 1
        badgePrefs.edit { putInt(key, next) }
        publishBadge()
        next
    }

    private fun resetUnread(key: String) = synchronized(badgeLock) {
        if (badgePrefs.contains(key)) {
            badgePrefs.edit { remove(key) }
            publishBadge()
        }
    }

    private fun resetUnreadWithPrefix(prefix: String) = synchronized(badgeLock) {
        val keys = badgePrefs.all.keys.filter { it.startsWith(prefix) }
        if (keys.isNotEmpty()) {
            badgePrefs.edit {
                keys.forEach { remove(it) }
            }
            publishBadge()
        }
    }

    private fun publishBadge() {
        onBadgeChanged?.invoke(getTotalUnread())
    }

    // ============================================================ Calls & location

    fun showMissedCallNotification(
        userId: String,
        userName: String,
        isVideoCall: Boolean,
        countInBadge: Boolean = true
    ) {
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(
            context,
            channelFor(CHANNEL_MISSED_CALL_SOUND, CHANNEL_MISSED_CALL_VIBRATE, CHANNEL_MISSED_CALL_SILENT)
        )
            .setStatusIcon()
            .setColor(accentColor)
            .setContentTitle(userName)
            .setContentText(if (isVideoCall) "Missed video call" else "Missed voice call")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setContentIntent(openChatIntent(Chat(userId)))   // opens that user's chat
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .addExtras(Bundle().apply { putBoolean(EXTRA_IS_MISSED_CALL, true) })
            .build()

        if (countInBadge) incrementUnread(CALL_KEY_PREFIX + userId)
        post(missedCallId(userId), notification)
    }

    fun clearMissedCallNotification(userId: String) {
        manager.cancel(missedCallId(userId))
        resetUnread(CALL_KEY_PREFIX + userId)
    }

    /** Removes only missed-call notifications. Messages are left alone. */
    fun clearAllMissedCallNotifications() {
        cancelAllWithExtra(EXTRA_IS_MISSED_CALL)
        resetUnreadWithPrefix(CALL_KEY_PREFIX)
    }

    /** Removes any "Open app to sync" style fallbacks currently showing. */
    fun clearFallbackNotifications() = cancelAllWithExtra(EXTRA_IS_FALLBACK)

    /** Ongoing and silent. Call clearLiveLocationNotification() when sharing stops. */
    fun showOrUpdateLiveLocationNotification(
        sender: String,
        text: String = "Live location sharing"
    ) {
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_LIVE_LOCATION)
            .setStatusIcon()
            .setColor(accentColor)
            .setContentTitle(sender)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openChatIntent(Chat(sender)))
            .build()

        post(liveLocationId(sender), notification)
    }

    fun clearLiveLocationNotification(sender: String) = manager.cancel(liveLocationId(sender))

    // ============================================================ Shared cleanup

    /** Chat opened / marked as read: remove everything for this key (sender or groupId). */
    fun clearConversation(key: String) {
        upgradeJobs.remove(key)?.cancel()
        conversations.remove(key)
        resetUnread(key)
        manager.cancel(conversationId(key))
        manager.cancel(tempId(key))
    }

    /** From NotificationDismissReceiver: the system already removed the notification. */
    fun clearHistory(key: String) {
        conversations.remove(key)
    }

    // ============================================================ Core posting

    private fun postPlaceholder(chat: Chat, title: String) {
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SILENT)
            .setStatusIcon()
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(PLACEHOLDER_TEXT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openChatIntent(chat))
            .setAutoCancel(true)
            .setSilent(true)
            .setTimeoutAfter(PLACEHOLDER_SAFETY_TIMEOUT_MS)
            .build()
        post(tempId(chat.key), notification)
    }

    /**
     * Failure: replaces the placeholder in the TEMP slot with a silent notification that
     * removes itself. The REAL slot is untouched, so earlier messages stay visible.
     */
    private fun postFallback(chat: Chat, title: String, text: String) {
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_SILENT)
            .setStatusIcon()
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openChatIntent(chat))
            .setAutoCancel(true)
            .setSilent(true)
            .setTimeoutAfter(FALLBACK_TIMEOUT_MS)
            .addExtras(Bundle().apply { putBoolean(EXTRA_IS_FALLBACK, true) })
            .build()

        post(tempId(chat.key), notification)
    }

    private fun postConversation(
        chat: Chat, title: String, text: CharSequence,
        style: NotificationCompat.MessagingStyle, timestamp: Long, messageId: String,
        largeIcon: Bitmap?, shortcutId: String?, number: Int?, silent: Boolean, wake: Boolean
    ) {
        postNow(chat, title, text, style, timestamp, messageId, largeIcon, shortcutId,
            number, silent, wake, withActions = true)
    }

    /** Builds and posts the REAL conversation notification, then removes the placeholder. */
    private fun postNow(
        chat: Chat,
        title: String,
        text: CharSequence,
        style: NotificationCompat.MessagingStyle,
        timestamp: Long,
        messageId: String,
        largeIcon: Bitmap?,
        shortcutId: String?,
        number: Int?,
        silent: Boolean,
        wake: Boolean,
        withActions: Boolean
    ) {
        val channelId = currentChannelId()

        val builder = NotificationCompat.Builder(context, channelId)
            .setStatusIcon()
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(text)
            .setLargeIcon(largeIcon)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // pre-Oreo only
            .setWhen(timestamp)
            .setShowWhen(true)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .setSilent(silent)
            .setContentIntent(openChatIntent(chat))
            .setDeleteIntent(dismissIntent(chat))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, channelId)
                    .setStatusIcon()
                    .setColor(accentColor)
                    .setContentTitle(if (chat.isGroup) "New Group Message" else "New message")
                    .setContentText("Unlock to view")
                    .build()
            )

        shortcutId?.let { builder.setShortcutId(it) }
        if (withActions) {
            builder.addAction(replyAction(chat, messageId))
            builder.addAction(markAsReadAction(chat, messageId))
        }
        number?.let { builder.setNumber(it) }
        if (wake) builder.setFullScreenIntent(openChatIntent(chat), true)

        val notification = builder.build()
        if (wake) withWakeLock { post(conversationId(chat.key), notification) }
        else post(conversationId(chat.key), notification)

        // Post first, cancel second, so there is never a gap on screen.
        manager.cancel(tempId(chat.key))
    }

    /**
     * The user replied from the notification: add "You: ..." to the SAME thread and
     * re-post silently. Title and shortcut are read back from the notification that is
     * already on screen, so the group name is never overwritten.
     */
    private fun appendOwnReply(chat: Chat, replyText: String, messageId: String) {
        if (!manager.areNotificationsEnabled()) return

        val active = activeNotification(conversationId(chat.key)) ?: return
        val style = conversations[chat.key]
            ?: NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(active)
            ?: return
        conversations[chat.key] = style
        resetUnread(chat.key) // replying means the chat has been read

        val now = System.currentTimeMillis()
        addToStyle(
            style,
            NotificationCompat.MessagingStyle.Message(boldText(replyText), now, style.user)
        )

        val title = NotificationCompat.getExtras(active)
            ?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()
            ?: style.conversationTitle?.toString()
            ?: "Messages"

        postConversation(
            chat = if (chat.isGroup) chat.copy(groupName = style.conversationTitle?.toString() ?: title) else chat,
            title = title,
            text = replyText,
            style = style,
            timestamp = now,
            messageId = messageId,
            largeIcon = null,
            shortcutId = NotificationCompat.getShortcutId(active),
            number = null,
            silent = true,
            wake = false
        )
    }

    // ============================================================ Pending intents

    private fun openChatIntent(chat: Chat): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Unique data => every chat gets its own PendingIntent (no extras mix-ups)
            data = (if (chat.isGroup) "app://group/${chat.key}" else "app://chat/${chat.key}").toUri()
            putExtra("open_chat_user", chat.key)
            if (chat.isGroup) putExtra("GROUP_ID", chat.key)
        }
        return PendingIntent.getActivity(
            context, conversationId(chat.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissIntent(chat: Chat): PendingIntent {
        val intent = Intent(context, NotificationDismissReceiver::class.java).apply {
            data = "app://dismiss/${chat.key}".toUri()
            putExtra("SENDER_ID", chat.key)
            if (chat.isGroup) putExtra("GROUP_ID", chat.key)
        }
        return PendingIntent.getBroadcast(
            context, conversationId(chat.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun replyAction(chat: Chat, messageId: String): NotificationCompat.Action {
        val intent = if (chat.isGroup) {
            Intent(context, GroupReplyReceiver::class.java).apply {
                action = "${context.packageName}.GROUP_REPLY_ACTION"
                data = "app://group-reply/${chat.key}".toUri()   // per-group, not per-message
                putExtra("groupId", chat.key)
                putExtra("groupName", chat.groupName)
                putExtra("conversationId", messageId)
            }
        } else {
            Intent(context, ReplyReceiver::class.java).apply {
                action = "${context.packageName}.REPLY_ACTION"
                data = "app://reply/${chat.key}".toUri()         // per-sender, not per-message
                putExtra("sender", chat.key)
                putExtra("conversationId", messageId)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, conversationId(chat.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // RemoteInput needs mutable
        )
        return NotificationCompat.Action.Builder(R.drawable.icon_chat, "Reply", pendingIntent)
            .addRemoteInput(RemoteInput.Builder(KEY_TEXT_REPLY_REC).setLabel("Reply").build())
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }

    private fun markAsReadAction(chat: Chat, messageId: String): NotificationCompat.Action {
        val intent = Intent(context, ReadMessageReceiver::class.java).apply {
            action = "ACTION_OK"
            data = (if (chat.isGroup) "app://group-ok/${chat.key}" else "app://ok/${chat.key}").toUri()
            putExtra("MESSAGE_ID", messageId)
            putExtra("SENDER_ID", chat.key)
            putExtra("IS_GROUP", chat.isGroup)
            if (chat.isGroup) putExtra("GROUP_ID", chat.key)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, conversationId(chat.key), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.done_outline_24px,
            if (chat.isGroup) "OK" else "Mark as read",
            pendingIntent
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }



    private fun NotificationCompat.Builder.setStatusIcon(): NotificationCompat.Builder =
        setSmallIcon(R.drawable.ic_stat_notification)

    /** res/values/colors.xml + res/values-night/colors.xml, so it adapts to dark mode. */
    private val accentColor: Int
        get() = ContextCompat.getColor(context, R.color.notification_accent)

    private fun youPerson(): Person = Person.Builder().setName("You").setKey("me").build()

    private fun boldText(text: String): CharSequence =
        SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    private fun styleFor(
        key: String,
        create: () -> NotificationCompat.MessagingStyle
    ): NotificationCompat.MessagingStyle =
        conversations.getOrPut(key) {
            activeNotification(conversationId(key))
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
                ?: create()
        }

    private fun addToStyle(
        style: NotificationCompat.MessagingStyle,
        message: NotificationCompat.MessagingStyle.Message
    ) {
        synchronized(style) { style.addMessage(message) }
    }

    private fun pushConversationShortcut(
        sender: String,
        displayName: String,
        person: Person,
        icon: IconCompat
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("open_chat_user", sender)
        }
        ShortcutManagerCompat.pushDynamicShortcut(
            context,
            ShortcutInfoCompat.Builder(context, sender)
                .setShortLabel(displayName)
                .setLongLived(true)
                .setPerson(person)
                .setIcon(icon)
                .setIntent(intent)
                .build()
        )
    }

    private fun conversationId(key: String): Int = key.hashCode()
    private fun tempId(key: String): Int = "$key:temp".hashCode()
    private fun missedCallId(userId: String): Int = "$userId:missed_call".hashCode()
    private fun liveLocationId(sender: String): Int = "$sender:live".hashCode()

    private fun currentChannelId(): String =
        channelFor(CHANNEL_SOUND, CHANNEL_VIBRATE, CHANNEL_SILENT)

    private fun channelFor(sound: String, vibrate: String, silent: String): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> sound
            AudioManager.RINGER_MODE_VIBRATE -> vibrate
            else -> silent
        }
    }

    /** Cancels every active notification that carries the given boolean extra. */
    private fun cancelAllWithExtra(extra: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.activeNotifications
            .filter { it.notification.extras.getBoolean(extra, false) }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    private fun activeNotification(id: Int): Notification? {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.activeNotifications.firstOrNull { it.id == id }?.notification
    }

    private fun isShowing(id: Int): Boolean = activeNotification(id) != null

    private fun post(id: Int, notification: Notification) {
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            DLog.e("Notification permission missing: $e")
        }
    }

    @Suppress("DEPRECATION")
    private inline fun withWakeLock(block: () -> Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        try {
            block()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun makeBitmapRound(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)

        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(
            (source.width - size) / 2,
            (source.height - size) / 2,
            (source.width + size) / 2,
            (source.height + size) / 2
        )

        canvas.drawBitmap(source, srcRect, rect, paint)
        return output
    }

    private val imageLoader by lazy { ImageLoader(context) }

    private suspend fun loadProfileBitmapFromUrl(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withTimeoutOrNull(3_000L.milliseconds) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
                    ((imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
            }
        }
    }

    companion object {
        const val CHANNEL_SOUND = "message_sound_channel"
        const val CHANNEL_VIBRATE = "message_vibrate_channel"
        const val CHANNEL_SILENT = "message_silent_channel"

        const val CHANNEL_MISSED_CALL_SOUND = "missed_call_sound_channel"
        const val CHANNEL_MISSED_CALL_VIBRATE = "missed_call_vibrate_channel"
        const val CHANNEL_MISSED_CALL_SILENT = "missed_call_silent_channel"
        const val CHANNEL_LIVE_LOCATION = "live_location_channel"

        private const val GROUP_KEY = "messages_group"
        private const val BADGE_PREFS = "notification_badges"
        private const val CALL_KEY_PREFIX = "call:"
        private const val DEFAULT_ACTIONS_DELAY_MS = 6_000L
        private const val EXTRA_IS_FALLBACK = "is_fallback"
        private const val EXTRA_IS_MISSED_CALL = "is_missed_call"

        private const val PLACEHOLDER_TEXT = "New message"
        private const val FALLBACK_TEXT = "Open app to sync"
        private const val FALLBACK_TIMEOUT_MS = 6_000L          // was 5_000
        private const val PLACEHOLDER_SAFETY_TIMEOUT_MS = 5 * 60_000L // was 60_000

        private const val WAKE_LOCK_TAG = "knotx:message_wake"
        private const val WAKE_LOCK_TIMEOUT_MS = 3_000L
    }
}