package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.connection.TypingManager
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.StoryCommentEntity
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.data.database.StoryLikeEntity
import com.senghol.knotx.data.database.StoryMediaType
import com.senghol.knotx.data.database.StorySyncStatus
import com.senghol.knotx.data.database.StoryViewEntity
import com.senghol.knotx.data.database.StoryVisibility
import com.senghol.knotx.data.database.UnsentFiles
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.data.datastore.DootawasStore
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.SosContact
import com.senghol.knotx.ui.components.formatToE164Indian
import com.senghol.knotx.ui.components.isValidIndianPhone
import com.senghol.knotx.ui.repository.HomeGroupRepository
import com.senghol.knotx.ui.repository.HomeSingleRepository
import com.senghol.knotx.ui.repository.StoryRepository
import com.senghol.knotx.ui.screens.stories.StoryBackground
import com.senghol.knotx.utils.data.CheckedContact
import com.senghol.knotx.utils.data.ContactState
import com.senghol.knotx.utils.data.GroupUiItem
import com.senghol.knotx.utils.data.UpbhogtaUiItem
import com.senghol.knotx.utils.data.UserStoryGroup
import com.senghol.knotx.utils.enums.FileDataType
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val singleRepository: HomeSingleRepository,
    private val groupRepository: HomeGroupRepository,
    private val callRepository: CallSessionRepository,
    private val storyRepository: StoryRepository,
    private val knotx: Knotx,
    private val dootawasStore: DootawasStore      // new
) : ViewModel() {

    companion object {
        private const val PROFILE_CHECK_DELAY = 3_000L
        private const val TYPING_TIMEOUT = 3_000L

        private val USERNAME_REGEX =
            Regex("^@[A-Za-z0-9_.]{3,32}$")

        private val TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

        private val MONTH_DAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

        private val MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery: StateFlow<String> = _userSearchQuery.asStateFlow()

    private val _groupSearchQuery = MutableStateFlow("")
    val groupSearchQuery: StateFlow<String> = _groupSearchQuery.asStateFlow()

    val userNameIdKhud: Flow<String?> = knotx.userNameIdKhud
    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    val upbhogtaUiItems: StateFlow<List<UpbhogtaUiItem>> =
        combine(
            singleRepository.observeUpbhogtas(),
            TypingManager.typingStates
        ) { users, typingStates ->

            users.map { user ->
                user.toUiItem(
                    isTyping = typingStates[user.userNumber]?.isTyping == true
                )
            }

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val unlockedHideKeys = MutableStateFlow<Set<String>>(emptySet())
    val filteredUpbhogtaItems: StateFlow<List<UpbhogtaUiItem>> =
        combine(
            singleRepository.observeUpbhogtas(),
            userSearchQuery,
            unlockedHideKeys,
            TypingManager.typingStates
        ) { users, query, unlockedKeys, typingStates ->

            val search = query.trim()

            val matchingKeys = users
                .mapNotNull { it.hideKey?.trim()?.takeIf(String::isNotEmpty) }
                .filter { it == search }
                .toSet()

            val newlyUnlocked = if (search.isNotEmpty()) matchingKeys else emptySet()
            val allUnlocked = unlockedKeys + newlyUnlocked

            users.filter { user ->
                val key = user.hideKey?.trim().orEmpty()
                val hidden = key.isNotEmpty()

                if (!hidden || key in allUnlocked) {
                    search.isBlank() ||
                            key in newlyUnlocked ||
                            user.contactName.orEmpty().contains(search, ignoreCase = true) ||
                            user.onlineName.orEmpty().contains(search, ignoreCase = true) ||
                            user.userNumber.contains(search, ignoreCase = true)
                } else {
                    false
                }
            }.map { user ->
                user.toUiItem(
                    isTyping = typingStates[user.userNumber]?.isTyping == true
                )
            }

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        userSearchQuery
            .combine(singleRepository.observeUpbhogtas()) { query, users ->
                val key = query.trim()
                if (key.isNotEmpty()) {
                    users.mapNotNull { it.hideKey?.trim()?.takeIf(String::isNotEmpty) }
                        .filter { it == key }
                        .toSet()
                } else {
                    emptySet()
                }
            }
            .onEach { keys ->
                if (keys.isNotEmpty()) {
                    unlockedHideKeys.update { it + keys }
                }
            }
            .launchIn(viewModelScope)
    }

    fun searchUpbhogtas(query: String) {
        _userSearchQuery.value = query
    }

    // -------------------------------------------------------------------------
    // Groups
    // -------------------------------------------------------------------------

    val groupUiItems: StateFlow<List<GroupUiItem>> =
        groupRepository
            .observeGroups()
            .map { groups ->
                groups.map { it.toUiItem() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val filteredGroupItems: StateFlow<List<GroupUiItem>> =
        combine(
            groupUiItems,
            groupSearchQuery
        ) { groups, query ->
            if (query.isBlank()) {
                groups
            } else {
                groups.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.description?.contains(query, ignoreCase = true) == true
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun searchGroups(query: String) {
        _groupSearchQuery.value = query
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    val connectionStatus = knotx.connectionStatus

    // -------------------------------------------------------------------------
    // Typing
    // -------------------------------------------------------------------------

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers: StateFlow<Set<String>> = _typingUsers.asStateFlow()

    private val typingJobs = mutableMapOf<String, Job>()

    // -------------------------------------------------------------------------
    // Update dialogs
    // -------------------------------------------------------------------------

    private val _forceUpdateVersionInfo =
        MutableStateFlow<Pair<String, String>?>(null)

    val forceUpdateVersionInfo: StateFlow<Pair<String, String>?> =
        _forceUpdateVersionInfo.asStateFlow()

    private val _optionalUpdateDialogState =
        MutableStateFlow<Pair<String, String>?>(null)

    val optionalUpdateDialogState: StateFlow<Pair<String, String>?> =
        _optionalUpdateDialogState.asStateFlow()

    fun clearForceUpdateRequest() {
        _forceUpdateVersionInfo.value = null
    }

    fun clearOptionalUpdateRequest(latest: String) {
        _optionalUpdateDialogState.value = null
        knotx.setLatestAvailable(latest)
    }

    // -------------------------------------------------------------------------
    // SOS
    // -------------------------------------------------------------------------

    val sosContactsFlow: StateFlow<List<SosContact>> =
        knotx.sosContactsFlow

    // -------------------------------------------------------------------------
    // Swipe tutorial
    // -------------------------------------------------------------------------

    val swipeTutorialShown: StateFlow<Boolean> =
        knotx.swipeTutorialShown.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    fun markSwipeTutorialShown() {
        viewModelScope.launch {
            knotx.markSwipeTutorialShown()
        }
    }

    // -------------------------------------------------------------------------
    // Profile checking
    // -------------------------------------------------------------------------

    private val checkedUsers = mutableSetOf<String>()
    private val pendingUsers = mutableSetOf<String>()
    private var userProfileCheckJob: Job? = null

    fun updateVisibleUserNumbers(newList: List<String>) {
        val newUsers = newList.filterNot { it in checkedUsers }

        if (newUsers.isEmpty()) return

        pendingUsers.addAll(newUsers)

        userProfileCheckJob?.cancel()

        userProfileCheckJob = viewModelScope.launch {
            delay(PROFILE_CHECK_DELAY.milliseconds)

            val usersToCheck = pendingUsers
                .filterNot { it in checkedUsers }
                .distinct()

            if (usersToCheck.isEmpty()) return@launch

            pendingUsers.removeAll(usersToCheck.toSet())
            checkedUsers.addAll(usersToCheck)

            knotx.checkVisibleProfiles(usersToCheck)
        }
    }

    private val checkedGroups = mutableSetOf<String>()
    private val pendingGroups = mutableSetOf<String>()
    private var groupProfileCheckJob: Job? = null

    fun updateVisibleGroupIds(newList: List<String>) {
        val newGroups = newList.filterNot { it in checkedGroups }

        if (newGroups.isEmpty()) return

        pendingGroups.addAll(newGroups)

        groupProfileCheckJob?.cancel()

        groupProfileCheckJob = viewModelScope.launch {
            delay(PROFILE_CHECK_DELAY.milliseconds)

            val groupsToCheck = pendingGroups
                .filterNot { it in checkedGroups }
                .distinct()

            if (groupsToCheck.isEmpty()) return@launch

            pendingGroups.removeAll(groupsToCheck.toSet())
            checkedGroups.addAll(groupsToCheck)

            knotx.checkVisibleGroupProfiles(groupsToCheck)
        }
    }

    // -------------------------------------------------------------------------
    // Single user actions
    // -------------------------------------------------------------------------

    fun updateBlockedStatus(
        userName: String,
        number: String,
        isBlocked: Boolean
    ) {
        knotx.blockOrUnblockUser(
            number,
            isBlocked
        )
    }

    fun updatePinnedStatus(
        number: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            singleRepository.updatePinnedStatus(
                number,
                isPinned
            )
        }
    }

    fun updateMutedStatus(
        number: String,
        isMuted: Boolean
    ) {
        viewModelScope.launch {
            singleRepository.updateMutedStatus(
                number,
                isMuted
            )
        }
    }

    fun deleteUpbhogta(number: String) {
        viewModelScope.launch {
            singleRepository.deleteUpbhogta(number)
            knotx.clearChatOfUser(number)
        }

        knotx.updateContactState(
            number,
            false
        )
    }

    fun clearChat(number: String) {
        knotx.clearChatOfUser(number)
    }

    // -------------------------------------------------------------------------
    // Group actions
    // -------------------------------------------------------------------------

    fun updateGroupPinnedStatus(
        groupId: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            groupRepository.updatePinnedStatus(
                groupId,
                isPinned
            )
        }
    }

    fun updateGroupMutedStatus(
        groupId: String,
        isMuted: Boolean
    ) {
        viewModelScope.launch {
            groupRepository.updateMutedStatus(
                groupId,
                isMuted
            )
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            groupRepository.clearGroupChat(groupId)
        }
    }

    fun clearGroupChat(groupId: String) {
        viewModelScope.launch {
            groupRepository.clearGroupChat(groupId)
        }
    }



    // -------------------------------------------------------------------------
    // Contact checking
    // -------------------------------------------------------------------------

    private val _contactState =
        MutableStateFlow<CheckedContact?>(null)

    val contactState: StateFlow<CheckedContact?> =
        _contactState.asStateFlow()

    // -------------------------------------------------------------------------
    // Lists
    // -------------------------------------------------------------------------

    data class ListItem(
        val sandeshId: String,
        val userNumber: String,
        val listData: ListData
    )

    val currentUserIdFlow: Flow<String?> =
        knotx.userNumberKhud

    val currentUserProfilePicFlow: Flow<String?> =
        knotx.profileImagePathFlow

    val lists: StateFlow<List<ListItem>> =
        currentUserIdFlow
            .filterNotNull()
            .filter { it.isNotBlank() }
            .flatMapLatest { userNumber ->
                knotx.observeListData(userNumber)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun deleteList(sandeshId: String) {
        viewModelScope.launch {
            currentUserIdFlow
                .filterNotNull()
                .filter { it.isNotBlank() }
                .first()
                .let { userNumber ->
                    knotx.deleteList(sandeshId, userNumber)
                }
        }
    }
    fun saveList(
        listData: ListData,
        sandeshId: String
    ) {
        viewModelScope.launch {
            knotx.saveList(
                listData,
                sandeshId
            )
        }
    }

    // -------------------------------------------------------------------------
    // Stories
    // -------------------------------------------------------------------------



    val myStories: StateFlow<List<StoryEntity>> =
        currentUserIdFlow
            .flatMapLatest { userId ->
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    storyRepository.observeMyStories(userId)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val userNameMap: StateFlow<Map<String, String>> =
        upbhogtaUiItems
            .map { users ->
                users.associate {
                    it.userNumber to it.name
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    val feedStoryGroups: StateFlow<List<UserStoryGroup>> =
        currentUserIdFlow
            .flatMapLatest { userId ->
                if (userId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    storyRepository.observeFeedStoryGroups(
                        myUserId = userId,
                        getUserName = { ownerId ->
                            userNameMap.value[ownerId] ?: ownerId
                        }
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun onStoryViewed(
        storyId: String,
        currentUserId: String,
        ownerId: String
    ) {
        viewModelScope.launch {
            storyRepository.markStoryAsViewed(
                storyId,
                currentUserId
            )

            storyRepository.fetchStoryDetails(storyId,ownerId)
        }
    }
    fun observeStoryComments(
        storyId: String
    ): Flow<List<StoryCommentEntity>> =
        storyRepository.observeComments(storyId)
    fun observeStoryViewers(
        storyId: String
    ): Flow<List<StoryViewEntity>> =
        storyRepository.observeViewers(storyId)

    fun observeStoryLikers(
        storyId: String
    ): Flow<List<StoryLikeEntity>> =
        storyRepository.observeLikers(storyId)
    fun onToggleStoryLike(
        storyId: String,
        currentUserId: String
    ) {
        viewModelScope.launch {
            storyRepository.toggleLike(
                storyId,
                currentUserId
            )
        }
    }

    fun onAddComment(
        storyId: String,
        currentUserId: String,
        text: String
    ) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return
        viewModelScope.launch {
            storyRepository.addComment(
                storyId,
                currentUserId,
                trimmedText
            )
        }
    }

    fun onUpdateStoryVisibility(storyId: String, currentUserId: String, visibility: StoryVisibility) {
        viewModelScope.launch {
            storyRepository.setVisibility(storyId, currentUserId, visibility)
        }
    }

    fun onDeleteStory(storyId: String) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId)
        }
    }

    fun createAndPublishStory(
        ownerId: String,
        background: StoryBackground,
        visibility: StoryVisibility = StoryVisibility.EVERYONE,
        durationMs: Long? = null
    ) {
        if (ownerId.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val storyId = java.util.UUID.randomUUID().toString()

            val (mediaUrl, localPath, mediaType) =
                when (background) {
                    is StoryBackground.Media -> {
                        val type = if (background.isVideo) {
                            StoryMediaType.VIDEO
                        } else {
                            StoryMediaType.IMAGE
                        }

                        Triple(null, background.uri.toString(), type)
                    }

                    is StoryBackground.Gradient -> {
                        Triple(null, null, StoryMediaType.TEXT)
                    }

                    is StoryBackground.SolidColor -> {
                        Triple(null, null, StoryMediaType.TEXT)
                    }
                }

            val finalDurationMs = durationMs ?: if (mediaType == StoryMediaType.VIDEO) {
                15_000L
            } else {
                5_000L
            }

            val visibilityValue = when (visibility) {
                StoryVisibility.EVERYONE -> 0
                StoryVisibility.CUSTOM -> 1
                StoryVisibility.PRIVATE -> 2
            }

            val newStory = StoryEntity(
                id = storyId,
                ownerId = ownerId,
                mediaUrl = mediaUrl,
                thumbnailUrl = null,
                localPath = localPath,
                mediaType = mediaType,
                visibility = visibility,
                allowedContactIds = emptyList(),
                durationMs = finalDurationMs,
                createdAt = now,
                expiresAt = now + 24 * 60 * 60 * 1000L,
                syncStatus = StorySyncStatus.PENDING_UPLOAD
            )

            storyRepository.saveNewStory(newStory)

            if (!localPath.isNullOrBlank()) {
                val storyData = JsonObject().apply {
                    addProperty("storyId", storyId)
                    addProperty("ownerId", ownerId)
                    addProperty("localPath", localPath)
                    addProperty("mediaType", mediaType.name)
                    addProperty("durationMs", finalDurationMs)
                    addProperty("visibility", visibilityValue)
                    addProperty(
                        "mimeType",
                        if (mediaType == StoryMediaType.VIDEO) {
                            "video/mp4"
                        } else {
                            "image/jpeg"
                        }
                    )
                }

                val unsentData = UnsentFiles(
                    messageId = storyId,
                    receiver = ownerId,
                    data = storyData.toString(),
                    dataType = FileDataType.STORY
                )

                knotx.sendStory(unsentData)
            }
        }
    }
    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    init {
        observeWebSocket()
        observeContactResults()
        purgeExpiredStories()
        viewModelScope.launch { dootawasStore.recordAppOpen() }
    }

    suspend fun shouldAskForReview(): Boolean = dootawasStore.shouldAskForReview()

    fun markReviewAsked() {
        viewModelScope.launch { dootawasStore.markReviewAsked() }
    }

    private fun observeWebSocket() {
        knotx.onTypingReceived = { userName ->
            _typingUsers.value += userName

            typingJobs[userName]?.cancel()

            typingJobs[userName] =
                viewModelScope.launch {
                    delay(TYPING_TIMEOUT.milliseconds)

                    _typingUsers.value -= userName

                    typingJobs.remove(userName)
                }
        }

        knotx.onForceUpdateRequired = { required, current ->
            _forceUpdateVersionInfo.value =
                required to current
        }

        knotx.onOptionalUpdateAvailable = { latest, current ->
            _optionalUpdateDialogState.value =
                latest to current
        }
    }

    private fun observeContactResults() {
        viewModelScope.launch {
            knotx.contactResultFlow.collect { result ->
                _contactState.value = result
            }
        }
    }

    private fun purgeExpiredStories() {
        viewModelScope.launch {
            storyRepository.purgeExpiredStories()
        }
    }

    // -------------------------------------------------------------------------
    // Contact scanner
    // -------------------------------------------------------------------------

    fun checkUser(rawInput: String) {
        val input = rawInput.trim()

        if (input.startsWith("@")) {
            if (!USERNAME_REGEX.matches(input)) {
                _contactState.value = CheckedContact(
                    phone = input,
                    state = ContactState.INVALID,
                    profileUri = ""
                )
                return
            }

            _contactState.value = CheckedContact(
                phone = input,
                state = ContactState.Checking,
                profileUri = ""
            )

            knotx.sendCheckUser(input)
            return
        }

        val formatted = formatToE164Indian(input)

        if (
            formatted == null ||
            !isValidIndianPhone(input)
        ) {
            _contactState.value = CheckedContact(
                phone = input,
                state = ContactState.INVALID,
                profileUri = ""
            )
            return
        }

        _contactState.value = CheckedContact(
            phone = formatted,
            state = ContactState.Checking,
            profileUri = ""
        )

        knotx.sendCheckUser(formatted)
    }

    fun onInvite(phone: String) {
        // TODO: call repository.invite(phone)
    }

    fun clearCurrentContact() {
        _contactState.value = null
    }

    fun onAdd(phone: String) {
        knotx.addNewScannerContact(phone)
    }

    fun removeContact(phone: String) {
        // TODO
    }

    // -------------------------------------------------------------------------
    // UI mapping
    // -------------------------------------------------------------------------

    private fun Upbhogta.toUiItem(
        isTyping: Boolean = false
    ): UpbhogtaUiItem =
        UpbhogtaUiItem(
            userNumber = userNumber,
            name = contactName?.takeIf { it.isNotBlank() }
                ?: onlineName?.takeIf { it.isNotBlank() }
                ?: userNumber,
            imageUrl = photoUrl,
            lastMessage = draftMessage
                ?.takeIf { it.isNotBlank() }
                ?.let { "Draft: $it" }
                ?: lastMessage,
            totalUnread = totalUnread,
            isPinned = isPinned,
            isMuted = isMuted,
            isBlocked = isBlocked,
            isFavourite = isFavourite,
            isTyping = isTyping,
            background = backgroundColor,
            lastMessageTime = lastMessageTime.toTimeString(),
            sandeshStithi = sandeshStithi
        )
    private fun GroupEntity.toUiItem(): GroupUiItem =
        GroupUiItem(
            id = groupId,
            name = groupName,
            description = groupDescription,
            imageUrl = groupImageUrl,
            lastMessage = lastMessage,
            totalUnread = totalUnread,
            isPinned = isPinned == true,
            isMuted = isMuted,
            lastMessageTime = lastMessageTime.toTimeString(),
            sandeshStithi = sandeshStithi,
            groupBackgroundColor = backgroundColor
        )

    private fun Long.toTimeString(): String {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(this).atZone(zone)
        val now = java.time.ZonedDateTime.now(zone)

        return when {
            dateTime.toLocalDate() == now.toLocalDate() ->
                TIME_FORMATTER.format(dateTime)

            dateTime.toLocalDate() ==
                    now.toLocalDate().minusDays(1) ->
                "Yesterday"

            dateTime.year == now.year ->
                MONTH_DAY_FORMATTER.format(dateTime)

            else ->
                MONTH_YEAR_FORMATTER.format(dateTime)
        }
    }

    fun getAllContactStories(){
        viewModelScope.launch {
            storyRepository.getAllContactStories()
        }

    }

    fun onDeleteComment(storyId: String, commentId: String, currentUserId: String) {
        viewModelScope.launch {
            try {
                storyRepository.deleteComment(commentId, storyId, currentUserId)
            } catch (e: IllegalAccessException) {
                DLog.e("onDeleteComment: not allowed", e)
                // TODO: surface this to the user (Toast/Snackbar) — right now it just logs
            }
        }
    }

    fun setCommentPrivate(storyId: String, private: Boolean, currentUserId: String) {
        viewModelScope.launch {
            storyRepository.setCommentsPrivate(storyId,currentUserId,private,)
        }
    }



        val history: StateFlow<List<CallHistoryItem>> =
            callRepository.observeAll()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )


}
