package com.senghol.knotx.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.text.format.DateFormat
import coil.ImageLoader
import coil.memory.MemoryCache
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.GroupDao
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.GroupMemberEntity
import com.senghol.knotx.data.database.GroupSandesh
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.data.database.SandeshStatusDao
import com.senghol.knotx.data.database.UnsentFiles
import com.senghol.knotx.data.database.UnsentSandesh
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.data.database.UpbhogtaDao
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.components.LedgerStatus
import com.senghol.knotx.ui.components.LedgerType
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.StoredThemeBundle
import com.senghol.knotx.ui.theme.toStored
import com.senghol.knotx.utils.data.AiWalletBalance
import com.senghol.knotx.utils.data.UserColors
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.PurchaseOrderType
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.objects.DLog
import com.senghol.knotx.utils.objects.UniversalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.UUID

class SandeshRepository(
    private val sandeshDao: SandeshDao,
    private val knotx: Knotx,
    private val groupDao: GroupDao,
    private val dootawasDatabase: DootawasDatabase,
    context: Context
) {
    private val appContext = context.applicationContext  // SAFE
    val liveRouteFlow = knotx.liveRouteFlow
    val applicationContext: Context
        get() = appContext

    val imageLoader = ImageLoader.Builder(appContext)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.25)
                .build()
        }
        .build()

    private val is24Hour = DateFormat.is24HourFormat(appContext)

    private val upbhogtaDao: UpbhogtaDao by lazy { dootawasDatabase.upbhogtaDao() }
    private val sandeshStatusDao: SandeshStatusDao by lazy { dootawasDatabase.sandeshStatusDao() }
    private val formatter12Hour = DateTimeFormatterBuilder()
        .appendPattern("h:mma")
        .toFormatter(Locale.getDefault())

    fun observePresence(userNumber: String): Flow<Boolean?> {
        DLog.i("observePresence called $userNumber")

        return knotx.presenceMap.map { map ->
            DLog.i("presenceMap emit size=${map.size}")
            map[userNumber]
        }
    }
    fun formatTime(timestamp: Long): String {
        val zoned = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return if (is24Hour) {
            zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            formatter12Hour.format(zoned)
        }
    }

    // ✅ Use Flow instead of List, so it auto-updates
    fun getSandeshByUser(userNumber: String): Flow<List<Sandesh>> {

        return sandeshDao.getMessagesByUser(userNumber)
    }

    fun observeUser(number: String): Flow<Upbhogta?> {
        return sandeshDao.observeUserByNumber(number)
    }

    fun setAsActiveUser(id: String){
        Knotx.activeScreenUser = id
    }

    suspend fun getOlderMessages(userNumber: String, lastTime: Long, pageSize: Int): List<Sandesh> {
        return sandeshDao.getMessagesBefore(userNumber, lastTime, pageSize)
    }
    suspend fun getSingleSandeshByUserAndId(sandeshId: String, userNumber: String): Sandesh? {
        return sandeshDao.getSandeshByIdAndUser(sandeshId, userNumber)
    }

    fun getStatusJson(userOrGroup: String ,sandeshId: String): String?{
        return groupDao.getStatusJson(userOrGroup,sandeshId)
    }
    suspend fun getFontSizeFlow(): StateFlow<Int> {
        return knotx.settingFlow(SettingPrefKeys.CHAT_CHAT_FONT_SIZE, 18)
    }

    suspend fun chatBehaviourFlow(): StateFlow<Boolean> {
        return knotx.settingFlow(SettingPrefKeys.CHAT_CHAT_ENTER_KEY_BEHAVIOR, true)
    }


    fun deleteMessages(messageIds: Set<String>, userNumber: String, deleteBoth: Boolean) {
        val messageList = messageIds.toList()
        knotx.deleteUserListMessage(messageList,userNumber,true)

        
        if (deleteBoth) {
            val sandeshObject = JsonObject() // ✅ Inner object
            val jsonArray = JsonArray()

            // Add all message IDs to JSON array
            messageIds.forEach { jsonArray.add(it) }

            sandeshObject.add(DootawasMessage.PAATH, jsonArray) // ✅ Add "paath"
            sandeshObject.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.DELETE.value) // ✅ Add "sandeshType"


            val jsonString = Gson().toJson(sandeshObject) // ✅ Convert JSON to string

            val unsentSandesh = UnsentSandesh(
                messId = UUID.randomUUID().toString(),
                receiver = userNumber,
                message = jsonString,  // ✅ Store the JSON string
                dataType = DataType.MESSAGE,
            )
            knotx.sendDelete(unsentSandesh)
            
        }
        }

    suspend fun insertSandesh(sandesh: Sandesh) {
        sandeshDao.insertMessage(sandesh)

    }

    fun generateColorMember(): UserColors {
        return knotx.generateUserColors()
    }
    suspend fun getNameByNumber(number: String): String {
        return knotx.getNameByNumberFromContactOnly(number)
    }

    suspend fun generateAndSaveThumbnailInternal(
        message: JsonObject,
        sandeshId: String,
        userNumber: String,
        isGroup: Boolean
    ) {



            val mutex =
                Knotx.SamagriUpdateLock.getMutexForId(
                    sandeshId
                )

            mutex.withLock {

                if (isGroup) {

                    knotx.generateAndSaveThumbnailInternalGroup(
                        message,
                        sandeshId,
                        userNumber
                    )

                } else {

                    knotx.generateAndSaveThumbnailInternal(
                        message,
                        sandeshId,
                        userNumber
                    )
                }
            }
    }

    val aiWalletBalanceFlow: Flow<AiWalletBalance> = knotx.aiWalletBalanceFlow
    fun sendSandesh(unsentSandesh: UnsentSandesh){
        knotx.sendMessage(unsentSandesh)
    }


    fun sendImage(unsentData: UnsentFiles){
        knotx.sendImage(unsentData)
    }

    suspend fun updateLastMessageToTop(number: String, message: String,messageId: String){
        knotx.checkAndInsertUser(number, message,messageId,true)
    }
    suspend fun updateGroupLastMessageToTop(groupId: String, message: String,messageId: String){
        knotx.checkAndInsertGroup(groupId, message,messageId)
    }
    fun sendLocation(unsentData: UnsentFiles){
        knotx.sendLocation(unsentData)
    }

    fun sendVideo(unsentData: UnsentFiles){
        knotx.sendVideo(unsentData)
    }

    fun sendAudio(unsentData: UnsentFiles){
        knotx.sendAudio(unsentData)
    }
    fun sendDocument(unsentData: UnsentFiles){
        knotx.sendDocument(unsentData)
    }
    fun saveFirstPageAsImage(
        context: Context,
        docUri: Uri
    ): Uri? {
        return knotx.saveFirstPageAsImage(context, docUri)
    }


    fun moveFileToCacheDir(context: Context, uri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val cacheFolder = File(context.cacheDir, "images")
        if (!cacheFolder.exists()) cacheFolder.mkdirs()

        val newFile = File(cacheFolder, "${System.currentTimeMillis()}.jpg")
        inputStream.use { input ->
            FileOutputStream(newFile).use { output ->
                input.copyTo(output)
            }
        }
        return newFile.absolutePath
    }

    fun downloadFile(sandeshId: String, userNumber: String) {
        knotx.downloadFile(sandeshId,userNumber)
    }

    fun processDhrist( userNumber: String) {
        knotx.sendDhrist(userNumber)
    }

    fun sendTypingEvent(userNumber: String) {
        knotx.typingMessage(userNumber)
    }


    suspend fun resetDownloadPercentage(sandeshId: String, userNumber: String) {

        knotx.updateDownloadPercent(userNumber, 0, sandeshId)

    }




    // ✅ Use Flow instead of List, so it auto-updates
    fun getSandeshByGroup(groudID: String): Flow<List<GroupSandesh>> {
        return groupDao.getMessagesByUser(groudID)
    }


    suspend fun getGroupSandeshByIdAndGroupId(sandeshId: String, groupId: String):
            GroupSandesh? {
        return groupDao.getGroupSandeshByIdAndGroupId(sandeshId,groupId)
    }



    fun deleteGroupMessages(messageIds: Set<String>, groupId: String, deleteBoth: Boolean, userNumber: String) {
//        groupDao.deleteMessages(messageIds.toList(),groupId) // ✅ Convert to list before deletion


        val messageList = messageIds.toList()
        knotx.deleteGroupListMessage(messageList,groupId, "You",true)

        if (deleteBoth) {
            val sandeshObject = JsonObject() // ✅ Inner object
            val jsonArray = JsonArray()

            // Add all message IDs to JSON array
            messageIds.forEach { jsonArray.add(it) }

            sandeshObject.add(DootawasMessage.PAATH, jsonArray) // ✅ Add "paath"
            sandeshObject.addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.DELETE.value) // ✅ Add "sandeshType"
            sandeshObject.addProperty(JsonKeys.GROUP_ID, groupId)

            val jsonString = Gson().toJson(sandeshObject) // ✅ Convert JSON to string

            val unsentSandesh = UnsentSandesh(
                messId = UUID.randomUUID().toString(),
                receiver = groupId,
                message = jsonString,  // ✅ Store the JSON string
                dataType = DataType.MESSAGE,
                isGroup = true
            )
            knotx.sendDelete(unsentSandesh)
            
        }

    }

    suspend fun insertSandesh(sandesh: GroupSandesh) {
        
        groupDao.insertMessage(sandesh)

    }

    //todo this method could be reason for time glitch of message
    fun updateLIKHIT(groupId: String, messageId: String, sender: String, time: Long) {
        knotx.updateGroupSandeshStithiTime(groupId, messageId, SandeshStithi.LIKHIT, sender, time)
    }


    fun storeMessagePreview(
        samagri: JsonObject,
        msgId: String,
        isGroup: Boolean,
        groupId: String? = null
    ) {
        knotx.storeMessagePreview(samagri,msgId,isGroup,groupId)
    }
    suspend fun deleteSandesh(sandesh: GroupSandesh) {
        groupDao.deleteMessage(sandesh)
    }



    fun downloadGroupFile(sandeshId: String, groupId: String) {
        knotx.downloadFileGroup(sandeshId,groupId)
    }

    fun processGroupDhrist( groupId: String) {
        knotx.sendGroupDhrist(groupId)
    }

    //region MEMBERS
    suspend fun getMembers(groupId: String): List<GroupMemberEntity> {
        return groupDao.getAllMembersInGroup(groupId)
    }
    fun getMembersFlow(groupId: String): Flow<List<GroupMemberEntity>> {
        return groupDao.getAllMembersInGroupFlow(groupId)
    }

    fun getEntityFlow(groupId: String):Flow<GroupEntity?> {
        return groupDao.getGroupByIdFlow(groupId)
    }

    suspend fun insertMembers(members: List<GroupMemberEntity>) {
        groupDao.insertMembers(members)

    }

    suspend fun insertMember(member: GroupMemberEntity) {
        groupDao.insertMember(member)
    }

    suspend fun deleteMember(groupId: String, userNumber: String) {
        groupDao.deleteMemberById(groupId, userNumber)
    }




    suspend fun updateMemberPermission(
        groupId: String,
        userNumber: String,
        field: String,
        newValue: Boolean
    ) {
        when (field) {
            "canMessage" -> groupDao.updateCanMessage(groupId, userNumber, newValue)
            "canUpdateProfile" -> groupDao.updateCanUpdateProfile(groupId, userNumber, newValue)
            "canModifyMembers" -> groupDao.updateCanModifyMembers(groupId, userNumber, newValue)
            "canStartCall" -> groupDao.updateCanStartCall(groupId, userNumber, newValue)
        }
    }

    fun resetGroupDownloadPercentage(sandeshId: String, userNumber: String) {
        knotx.updateGroupDownloadPercent(userNumber, 0, sandeshId)
    }

        suspend fun resetUnread(id: String, isGroup: Boolean) {
        knotx.resetUnread(id,isGroup)
        }

    suspend fun setProfilePic(path: String, numberOrGroupId: String) {
        groupDao.updateImageUrlByGroupId(path,numberOrGroupId)
        knotx.setGroupProfileImage(path,numberOrGroupId)
    }

    fun checkVisibleProfiles(userNumbers: List<String>) {
        knotx.checkVisibleGroupMembers(userNumbers)
    }

    suspend fun updateDraft(userNumber: String, draft: String, isGroupPassed: Boolean) {

        if (isGroupPassed){
            groupDao.updateDraft(userNumber,draft)
        }else{
            sandeshDao.updateDraft(userNumber, draft)
        }


    }

    fun removeUserFromNotification(user: String){
        knotx.removeUserFromNotification(user)
    }
    fun reactToMsg(emoji: String, msgId: String, groupPassed: Boolean, numberOrGroupId: String) {
        knotx.storeReaction(msgId,emoji,groupPassed,numberOrGroupId,"self")
    }

    suspend fun updateGroupActive(groupId: String) {
        groupDao.updateGroupActive(groupId,false)
    }
    fun addUniversalMessage(
        type: String,
        data: JsonObject,
        groupId: String
    ) {
        knotx.insertUniversalGroupMessage(type,data,groupId)
    }

    suspend fun requestLiveLocationRoute(
        sandeshId: String,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ) {

        knotx.requestLiveLocationRoute(
            sandeshId = sandeshId,
            originLat = originLat,
            originLng = originLng,
            destinationLat = destinationLat,
            destinationLng = destinationLng
        )
    }

    fun stopLiveLocationSharing(
        context: Context,
        sandeshId: String,
        targetId: String,
        isGroup: Boolean
    ) {

        knotx.stopLiveLocationSharing(
            context = context,
            sandeshId = sandeshId,
            targetId = targetId,
            isGroup = isGroup
        )
    }

    fun updateWallpaper(uri: Uri?, userNumberOrGroupId: String, groupPassed: Boolean) {
        knotx.updateWallPaper(uri,userNumberOrGroupId,groupPassed)

    }

    suspend fun updateUserTheme(
        userNumber: String,
        light: DootawasColors,
        dark: DootawasColors,
        borderMode: BorderMode,
        useModernStyle: Boolean, // 👈 1. Added signature parameter
        uuid: String
    ){
        val bundle = StoredThemeBundle(
            light = light.toStored(),
            dark = dark.toStored(),
            borderMode = borderMode.ordinal,
            useModernStyle = useModernStyle // 👈 2. Added to bundle initialization
        )

        val json = knotx.gson.toJson(bundle)

        knotx.updateUserTheme(userNumber, json)

        val messageSamagri = JsonObject().apply {
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.SCREEN_COLORS.value)
            addProperty(
                DootawasMessage.ALL_DATA,
                json
            )
        }

        val unsentSandesh = UnsentSandesh(
            messId = uuid,
            receiver = userNumber,
            message = messageSamagri.toString(),
            dataType = DataType.MESSAGE
        )
        knotx.sendMessage(unsentSandesh)
    }
    suspend fun updateSandeshSamagri(sandeshId: String, samagri: String) {
        sandeshDao.updateSamagri(sandeshId, samagri)
    }
    suspend fun updateGroupSandeshSamagri(sandeshId: String,samagri: String,groupId: String){
        groupDao.updateSamagri(sandeshId,samagri,groupId)
    }

    suspend fun updateLedgerStatusAndBalance(
        sandeshId: String,
        userNumber: String,
        status: LedgerStatus,
        ledgerType: LedgerType,
        amount: Double,
    ) {
        knotx.updateLedgerStatusAndBalance(
            sandeshId, userNumber, status, ledgerType, amount
        )
    }

    fun fetchUserBalance(userNumberOrGroupId: String) {
        knotx.fetchUserSettingsFromServer(userNumberOrGroupId)
    }

    suspend fun updateDisappearingMsg(userNumber: String,duration: Int,) {

        upbhogtaDao.updateAutoDisappearDuration(userNumber,duration)
    }

    suspend fun revealPhoneNumber(userNumberOrGroupId: String) {
        upbhogtaDao.updateRevealSender(userNumberOrGroupId, true)

        val data = JsonObject().apply {
            addProperty("isSelf", true)
        }

            knotx.insertUniversalMessage(
            type = UniversalType.REVEAL_PHONE_NUMBER,
            data = data,
            userNumber = userNumberOrGroupId
        )
                knotx.addRevealedPhoneNumber(userNumberOrGroupId) // 👈 new
    }

    suspend fun testCall(groupId: String) {
        knotx.resetGroupForReinstallTest(groupId)
    }

    suspend fun cleanupExpiredDisappearingMessages(userNumber: String) {
        knotx.cleanupExpiredDisappearingMessages(userNumber)
    }

    suspend fun getUserDisappearDuration(userNumber: String): Int {
        return upbhogtaDao.getAutoDisappearDuration(userNumber) ?: 0
    }

    fun checkSingleProfile(userNumberOrGroupId: String) {
        knotx.checkSingleProfile(userNumberOrGroupId)
    }

    suspend fun updateHideKeyForUser(userNumberOrGroupId: String, passkey: String, isMuted: Boolean) {
        upbhogtaDao.updateHideKey(userNumberOrGroupId,passkey,isMuted)
    }

    fun createOrder(amount: Int, orderType: PurchaseOrderType) {
        knotx.createOrderID(amount,orderType)
    }

    suspend fun fetchAiTokens() {
        knotx.fetchAiTokens()
    }
}
