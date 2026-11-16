package com.senghol.knotx.calling

import android.content.Context
import com.senghol.knotx.data.sqldelight.SengholDB
import com.senghol.knotx.calling.data.webrtc.IceServerProvider
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.manager.CallAudioManager
import com.senghol.knotx.calling.domain.manager.CallCameraManager
import com.senghol.knotx.calling.domain.manager.CallMediaManager
import com.senghol.knotx.calling.domain.manager.CallNetworkManager
import com.senghol.knotx.calling.domain.manager.CallManager
import com.senghol.knotx.calling.domain.repository.CallRepository
import com.senghol.knotx.calling.data.signaling.CallSignalingManager
import com.senghol.knotx.calling.domain.repository.CallParticipantRepository
import com.senghol.knotx.calling.data.repository.CallParticipantRepositoryImpl
import com.senghol.knotx.calling.service.CallService
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.ContactDao
import com.senghol.knotx.data.database.UpbhogtaDao

import com.senghol.knotx.calling.data.repository.CallRepositorySqlDelightImpl
import com.senghol.knotx.calling.domain.manager.CallSoundManager
import com.senghol.knotx.calling.permission.CallPermissionManager
import com.senghol.knotx.calling.ui.CallNotificationManager
import com.senghol.knotx.data.database.DootawasDatabase


class CallContainer(
    context: Context,
    private val dootawasDatabase: DootawasDatabase,
    private val sqlDelightDatabase: SengholDB,
    knotx: Knotx
) {
    private val appContext = context.applicationContext

    private val upbhogtaDao: UpbhogtaDao by lazy {
        dootawasDatabase.upbhogtaDao()
    }

    private val contactDao: ContactDao by lazy {
        dootawasDatabase.contactDao()
    }

    val iceServerProvider = IceServerProvider()

    val webRtcManager = WebRtcManager(
        context = appContext,
        iceServerProvider = iceServerProvider
    )

    val callRepository: CallRepository =
        CallRepositorySqlDelightImpl(
            database = sqlDelightDatabase
        )

    val callManager = CallManager(
        repository = callRepository,
        webRtcManager = webRtcManager
    )

    val callMediaManager = CallMediaManager(
        webRtcManager = webRtcManager
    )

    val callAudioManager = CallAudioManager(
        context = appContext,
        webRtcManager = webRtcManager
    )

    val callCameraManager = CallCameraManager(
        webRtcManager = webRtcManager
    )

    val callNetworkManager = CallNetworkManager(
        context = appContext
    )

    val callParticipantRepository: CallParticipantRepository =
        CallParticipantRepositoryImpl(
            contactDao = contactDao,
            upbhogtaDao = upbhogtaDao
        )

    val callPermissionManager = CallPermissionManager(appContext)
    val callNotificationManager = CallNotificationManager(
        context = appContext,
        callParticipantRepository = callParticipantRepository
    )

    // NEW: must be created before callSignalingManager
    val callSoundManager = CallSoundManager(
        context = appContext,
        callManager = callManager
    )

    val callSignalingManager = CallSignalingManager(
        callManager = callManager,
        webRtcManager = webRtcManager,
        callMediaManager = callMediaManager,
        callAudioManager = callAudioManager,
        callCameraManager = callCameraManager,
        callNetworkManager = callNetworkManager,
        knotx = knotx,
        callNotificationManager = callNotificationManager,
        callSoundManager = callSoundManager          // NEW
    )
    fun startCallService() {
        CallService.start(appContext)
    }

    fun stopCallService() {
        CallService.stop(appContext)
    }

    init {
        callNetworkManager.start()
        callManager.start()
        callSoundManager.start()                     // NEW
    }
}