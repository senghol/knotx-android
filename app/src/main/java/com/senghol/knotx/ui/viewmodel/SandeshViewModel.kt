package com.senghol.knotx.ui.viewmodel


import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.data.database.UnsentFiles
import com.senghol.knotx.data.database.UnsentSandesh
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.senghol.knotx.utils.enums.DootawasMessage
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.maps.android.PolyUtil
import com.senghol.knotx.BuildConfig
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.connection.TypingManager
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.GroupMemberEntity
import com.senghol.knotx.data.database.GroupSandesh
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.services.LiveLocationService
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.components.BubbleType
import com.senghol.knotx.ui.components.LedgerStatus
import com.senghol.knotx.ui.components.LedgerType
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.ListMessage
import com.senghol.knotx.ui.components.ListMessageType
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.screens.MediaCategory
import com.senghol.knotx.ui.screens.MediaVisibility
import com.senghol.knotx.ui.screens.getFileName
import com.senghol.knotx.ui.screens.saveMediaToAppStorageNext
import com.senghol.knotx.ui.theme.DarkDootawasColors
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LightDootawasColors
import com.senghol.knotx.ui.theme.StoredThemeBundle
import com.senghol.knotx.ui.theme.toCompose
import com.senghol.knotx.utils.converters.parseSingleStatusJson
import com.senghol.knotx.utils.converters.parseStatusJson
import com.senghol.knotx.utils.data.AiBlock
import com.senghol.knotx.utils.data.AiListItem
import com.senghol.knotx.utils.data.AiWalletBalance
import com.senghol.knotx.utils.data.ContactInfo
import com.senghol.knotx.utils.data.GroupParsedAudioSandesh
import com.senghol.knotx.utils.data.GroupParsedContactSandesh
import com.senghol.knotx.utils.data.GroupParsedDateHeaderSandesh
import com.senghol.knotx.utils.data.GroupParsedDocumentSandesh
import com.senghol.knotx.utils.data.GroupParsedImageSandesh
import com.senghol.knotx.utils.data.GroupParsedListSandesh
import com.senghol.knotx.utils.data.GroupParsedLocationSandesh
import com.senghol.knotx.utils.data.GroupParsedSandeshItem
import com.senghol.knotx.utils.data.GroupParsedTextSandesh
import com.senghol.knotx.utils.data.GroupParsedUniversalSandesh
import com.senghol.knotx.utils.data.GroupParsedVideoSandesh
import com.senghol.knotx.utils.data.GroupUserProfile
import com.senghol.knotx.utils.data.LiveLocationItem
import com.senghol.knotx.utils.data.MSG_LINK
import com.senghol.knotx.utils.data.MSG_SINGLE_EMOJI
import com.senghol.knotx.utils.data.MSG_TEXT
import com.senghol.knotx.utils.data.ParsedAiSandesh
import com.senghol.knotx.utils.data.ParsedAudioSandesh
import com.senghol.knotx.utils.data.ParsedContactSandesh
import com.senghol.knotx.utils.data.ParsedDateHeaderSandesh
import com.senghol.knotx.utils.data.ParsedDisappearingSandesh
import com.senghol.knotx.utils.data.ParsedDocumentSandesh
import com.senghol.knotx.utils.data.ParsedImageSandesh
import com.senghol.knotx.utils.data.ParsedLedgerSandesh
import com.senghol.knotx.utils.data.ParsedListSandesh
import com.senghol.knotx.utils.data.ParsedLocationSandesh
import com.senghol.knotx.utils.data.ParsedSandeshItem
import com.senghol.knotx.utils.data.ParsedTextSandesh
import com.senghol.knotx.utils.data.ParsedUniversalSandesh
import com.senghol.knotx.utils.data.ParsedVideoSandesh
import com.senghol.knotx.utils.data.ReactionUi
import com.senghol.knotx.utils.data.SandeshStatusInfo
import com.senghol.knotx.utils.data.SingleMessageStatus
import com.senghol.knotx.utils.enums.CallResult

import com.senghol.knotx.utils.enums.FileDataType
import com.senghol.knotx.utils.enums.GroupSandeshType

import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.PurchaseOrderType
import com.senghol.knotx.utils.enums.SearchFilter
import com.senghol.knotx.utils.objects.DLog
import com.senghol.knotx.utils.objects.SandeshTestData
import com.senghol.knotx.utils.objects.UniversalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.collections.associateBy
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.iterator
import kotlin.collections.map
import kotlin.collections.mapNotNull

class SandeshViewModel(
    private val repository: SandeshRepository,
    private val userNumberOrGroupId: String,
    private val isGroupPassed: Boolean,
    private val knotx: Knotx,
) : ViewModel() {

    companion object {
        private const val DATE_HEADER_PREFIX = "date_header_"
    }

    // Backing states
    val imageLoader = repository.imageLoader
    private val _selectedSandesh = MutableStateFlow<Sandesh?>(null)
    val selectedSandesh: StateFlow<Sandesh?> = _selectedSandesh

    private val _replyToType = MutableStateFlow<SandeshType?>(null)
    val replyToType: StateFlow<SandeshType?> = _replyToType
    val isAi: Boolean = userNumberOrGroupId.startsWith("&ai", ignoreCase = true)
    private val _parsedSandeshItems = MutableStateFlow<List<ParsedSandeshItem>>(emptyList())
    val parsedSandeshItems: StateFlow<List<ParsedSandeshItem>> = _parsedSandeshItems.asStateFlow()

    private val _allSandeshList = MutableStateFlow<List<Sandesh>>(emptyList())

    private val _fontSizeFlow = MutableStateFlow(14)
    val fontSizeFlow: StateFlow<Int> = _fontSizeFlow

    private val _chatBehaviourFlow = MutableStateFlow(true)
    val chatBehaviourFlow: StateFlow<Boolean> = _chatBehaviourFlow

    val isTyping: State<Boolean>field = mutableStateOf(false)

    private val _latestSandeshId = MutableStateFlow<String?>(null)
    val latestSandeshId: StateFlow<String?> = _latestSandeshId.asStateFlow()
    val parsedList = parsedSandeshItems.map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imageList = parsedList.map { list ->
        list.filterIsInstance<ParsedImageSandesh>()
            .mapNotNull { it.displayUri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val aiWalletBalance: StateFlow<AiWalletBalance> =
        repository.aiWalletBalanceFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AiWalletBalance()
            )

    fun formatTime(timestamp: Long): String {
        return repository.formatTime(timestamp)
    }


    //todo shouldn't we get this message from list instead of db??
    fun fetchSingleSandesh(sandeshId: String, userNumber: String) {
        viewModelScope.launch {
            val sandesh = repository.getSingleSandeshByUserAndId(sandeshId, userNumber)
            _selectedSandesh.value = sandesh
            _replyToType.value = sandesh?.sandeshType?.let { SandeshType.fromInt(it) }
        }
    }

//    fun preloadImages(items: List<ParsedSandeshItem>) {
//        viewModelScope.launch(Dispatchers.IO) {
//            items.filterIsInstance<ParsedImageSandesh>()
//                .forEach { imageItem ->
//                    imageItem.displayUri?.let { uri ->
//                        try {
//                            val request = ImageRequest.Builder(repository.context)
//                                .data(uri)
//                                .size(coil.size.Size.ORIGINAL) // You can limit size if needed
//                                .build()
//
////                            repository.imageLoader.execute(request)
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//                }
//        }
//    }


    var exoPlayer: ExoPlayer? = null
        private set

    var currentlyPlayingId by mutableStateOf<String?>(null)
        private set

    // VIDEO PREVIEW PLAYER
    var videoPreviewPlayer: ExoPlayer? = null
        private set

    /**
     * VIDEO PREVIEW PLAYBACK
     */
    fun playPreviewVideo(uri: Uri) {

        val player = videoPreviewPlayer ?: return

        player.stop()

        player.clearMediaItems()

        player.setMediaItem(
            MediaItem.fromUri(uri)
        )

        player.prepare()

        player.playWhenReady = true
    }

    /**
     * STOP VIDEO PREVIEW
     */
    fun stopPreviewVideo() {

        videoPreviewPlayer?.apply {

            pause()

            clearMediaItems()
        }
    }


    fun initVideoPreviewPlayer(context: Context) {

        if (videoPreviewPlayer == null) {

            videoPreviewPlayer =
                ExoPlayer.Builder(context).build().apply {

                    repeatMode = Player.REPEAT_MODE_OFF
                }
        }
    }


    fun initPlayer(context: Context) {
        if (exoPlayer == null) {

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF

                addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@SandeshViewModel.isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            pause()
                            seekTo(0)
                            this@SandeshViewModel.isPlaying = false
                            currentlyPlayingId = null
                            currentlyPlayingId = null
                        }
                    }
                })
            }
        }
    }

    var isPlaying by mutableStateOf(false)
        private set

    fun togglePlay(sandeshId: String, uri: String) {
        val player = exoPlayer ?: return

        // SAME AUDIO MESSAGE
        if (currentlyPlayingId == sandeshId) {

            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }

            return
        }

        // DIFFERENT AUDIO MESSAGE
        player.stop()
        player.clearMediaItems()

        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()

        currentlyPlayingId = sandeshId
    }

    override fun onCleared() {
        exoPlayer?.release()
    }

    fun clearReply() {
        _selectedSandesh.value = null
        _replyToType.value = null
        _groupSelectedSandesh.value = null
        _groupReplyToType.value = null
    }

    /** Call when the screen stops (screen off / app backgrounded) */
    fun pauseAllPlayback() {
        exoPlayer?.pause()
        videoPreviewPlayer?.pause()
    }

    private var searchJob: Job? = null

    fun filterSearch(query: String) {
        currentQuery = query
        rebuildItems(debounce = query.isNotBlank())   // debounce typing, clear instantly
    }

    fun saveDraft(draft: String) {
        viewModelScope.launch {
            repository.updateDraft(userNumberOrGroupId, draft, isGroupPassed)
        }
    }


// ---------- Basic date helpers ----------

    fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    fun isSameDay(a: Long, b: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        a.toLocalDate(zone) == b.toLocalDate(zone)

// ---------- Labels ----------

    /** Today / Yesterday / Tomorrow / Monday / 12 Jun 2025 */
    fun shortDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        return when (ChronoUnit.DAYS.between(date, today)) {
            0L -> "Today"
            1L -> "Yesterday"
            -1L -> "Tomorrow"
            in 2L..6L -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }
    }

    /** Sunday, 4 October 2026 · Today / 3 days ago / In 2 days */
    fun detailedDayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val full =
            date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))
        val diff = ChronoUnit.DAYS.between(date, today)
        val relative = when {
            diff == 0L -> "Today"
            diff == 1L -> "Yesterday"
            diff == -1L -> "Tomorrow"
            diff > 1L -> "$diff days ago"
            else -> "In ${-diff} days"
        }
        return "$full · $relative"
    }

// ---------- Header creation ----------

    fun parseDateHeader(
        time: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): ParsedDateHeaderSandesh {
        val date = time.toLocalDate(zone)
        val epochDay = date.toEpochDay()
        return ParsedDateHeaderSandesh(
            sandeshId = "$DATE_HEADER_PREFIX$epochDay",
            time = time,
            epochDay = epochDay,
            formattedDate = shortDayLabel(date, today),
            detailedDate = detailedDayLabel(date, today),
            sandeshStithi = SandeshStithi.LIKHIT   // replace with a neutral value from your enum
        )
    }

    /**
     * Input is newest first, so each day's header goes AFTER that day's oldest message
     * (it appears above that day's messages when reverseLayout = true).
     */
    fun addDateHeaders(items: List<ParsedSandeshItem>): List<ParsedSandeshItem> {
        if (items.isEmpty()) return items

        // Computed once per call, so every header uses the same "today" and zone
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val result = ArrayList<ParsedSandeshItem>(items.size + 8)
        var lastDay = Long.MIN_VALUE
        var lastTime = 0L

        for (item in items) {
            val day = item.time.toLocalDate(zone).toEpochDay()

            // Day changed: close the previous day's block with its header
            if (lastDay != Long.MIN_VALUE && day != lastDay) {
                result.add(parseDateHeader(lastTime, zone, today))
            }

            result.add(item)
            lastDay = day
            lastTime = item.time
        }

        // Close the final (oldest) day block
        result.add(parseDateHeader(lastTime, zone, today))
        return result
    }

// ---------- Filtering helpers ----------

    fun ParsedSandeshItem.isDateHeader(): Boolean = this is ParsedDateHeaderSandesh

    /** Use for search, counts, selection, etc. so headers aren't treated as messages */
    fun List<ParsedSandeshItem>.withoutDateHeaders(): List<ParsedSandeshItem> =
        filter { it !is ParsedDateHeaderSandesh }

    // Same type family = same group. Replies count as their base type.
    private fun groupTypeOf(s: Sandesh): SandeshType? = when (SandeshType.fromInt(s.sandeshType)) {
        SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE -> SandeshType.MESSAGE
        SandeshType.IMAGE, SandeshType.REPLY_IMAGE -> SandeshType.IMAGE
        SandeshType.VIDEO, SandeshType.REPLY_VIDEO -> SandeshType.VIDEO
        SandeshType.AUDIO, SandeshType.REPLY_AUDIO -> SandeshType.AUDIO
        SandeshType.DOCUMENT, SandeshType.REPLY_DOCUMENT -> SandeshType.DOCUMENT
        SandeshType.CONTACT, SandeshType.REPLY_CONTACT -> SandeshType.CONTACT
        SandeshType.LOCATION, SandeshType.REPLY_LOCATION -> SandeshType.LOCATION
        else -> SandeshType.fromInt(s.sandeshType)
    }

    private fun hourBucket(timeMillis: Long): Long {
        val offset = java.util.TimeZone.getDefault().getOffset(timeMillis)
        return (timeMillis + offset) / 3_600_000L
    }

    private fun isSameGroup(a: Sandesh, b: Sandesh): Boolean =
        a.isSelf == b.isSelf &&
                groupTypeOf(a) == groupTypeOf(b) &&
                hourBucket(a.time) == hourBucket(b.time)

    private fun shouldShow(s: Sandesh): Boolean {
        return when (SandeshType.fromInt(s.sandeshType)) {
            SandeshType.DELETE -> false
            SandeshType.UNIVERSAL_MESSAGE -> {
                val json = try {
                    JsonParser.parseString(s.samagri).asJsonObject
                } catch (_: Exception) {
                    return false
                }
                val type = json.get(DootawasMessage.UNIVERSAL_TYPE)?.asString
                    ?: json.get("type")?.asString
                if (type == "call") {
                    val outcome = json.getAsJsonObject("data")?.get("outcome")?.asInt
                    outcome == CallResult.MISSED.value
                } else true
            }

            else -> true
        }
    }

    private fun parseMessages(messages: List<Sandesh>): List<ParsedSandeshItem> {
        val visible = messages.filter(::shouldShow)   // newest first, hidden ones removed

        return visible.mapIndexedNotNull { index, sandesh ->
            try {
                val prev = visible.getOrNull(index - 1)   // newer message
                val next = visible.getOrNull(index + 1)   // older message

                val hasPrevBuddy = prev != null && isSameGroup(prev, sandesh)
                val hasNextBuddy = next != null && isSameGroup(next, sandesh)

                // List is newest first
                val bubbleType = when {
                    hasPrevBuddy && hasNextBuddy -> BubbleType.MID
                    !hasPrevBuddy && hasNextBuddy -> BubbleType.END    // newest in group
                    hasPrevBuddy && !hasNextBuddy -> BubbleType.START  // oldest in group
                    else -> BubbleType.SINGLE
                }

                when (SandeshType.fromInt(sandesh.sandeshType)) {
                    SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE -> sandesh.toParsedTextSandesh(
                        bubbleType
                    )

                    SandeshType.IMAGE, SandeshType.REPLY_IMAGE -> sandesh.toParsedImageSandesh()
                    SandeshType.VIDEO, SandeshType.REPLY_VIDEO -> sandesh.toParsedVideoSandesh()
                    SandeshType.AUDIO, SandeshType.REPLY_AUDIO -> sandesh.toParsedAudioSandesh()
                    SandeshType.DOCUMENT, SandeshType.REPLY_DOCUMENT -> sandesh.toParsedDocumentSandesh()
                    SandeshType.CONTACT, SandeshType.REPLY_CONTACT -> sandesh.toParsedContactSandesh()
                    SandeshType.LOCATION, SandeshType.REPLY_LOCATION -> sandesh.toParsedLocationSandesh()

                    SandeshType.AI_MESSAGE -> sandesh.toParsedAiSandesh(bubbleType)
                    SandeshType.UNIVERSAL_MESSAGE -> sandesh.toParsedUniversalSandesh()
                    SandeshType.LEDGER -> sandesh.toParsedLedgerSandesh()
                    SandeshType.LIST -> sandesh.toParsedListSandesh()
                    SandeshType.REQUEST_DISAPPEARING -> sandesh.toParsedDisappearingSandesh()
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun AiBlock.searchableText(): String = when (this) {
        is AiBlock.Text -> text
        is AiBlock.Heading -> text
        is AiBlock.Code -> code
        is AiBlock.Quote -> text
        is AiBlock.Table -> (header + rows.flatten()).joinToString(" ")
        is AiBlock.ListBlock -> items.joinToString(" ") { it.text }
        AiBlock.Divider -> ""
    }
    private fun Sandesh.filterType(): SearchFilter? = when (groupTypeOf(this)) {
        SandeshType.MESSAGE -> SearchFilter.TEXT
        SandeshType.IMAGE -> SearchFilter.IMAGE
        SandeshType.VIDEO -> SearchFilter.VIDEO
        SandeshType.AUDIO -> SearchFilter.AUDIO
        SandeshType.DOCUMENT -> SearchFilter.DOCUMENT
        SandeshType.CONTACT -> SearchFilter.CONTACT
        SandeshType.LOCATION -> SearchFilter.LOCATION
        SandeshType.AI_MESSAGE -> SearchFilter.AI
        else -> null
    }

    private fun ParsedSandeshItem.matchesQuery(q: String): Boolean = when (this) {
        is ParsedTextSandesh ->
            paath.contains(q, ignoreCase = true) || linkTitle?.contains(q, ignoreCase = true) == true
        is ParsedImageSandesh -> paath.contains(q, ignoreCase = true)
        is ParsedVideoSandesh -> paath.contains(q, ignoreCase = true)
        is ParsedAiSandesh ->
            paath.contains(q, ignoreCase = true) ||
                    blocks.any { it.searchableText().contains(q, ignoreCase = true) }
        else -> false
    }

    private fun ParsedSandeshItem.asStandalone(): ParsedSandeshItem = when (this) {
        is ParsedTextSandesh -> copy(bubbleType = BubbleType.SINGLE)
        is ParsedAiSandesh -> copy(bubbleType = BubbleType.SINGLE)
        else -> this
    }

    private fun buildItems(
        messages: List<Sandesh>,
        query: String,
        filters: Set<SearchFilter>
    ): List<ParsedSandeshItem> {
        val q = query.trim()
        val source = if (filters.isEmpty()) messages
        else messages.filter { it.filterType() in filters }
        val parsed = parseMessages(source)

        return when {
            q.isEmpty() && filters.isEmpty() -> addDateHeaders(parsed)
            q.isEmpty() -> parsed.map { it.asStandalone() }   // chips only
            else -> parsed.filter { it.matchesQuery(q) }.map { it.asStandalone() }
        }
    }

    private var currentQuery = ""

    private val _searchFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())
    val searchFilters = _searchFilters.asStateFlow()

    fun toggleSearchFilter(filter: SearchFilter) {
        _searchFilters.value = _searchFilters.value.let {
            if (filter in it) it - filter else it + filter
        }
        rebuildItems(debounce = false)
    }

    fun clearSearchFilters() {
        if (_searchFilters.value.isEmpty()) return
        _searchFilters.value = emptySet()
        rebuildItems(debounce = false)
    }

    private fun rebuildItems(debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(250)

            val messages = _allSandeshList.value
            val query = currentQuery
            val filters = _searchFilters.value
            _parsedSandeshItems.value = withContext(Dispatchers.Default) {
                buildItems(messages, query, filters)
            }
        }
    }

    fun fetchMessages(userNumber: String) {
        viewModelScope.launch {
            repository.getSandeshByUser(userNumber)
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { messages ->
                    _allSandeshList.value = messages

                    val query = currentQuery
                    val filters = _searchFilters.value
                    val items = withContext(Dispatchers.Default) {
                        buildItems(messages, query, filters)
                    }
                    _parsedSandeshItems.value = items

                    // Only update "latest" when not searching, so search results don't trigger auto-scroll
                    if (query.isBlank() && filters.isEmpty()) {
                        items.firstOrNull { !it.isDateHeader() }?.let {
                            _latestSandeshId.value = it.sandeshId
                        }
                    }
                }
        }
    }

    fun setAsActiveUser(id: String) {
        repository.setAsActiveUser(id)
    }

    fun clearActiveUser() {
        setAsActiveUser("")
    }

    fun checkSingleProfile() {
        repository.checkSingleProfile(userNumberOrGroupId)
    }

    fun isSingleEmoji(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        val codePoints = trimmed.codePoints().toArray()

        return codePoints.size == 1 &&
                Character.getType(codePoints[0]) == Character.OTHER_SYMBOL.toInt()
    }


    fun Sandesh.toParsedAiSandesh(bubbleType: BubbleType): ParsedAiSandesh {
        val json = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val paath = json.get(DootawasMessage.PAATH)?.asString ?: ""
        val status = json.get("status")?.asString ?: "OK"

        val blocksJson = json.getAsJsonArray("blocks")
        val isTyping =
            blocksJson == null && status == "OK"   // blocks are saved only when typing finishes

        val blocks = when {
            blocksJson != null -> parseAiBlocks(blocksJson)
            paath.isNotBlank() -> listOf(AiBlock.Text(paath))  // still typing, or an error/no-credits message
            else -> emptyList()
        }

        return ParsedAiSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            bubbleType = bubbleType,
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(json, time),
            sandeshStithi = sandeshStithi,
            paath = paath,
            blocks = blocks,
            isTyping = isTyping,
            status = status
        )
    }

    private fun parseAiBlocks(array: JsonArray): List<AiBlock> = array.mapNotNull { el ->
        runCatching {
            val o = el.asJsonObject
            when (o.get("type").asString) {
                "text" -> AiBlock.Text(o.get("text").asString)
                "heading" -> AiBlock.Heading(o.get("level").asInt, o.get("text").asString)
                "code" -> AiBlock.Code(o.get("lang").asString, o.get("code").asString)
                "quote" -> AiBlock.Quote(o.get("text").asString)
                "divider" -> AiBlock.Divider
                "table" -> AiBlock.Table(
                    header = o.getAsJsonArray("header").map { it.asString },
                    align = o.getAsJsonArray("align").map { it.asString },
                    rows = o.getAsJsonArray("rows")
                        .map { row -> row.asJsonArray.map { it.asString } }
                )

                "list" -> AiBlock.ListBlock(
                    ordered = o.get("ordered").asBoolean,
                    items = o.getAsJsonArray("items").map {
                        val item = it.asJsonObject
                        AiListItem(item.get("level").asInt, item.get("text").asString)
                    }
                )

                else -> AiBlock.Text(
                    o.get("text")?.asString ?: ""
                )   // unknown future block type: show its text
            }
        }.getOrNull()
    }

    fun Sandesh.toParsedUniversalSandesh(): ParsedUniversalSandesh {

        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val type = jsonObject.get(DootawasMessage.UNIVERSAL_TYPE)?.asString
            ?: jsonObject.get("type")?.asString // fallback for old messages before the key rename
        val data = jsonObject.getAsJsonObject("data")

        val msgTime = jsonObject.get("time")?.asLong ?: time

        val paath: String
        val icon: ImageVector

        when (type) {

            UniversalType.CALL -> {

                val outcome = data?.get("outcome")?.asInt
                    ?: CallResult.ENDED.value

                val isVideoCall = data
                    ?.get("isVideoCall")
                    ?.asBoolean == true

                val isOutgoing = data
                    ?.get("isOutgoing")
                    ?.asBoolean == true

                val callType = "Call"

                paath = when (outcome) {
                    CallResult.PICKED.value -> if (isOutgoing) "Outgoing $callType" else "Incoming $callType"
                    CallResult.MISSED.value -> "Missed $callType"
                    CallResult.DECLINED.value -> "$callType Declined"
                    CallResult.CANCELED.value -> "$callType Canceled"
                    CallResult.FAILED.value -> "$callType Failed"
                    CallResult.BUSY.value -> "User Busy"
                    CallResult.NO_ANSWER.value -> "No Answer"
                    CallResult.DISCONNECTED.value -> "$callType Disconnected"
                    CallResult.ENDED.value -> "$callType Ended"
                    else -> callType
                }

                icon = when {
                    outcome == CallResult.MISSED.value -> Icons.AutoMirrored.Rounded.CallMissed
                    outcome == CallResult.DECLINED.value ||
                            outcome == CallResult.CANCELED.value ||
                            outcome == CallResult.FAILED.value ||
                            outcome == CallResult.DISCONNECTED.value -> Icons.Rounded.CallEnd

                    isVideoCall -> Icons.Rounded.Videocam
                    else -> Icons.Rounded.Call
                }
            }

            UniversalType.REVEAL_PHONE_NUMBER -> {

                val isSelfReveal = data?.get("isSelf")?.asBoolean == true

                paath = "Number revealed"

                icon = Icons.Rounded.Phone
            }

            else -> {
                paath = jsonObject.get("paath")?.asString ?: samagri
                icon = Icons.Rounded.Info
            }
        }

        return ParsedUniversalSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            time = msgTime,
            formattedDate = getRelativeTimeSpanMinuteLevel(msgTime),
            formattedTime = buildFormattedTime(jsonObject, msgTime),
            sandeshStithi = sandeshStithi,
            paath = paath,
            icon = icon
        )
    }

    fun Sandesh.toParsedLedgerSandesh(): ParsedLedgerSandesh? {
        val json = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            return null
        }

        val dataJson = json.getAsJsonObject(DootawasMessage.LEDGER_DATA)
        val ledgerType = LedgerType.fromInt(json.get(DootawasMessage.LEDGER_TYPE)?.asInt ?: 0)
        val ledgerStatus = LedgerStatus.fromInt(json.get(DootawasMessage.LEDGER_STATUS)?.asInt ?: 0)

        val billTotal = dataJson?.get(DootawasMessage.BILL_TOTAL)?.asDouble ?: 0.0
        val amountPaid = dataJson?.get(DootawasMessage.BILL_AMOUNT_PAID)?.asDouble ?: 0.0
        val rawAmount = json.get(DootawasMessage.LEDGER_AMOUNT)?.asDouble ?: 0.0

        // Compute delta value
        val billDelta = billTotal - amountPaid

        val effectiveAmount = when (ledgerType) {
            // Strict mapping: If overpaid, take the absolute value of the reverse credit balance
            LedgerType.BILL -> kotlin.math.abs(billDelta)
            else -> rawAmount
        }

        // UPDATED: Fully paid ONLY when paid matches the amount exactly
        val isFullyPaid = ledgerType == LedgerType.BILL && amountPaid == billTotal && billTotal > 0
        val isOverPaid = ledgerType == LedgerType.BILL && amountPaid > billTotal

        val isPositive = when (ledgerType) {
            LedgerType.BILL -> when {
                isFullyPaid -> false // Exact settlement = clear baseline zero
                isOverPaid -> !isSelf // UPDATED: Overpayment goes in complete reverse direction
                else -> isSelf  // Underpaid means default bill debt rules apply
            }

            LedgerType.CREDIT -> isSelf
            LedgerType.DEBIT -> !isSelf
            else -> true
        }

        return ParsedLedgerSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            time = time,
            formattedTime = buildFormattedTime(json, time),
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            sandeshStithi = sandeshStithi,
            ledgerType = ledgerType,
            // Automatically label exact clean settlements as ACCEPTED/Settled
            ledgerStatus = if (isFullyPaid) LedgerStatus.ACCEPTED else ledgerStatus,
            amount = effectiveAmount,
            isPositive = isPositive,
            contactName = json.get(DootawasMessage.LEDGER_CONTACT_NAME)?.asString ?: "",
            note = json.get(DootawasMessage.LEDGER_NOTE)?.asString ?: "",
            resolvedBalance = json.get(DootawasMessage.LEDGER_RESOLVED_BALANCE)?.asDouble,
            dataJson = json,
            billTotal = billTotal,
            amountPaid = amountPaid,
            invoiceNumber = dataJson?.get("invoiceNumber")?.asString ?: "",
        )
    }

    private fun buildListUpdateSummary(
        old: ListData,
        new: ListData
    ): List<String> {

        val summary = mutableListOf<String>()

        val oldMap = old.items.associateBy { it.id }
        val newMap = new.items.associateBy { it.id }

        val added =
            newMap.keys.subtract(oldMap.keys).size

        val removed =
            oldMap.keys.subtract(newMap.keys).size

        val completed =
            newMap.values.count { newItem ->
                val oldItem = oldMap[newItem.id]
                oldItem != null &&
                        !oldItem.checked &&
                        newItem.checked
            }

        if (added > 0)
            summary += "+$added item${if (added > 1) "s" else ""}"

        if (completed > 0)
            summary += "✓$completed completed"

        if (removed > 0)
            summary += "−$removed removed"

        return summary
    }

    fun Sandesh.toParsedListSandesh(): ParsedListSandesh {
        val json = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }


        val listMessage = try {
            // Fix: Detect if incoming JSON is directly a ListData object or a wrapped ListMessage
            if (json.has("items") || json.has("title")) {
                // It's a raw ListData object! Let's parse it and wrap it.
                val parsedListData = Gson().fromJson(json, ListData::class.java) ?: ListData()
                ListMessage(
                    messageType = ListMessageType.CREATE,
                    baseVersion = 0,
                    current = parsedListData,
                    updated = null
                )
            } else {
                // It's a standard ListMessage object structure
                Gson().fromJson(json, ListMessage::class.java) ?: ListMessage(
                    messageType = ListMessageType.CREATE,
                    current = ListData()
                )
            }
        } catch (_: Exception) {
            ListMessage(
                messageType = ListMessageType.CREATE,
                current = ListData()
            )
        }


        // Display the proposed version if it's an active update, otherwise default back to current
        val displayList =
            if (listMessage.messageType == ListMessageType.UPDATE && listMessage.updated != null) {
                listMessage.updated
            } else {
                listMessage.current
            }

        val completed = displayList.items.count { it.checked }
        val total = displayList.items.size
        val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
        val previewItems = displayList.items.take(3)
        val remainingItems = (total - previewItems.size).coerceAtLeast(0)


        return ParsedListSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(json, time),
            sandeshStithi = sandeshStithi,
            listMessage = listMessage,
            displayList = displayList,
            title = displayList.title,
            completed = completed,
            total = total,
            progress = progress,
            previewItems = previewItems,
            remainingItems = remainingItems,
            isUpdate = listMessage.messageType == ListMessageType.UPDATE && listMessage.updated != null
        )
    }

    fun formatDisappearingDuration(seconds: Int): String {
        val nbsp = '\u00A0' // non-breaking space
        return when {
            seconds < 60 -> "$seconds${nbsp}seconds"
            seconds < 3600 -> {
                val minutes = seconds / 60
                if (minutes == 1) "1${nbsp}minute" else "$minutes${nbsp}minutes"
            }

            seconds < 86400 -> {
                val hours = seconds / 3600
                if (hours == 1) "1${nbsp}hour" else "$hours${nbsp}hours"
            }

            seconds < 604800 -> {
                val days = seconds / 86400
                if (days == 1) "1${nbsp}day" else "$days${nbsp}days"
            }

            else -> {
                val weeks = seconds / 604800
                if (weeks == 1) "1${nbsp}week" else "$weeks${nbsp}weeks"
            }
        }
    }

    fun Sandesh.toParsedDisappearingSandesh(): ParsedDisappearingSandesh? {
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            return null
        }

        val duration = jsonObject.get(JsonKeys.DURATION)?.asInt ?: return null
        val requestTimeStamp = jsonObject.get(JsonKeys.TIME_STAMP)?.asString?.toLongOrNull() ?: time
        val accepted = if (jsonObject.has(JsonKeys.DATA_BOOLEAN)) {
            jsonObject.get(JsonKeys.DATA_BOOLEAN).asBoolean
        } else {
            null // no response yet
        }

        val isTurningOff = duration == 0
        val formattedDuration = if (isTurningOff) "" else formatDisappearingDuration(duration)

        val title = when (accepted) {
            null -> when {
                isSelf -> "Request sent"
                isTurningOff -> "Turn off disappearing messages?"
                else -> "Enable disappearing messages?"
            }

            true -> if (isTurningOff) "Disappearing messages off" else "Disappearing messages on"
            false -> "Request declined"
        }

        val message = when (accepted) {
            true -> when {
                isTurningOff && isSelf -> "You turned off disappearing messages"
                isTurningOff && !isSelf -> "Disappearing messages turned off"
                isSelf -> "You turned on disappearing messages ($formattedDuration)"
                else -> "Disappearing messages turned on ($formattedDuration)"
            }

            false -> when {
                isTurningOff && isSelf -> "You declined the request to turn off disappearing messages"
                isTurningOff && !isSelf -> "Request to turn off disappearing messages declined"
                isSelf -> "You declined disappearing messages request"
                else -> "Disappearing messages request declined"
            }

            null -> when {
                isTurningOff && isSelf -> "You requested to turn off disappearing messages"
                isTurningOff && !isSelf -> "Messages will no longer disappear."
                isSelf -> "You requested disappearing messages ($formattedDuration)"
                else -> "Messages will vanish $formattedDuration after being read."
            }
        }

        return ParsedDisappearingSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            sandeshStithi = sandeshStithi,
            duration = duration,
            formattedDuration = formattedDuration,
            title = title,
            message = message,
            requestTimeStamp = requestTimeStamp,
            accepted = accepted
        )
    }

    fun Sandesh.toParsedTextSandesh(bubbleType: BubbleType): ParsedTextSandesh { // 🆕 Added parameter
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val paath = jsonObject.get(DootawasMessage.PAATH).asString
        val hasLink = jsonObject.get(DootawasMessage.IS_LINK)?.asBoolean == true

        val msgType = when {
            hasLink -> MSG_LINK
            isSingleEmoji(paath) -> MSG_SINGLE_EMOJI
            else -> MSG_TEXT
        }

        val replyMessage = jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        val linkTitle =
            if (msgType == MSG_LINK) jsonObject.get(DootawasMessage.LINK_TITLE)?.asString else null
        val linkUri =
            if (msgType == MSG_LINK) jsonObject.get(DootawasMessage.LINK_URI)?.asString else null

        return ParsedTextSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            msgType = msgType,
            bubbleType = bubbleType, // 🆕 Pass it down here
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            sandeshStithi = sandeshStithi,
            paath = paath,
            replyOriginalMessage = replyMessage,
            linkTitle = linkTitle,
            linkUri = linkUri
        )
    }

    fun Sandesh.toParsedImageSandesh(): ParsedImageSandesh {
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val replyOriginalMessage =
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        // ✅ Extract ALL images
        val dataArray = jsonObject.getAsJsonArray(DootawasMessage.ALL_DATA)
        val imageUris = dataArray?.mapNotNull {
            it.asJsonObject
                .get(DootawasMessage.ORIGINAL_URI)
                ?.asString
        } ?: emptyList()
        // ✅ First image for preview
        val firstUri = imageUris.firstOrNull()
        return ParsedImageSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedTime = buildFormattedTime(jsonObject, time),
            formattedDate = getRelativeTimeSpanMinuteLevel(time),

            imageUris = imageUris,        // ✅ full list
            displayUri = firstUri,        // ✅ preview = first image
            blurUri = firstUri,           // ✅ same as you decided
            paath = jsonObject.get(DootawasMessage.PAATH)?.asString ?: "",
            downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0,
            downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "100kb",
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )
    }


    fun Sandesh.toParsedVideoSandesh(): ParsedVideoSandesh {

        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val videoUris = jsonObject
            .getAsJsonArray(DootawasMessage.ALL_DATA)
            ?.mapNotNull {
                it.asJsonObject
                    .get(DootawasMessage.ORIGINAL_URI)
                    ?.asString
            } ?: emptyList()

        val previewUris = jsonObject
            .getAsJsonArray(DootawasMessage.IMAGE_PREVIEW)
            ?.mapNotNull {
                it.asString
            } ?: emptyList()

        val blurUri =
            jsonObject.get(DootawasMessage.BLUR_URI)?.asString

        val downloadPercent =
            jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0

        val downloadSize =
            jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"

        val replyOriginalMessage =
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        return ParsedVideoSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            paath = jsonObject.get(DootawasMessage.PAATH)?.asString ?: "",
            videoUris = videoUris,
            previewUris = previewUris,
            blurUri = blurUri,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )
    }

    fun Sandesh.toParsedAudioSandesh(): ParsedAudioSandesh? {
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
        val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
        val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"
        val duration = jsonObject.get(DootawasMessage.DURATION)?.asLong ?: 0L

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage =
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject

        return ParsedAudioSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            duration = duration
        )

    }

    fun Sandesh.toParsedDocumentSandesh(): ParsedDocumentSandesh? {
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
        val mimeType =
            jsonObject.get(DootawasMessage.MIME_TYPE)?.asString ?: "application/octet-stream"
        val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
        val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage = if (isReply) {
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        } else null

        return ParsedDocumentSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            mimeType = mimeType,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )

    }


    fun Sandesh.toParsedContactSandesh(): ParsedContactSandesh? {
        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val contactArray = jsonObject.getAsJsonArray(DootawasMessage.CONTACTS) ?: JsonArray()
        val contacts = contactArray.mapNotNull { element ->
            try {
                val obj = element.asJsonObject
                val name = obj.get(DootawasMessage.CONTACT_NAME)?.asString ?: "Unknown"
                val phone = obj.get(DootawasMessage.NUMBER)?.asString ?: "N/A"
                ContactInfo(name, phone)
            } catch (e: Exception) {
                null  // Skip invalid entries
            }
        }

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage = if (isReply) {
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        } else null

        return ParsedContactSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            contacts = contacts,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )
    }

    private val _liveLocationItems =
        MutableStateFlow<Map<String, LiveLocationItem>>(
            emptyMap()
        )

    val liveLocationItems = _liveLocationItems.asStateFlow()
    private val _liveRoutes =
        MutableStateFlow<Map<String, List<LatLng>>>(
            emptyMap()
        )

    val liveRoutes =
        _liveRoutes.asStateFlow()

    fun Sandesh.toParsedLocationSandesh(): ParsedLocationSandesh? {

        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
        val latitude = jsonObject.get(DootawasMessage.LATITUDE)?.asDouble ?: return null
        val longitude = jsonObject.get(DootawasMessage.LONGITUDE)?.asDouble ?: return null
        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage =
            if (isReply) jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject else null
        val isLive = jsonObject.get(DootawasMessage.IS_LIVE)?.asBoolean ?: false
        val liveUntil = try {
            jsonObject.get(DootawasMessage.LIVE_UNTIL)
                ?.takeIf { !it.isJsonNull && it.asString.isNotBlank() }
                ?.asLong
        } catch (e: Exception) {
            null
        }
        val isStopped = jsonObject.get(DootawasMessage.IS_STOPPED)?.asBoolean ?: false

        if (isLive && !isStopped && (liveUntil == null || liveUntil > System.currentTimeMillis())) {

            updateLiveLocationItem(
                LiveLocationItem(
                    sandeshId = sandeshId,
                    latitude = latitude,
                    longitude = longitude,
                    isLive = true,
                    displayName = upbhogta.value?.contactName?.takeIf { it.isNotBlank() }
                        ?: upbhogta.value?.userNumber ?: userNumberKhud,
                    profileUri = upbhogta.value?.photoUrl?.toUri()
                )
            )
        }
        return ParsedLocationSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = getRelativeTimeSpanMinuteLevel(time),
            formattedTime = buildFormattedTime(jsonObject, time),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            latitude = latitude,
            longitude = longitude,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            isLive = isLive,
            liveUntil = liveUntil,
            isStopped = isStopped
        )
    }

    private fun observeTyping(userNumber: String) {
        viewModelScope.launch {
            TypingManager.typingStates.collect { states ->

                isTyping.value = states.values.any { state ->
                    state.userNumber == userNumber && state.isTyping
                }
            }
        }
    }


    fun deleteSelectedMessages(
        selectedMessageIds: Set<String>,
        userNumber: String,
        deleteBoth: Boolean
    ) {
        viewModelScope.launch {
            repository.deleteMessages(
                selectedMessageIds,
                userNumber,
                deleteBoth
            ) // ✅ Call repository to delete
        }
    }

    fun downloadFile(sandeshId: String, userNumber: String) {
        repository.downloadFile(sandeshId, userNumber)
    }

    fun generateUniqueSandeshId(): String {
        val currentParsedItems = _parsedSandeshItems.value
        var uniqueSandeshId: String
        do {
            uniqueSandeshId = UUID.randomUUID().toString()
        } while (currentParsedItems.any { it.sandeshId == uniqueSandeshId })
        return uniqueSandeshId
    }

    fun sendLedgerSandesh(
        userNumbers: List<String>,
        ledgerJson: JsonObject
    ) {
        viewModelScope.launch {
            val amount = ledgerJson.get(DootawasMessage.LEDGER_AMOUNT)?.asDouble ?: 0.0
            val newBalance = ledgerJson.get(DootawasMessage.LEDGER_NEW_BALANCE)?.asDouble ?: 0.0
            val note = ledgerJson.get("note")?.asString ?: ""

            val type = LedgerType.fromInt(ledgerJson.get(DootawasMessage.LEDGER_TYPE)?.asInt ?: 0)

            val summaryText = when (type) {
                LedgerType.BILL -> "📄 Bill: ₹%.2f".format(amount)
                LedgerType.CREDIT -> "➕ Added ₹%.2f to balance".format(amount)
                LedgerType.DEBIT -> "➖ Reduced balance by ₹%.2f".format(amount)
                LedgerType.ADJUSTMENT -> "⚖️ Balance adjusted to ₹%.2f".format(newBalance)
                LedgerType.LEDGER_ACK_DEBIT -> "✓ Acknowledged"
                LedgerType.LEDGER_ACK_BILL_CREDIT -> "✓ Acknowledged"
            }


            ledgerJson.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LEDGER.value)

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                repository.insertSandesh(
                    Sandesh(
                        sandeshId = sandeshId,
                        userNumber = receiver,
                        samagri = ledgerJson.toString(),
                        isSelf = true,
                        time = Instant.now().toEpochMilli(),
                        sandeshType = SandeshType.LEDGER.value,
                        sandeshStithi = SandeshStithi.LIKHIT
                    )
                )
                repository.sendSandesh(
                    UnsentSandesh(
                        messId = sandeshId,
                        receiver = receiver,
                        message = ledgerJson.toString(),
                        dataType = DataType.MESSAGE
                    )
                )
                repository.updateLastMessageToTop(receiver, summaryText, sandeshId)
            }
        }
    }

    //todo we dont need to add SANDESH_TYPE TO Sandesh but need to UnsentSandesh
    fun sendMessage(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Add message type

            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.MESSAGE.value)
            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.MESSAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )

                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = receiver,
                    message = message.toString(),
                    dataType = if (!isAi) DataType.MESSAGE else DataType.AI_MESSAGE
                )

                repository.insertSandesh(newSandesh)
                repository.sendSandesh(unsentSandesh)

                val paathValue: String = message.get(DootawasMessage.PAATH).asString
                repository.updateLastMessageToTop(receiver, paathValue, sandeshId)

                // Optionally: fetchMessages(userNumber)

                if (message.get(DootawasMessage.IS_LINK)?.asBoolean == true) {
                    // cleaner

                    repository.storeMessagePreview(message, sandeshId, false)
                }
            }
        }
    }

    // --- CONDITION 1: CREATE (Save locally + Send over network) ---
    fun sendList(targetSandeshId: String, userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)

            userNumbers.forEach { receiver ->
                val newSandesh = Sandesh(
                    sandeshId = targetSandeshId, // Keep tracking context id bound upstream
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.LIST.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )

                val unsentSandesh = UnsentSandesh(
                    messId = targetSandeshId,
                    receiver = receiver,
                    message = message.toString(),
                    dataType = DataType.MESSAGE
                )

                repository.insertSandesh(newSandesh)
                repository.sendSandesh(unsentSandesh)
                repository.updateLastMessageToTop(receiver, "List", targetSandeshId)
            }
        }
    }


    // --- CONDITION 2: UPDATE (Mutate message payload in-place) ---
    fun modifyListLocally(targetSandeshId: String, userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            // 1. Append your standard system core keys required for validation parsers
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)
            message.addProperty(JsonKeys.TIME_STAMP, Instant.now().toEpochMilli().toString())

            // 2. Perform the specialized database mutation in-place via the DAO
            repository.updateSandeshSamagri(
                sandeshId = targetSandeshId,
                samagri = message.toString()
            )

            // 3. Optional: Move the chat row back to the top of the conversation list hierarchy
            repository.updateLastMessageToTop(userNumber, "List updated", targetSandeshId)
        }
    }

    // --- CONDITION 3: SEND UPDATE (Send over network only) ---
    fun sendListUpdateOnly(
        targetSandeshId: String,
        userNumbers: List<String>,
        message: JsonObject
    ) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)

            userNumbers.forEach { receiver ->
                val unsentSandesh = UnsentSandesh(
                    messId = targetSandeshId,
                    receiver = receiver,
                    message = message.toString(),
                    dataType = DataType.MESSAGE
                )

                // Only fire across the socket layer engine
                repository.sendSandesh(unsentSandesh)
            }
        }
    }


    fun replyMessage(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true,
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_MESSAGE.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_MESSAGE.value)
            val unsentSandesh = UnsentSandesh(
                messId = newSandesh.sandeshId,
                receiver = userNumber,
                message = message.toString(),
                dataType = DataType.MESSAGE
            )

            repository.insertSandesh(newSandesh)
            repository.sendSandesh(unsentSandesh)
            repository.updateLastMessageToTop(userNumber, "Replied: MESSAGE", sandeshId)
//            fetchMessages(userNumber)
        }
    }

    fun sendImage(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Add sandesh type to message

            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.IMAGE.value)

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.IMAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
                repository.insertSandesh(newSandesh)


//                repository.sendSandesh(unsentSandesh)
                // Create UnsentFiles entry with JSON arrays
                val unsentData = UnsentFiles(
                    messageId = sandeshId,  // JSON string of all IDs
                    receiver = receiver,    // JSON string of all receivers
                    data = message.toString(),
                    dataType = FileDataType.IMAGE
                )

                // Insert or update unsent data
                repository.sendImage(unsentData)


                // Optionally update last message preview
                repository.updateLastMessageToTop(receiver, "IMAGE", sandeshId)
            }
        }
    }

    fun sendReplyImage(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true, // ✅ Always true for self-messages
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_IMAGE.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_IMAGE.value)
            val unsentData = UnsentFiles(
                messageId = newSandesh.sandeshId,
                receiver = userNumber,
                data = message.toString(),
                dataType = FileDataType.REPLY_IMAGE
            )

            repository.insertSandesh(newSandesh)
            repository.sendImage(unsentData)
            repository.updateLastMessageToTop(userNumber, "Replied: IMAGE", sandeshId)
//            fetchMessages(userNumber)
        }
    }

    fun sendVideo(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {
            val now = Instant.now().toEpochMilli()
            // Add sandesh type to message
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.VIDEO.value)

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = now,
                    sandeshType = SandeshType.VIDEO.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )

                // ✅ Insert sandesh immediately
                repository.insertSandesh(newSandesh)
                repository.generateAndSaveThumbnailInternal(message, sandeshId, receiver, false)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = receiver,
                    data = message.toString(),
                    dataType = FileDataType.VIDEO
                )

                repository.sendVideo(unsentData)

                // Optionally update last message preview once
                repository.updateLastMessageToTop(receiver, "VIDEO", sandeshId)
            }

        }
    }

    fun sendReplyVideo(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true, // ✅ Always true for self-messages
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_VIDEO.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_VIDEO.value)
            val unsentData = UnsentFiles(
                messageId = newSandesh.sandeshId,
                receiver = userNumber,
                data = message.toString(),
                dataType = FileDataType.REPLY_VIDEO
            )

            repository.insertSandesh(newSandesh)
            repository.generateAndSaveThumbnailInternal(message, sandeshId, userNumber, false)
            repository.sendVideo(unsentData)
            repository.updateLastMessageToTop(userNumber, "Replied: VIDEO", sandeshId)
//            fetchMessages(userNumber)
        }
    }


    fun sendDocument(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {

            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.DOCUMENT.value)

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.DOCUMENT.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
                repository.insertSandesh(newSandesh)


                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = receiver,
                    data = message.toString(),
                    dataType = FileDataType.DOCUMENT
                )

                repository.sendDocument(unsentData)
                repository.updateLastMessageToTop(receiver, "DOCUMENT", sandeshId)
            }

        }
    }

    fun sendReplyDocument(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true, // ✅ Always true for self-messages
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_DOCUMENT.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )

            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_DOCUMENT.value)

            val unsentData = UnsentFiles(
                messageId = newSandesh.sandeshId,
                receiver = userNumber,
                data = message.toString(),
                dataType = FileDataType.REPLY_DOCUMENT
            )

            repository.insertSandesh(newSandesh)
            repository.sendDocument(unsentData)
            repository.updateLastMessageToTop(userNumber, "Replied: DOCUMENT", sandeshId)
        }
    }


    fun sendLocation(userNumbers: List<String>, message: JsonObject, context: Context? = null) {
        viewModelScope.launch {

            // Add sandesh type to message
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LOCATION.value)


            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()
                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.LOCATION.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
                repository.insertSandesh(newSandesh)

                // Create UnsentFiles entry with JSON arrays
                val unsentData = UnsentFiles(
                    messageId = sandeshId,  // JSON string of all IDs
                    receiver = receiver,    // JSON string of all receivers
                    data = message.toString(),
                    dataType = FileDataType.LOCATION
                )
                // Insert or update unsent data
                repository.sendLocation(unsentData)

                // Optionally update last message preview
                repository.updateLastMessageToTop(receiver, "LOCATION", sandeshId)
                if (
                    context != null &&
                    message.get(DootawasMessage.IS_LIVE)
                        ?.asBoolean == true
                ) {
                    LiveLocationService.addLiveLocation(
                        context = context,
                        sandeshId = sandeshId,
                        targetId = receiver,
                        isGroup = false,
                        expiresAt = message.get(DootawasMessage.LIVE_UNTIL)?.asLong
                    )
                }
            }
        }
    }

    fun replyLocation(userNumber: String, message: JsonObject, context: Context) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true,
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_LOCATION.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_LOCATION.value)

            // Create UnsentFiles entry with JSON arrays
            val unsentData = UnsentFiles(
                messageId = newSandesh.sandeshId,
                receiver = userNumber,
                data = message.toString(),
                dataType = FileDataType.REPLY_LOCATION
            )
            repository.insertSandesh(newSandesh)
            repository.sendLocation(unsentData)
            repository.updateLastMessageToTop(userNumber, "Replied: LOCATION", sandeshId)

            if (
                message.get(DootawasMessage.IS_LIVE)
                    ?.asBoolean == true
            ) {


                LiveLocationService.addLiveLocation(
                    context = context,
                    sandeshId = sandeshId,
                    targetId = userNumber,
                    isGroup = false,
                    expiresAt = message.get(DootawasMessage.LIVE_UNTIL)?.asLong
                )
            }
        }
    }


    fun sendContact(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.CONTACT.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
                message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.CONTACT.value)

                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = receiver,
                    message = message.toString(),
                    dataType = DataType.MESSAGE // ✅ Confirm if you want MESSAGE type for contact, or separate CONTACT type
                )

                repository.insertSandesh(newSandesh)
                repository.sendSandesh(unsentSandesh)
                repository.updateLastMessageToTop(receiver, "CONTACT", sandeshId)
            }
        }
    }


    fun sendReplyContact(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true, // ✅ Always true for self-messages
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_CONTACT.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_CONTACT.value)
            val unsentSandesh = UnsentSandesh(
                messId = newSandesh.sandeshId,
                receiver = userNumber,
                message = message.toString(),
                dataType = DataType.MESSAGE
            )

            repository.insertSandesh(newSandesh)
            repository.sendSandesh(unsentSandesh)
            repository.updateLastMessageToTop(userNumber, "Replied: CONTACT", sandeshId)
//            fetchMessages(userNumber)
        }
    }


    fun sendAudio(userNumbers: List<String>, message: JsonObject) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.AUDIO.value)

            userNumbers.forEach { receiver ->
                val sandeshId = generateUniqueSandeshId()

                val newSandesh = Sandesh(
                    sandeshId = sandeshId,
                    userNumber = receiver,
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.AUDIO.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
                repository.insertSandesh(newSandesh)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = receiver,
                    data = message.toString(),
                    dataType = FileDataType.AUDIO
                )

                repository.sendAudio(unsentData)
                repository.updateLastMessageToTop(receiver, "AUDIO", sandeshId)
            }
        }
    }


    fun sendReplyAudio(userNumber: String, message: JsonObject) {
        viewModelScope.launch {
            val sandeshId = generateGroupUniqueSandeshId()
            val newSandesh = Sandesh(
                sandeshId = sandeshId,
                userNumber = userNumber,
                samagri = message.toString(),
                isSelf = true, // ✅ Always true for self-messages
                time = Instant.now().toEpochMilli(), // ✅ Instant-based timestamp
                sandeshType = SandeshType.REPLY_AUDIO.value,
                sandeshStithi = SandeshStithi.LIKHIT
            )
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_AUDIO.value)
            val unsentData = UnsentFiles(
                messageId = newSandesh.sandeshId,
                receiver = userNumber,
                data = message.toString(),
                dataType = FileDataType.REPLY_AUDIO
            )

            repository.insertSandesh(newSandesh)
            repository.sendAudio(unsentData)
            repository.updateLastMessageToTop(userNumber, "Replied: AUDIO", sandeshId)
        }
    }


    // In ViewModel
    fun processDhrist() {
        viewModelScope.launch {
            repository.processDhrist(userNumberOrGroupId)
        }
    }

    private var lastTypingSentTime = 0L

    fun sendTyping(userNumber: String) {
        if (isAi) return
        val now = System.currentTimeMillis()
        if (now - lastTypingSentTime < 1000) {
            // Ignore if last event was less than 1 sec ago
            return
        }
        lastTypingSentTime = now

        viewModelScope.launch {
            repository.sendTypingEvent(userNumber) // Your repo method to send typing event
        }
    }

    fun processForward(
        selectedMessageIds: List<String>,
        selectedTargets: List<Pair<String, Boolean>> // (id, isGroup)
    ) {


        viewModelScope.launch {
            selectedMessageIds.forEach { sandeshId ->
                selectedTargets.forEach { (targetId, isGroup) ->

                    // ✅ Fetch message depending on type
                    val original = if (isGroupPassed) {
                        repository.getGroupSandeshByIdAndGroupId(sandeshId, userNumberOrGroupId)
                    } else {
                        repository.getSingleSandeshByUserAndId(sandeshId, userNumberOrGroupId)
                    }

                    if (original == null) {
                        Log.w(
                            "Forwarding",
                            "❌ Message $sandeshId not found for ${if (isGroup) "group" else "user"} $targetId"
                        )
                        return@forEach
                    }

                    // ✅ Extract fields safely (both Sandesh & GroupSandesh have same structure)
                    val samagri: String
                    val sandeshType: Int

                    when (original) {
                        is Sandesh -> {
                            samagri = original.samagri
                            sandeshType = original.sandeshType
                        }

                        is GroupSandesh -> {
                            samagri = original.samagri
                            sandeshType = original.sandeshType
                        }

                        else -> {
                            Log.w("Forwarding", "❌ Unknown message type for $sandeshId")
                            return@forEach
                        }
                    }

                    val originalJson = JsonParser.parseString(samagri).asJsonObject
                    originalJson.remove(DootawasMessage.ORIGINAL_MESSAGE)

                    val type = SandeshType.fromInt(sandeshType)

                    // ✅ Call correct sending method
                    when (type) {
                        SandeshType.MESSAGE,
                        SandeshType.REPLY_MESSAGE -> {
                            if (isGroup) sendMessageToGroups(listOf(targetId), originalJson)
                            else sendMessage(listOf(targetId), originalJson)
                        }

                        SandeshType.IMAGE,
                        SandeshType.REPLY_IMAGE -> {
                            originalJson.remove(DootawasMessage.DOWNLOAD_PERCENT)
                            originalJson.remove(DootawasMessage.BLUR_URI)
                            originalJson.remove(DootawasMessage.THUMBNAIL_URI)
                            if (isGroup) sendImageToGroups(listOf(targetId), originalJson)
                            else sendImage(listOf(targetId), originalJson)
                        }

                        SandeshType.VIDEO,
                        SandeshType.REPLY_VIDEO -> {
                            originalJson.remove(DootawasMessage.DOWNLOAD_PERCENT)
                            originalJson.remove(DootawasMessage.BLUR_URI)

                            if (isGroup) sendVideoToGroups(listOf(targetId), originalJson)
                            else sendVideo(listOf(targetId), originalJson)
                        }

                        SandeshType.AUDIO,
                        SandeshType.REPLY_AUDIO -> {
                            originalJson.remove(DootawasMessage.DOWNLOAD_PERCENT)
                            if (isGroup) sendAudioToGroups(listOf(targetId), originalJson)
                            else sendAudio(listOf(targetId), originalJson)
                        }

                        SandeshType.DOCUMENT,
                        SandeshType.REPLY_DOCUMENT -> {
                            originalJson.remove(DootawasMessage.DOWNLOAD_PERCENT)
                            if (isGroup) sendDocumentToGroups(listOf(targetId), originalJson)
                            else sendDocument(listOf(targetId), originalJson)
                        }

                        SandeshType.CONTACT,
                        SandeshType.REPLY_CONTACT -> {
                            if (isGroup) sendContactToGroups(listOf(targetId), originalJson)
                            else sendContact(listOf(targetId), originalJson)
                        }

                        SandeshType.LOCATION,
                        SandeshType.REPLY_LOCATION -> {
                            originalJson.remove(DootawasMessage.DOWNLOAD_PERCENT)
                            if (isGroup) sendLocationToGroups(listOf(targetId), originalJson)
                            else sendLocation(listOf(targetId), originalJson)
                        }

                        else -> Log.w("Forwarding", "❌ Unsupported type: $type")
                    }
                }
            }
        }
    }


    fun processShareSend(
        context: Context,
        sharedText: String?,
        sharedUris: List<Uri>?,
        selectedTargets: List<Pair<String, Boolean>>,
        caption: String?
    ) {

        viewModelScope.launch {
            selectedTargets.forEach { (targetId, isGroup) ->

                // fetch once per target, only needed for non-group
                val disappearDuration = if (!isGroup) {
                    repository.getUserDisappearDuration(targetId)
                } else null

                // ✅ CASE 1 — FILE / MEDIA SHARE
                if (!sharedUris.isNullOrEmpty()) {

                    val imageArray = JsonArray()
                    val videoArray = JsonArray()

                    var totalImageDownloadSize = 0L
                    var totalVideoDownloadSize = 0L
                    sharedUris.forEach { uri ->

                        val mimeType = context.contentResolver.getType(uri) ?: ""

                        when {

                            // ✅ IMAGE
                            mimeType.startsWith("image/") -> {
                                val savedMedia = saveMediaToAppStorageNext(
                                    context, uri, MediaCategory.IMAGE
                                ) ?: return@forEach

                                totalImageDownloadSize += savedMedia.fileSize
                                val imageObject = JsonObject().apply {
                                    addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                                    addProperty(DootawasMessage.MIME_TYPE, savedMedia.mimeType)
                                }
                                imageArray.add(imageObject)
                            }

                            // ✅ VIDEO
                            mimeType.startsWith("video/") -> {
                                val savedMedia = saveMediaToAppStorageNext(
                                    context, uri, MediaCategory.VIDEO
                                ) ?: return@forEach

                                totalVideoDownloadSize += savedMedia.fileSize
                                videoArray.add(
                                    JsonObject().apply {
                                        addProperty(
                                            DootawasMessage.ORIGINAL_URI,
                                            savedMedia.localPath
                                        )
                                        addProperty(DootawasMessage.MIME_TYPE, savedMedia.mimeType)
                                    }
                                )
                            }

                            // ✅ AUDIO
                            mimeType.startsWith("audio/") -> {
                                val savedMedia =
                                    saveMediaToAppStorageNext(context, uri, MediaCategory.AUDIO)
                                val duration = getAudioDuration(context, uri)

                                val json = JsonObject().apply {
                                    addProperty(DootawasMessage.ORIGINAL_URI, savedMedia?.localPath)
                                    addProperty(DootawasMessage.MIME_TYPE, savedMedia?.mimeType)
                                    addProperty(DootawasMessage.DOWNLOAD_SIZE, savedMedia?.fileSize)
                                    addProperty(DootawasMessage.DURATION, duration)
                                    addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                                    if (!isGroup) {
                                        addProperty(
                                            DootawasMessage.DISAPPEAR_DURATION,
                                            disappearDuration
                                        )
                                    }
                                }

                                if (isGroup) sendAudioToGroups(listOf(targetId), json)
                                else sendAudio(listOf(targetId), json)
                            }

                            // ✅ DOCUMENT
                            else -> {
                                val fileName = getFileName(context, uri)
                                val savedMedia =
                                    saveMediaToAppStorageNext(context, uri, MediaCategory.DOCUMENT)
                                val previewUri = saveFirstPageAsImage(context, uri)

                                val json = JsonObject().apply {
                                    addProperty(DootawasMessage.ORIGINAL_URI, savedMedia?.localPath)
                                    addProperty(DootawasMessage.MIME_TYPE, savedMedia?.mimeType)
                                    addProperty(DootawasMessage.DOWNLOAD_SIZE, savedMedia?.fileSize)
                                    addProperty(
                                        DootawasMessage.DUPLICATE_URI,
                                        previewUri.toString()
                                    )
                                    addProperty(DootawasMessage.FILE_NAME, fileName)
                                    if (!isGroup) {
                                        addProperty(
                                            DootawasMessage.DISAPPEAR_DURATION,
                                            disappearDuration
                                        )
                                    }
                                }

                                if (isGroup) sendDocumentToGroups(listOf(targetId), json)
                                else sendDocument(listOf(targetId), json)
                            }
                        }
                    }

                    // ✅ SEND GROUPED IMAGES
                    if (imageArray.size() > 0) {
                        val groupedImages = JsonObject().apply {
                            addProperty(DootawasMessage.DATA_COUNT, imageArray.size())
                            add(DootawasMessage.ALL_DATA, imageArray)
                            addProperty(DootawasMessage.DOWNLOAD_SIZE, totalImageDownloadSize)
                            addProperty(DootawasMessage.PAATH, caption)
                            if (!isGroup) {
                                addProperty(DootawasMessage.DISAPPEAR_DURATION, disappearDuration)
                            }
                        }

                        if (isGroup) sendImageToGroups(listOf(targetId), groupedImages)
                        else sendImage(listOf(targetId), groupedImages)
                    }

                    if (videoArray.size() > 0) {
                        val groupedVideos = JsonObject().apply {
                            addProperty(DootawasMessage.DATA_COUNT, videoArray.size())
                            add(DootawasMessage.ALL_DATA, videoArray)
                            addProperty(DootawasMessage.DOWNLOAD_SIZE, totalVideoDownloadSize)
                            addProperty(DootawasMessage.PAATH, caption)
                            if (!isGroup) {
                                addProperty(DootawasMessage.DISAPPEAR_DURATION, disappearDuration)
                            }
                        }

                        if (isGroup) sendVideoToGroups(listOf(targetId), groupedVideos)
                        else sendVideo(listOf(targetId), groupedVideos)
                    }
                }

                // ✅ CASE 2 — TEXT OR LINK SHARE
                if (!sharedText.isNullOrBlank()) {
                    val json = JsonObject().apply {
                        addProperty(DootawasMessage.IS_LINK, true)
                        addProperty(DootawasMessage.PAATH, sharedText)
                        if (!isGroup) {
                            addProperty(DootawasMessage.DISAPPEAR_DURATION, disappearDuration)
                        }
                    }

                    if (isGroup) sendMessageToGroups(listOf(targetId), json)
                    else sendMessage(listOf(targetId), json)
                }
            }
        }
    }

    fun cleanNotification() {
        viewModelScope.launch {
            repository.removeUserFromNotification(userNumberOrGroupId)
        }
    }

    fun reactToMsg(emoji: String, msgId: String) {
        viewModelScope.launch {

            val messageSamagri = JsonObject().apply {
                addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, msgId)
                addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REACTION.value)
                addProperty(DootawasMessage.REACTION, emoji)
            }
            val sandeshId = generateUniqueSandeshId()
            val unsentSandesh = UnsentSandesh(
                messId = sandeshId,
                receiver = userNumberOrGroupId,
                message = messageSamagri.toString(),
                dataType = DataType.MESSAGE
            )
            repository.reactToMsg(emoji, msgId, isGroupPassed, userNumberOrGroupId)
            repository.sendSandesh(unsentSandesh)

            delay(250)
            if (isGroupPassed) {
                loadGroupReactions(msgId)
            } else {
                loadReactions(msgId)   // 🔥 refresh reactions
            }
        }
    }


    fun saveFirstPageAsImage(
        context: Context,
        docUri: Uri
    ): Uri? {
        return repository.saveFirstPageAsImage(context, docUri)
    }


    private fun copyDocumentToAppStorage(
        context: Context,
        uri: Uri,
        fileName: String
    ): String {

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: throw IllegalStateException("External storage unavailable")

        val file = File(directory, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    private fun copyToAppMediaStorage(
        context: Context,
        uri: Uri
    ): String {

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType) ?: "dat"

        val fileName = "media_${System.currentTimeMillis()}.$extension"

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IllegalStateException("External storage unavailable")

        val file = File(directory, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    private fun getAudioDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun resetDownloadPercentage(sandeshId: String) {
        viewModelScope.launch {
            repository.resetDownloadPercentage(sandeshId, userNumberOrGroupId)
        }
    }

    private val _groupSelectedSandesh = MutableStateFlow<GroupSandesh?>(null)
    val groupSelectedSandesh: StateFlow<GroupSandesh?> = _groupSelectedSandesh

    private val _groupReplyToType = MutableStateFlow<SandeshType?>(null)
    val groupReplyToType: StateFlow<SandeshType?> = _groupReplyToType

    private val _groupParsedSandeshItems =
        MutableStateFlow<List<GroupParsedSandeshItem>>(emptyList())
    val groupParsedSandeshItems: StateFlow<List<GroupParsedSandeshItem>> = _groupParsedSandeshItems
        .asStateFlow()

    private val _allGroupSandeshList = MutableStateFlow<List<GroupSandesh>>(emptyList())


    private val _groupMembers =
        MutableStateFlow<List<GroupUserProfile>>(emptyList())

    val groupMembers: StateFlow<List<GroupUserProfile>> =
        _groupMembers.asStateFlow()


    private val _groupMembersMap = MutableStateFlow<Map<String, GroupUserProfile>>(emptyMap())


    val groupParsedList = groupParsedSandeshItems.map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupImageList = groupParsedList.map { list ->
        list.filterIsInstance<ParsedImageSandesh>()
            .mapNotNull { it.displayUri }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _upbhogta = MutableStateFlow<Upbhogta?>(null)
    val upbhogta: StateFlow<Upbhogta?> = _upbhogta

    var userNumberKhud by mutableStateOf("")
        private set

    private val _borderMode =
        MutableStateFlow(BorderMode.NONE)

    val borderMode =
        _borderMode.asStateFlow()
    private val _lightColors =
        MutableStateFlow(LightDootawasColors)

    val lightColors =
        _lightColors.asStateFlow()

    private val _darkColors =
        MutableStateFlow(DarkDootawasColors)

    // 🌟 ADD THIS: State tracking for the bubble design variation
    private val _useModernStyle =
        MutableStateFlow(false)

    val useModernStyle =
        _useModernStyle.asStateFlow()

    val darkColors =
        _darkColors.asStateFlow()
    private val gson = Gson()
    private fun applyTheme(
        user: Upbhogta?
    ) {
        val json = user?.customColors

        // Fallback if no custom configuration exists yet
        if (json == null) {
            _lightColors.value = LightDootawasColors
            _darkColors.value = DarkDootawasColors
            _borderMode.value = BorderMode.NONE
            _useModernStyle.value = false // 👈 Reset to classic layout default
            return
        }

        try {
            val bundle = gson.fromJson(
                json,
                StoredThemeBundle::class.java
            )

            _lightColors.value = bundle.light.toCompose()
            _darkColors.value = bundle.dark.toCompose()
            _borderMode.value = BorderMode.entries[bundle.borderMode]

            // 🌟 Apply the extracted design choice to your UI state flow
            _useModernStyle.value = bundle.useModernStyle

        } catch (e: Exception) {
            e.printStackTrace()

            // Safe fallback values if JSON parsing corrupts or errors out
            _lightColors.value = LightDootawasColors
            _darkColors.value = DarkDootawasColors
            _borderMode.value = BorderMode.NONE
            _useModernStyle.value = false // 👈 Safe fallback default
        }
    }

    fun saveCurrentTheme(
        isDark: Boolean,
        colors: DootawasColors,
        borderMode: BorderMode,
        useModernStyle: Boolean // 👈 Added signature parameter
    ) {
        viewModelScope.launch {

            // update memory instantly
            if (isDark) {
                _darkColors.value = colors
            } else {
                _lightColors.value = colors
            }

            _borderMode.value = borderMode

            // Ensure you update your state flow here so the main chat view live updates
            _useModernStyle.value = useModernStyle

            repository.updateUserTheme(
                userNumber = userNumberOrGroupId,
                light = _lightColors.value,
                dark = _darkColors.value,
                borderMode = borderMode,
                useModernStyle = useModernStyle, // 👈 Update your repository persistence model
                uuid = generateUniqueSandeshId()
            )
        }
    }

    private fun observeUser(
        number: String
    ) {

        viewModelScope.launch {

            repository.observeUser(number)
                .collect { user ->

                    _upbhogta.value = user

                    applyTheme(user)
                }
        }
    }

    init {
        if (isGroupPassed) {
            observeGroupData(userNumberOrGroupId)
            observeGroupEntity(userNumberOrGroupId)
        } else {
            observeUser(userNumberOrGroupId)   // 👈 ADD THIS
            fetchMessages(userNumberOrGroupId)
        }



        observeTyping(userNumberOrGroupId)
        viewModelScope.launch {
            _fontSizeFlow.value = repository.getFontSizeFlow().value
            repository.getFontSizeFlow().collect {
                _fontSizeFlow.value = it
            }
        }
        viewModelScope.launch {
            _chatBehaviourFlow.value = repository.chatBehaviourFlow().value
            repository.chatBehaviourFlow().collect {
                _chatBehaviourFlow.value = it
            }
        }
        viewModelScope.launch {
            userNumberKhud = knotx.userNumberKhud.firstOrNull().orEmpty()
        }

        viewModelScope.launch {

            repository.liveRouteFlow.collect {

                updateLiveRoute(
                    sandeshId = it.first,
                    polyline = it.second
                )
            }
        }
        viewModelScope.launch {
            repository.cleanupExpiredDisappearingMessages(userNumberOrGroupId)
        }
//        loadTestMessages()
    }


    private fun loadTestMessages() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            SandeshTestData.generate(userNumberOrGroupId, 100).forEach { sandesh ->
                repository.insertSandesh(sandesh)
            }
        }
    }

    fun observePresence(userNumber: String): StateFlow<Boolean?> =
        repository
            .observePresence(userNumber)
            .catch { }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )


    suspend fun getSingleGroupMessage(
        sandeshId: String,
        groupId: String
    ): String? {
        return repository.getGroupSandeshByIdAndGroupId(sandeshId, groupId)?.samagri
    }

    suspend fun getSingleMessage(
        sandeshId: String,
        groupId: String
    ): String? {
        return repository.getSingleSandeshByUserAndId(sandeshId, groupId)?.samagri
    }

    private val _reactionsFlow = MutableStateFlow<List<ReactionUi>>(emptyList())
    val reactionsFlow: StateFlow<List<ReactionUi>> = _reactionsFlow


    fun loadReactions(
        sandeshId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            val message = repository
                .getSingleSandeshByUserAndId(sandeshId, userNumberOrGroupId)
                ?: return@launch

            try {
                val json = JsonParser.parseString(message.samagri).asJsonObject

                if (!json.has("reactions") || !json.get("reactions").isJsonObject) {
                    _reactionsFlow.value = emptyList()
                    return@launch
                }

                val reactionsJson = json.getAsJsonObject("reactions")

                val list = reactionsJson.entrySet().map {
                    ReactionUi(
                        userId = it.key,      // "self" or "other"
                        emoji = it.value.asString
                    )
                }

                _reactionsFlow.value = list

            } catch (e: Exception) {
                _reactionsFlow.value = emptyList()
            }
        }
    }

    suspend fun getMessageStatusInfo(
        userNumber: String,
        sandeshId: String
    ): SandeshStatusInfo = withContext(Dispatchers.IO) {
        val json = repository.getStatusJson(userNumber, sandeshId)
        parseStatusJson(json)
    }

    private val _groupReactionsFlow = MutableStateFlow<List<ReactionUi>>(emptyList())
    val groupReactionsFlow: StateFlow<List<ReactionUi>> = _groupReactionsFlow

    fun loadGroupReactions(
        sandeshId: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            val message = repository
                .getGroupSandeshByIdAndGroupId(sandeshId, userNumberOrGroupId)
                ?: return@launch

            try {
                val json = JsonParser.parseString(message.samagri).asJsonObject

                if (!json.has("reactions") || !json.get("reactions").isJsonObject) {
                    _groupReactionsFlow.value = emptyList()
                    return@launch
                }

                val reactionsJson = json.getAsJsonObject("reactions")

                val list = reactionsJson.entrySet().map {
                    ReactionUi(
                        userId = it.key,       // self or phone number
                        emoji = it.value.asString
                    )
                }

                _groupReactionsFlow.value = list

            } catch (e: Exception) {
                _groupReactionsFlow.value = emptyList()
            }
        }
    }

    suspend fun getSingleMessageStatusInfo(
        userNumber: String,
        sandeshId: String
    ): SingleMessageStatus = withContext(Dispatchers.IO) {
        val json = repository.getStatusJson(userNumber, sandeshId)
        parseSingleStatusJson(json)
    }

    fun formatDateTime(timestamp: Long): String {
        val date = getRelativeTimeSpanMinuteLevel(timestamp)  // Today, Yesterday, 14 Feb 2025
        val time = formatTime(timestamp)           // 9:42 PM or 21:42

        return "$date • $time"
    }


    fun fetchSingleGroupSandesh(sandeshId: String, userNumber: String) {
        viewModelScope.launch {
            val sandesh = repository.getGroupSandeshByIdAndGroupId(sandeshId, userNumberOrGroupId)

            _groupSelectedSandesh.value = sandesh
            _groupReplyToType.value = sandesh?.sandeshType?.let { SandeshType.fromInt(it) }
        }
    }


    private val _allParsedSandeshItemsFilter =
        MutableStateFlow<List<GroupParsedSandeshItem>>(emptyList())
    private var currentGroupQuery = ""
    private var groupSearchJob: Job? = null

    private fun buildGroupItems(
        parsedMessages: List<GroupParsedSandeshItem>,
        query: String
    ): List<GroupParsedSandeshItem> {
        val q = query.trim()
        return if (q.isEmpty()) {
            addGroupDateHeaders(parsedMessages)
        } else {
            parsedMessages.filter { item ->
                item is GroupParsedTextSandesh &&
                        (item.paath.contains(q, ignoreCase = true) ||
                                item.linkTitle?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    fun filterGroupSearch(query: String) {
        currentGroupQuery = query
        groupSearchJob?.cancel()
        groupSearchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(250) // debounce typing, clear instantly

            val allParsed = _allParsedSandeshItemsFilter.value
            _groupParsedSandeshItems.value = withContext(Dispatchers.Default) {
                buildGroupItems(allParsed, query)
            }
        }
    }


    private fun buildFormattedTime(json: JsonObject, time: Long): String {

        val reactions = json.getAsJsonObject("reactions")
            ?: return formatTime(time)

        if (reactions.size() == 0) {
            return formatTime(time)
        }

        val reactionText = reactions.entrySet()
            .take(3)
            .joinToString(separator = " ") { it.value.asString }

        return "$reactionText ${formatTime(time)}"
    }

    private fun toParsedUniversalGroupSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>
    ): GroupParsedUniversalSandesh {

        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val type = jsonObject.get(DootawasMessage.UNIVERSAL_TYPE)?.asString
            ?: jsonObject.get("type")?.asString // fallback for old messages before the key rename
        val data = jsonObject.getAsJsonObject("data")

        val msgTime = jsonObject.get("time")?.asLong ?: sandesh.time

        val paath = when (type) {

            UniversalType.MEMBER_ADDED -> {
                val members = data?.getAsJsonArray("members")?.map { it.asString } ?: emptyList()

                val names = members.map { number ->
                    memberMap[number]?.userName?.takeIf { it.isNotBlank() } ?: number
                }

                if (names.size == 1) {
                    "${names.first()} joined the group"
                } else {
                    names.joinToString(", ") + " joined the group"
                }
            }

            UniversalType.MEMBER_REMOVED -> {

                val members = data
                    ?.getAsJsonArray("members")
                    ?.map { it.asString }
                    ?: emptyList()

                val names = members.map { number ->
                    memberMap[number]
                        ?.userName
                        ?.takeIf { it.isNotBlank() }
                        ?: number
                }

                if (names.size == 1) {
                    "${names.first()} was removed"
                } else {
                    names.joinToString(", ") + " were removed"
                }
            }

            UniversalType.YOU_ADDED -> {
                "You were added to the group"
            }

            UniversalType.YOU_REMOVED -> {
                "You were removed from the group"
            }

            UniversalType.GROUP_TERMINATED -> {
                "This group was terminated"
            }

            UniversalType.GROUP_UPDATED -> {
                val newName = data?.get("newName")?.asString
                if (!newName.isNullOrBlank()) {
                    "Group name changed to $newName"
                } else {
                    "Group updated"
                }
            }

            UniversalType.CALL_STARTED -> {
                val caller = data?.get("caller")?.asString
                val name = memberMap[caller]?.userName ?: caller
                "$name started a call"
            }

            UniversalType.YOU_LEFT -> {
                "You LEFT"
            }


            else -> {
                jsonObject.get("paath")?.asString ?: sandesh.samagri
            }
        }

        return GroupParsedUniversalSandesh(
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            time = msgTime,
            formattedDate = getRelativeTimeSpanMinuteLevel(msgTime),
            formattedTime = buildFormattedTime(jsonObject, msgTime),
            sandeshStithi = sandesh.sandeshStithi,
            paath = paath,
            displayNameOrNumber = ""
        )
    }

    private fun toParsedTextSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>,
        isFirstInGroup: Boolean
    ): GroupParsedTextSandesh? {
        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }


        val paath = jsonObject.get(DootawasMessage.PAATH).asString
        val hasLink = jsonObject.get(DootawasMessage.IS_LINK)?.asBoolean == true

        val msgType = when {
            hasLink -> MSG_LINK
            isSingleEmoji(paath) -> MSG_SINGLE_EMOJI
            else -> MSG_TEXT
        }


        val linkTitle =
            if (msgType == MSG_LINK) jsonObject.get(DootawasMessage.LINK_TITLE)?.asString else null

        val linkUri =
            if (msgType == MSG_LINK) jsonObject.get(DootawasMessage.LINK_URI)?.asString else null


        val replyMessage = jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject

        val member = memberMap[sandesh.userNumber]

        val displayName = member?.userName
            ?.takeIf { it.isNotBlank() }
            ?: sandesh.userNumber

        val userImageUri: Uri? = member?.profileUri


        return GroupParsedTextSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            msgType = msgType,
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(jsonObject, sandesh.time),
            sandeshStithi = sandesh.sandeshStithi,
            paath = paath,
            replyOriginalMessage = replyMessage,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0xFF6B46C1.toInt(),
            senderUserLightColor = member?.userLightColor ?: 0xFF9C27B0.toInt(),
            senderProfile = userImageUri,
            linkTitle = linkTitle,
            linkUri = linkUri,
        )
    }

    fun toParsedListSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>,
        isFirstInGroup: Boolean
    ): GroupParsedListSandesh {
        val json = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }

        // 1. Re-use type-safe configuration handles to scan keys safely
        val keyMessageType = ListMessage::messageType.name
        val keyUpdated = ListMessage::updated.name
        val keyCurrent = ListMessage::current.name

        val listMessage = try {
            if (json.has("items") || json.has("title")) {
                val parsedListData = Gson().fromJson(json, ListData::class.java) ?: ListData()
                ListMessage(
                    messageType = ListMessageType.CREATE,
                    baseVersion = 0,
                    current = parsedListData,
                    updated = null
                )
            } else {
                Gson().fromJson(json, ListMessage::class.java) ?: ListMessage(
                    messageType = ListMessageType.CREATE,
                    current = ListData()
                )
            }
        } catch (_: Exception) {
            ListMessage(
                messageType = ListMessageType.CREATE,
                current = ListData()
            )
        }

        // 2. Select the accurate structural layout frame targeting active revisions
        val displayList =
            if (listMessage.messageType == ListMessageType.UPDATE && listMessage.updated != null) {
                listMessage.updated
            } else {
                listMessage.current
            }

        // 3. Compute structural dashboard aggregates
        val completed = displayList.items.count { it.checked }
        val total = displayList.items.size
        val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
        val previewItems = displayList.items.take(3)
        val remainingItems = (total - previewItems.size).coerceAtLeast(0)

        // 4. Resolve sender configuration metrics from current membership indices
        val member = memberMap[sandesh.userNumber]
        val displayName = member?.userName?.takeIf { it.isNotBlank() } ?: sandesh.userNumber
        val userImageUri: Uri? = member?.profileUri

        return GroupParsedListSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(json, sandesh.time),
            sandeshStithi = sandesh.sandeshStithi,
            listMessage = listMessage,
            displayList = displayList,
            title = displayList.title,
            completed = completed,
            total = total,
            progress = progress,
            previewItems = previewItems,
            remainingItems = remainingItems,
            isUpdate = listMessage.messageType == ListMessageType.UPDATE && listMessage.updated != null,
            // Group fields
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0xFF6B46C1.toInt(),
            senderUserLightColor = member?.userLightColor ?: 0xFF9C27B0.toInt(),
            senderProfile = userImageUri
        )
    }


    fun toParsedImageSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>,
        isFirstInGroup: Boolean
    ): GroupParsedImageSandesh {
        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val replyOriginalMessage = jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        val dataArray = jsonObject.getAsJsonArray(DootawasMessage.ALL_DATA)
        val imageUris = dataArray?.mapNotNull {
            it.asJsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        } ?: emptyList()

        val firstUri = imageUris.firstOrNull()
        val member = memberMap[sandesh.userNumber]
        val displayName = member?.userName?.takeIf { it.isNotBlank() } ?: sandesh.userNumber
        val userImageUri: Uri? = member?.profileUri

        return GroupParsedImageSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(sandesh.sandeshType),
            time = sandesh.time,
            formattedTime = buildFormattedTime(jsonObject, sandesh.time),
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            imageUris = imageUris,
            displayUri = firstUri,
            blurUri = firstUri,
            paath = jsonObject.get(DootawasMessage.PAATH)?.asString ?: "",
            downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0,
            downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown",
            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0,
            senderUserLightColor = member?.userLightColor ?: 0,
            senderUserProfile = userImageUri
        )
    }

    fun toParsedVideoSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>,
        isFirstInGroup: Boolean
    ): GroupParsedVideoSandesh? {

        val jsonObject = try {
            JsonParser.parseString(
                sandesh.samagri
            ).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }


        val videoUris = jsonObject
            .getAsJsonArray(DootawasMessage.ALL_DATA)
            ?.mapNotNull {
                it.asJsonObject
                    .get(DootawasMessage.ORIGINAL_URI)
                    ?.asString
            } ?: emptyList()

        val previewUris = jsonObject
            .getAsJsonArray(DootawasMessage.IMAGE_PREVIEW)
            ?.mapNotNull {
                it.asString
            } ?: emptyList()

        val blurUri =
            jsonObject.get(
                DootawasMessage.BLUR_URI
            )?.asString

        val downloadPercent =
            jsonObject.get(
                DootawasMessage.DOWNLOAD_PERCENT
            )?.asInt ?: 0

        val downloadSize =
            jsonObject.get(
                DootawasMessage.DOWNLOAD_SIZE
            )?.asString ?: "Unknown"

        val replyOriginalMessage =
            jsonObject.get(
                DootawasMessage.ORIGINAL_MESSAGE
            )?.asJsonObject

        val member =
            memberMap[sandesh.userNumber]

        val displayName =
            member?.userName
                ?.takeIf { it.isNotBlank() }
                ?: sandesh.userNumber

        val userImageUri =
            member?.profileUri

        return GroupParsedVideoSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(
                sandesh.sandeshType
            ),
            time = sandesh.time,
            formattedDate =
                getRelativeTimeSpanMinuteLevel(
                    sandesh.time
                ),
            formattedTime =
                buildFormattedTime(
                    jsonObject,
                    sandesh.time
                ),

            videoUris = videoUris,
            previewUris = previewUris,
            blurUri = blurUri,

            paath = jsonObject.get(DootawasMessage.PAATH)?.asString ?: "",
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,

            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,

            displayNameOrNumber = displayName,
            senderUserDarkColor =
                member?.userDarkColor ?: 0,
            senderUserLightColor =
                member?.userLightColor ?: 0,
            senderUserProfile = userImageUri
        )
    }

    fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }

    fun toParsedAudioSandesh(
        sandesh: GroupSandesh, memberMap: Map<String, GroupUserProfile>, isFirstInGroup: Boolean
    ): GroupParsedAudioSandesh? {
        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
        val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
        val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"
        val duration = jsonObject.get(DootawasMessage.DURATION)?.asLong ?: 0L

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage = if (isReply) {
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        } else null

        val member = memberMap[sandesh.userNumber]

        val displayName = member?.userName
            ?.takeIf { it.isNotBlank() }
            ?: sandesh.userNumber

        val userImageUri: Uri? = member?.profileUri
        return GroupParsedAudioSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(sandesh.sandeshType),
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(jsonObject, sandesh.time),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            duration = duration,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0,
            senderUserLightColor = member?.userLightColor ?: 0,
            senderUserProfile = userImageUri
        )
    }

    fun toParsedDocumentSandesh(
        sandesh: GroupSandesh, memberMap: Map<String, GroupUserProfile>, isFirstInGroup: Boolean
    ): GroupParsedDocumentSandesh? {
        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
        val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
        val mimeType =
            jsonObject.get(DootawasMessage.MIME_TYPE)?.asString ?: "application/octet-stream"
        val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
        val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage = if (isReply) {
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        } else null

        val member = memberMap[sandesh.userNumber]

        val displayName = member?.userName
            ?.takeIf { it.isNotBlank() }
            ?: sandesh.userNumber

        val userImageUri: Uri? = member?.profileUri
        return GroupParsedDocumentSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(sandesh.sandeshType),
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(jsonObject, sandesh.time),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            mimeType = mimeType,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0,
            senderUserLightColor = member?.userLightColor ?: 0,
            senderUserProfile = userImageUri
        )
    }

    fun toParsedContactSandesh(
        sandesh: GroupSandesh, memberMap: Map<String, GroupUserProfile>, isFirstInGroup: Boolean
    ): GroupParsedContactSandesh? {
        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }
        val contactArray = jsonObject.getAsJsonArray(DootawasMessage.CONTACTS) ?: JsonArray()
        val contacts = contactArray.mapNotNull { element ->
            try {
                val obj = element.asJsonObject
                val name = obj.get(DootawasMessage.CONTACT_NAME)?.asString ?: "Unknown"
                val phone = obj.get(DootawasMessage.NUMBER)?.asString ?: "N/A"
                ContactInfo(name, phone)
            } catch (e: Exception) {
                null // Skip invalid entries
            }
        }

        val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
        val replyOriginalMessage = if (isReply) {
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
        } else null

        val member = memberMap[sandesh.userNumber]

        val displayName = member?.userName
            ?.takeIf { it.isNotBlank() }
            ?: sandesh.userNumber

        val userImageUri: Uri? = member?.profileUri
        return GroupParsedContactSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(sandesh.sandeshType),
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(jsonObject, sandesh.time),
            contacts = contacts,
            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0,
            senderUserLightColor = member?.userLightColor ?: 0,
            senderUserProfile = userImageUri
        )
    }

    fun toParsedLocationSandesh(
        sandesh: GroupSandesh,
        memberMap: Map<String, GroupUserProfile>,
        isFirstInGroup: Boolean
    ): GroupParsedLocationSandesh? {

        val jsonObject = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val savedFileUri =
            jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString

        val duplicateUri =
            jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString

        val latitude =
            jsonObject.get(DootawasMessage.LATITUDE)?.asDouble
                ?: return null

        val longitude =
            jsonObject.get(DootawasMessage.LONGITUDE)?.asDouble
                ?: return null

        val isReply =
            jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)

        val replyOriginalMessage =
            if (isReply) {
                jsonObject
                    .get(DootawasMessage.ORIGINAL_MESSAGE)
                    ?.asJsonObject
            } else null

        // LIVE LOCATION
        val isLive =
            jsonObject.get(DootawasMessage.IS_LIVE)
                ?.asBoolean ?: false

        val liveUntil = try {
            jsonObject.get(DootawasMessage.LIVE_UNTIL)
                ?.takeIf { !it.isJsonNull && it.asString.isNotBlank() }
                ?.asLong
        } catch (e: Exception) {
            null
        }

        val isStopped =
            jsonObject.get(DootawasMessage.IS_STOPPED)
                ?.asBoolean ?: false

        val member = memberMap[sandesh.userNumber]

        val displayName =
            member?.userName
                ?.takeIf { it.isNotBlank() }
                ?: sandesh.userNumber

        val userImageUri: Uri? = member?.profileUri

        // ACTIVE LIVE LOCATION UPDATE
        if (
            isLive &&
            !isStopped &&
            (
                    liveUntil == null ||
                            liveUntil > System.currentTimeMillis()
                    )
        ) {

            updateLiveLocationItem(
                LiveLocationItem(
                    sandeshId = sandesh.sandeshId,
                    latitude = latitude,
                    longitude = longitude,
                    isLive = true,
                    displayName = displayName,
                    profileUri = userImageUri
                )
            )
        }

        return GroupParsedLocationSandesh(
            isFirstInGroup = isFirstInGroup,
            sandeshId = sandesh.sandeshId,
            isSelf = sandesh.isSelf,
            sandeshType = SandeshType.fromInt(sandesh.sandeshType),
            time = sandesh.time,
            formattedDate = getRelativeTimeSpanMinuteLevel(sandesh.time),
            formattedTime = buildFormattedTime(
                jsonObject,
                sandesh.time
            ),
            savedFileUri = savedFileUri,
            duplicateUri = duplicateUri,
            latitude = latitude,
            longitude = longitude,
            sandeshStithi = sandesh.sandeshStithi,
            replyOriginalMessage = replyOriginalMessage,
            displayNameOrNumber = displayName,
            senderUserDarkColor = member?.userDarkColor ?: 0,
            senderUserLightColor = member?.userLightColor ?: 0,
            senderUserProfile = userImageUri,

            // ADD THESE
            isLive = isLive,
            liveUntil = liveUntil,
            isStopped = isStopped
        )
    }

    fun deleteGroupSelectedMessages(
        selectedMessageIds: Set<String>,
        userNumber: String,
        deleteBoth: Boolean
    ) {
        viewModelScope.launch {
            repository.deleteGroupMessages(
                selectedMessageIds,
                userNumber,
                deleteBoth,
                userNumberKhud
            ) // ✅ Call repository to delete
//            fetchMessages(userNumber) // ✅ Refresh UI
        }
    }

    fun downloadGroupFile(sandeshId: String, groupId: String) {
        repository.downloadGroupFile(sandeshId, groupId)
    }

    fun generateGroupUniqueSandeshId(): String {
        val currentParsedItems = _groupParsedSandeshItems.value
        var uniqueSandeshId: String
        do {
            uniqueSandeshId = UUID.randomUUID().toString()
        } while (currentParsedItems.any { it.sandeshId == uniqueSandeshId })
        return uniqueSandeshId
    }

    fun sendMessageToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Add message type
            groupIds.forEach { userNumber ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.MESSAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = userNumber
                )
                message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.MESSAGE.value)
                message.addProperty(JsonKeys.GROUP_ID, userNumber)
                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = userNumber,
                    message = message.toString(),
                    dataType = DataType.MESSAGE,
                    isGroup = true
                )

                repository.insertSandesh(newSandesh)
                val paathValue: String = message.get(DootawasMessage.PAATH).asString
                repository.updateGroupLastMessageToTop(userNumber, paathValue, sandeshId)
                repository.updateLIKHIT(userNumber, sandeshId, "YOU", newSandesh.time)
                if (message.get(DootawasMessage.IS_LINK)?.asBoolean == true) {
                    repository.storeMessagePreview(message, sandeshId, true, userNumber)
                }
                repository.sendSandesh(unsentSandesh)
            }
        }
    }

    fun sendGroupList(targetSandeshId: String, groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)

            groupIds.forEach { groupId ->
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                val newSandesh = GroupSandesh(
                    sandeshId = targetSandeshId, // Tracks upstream visual context ID
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.LIST.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                val unsentSandesh = UnsentSandesh(
                    messId = targetSandeshId,
                    receiver = groupId,
                    message = message.toString(),
                    dataType = DataType.MESSAGE,
                    isGroup = true
                )

                repository.insertSandesh(newSandesh)
                repository.updateGroupLastMessageToTop(groupId, "List", targetSandeshId)
                repository.updateLIKHIT(groupId, targetSandeshId, "YOU", newSandesh.time)
                repository.sendSandesh(unsentSandesh)
            }
        }
    }

    fun modifyGroupListLocally(targetSandeshId: String, groupId: String, message: JsonObject) {
        viewModelScope.launch {
            // Append baseline validation keys
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)
            message.addProperty(JsonKeys.GROUP_ID, groupId)
            message.addProperty(JsonKeys.TIME_STAMP, Instant.now().toEpochMilli().toString())

            // Mutate target table layout entry in-place
            repository.updateGroupSandeshSamagri(
                sandeshId = targetSandeshId,
                samagri = message.toString(),
                groupId = userNumberOrGroupId
            )
            // Elevate chat row item ranking
            repository.updateGroupLastMessageToTop(groupId, "List updated", targetSandeshId)
        }
    }

    fun sendGroupListUpdateOnly(
        targetSandeshId: String,
        groupIds: List<String>,
        message: JsonObject
    ) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LIST.value)

            groupIds.forEach { groupId ->
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                val unsentSandesh = UnsentSandesh(
                    messId = targetSandeshId,
                    receiver = groupId,
                    message = message.toString(),
                    dataType = DataType.MESSAGE,
                    isGroup = true
                )

                // Push strictly onto the network pipeline layer
                repository.sendSandesh(unsentSandesh)
            }
        }
    }

    //region REPLIES
    fun replyToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()


                // Create local copy of reply message
                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_MESSAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                // Add metadata
                message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_MESSAGE.value)
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                // Prepare unsent data for background send
                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = groupId,
                    message = message.toString(),
                    dataType = DataType.MESSAGE,
                    isGroup = true
                )

                // Update DB + send
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)
                repository.insertSandesh(newSandesh)
                // Update recent chats list or UI
                repository.updateGroupLastMessageToTop(groupId, "REPLIED: MESSAGE", sandeshId)
                repository.sendSandesh(unsentSandesh)
            }
        }
    }


    fun sendImageToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {

            // Set sandesh type for image
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.IMAGE.value)

            groupIds.forEach { groupIdInternal ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.IMAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupIdInternal
                )
                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupIdInternal)
                repository.updateLIKHIT(groupIdInternal, sandeshId, "YOU", newSandesh.time)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupIdInternal,
                    data = message.toString(),
                    dataType = FileDataType.IMAGE,
                    isGroup = true
                )
                repository.sendImage(unsentData)
                repository.updateGroupLastMessageToTop(userNumberOrGroupId, "IMAGE", sandeshId)
            }


        }
    }


    fun replyImageToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_IMAGE.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_IMAGE.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupId)
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupId,
                    data = message.toString(),
                    dataType = FileDataType.REPLY_IMAGE,
                    isGroup = true
                )
                repository.sendImage(unsentData)

                repository.updateGroupLastMessageToTop(groupId, "REPLIED: IMAGE", sandeshId)
            }
        }
    }

    fun sendVideoToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch(Dispatchers.IO) {  // ← add Dispatchers.IO
            // Set sandesh type for image
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.VIDEO.value)

            groupIds.forEach { groupIdInternal ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.VIDEO.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = userNumberOrGroupId
                )

                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupIdInternal)
                repository.updateLIKHIT(userNumberOrGroupId, sandeshId, "YOU", newSandesh.time)

                repository.generateAndSaveThumbnailInternal(
                    message,
                    sandeshId,
                    groupIdInternal,
                    true
                )

                // Create UnsentFiles entry with JSON arrays
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupIdInternal,
                    data = message.toString(),
                    dataType = FileDataType.VIDEO,
                    isGroup = true
                )

                // Insert or update unsent data
                repository.sendVideo(unsentData)
                // Optionally update last message preview
                repository.updateGroupLastMessageToTop(
                    groupIds.firstOrNull() ?: "",
                    "VIDEO",
                    sandeshId
                )
            }

        }
    }

    fun replyVideoToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_VIDEO.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_VIDEO.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupId)
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)
                repository.generateAndSaveThumbnailInternal(message, sandeshId, groupId, true)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupId,
                    data = message.toString(),
                    dataType = FileDataType.REPLY_VIDEO,
                    isGroup = true
                )
                repository.sendVideo(unsentData)

                repository.updateGroupLastMessageToTop(groupId, "REPLIED: VIDEO", sandeshId)
            }
        }
    }

    fun sendDocumentToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Set sandesh type for document
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.DOCUMENT.value)

            groupIds.forEach { groupIdInternal ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.DOCUMENT.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupIdInternal
                )

                // Insert into local DB
                repository.insertSandesh(newSandesh)

                // Add group ID into message
                message.addProperty(JsonKeys.GROUP_ID, groupIdInternal)

                // Update state in repository
                repository.updateLIKHIT(groupIdInternal, sandeshId, "YOU", newSandesh.time)

                // Prepare unsent data for documents
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupIdInternal,
                    data = message.toString(),
                    dataType = FileDataType.DOCUMENT,
                    isGroup = true
                )

                // Send document
                repository.sendDocument(unsentData)
                repository.updateGroupLastMessageToTop(
                    groupIds.firstOrNull() ?: "",
                    "DOCUMENT",
                    sandeshId
                )
            }

            // Optionally update last message preview for UI


        }
    }

    fun replyDocumentToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Set sandesh type for reply document
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_DOCUMENT.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_DOCUMENT.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                // Insert locally
                repository.insertSandesh(newSandesh)

                // Add group ID inside message for backend routing
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                // Update DB state
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)

                // Prepare for sending
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupId,
                    data = message.toString(),
                    dataType = FileDataType.REPLY_DOCUMENT,
                    isGroup = true
                )

                // Send document reply
                repository.sendDocument(unsentData)

                // Update last message preview
                repository.updateGroupLastMessageToTop(groupId, "REPLIED: DOCUMENT", sandeshId)
            }
        }
    }


    fun sendLocationToGroups(
        groupIds: List<String>,
        message: JsonObject,
        context: Context? = null
    ) {
        viewModelScope.launch {
            // Set sandesh type for image
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LOCATION.value)
            groupIds.forEach { groupIdInternal ->
                val sandeshId = generateGroupUniqueSandeshId()
                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.LOCATION.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupIdInternal
                )
                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupIdInternal)
                repository.updateLIKHIT(groupIdInternal, sandeshId, "YOU", newSandesh.time)
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupIdInternal,
                    data = message.toString(),
                    dataType = FileDataType.LOCATION,
                    isGroup = true
                )
                repository.sendLocation(unsentData)
                repository.updateGroupLastMessageToTop(userNumberOrGroupId, "LOCATION", sandeshId)
                if (context != null && message.get(DootawasMessage.IS_LIVE)?.asBoolean == true) {

                    LiveLocationService.addLiveLocation(
                        context = context,
                        sandeshId = sandeshId,
                        targetId = groupIdInternal,
                        isGroup = true,
                        expiresAt = message.get(DootawasMessage.LIVE_UNTIL)?.asLong
                    )
                }
            }
        }
    }


    fun replyLocationToGroups(groupIds: List<String>, message: JsonObject, context: Context) {
        viewModelScope.launch {
            // Set sandesh type for reply location
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_LOCATION.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_LOCATION.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupId)
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)

                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupId,
                    data = message.toString(),
                    dataType = FileDataType.REPLY_LOCATION,
                    isGroup = true
                )

                repository.sendLocation(unsentData)
                repository.updateGroupLastMessageToTop(groupId, "REPLIED: LOCATION", sandeshId)
                if (
                    message.get(DootawasMessage.IS_LIVE)
                        ?.asBoolean == true
                ) {

                    LiveLocationService.addLiveLocation(
                        context = context,
                        sandeshId = sandeshId,
                        targetId = groupId,
                        isGroup = true,
                        expiresAt = message.get(DootawasMessage.LIVE_UNTIL)?.asLong
                    )
                }
            }
        }
    }

    fun sendContactToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Add sandesh type for contact
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.CONTACT.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.CONTACT.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                // Attach group info into JSON
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = groupId,
                    message = message.toString(),
                    dataType = DataType.MESSAGE, // ✅ Better to create/use CONTACT instead of MESSAGE
                    isGroup = true
                )

                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)
                repository.insertSandesh(newSandesh)
                repository.sendSandesh(unsentSandesh)

                // Update last message preview
                repository.updateGroupLastMessageToTop(groupId, "CONTACT", sandeshId)
            }
        }
    }

    fun replyContactToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Add sandesh type for reply contact
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_CONTACT.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_CONTACT.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                // Attach group info into JSON
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                val unsentSandesh = UnsentSandesh(
                    messId = sandeshId,
                    receiver = groupId,
                    message = message.toString(),
                    dataType = DataType.MESSAGE, // ✅ new constant for reply contact (for clarity)
                    isGroup = true
                )

                // Update database states
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)
                repository.insertSandesh(newSandesh)
                repository.sendSandesh(unsentSandesh)

                // Update chat preview
                repository.updateGroupLastMessageToTop(groupId, "REPLIED: CONTACT", sandeshId)
            }
        }
    }

    fun sendAudioToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Set sandesh type for image
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.AUDIO.value)
            groupIds.forEach { groupIdInternal ->
                val sandeshId = generateGroupUniqueSandeshId()
                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.AUDIO.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupIdInternal
                )
                repository.insertSandesh(newSandesh)
                message.addProperty(JsonKeys.GROUP_ID, groupIdInternal)
                repository.updateLIKHIT(groupIdInternal, sandeshId, "YOU", newSandesh.time)
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupIdInternal,
                    data = message.toString(),
                    dataType = FileDataType.AUDIO,
                    isGroup = true
                )
                repository.sendAudio(unsentData)
                repository.updateGroupLastMessageToTop(userNumberOrGroupId, "AUDIO", sandeshId)
            }


        }
    }

    fun replyAudioToGroups(groupIds: List<String>, message: JsonObject) {
        viewModelScope.launch {
            // Set sandesh type for reply audio
            message.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.REPLY_AUDIO.value)

            groupIds.forEach { groupId ->
                val sandeshId = generateGroupUniqueSandeshId()

                val newSandesh = GroupSandesh(
                    sandeshId = sandeshId,
                    userNumber = "You",
                    samagri = message.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REPLY_AUDIO.value,
                    sandeshStithi = SandeshStithi.LIKHIT,
                    groupId = groupId
                )

                // Insert locally
                repository.insertSandesh(newSandesh)

                // Add group ID into message JSON
                message.addProperty(JsonKeys.GROUP_ID, groupId)

                // Update DB for message status
                repository.updateLIKHIT(groupId, sandeshId, "YOU", newSandesh.time)

                // Prepare unsent audio data
                val unsentData = UnsentFiles(
                    messageId = sandeshId,
                    receiver = groupId,
                    data = message.toString(),
                    dataType = FileDataType.REPLY_AUDIO,
                    isGroup = true
                )

                // Send audio via repository
                repository.sendAudio(unsentData)

                // Update chat preview or last message
                repository.updateGroupLastMessageToTop(groupId, "REPLIED: AUDIO", sandeshId)
            }
        }
    }

    //endregion

    //region Message Builder


    /**
     * Builds the message once, then routes it to the four existing methods, which are not modified:
     *  people  -> sendImage / sendReplyImage
     *  groups  -> sendImageToGroups / replyImageToGroups
     *
     * [replyOriginal] (the ORIGINAL_MESSAGE object, built exactly as in your current code) is applied
     * only to the chat the user is in, and only if that chat is still a recipient.
     */
    fun sendImages(
        context: Context,                     // pass context.applicationContext
        files: List<Uri>,
        caption: String,
        disappearSeconds: Int,
        recipients: List<PickedContact>,
        visibility: MediaVisibility,
        currentChatId: String,                // person number or group id of the open chat
        currentChatIsGroup: Boolean,
        replyOriginal: JsonObject? = null
    ) {
        viewModelScope.launch {
            val base = withContext(Dispatchers.IO) {
                ImageMessageBuilder.build(context, files, caption, disappearSeconds, visibility)
            } ?: return@launch

            val people = recipients.filter { !it.isGroup }.map { it.userNumber }.distinct()
            val groups = recipients.filter { it.isGroup }.map { it.userNumber }.distinct()

            val replyPerson =
                currentChatId.takeIf { replyOriginal != null && !currentChatIsGroup && it in people }
            val replyGroup =
                currentChatId.takeIf { replyOriginal != null && currentChatIsGroup && it in groups }

            if (replyOriginal != null) {
                replyPerson?.let {
                    sendReplyImage(
                        it,
                        ImageMessageBuilder.withReply(base, replyOriginal)
                    )
                }
                replyGroup?.let {
                    replyImageToGroups(
                        listOf(it),
                        ImageMessageBuilder.withReply(base, replyOriginal)
                    )
                }
            }

            val otherPeople = people.filter { it != replyPerson }
            if (otherPeople.isNotEmpty()) sendImage(otherPeople, base.deepCopy())

            // One call per group, each with its own copy, so one group's id never carries over to the next
            groups.filter { it != replyGroup }.forEach { groupId ->
                sendImageToGroups(listOf(groupId), base.deepCopy())
            }
        }
    }

    /**
     * Builds the video message once, then routes it to the existing methods (not modified):
     *  people  -> sendVideo / sendReplyVideo
     *  groups  -> sendVideoToGroups / replyVideoToGroups      // ASSUMPTION: these names
     */
    fun sendVideos(
        context: Context,                     // pass context.applicationContext
        files: List<Uri>,
        caption: String,
        disappearSeconds: Int,
        recipients: List<PickedContact>,
        visibility: MediaVisibility,
        currentChatId: String,
        currentChatIsGroup: Boolean,
        replyOriginal: JsonObject? = null
    ) {
        viewModelScope.launch {
            val base = withContext(Dispatchers.IO) {
                VideoMessageBuilder.build(context, files, caption, disappearSeconds, visibility)
            }
            if (base == null) {
//                Toast.makeText(context, "No valid videos selected", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val people = recipients.filter { !it.isGroup }.map { it.userNumber }.distinct()
            val groups = recipients.filter { it.isGroup }.map { it.userNumber }.distinct()

            val replyPerson =
                currentChatId.takeIf { replyOriginal != null && !currentChatIsGroup && it in people }
            val replyGroup =
                currentChatId.takeIf { replyOriginal != null && currentChatIsGroup && it in groups }

            if (replyOriginal != null) {
                replyPerson?.let {
                    sendReplyVideo(
                        it,
                        VideoMessageBuilder.withReply(base, replyOriginal)
                    )
                }
                replyGroup?.let {
                    replyVideoToGroups(
                        listOf(it),
                        VideoMessageBuilder.withReply(base, replyOriginal)
                    )
                }
            }

            val otherPeople = people.filter { it != replyPerson }
            if (otherPeople.isNotEmpty()) sendVideo(userNumbers = otherPeople, base.deepCopy())

            // One call per group, each with its own copy
            groups.filter { it != replyGroup }.forEach { groupId ->
                sendVideoToGroups(listOf(groupId), base.deepCopy())
            }
        }
    }
    //endregion

    fun processGroupDhrist() {
        viewModelScope.launch {
            repository.processGroupDhrist(userNumberOrGroupId)

        }
    }


    fun sendGroupTyping(userNumber: String) {
        val now = System.currentTimeMillis()
        if (now - lastTypingSentTime < 1000) {
            // Ignore if last event was less than 1 sec ago
            return
        }
        lastTypingSentTime = now
        viewModelScope.launch {
            repository.sendTypingEvent(userNumber) // Your repo method to send typing event
        }
    }

    private val _onlyOneMember = MutableStateFlow(false)
    val onlyOneMember: StateFlow<Boolean> = _onlyOneMember

    private val _groupEntity = MutableStateFlow<GroupEntity?>(null)
    val groupEntity = _groupEntity.asStateFlow()

    private fun observeGroupEntity(groupId: String) {
        viewModelScope.launch {
            repository.getEntityFlow(groupId).collect { group ->
                if (group != null) {

                    _groupEntity.value = group // or use MutableStateFlow / LiveData
                }
            }
        }
    }

    fun GroupMemberEntity.toUserProfile(): GroupUserProfile {
        val resolvedName = contactName.takeIf { it.isNotBlank() }
            ?: onlineName
            ?: "" // No userNumber fallback
        return GroupUserProfile(
            userNumber = userNumber,
            userName = resolvedName,
            profileUri = profileUrl.toUri(),
            userLightColor = userLightColor,
            userDarkColor = userDarkColor,

            isSwami = isSwami,
            canMessage = canMessage,
            canUpdateProfile = canUpdateProfile,
            canModifyMembers = canModifyMembers,
            canStartCall = canStartCall,
            joinedAt = joinedAt
        )
    }

    // Shared loop: input is newest first, header goes AFTER each day's oldest message
    private inline fun <T> insertHeaders(
        items: List<T>,
        timeOf: (T) -> Long,
        makeHeader: (time: Long, zone: ZoneId, today: LocalDate) -> T
    ): List<T> {
        if (items.isEmpty()) return items

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val result = ArrayList<T>(items.size + 8)
        var lastDay = Long.MIN_VALUE
        var lastTime = 0L

        for (item in items) {
            val time = timeOf(item)
            val day = time.toLocalDate(zone).toEpochDay()

            if (lastDay != Long.MIN_VALUE && day != lastDay) {
                result.add(makeHeader(lastTime, zone, today))
            }
            result.add(item)
            lastDay = day
            lastTime = time
        }

        result.add(makeHeader(lastTime, zone, today))
        return result
    }

    fun parseGroupDateHeader(
        time: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): GroupParsedDateHeaderSandesh {
        val date = time.toLocalDate(zone)
        val epochDay = date.toEpochDay()
        return GroupParsedDateHeaderSandesh(
            sandeshId = "$DATE_HEADER_PREFIX$epochDay",
            time = time,
            epochDay = epochDay,
            formattedDate = shortDayLabel(date, today),
            detailedDate = detailedDayLabel(date, today),
            sandeshStithi = SandeshStithi.LIKHIT   // replace with a neutral value from your enum
        )
    }

    fun addGroupDateHeaders(items: List<GroupParsedSandeshItem>): List<GroupParsedSandeshItem> =
        insertHeaders(items, { it.time }) { t, z, today -> parseGroupDateHeader(t, z, today) }

    private fun isFirstInSenderRun(
        current: GroupSandesh,
        next: GroupSandesh?
    ): Boolean {
        if (next == null) return true
        if (current.isSelf != next.isSelf) return true
        if (current.userNumber != next.userNumber) return true
        return false
    }

    fun observeGroupData(groupId: String) {
        viewModelScope.launch {
            combine(
                repository.getMembersFlow(groupId),
                repository.getSandeshByGroup(groupId)
            ) { members, messages ->

                // Convert Entity -> UI model first
                val memberProfiles = members.map { it.toUserProfile() }
                val memberMap = memberProfiles.associateBy { it.userNumber }

                val visible = messages.filter { sandesh ->
                    SandeshType.fromInt(sandesh.sandeshType) != SandeshType.DELETE
                }

                val parsedMessages: List<GroupParsedSandeshItem> =
                    visible.mapIndexedNotNull { index, sandesh ->
                        try {
                            val next = visible.getOrNull(index + 1)
                            val isFirstInGroup = isFirstInSenderRun(sandesh, next)

                            when (SandeshType.fromInt(sandesh.sandeshType)) {
                                SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE ->
                                    toParsedTextSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.IMAGE, SandeshType.REPLY_IMAGE ->
                                    toParsedImageSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.VIDEO, SandeshType.REPLY_VIDEO ->
                                    toParsedVideoSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.AUDIO, SandeshType.REPLY_AUDIO ->
                                    toParsedAudioSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.DOCUMENT, SandeshType.REPLY_DOCUMENT ->
                                    toParsedDocumentSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.CONTACT, SandeshType.REPLY_CONTACT ->
                                    toParsedContactSandesh(sandesh, memberMap, isFirstInGroup)

                                SandeshType.LOCATION, SandeshType.REPLY_LOCATION ->
                                    toParsedLocationSandesh(sandesh, memberMap, isFirstInGroup)

                                else -> null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                // Build the UI list off the main thread, respecting any active search
                val uiItems = buildGroupItems(parsedMessages, currentGroupQuery)

                GroupData(
                    members = memberProfiles,
                    messages = messages,
                    parsedMessages = parsedMessages,
                    uiItems = uiItems
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { data ->
                    _groupMembers.value = data.members
                    _allParsedSandeshItemsFilter.value =
                        data.parsedMessages   // no headers: search source
                    _groupParsedSandeshItems.value =
                        data.uiItems              // headers, or filtered results
                    _allGroupSandeshList.value = data.messages

                    _onlyOneMember.value = data.members.size <= 1

                    // Only update "latest" when not searching, so search results don't trigger auto-scroll
                    if (currentGroupQuery.isBlank()) {
                        data.parsedMessages.firstOrNull()?.let {
                            _latestSandeshId.value = it.sandeshId
                        }
                    }
                }
        }
    }

    private data class GroupData(
        val members: List<GroupUserProfile>,
        val messages: List<GroupSandesh>,   // was List<Sandesh>
        val parsedMessages: List<GroupParsedSandeshItem>,
        val uiItems: List<GroupParsedSandeshItem>
    )

    fun onCheckVisibilityRequested() {
        viewModelScope.launch {
            val currentMembers = _groupMembers.value
            if (currentMembers.isEmpty()) return@launch

            val userNumbers = currentMembers.map { it.userNumber }.distinct()
            repository.checkVisibleProfiles(userNumbers)
        }
    }


    fun addMember(member: GroupMemberEntity) {
        viewModelScope.launch {
            repository.insertMember(member)
            // Refresh list after insert
//            loadGroupMembers(member.groupId)
        }
    }


    fun addMembersToGroup(userNumbers: List<String>) {
        viewModelScope.launch {
            val newMembers = userNumbers.map { userNumber ->

                val colors = repository.generateColorMember()
                GroupMemberEntity(
                    groupId = userNumberOrGroupId,
                    userNumber = userNumber,
                    contactName = repository.getNameByNumber(userNumber),
                    userDarkColor = colors.darkModeColor,
                    userLightColor = colors.lightModeColor,
                    joinedAt = System.currentTimeMillis(),
                    canMessage = true,
                    canUpdateProfile = false,
                    canModifyMembers = false,
                    canStartCall = false,
                    isSwami = false,
                    profileUrl = "",
                    publicKey = ""
                )
            }
            val jsonArray = JsonArray()
            userNumbers.forEach { jsonArray.add(it) }
            val notifyUserJson = JsonObject().apply {
                addProperty(JsonKeys.GROUP_ID, userNumberOrGroupId)
                addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.MEMBERS_UPDATED.value)
                add(JsonKeys.USER_NUMBER, jsonArray)
            }

            val jsonObject = JsonObject().apply {
                addProperty(JsonKeys.GROUP_ID, userNumberOrGroupId)
                add(JsonKeys.USER_NUMBER, jsonArray)
                addProperty(DootawasMessage.SANDESH_TYPE, GroupSandeshType.ACTION_ADD_MEMBERS.value)
                add(JsonKeys.SANDESH, notifyUserJson)
            }


            val unsentSandesh = UnsentSandesh(
                messId = generateGroupUniqueSandeshId(),
                receiver = userNumberOrGroupId,
                message = jsonObject.toString(),
                dataType = DataType.GROUP_ACTION
            )

            repository.sendSandesh(unsentSandesh)
            repository.insertMembers(newMembers)

        }
    }


    fun removeMember(groupId: String, userNumber: String) {
        viewModelScope.launch {
            repository.deleteMember(groupId, userNumber)
            val data = JsonObject().apply {
                addProperty("member", userNumber)
            }

            repository.addUniversalMessage(
                UniversalType.MEMBER_REMOVED,
                data,
                groupId
            )
        }
    }

    fun setProfileImage(path: String) {
        viewModelScope.launch {
            if (isGroupPassed) {
                repository.setProfilePic(path, userNumberOrGroupId)
            }
        }
    }

    fun groupAction(jsonObject: JsonObject) {

        viewModelScope.launch {
            val unsentSandesh = UnsentSandesh(
                messId = generateGroupUniqueSandeshId(),
                receiver = userNumberOrGroupId,
                message = jsonObject.toString(),
                dataType = DataType.GROUP_ACTION
            )
            repository.sendSandesh(unsentSandesh)
        }
    }

    fun updateGroupActive(type: Boolean) {
        viewModelScope.launch {


            repository.updateGroupActive(userNumberOrGroupId)
            repository.addUniversalMessage(
                if (type) UniversalType.YOU_LEFT else UniversalType.GROUP_TERMINATED,
                JsonObject(),
                userNumberOrGroupId
            )
        }
    }

    fun updateMemberPermissionsBulk(
        groupId: String,
        userNumber: String,
        permissions: Map<String, Boolean>
    ) {
        viewModelScope.launch {
            val permissionsJson = JsonObject().apply {
                addProperty(JsonKeys.GROUP_ID, groupId)
                addProperty(JsonKeys.USER_NUMBER, userNumber)
                addProperty(
                    DootawasMessage.SANDESH_TYPE,
                    GroupSandeshType.ACTION_MEMBER_PERMISSION_UPDATED.value
                )
            }

            val notifyUserJson = JsonObject().apply {
                addProperty(JsonKeys.GROUP_ID, groupId)
                addProperty(JsonKeys.USER_NUMBER, userNumber)
                addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.MEMBERS_UPDATED.value)
            }
            permissionsJson.add(JsonKeys.SANDESH, notifyUserJson)

            // Update each field in DB and build the JSON
            for ((field, newValue) in permissions) {
                when (field) {
                    "canMessage",
                    "canUpdateProfile",
                    "canModifyMembers",
                    "canStartCall" -> {

                        repository.updateMemberPermission(groupId, userNumber, field, newValue)
                        permissionsJson.addProperty(field, newValue)
                    }
                }
            }

            val unsentSandesh = UnsentSandesh(
                messId = generateGroupUniqueSandeshId(),
                receiver = userNumberKhud,
                message = permissionsJson.toString(),
                dataType = DataType.GROUP_ACTION
            )

            repository.sendSandesh(unsentSandesh)

        }
    }

    fun resetGroupDownloadPercentage(sandeshId: String) {
        viewModelScope.launch {
            repository.resetGroupDownloadPercentage(sandeshId, userNumberOrGroupId)
        }
    }

    fun resetUnread() {
        viewModelScope.launch {
            repository.resetUnread(userNumberOrGroupId, isGroupPassed)
        }
    }

    fun updateLiveLocationItem(
        item: LiveLocationItem
    ) {

        _liveLocationItems.update { current ->

            current.toMutableMap().apply {

                put(
                    item.sandeshId,
                    item
                )
            }
        }
    }

    fun requestLiveLocationRoute(
        sandeshId: String,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ) {

        viewModelScope.launch {

            repository.requestLiveLocationRoute(
                sandeshId = sandeshId,
                originLat = originLat,
                originLng = originLng,
                destinationLat = destinationLat,
                destinationLng = destinationLng
            )
        }
    }

    fun updateLiveRoute(
        sandeshId: String,
        polyline: String
    ) {

        val decoded = PolyUtil.decode(polyline)

        _liveRoutes.update { current ->

            current.toMutableMap().apply {

                put(
                    sandeshId,
                    decoded
                )
            }
        }
    }

    fun stopLiveLocationSharing(
        context: Context,
        sandeshId: String,
    ) {

        viewModelScope.launch {

            repository.stopLiveLocationSharing(
                context = context,
                sandeshId = sandeshId,
                targetId = userNumberOrGroupId,
                isGroup = isGroupPassed
            )
        }
    }

    fun updateWallpaper(uri: Uri?) {
        viewModelScope.launch {
            repository.updateWallpaper(uri, userNumberOrGroupId, isGroupPassed)
        }

    }

    fun acceptLedger(sandeshId: String, resolvedBalance: Double) {
        viewModelScope.launch {
            updateLedgerStatusInSamagri(sandeshId, LedgerStatus.ACCEPTED, resolvedBalance)
        }
    }

    fun rejectLedger(sandeshId: String, resolvedBalance: Double) {
        viewModelScope.launch {
            updateLedgerStatusInSamagri(sandeshId, LedgerStatus.REJECTED, resolvedBalance)
        }
    }

    private suspend fun updateLedgerStatusInSamagri(
        sandeshId: String,
        status: LedgerStatus,
        resolvedBalance: Double
    ) {
        val sandesh =
            repository.getSingleSandeshByUserAndId(sandeshId, userNumberOrGroupId) ?: return

        val json = try {
            JsonParser.parseString(sandesh.samagri).asJsonObject
        } catch (e: Exception) {
            return
        }

        var ledgerType = LedgerType.fromInt(json.get(DootawasMessage.LEDGER_TYPE)?.asInt ?: 0)
        var amount = json.get(DootawasMessage.LEDGER_AMOUNT)?.asDouble ?: 0.0

        // --- DYNAMIC PAYMENT SUBTRACTION FOR RECEIVER ---
        if (ledgerType == LedgerType.BILL && json.has("ledgerData")) {
            try {
                val ledgerData = json.getAsJsonObject("ledgerData")
                if (ledgerData != null && ledgerData.has("amountPaid")) {
                    val amountPaid = ledgerData.get("amountPaid").asDouble
                    val billTotal = ledgerData.get(DootawasMessage.BILL_TOTAL).asDouble
                    val pendingAmount = billTotal - amountPaid
                    amount = pendingAmount
//                    if (pendingAmount > 0.0) {
//
//                        // Change the type locally to CREDIT or DEBIT depending on how you converted it on the sender side
//                        ledgerType = LedgerType.CREDIT
//
//                        // Update the active properties inside the JSON object itself
////                        json.addProperty(DootawasMessage.LEDGER_AMOUNT, amount)
////                        json.addProperty(DootawasMessage.LEDGER_TYPE, ledgerType.value)
//                    }
                }
            } catch (e: Exception) {
            }
        }
        // --- END OF DYNAMIC CONVERSION ---

        json.addProperty(DootawasMessage.LEDGER_STATUS, status.value)
        json.addProperty(DootawasMessage.LEDGER_RESOLVED_BALANCE, resolvedBalance)

        repository.updateSandeshSamagri(sandeshId, json.toString())

        // Apply the synchronized net unpaid balance change to upbhogta
        repository.updateLedgerStatusAndBalance(
            sandeshId = sandeshId,
            userNumber = sandesh.userNumber,
            status = status,
            ledgerType = ledgerType,
            amount = -amount
        )

        // Determine the inverse ACK mapping
        val targetAckType = when (ledgerType) {
            LedgerType.DEBIT -> LedgerType.LEDGER_ACK_BILL_CREDIT // Value 4
            else -> LedgerType.LEDGER_ACK_DEBIT                  // Value 5
        }

        // send ACK payload with the matching recalculated amount
        val ackJson = JsonObject().apply {
            addProperty(DootawasMessage.LEDGER_TYPE, targetAckType.value)
            addProperty(
                DootawasMessage.LEDGER_AMOUNT,
                amount
            ) // Crucial for passing server anti-fraud!
            addProperty(DootawasMessage.LEDGER_STATUS, status.value)
            addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, sandeshId)
            addProperty(DootawasMessage.LEDGER_CREATED_AT, System.currentTimeMillis())
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LEDGER.value)
        }

        repository.sendSandesh(
            UnsentSandesh(
                messId = sandeshId,
                receiver = userNumberOrGroupId,
                message = ackJson.toString(),
                dataType = DataType.MESSAGE
            )
        )
    }

    fun fetchUserBalance() {
        repository.fetchUserBalance(userNumberOrGroupId)
    }

    fun requestDisappearingMsg(duration: Int) {
        viewModelScope.launch {

            val privacyJson = JsonObject()
            privacyJson.addProperty(
                DootawasMessage.SANDESH_TYPE,
                SandeshType.REQUEST_DISAPPEARING.value
            )
            privacyJson.addProperty(DootawasMessage.DURATION, duration)
            // 👈 DATA_BOOLEAN removed — don't add it here, so parser sees "pending" via jsonObject.has() == false

            val sandeshId = generateUniqueSandeshId()

            repository.insertSandesh(
                Sandesh(
                    sandeshId = sandeshId,
                    userNumber = userNumberOrGroupId,
                    samagri = privacyJson.toString(),
                    isSelf = true,
                    time = Instant.now().toEpochMilli(),
                    sandeshType = SandeshType.REQUEST_DISAPPEARING.value,
                    sandeshStithi = SandeshStithi.LIKHIT
                )
            )

            repository.sendSandesh(
                UnsentSandesh(
                    messId = sandeshId,
                    receiver = userNumberOrGroupId,
                    message = privacyJson.toString(),
                    dataType = DataType.MESSAGE
                )
            )
        }
    }

    fun respondToDisappearingRequest(sandeshId: String, accepted: Boolean, duration: Int) {
        viewModelScope.launch {

            // 1. Update the original request's samagri in local DB
            val responseJson = JsonObject()
            responseJson.addProperty(
                DootawasMessage.SANDESH_TYPE,
                SandeshType.RESPONSE_DISAPPEARING.value
            )
            responseJson.addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, sandeshId)
            responseJson.addProperty(DootawasMessage.DURATION, duration)
            responseJson.addProperty(JsonKeys.DATA_BOOLEAN, accepted)

            repository.updateSandeshSamagri(
                sandeshId = sandeshId,
                samagri = responseJson.toString()
            )

            // 2. Notify the other side so their copy updates too
            repository.sendSandesh(
                UnsentSandesh(
                    messId = sandeshId,
                    receiver = userNumberOrGroupId,
                    message = responseJson.toString(),
                    dataType = DataType.MESSAGE
                )
            )

            // 3. If accepted, actually apply the setting to the chat
            if (accepted) {
                repository.updateDisappearingMsg(
                    userNumber = userNumberOrGroupId,
                    duration = duration
                )
            }
        }
    }

    fun revealPhoneNumber() {

        viewModelScope.launch {
            repository.revealPhoneNumber(userNumberOrGroupId)
            // 2. Notify the other side so their copy updates too
            if (!parsedList.value.isEmpty()) {
                val responseJson = JsonObject()
                responseJson.addProperty(
                    DootawasMessage.SANDESH_TYPE,
                    SandeshType.UNIVERSAL_MESSAGE.value
                )
                responseJson.addProperty(
                    DootawasMessage.UNIVERSAL_TYPE,
                    UniversalType.REVEAL_PHONE_NUMBER
                )
                responseJson.addProperty(DootawasMessage.PAATH, "")
                repository.sendSandesh(
                    UnsentSandesh(
                        messId = generateUniqueSandeshId(),
                        receiver = userNumberOrGroupId,
                        message = responseJson.toString(),
                        dataType = DataType.MESSAGE
                    )
                )
            } else {
                DLog.i("Skipping reveal phone number — chat has no messages")
            }
        }
    }

    fun saveList(listData: ListData, sandeshId: String) {
        viewModelScope.launch {
            knotx.saveList(listData, sandeshId = sandeshId, userNumber = userNumberOrGroupId)
        }
    }

    // --- ACCEPT PROPOSED LIST UPDATE ---
    fun acceptProposedListUpdate(targetSandeshId: String, userNumber: String) {
        viewModelScope.launch {
            // Cache our model property keys statically to avoid hardcoded typos
            val keyMessageType = ListMessage::messageType.name
            val keyCurrent = ListMessage::current.name
            val keyUpdated = ListMessage::updated.name
            val keyCurrentVersion = ListMessage::currentVersion.name
            val keyVersion = ListData::version.name

            val existingSandesh = repository.getSingleSandeshByUserAndId(
                targetSandeshId,
                userNumber
            ) ?: return@launch

            try {
                val existingJson = JsonParser.parseString(existingSandesh.samagri).asJsonObject
                val proposedUpdateJson = existingJson.get(keyUpdated)

                // Make sure there is actually a pending update to promote
                if (proposedUpdateJson != null && !proposedUpdateJson.isJsonNull) {
                    val updatedVersion = proposedUpdateJson
                        .asJsonObject
                        .get(keyVersion)

                    // Copy accepted version to top-level currentVersion
                    if (updatedVersion != null && updatedVersion.isJsonPrimitive) {
                        existingJson.add(
                            keyCurrentVersion,
                            updatedVersion.deepCopy()
                        )
                    }

                    // Promote 'updated' to 'current'
                    existingJson.add(keyCurrent, proposedUpdateJson)

                    // Clear the 'updated' slot
                    existingJson.add(keyUpdated, null)

                    // Reset messageType back to CREATE
                    existingJson.addProperty(
                        keyMessageType,
                        ListMessageType.CREATE.name
                    )

                    // Save updated transaction record back to DB
                    repository.updateSandeshSamagri(
                        targetSandeshId,
                        existingJson.toString()
                    )

                    // Keep conversation history list updated
                    repository.updateLastMessageToTop(
                        userNumber,
                        "List updated",
                        targetSandeshId
                    )

                    // Optional: Push network synchronization packet here if needed
                    // sendListResolutionSync(targetSandeshId, userNumber, existingJson)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- REJECT PROPOSED LIST UPDATE ---
    fun rejectProposedListUpdate(targetSandeshId: String, userNumber: String) {
        viewModelScope.launch {
            val keyMessageType = ListMessage::messageType.name
            val keyUpdated = ListMessage::updated.name

            val existingSandesh =
                repository.getSingleSandeshByUserAndId(targetSandeshId, userNumber) ?: return@launch
            try {
                val existingJson = JsonParser.parseString(existingSandesh.samagri).asJsonObject

                // Rejections simply wipe out the "updated" slot entirely
                existingJson.add(keyUpdated, null)

                // Revert message state back to standard CREATE view format
                existingJson.addProperty(keyMessageType, ListMessageType.CREATE.name)

                // Persist the clean original list state back to DB
                repository.updateSandeshSamagri(targetSandeshId, existingJson.toString())

                repository.updateLastMessageToTop(userNumber, "Update rejected", targetSandeshId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun testCall() {
        viewModelScope.launch {
            repository.testCall(userNumberOrGroupId)
        }

    }

    fun updateHideKeyForUser(userNumber: String, passkey: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.updateHideKeyForUser(userNumberOrGroupId, passkey, isMuted)
        }
    }

    fun createOrderID(amount: Int, orderType: PurchaseOrderType) {
        repository.createOrder(amount, orderType)
    }

    fun fetchAiTokens() {
        viewModelScope.launch{
        repository.fetchAiTokens()
        }
    }


    // keep / adjust the package line to wherever you put this file

    /**
     * Builds the image message JSON. Does not send anything.
     */
    object ImageMessageBuilder {

        /** Message key for the caption. Move it into DootawasMessage whenever you like. */
        private const val CAPTION_KEY = "caption"

        private const val MAX_VIDEO_BYTES = 200 * 1_000_000L   // same limit as the old preview screen

        /**
         * Saves each editor file into app storage (deleting the editor's temp file) and builds the
         * JSON your four send methods already expect. Returns null if no file could be saved.
         */
        suspend fun build(
            context: Context,
            files: List<Uri>,
            caption: String,
            disappearSeconds: Int,
            visibility: MediaVisibility
        ): JsonObject? {
            val images = JsonArray()
            var totalSize = 0L

            files.forEach { uri ->
                val saved = saveMediaToAppStorageNext(context, uri, MediaCategory.IMAGE, visibility)
                uri.path?.let { File(it).delete() }        // the editor's temp file, no longer needed
                if (saved != null) {
                    totalSize += saved.fileSize
                    images.add(JsonObject().apply {
                        addProperty(DootawasMessage.ORIGINAL_URI, saved.localPath)
                        addProperty(DootawasMessage.MIME_TYPE, saved.mimeType)
                    })
                }
            }
            if (images.size() == 0) return null            // nothing saved, nothing to send

            return JsonObject().apply {
                addProperty(DootawasMessage.DATA_COUNT, images.size())   // files actually saved
                add(DootawasMessage.ALL_DATA, images)
                addProperty(DootawasMessage.DOWNLOAD_SIZE, totalSize)
                addProperty(DootawasMessage.DISAPPEAR_DURATION, disappearSeconds)
                addProperty(DootawasMessage.PAATH, caption)
            }
        }

        /** A copy of [base] carrying the replied-to message. [original] is the same object you build today. */
        fun withReply(base: JsonObject, original: JsonObject): JsonObject =
            base.deepCopy().apply { add(DootawasMessage.ORIGINAL_MESSAGE, original.deepCopy()) }
    }

    /**
     * Builds the video message JSON. Does not send anything.
     */
    object VideoMessageBuilder {

        private const val MAX_VIDEO_BYTES = 200 * 1_000_000L   // same limit as the old preview screen

        /**
         * Saves each editor mp4 into app storage (deleting the editor's temp file) and builds the JSON
         * your video send methods already expect. Returns null if no file could be saved.
         */
        suspend fun build(
            context: Context,
            files: List<Uri>,
            caption: String,
            disappearSeconds: Int,
            visibility: MediaVisibility
        ): JsonObject? {
            val videos = JsonArray()
            var totalSize = 0L

            files.forEach { uri ->
                val tooBig = (uri.path?.let { File(it).length() } ?: 0L) > MAX_VIDEO_BYTES
                val saved = if (tooBig) null
                else saveMediaToAppStorageNext(context, uri, MediaCategory.VIDEO, visibility)   // ASSUMPTION: MediaCategory.VIDEO
                uri.path?.let { File(it).delete() }          // the editor's temp file, no longer needed
                if (saved != null) {
                    totalSize += saved.fileSize
                    videos.add(JsonObject().apply {
                        addProperty(DootawasMessage.ORIGINAL_URI, saved.localPath)
                        addProperty(DootawasMessage.MIME_TYPE, saved.mimeType)
                    })
                }
            }
            if (videos.size() == 0) return null

            return JsonObject().apply {
                addProperty(DootawasMessage.DATA_COUNT, videos.size())
                add(DootawasMessage.ALL_DATA, videos)
                addProperty(DootawasMessage.DOWNLOAD_SIZE, totalSize)
                addProperty(DootawasMessage.DISAPPEAR_DURATION, disappearSeconds)
                addProperty(DootawasMessage.PAATH, caption)   // ASSUMPTION: video bubbles read the caption from PAATH too
            }
        }

        fun withReply(base: JsonObject, original: JsonObject): JsonObject =
            base.deepCopy().apply { add(DootawasMessage.ORIGINAL_MESSAGE, original.deepCopy()) }
    }

}
