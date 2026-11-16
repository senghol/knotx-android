package com.senghol.knotx.calling.data.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.senghol.knotx.calling.domain.model.CallMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val LOCAL_STREAM_ID = "dootawas_call_stream"
private const val SCREEN_CAPTURE_FPS = 15
private const val SCREEN_CAPTURE_MAX_SIDE = 1280f

class WebRtcManager(
    private val context: Context,
    private val iceServerProvider: IceServerProvider
) {

    private val eglBase: EglBase = EglBase.create()

    private var factory: PeerConnectionFactory? = null
    private var sdkInitialized = false

    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var audioDeviceModule: JavaAudioDeviceModule? = null

    // Camera capture is started lazily. The VideoTrack itself exists for the
    // whole call so it can be negotiated in the very first offer/answer.
    @Volatile
    private var capturing = false

    // Screen sharing feeds the SAME VideoSource as the camera (so no
    // renegotiation is needed): the camera is paused while the screen is shared.
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var screenSurfaceHelper: SurfaceTextureHelper? = null

    @Volatile
    private var screenCapturing = false

    private var resumeCameraAfterShare = false

    // Touched from Main (ViewModel), the signaling thread and WebRTC's own
    // threads, so it must be thread-safe.
    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()

    private val iceLock = Any()
    private val pendingIceCandidates =
        mutableMapOf<String, MutableList<IceCandidate>>()

    private var eventListener: WebRtcEventListener? = null

    private val _localAudioEnabled = MutableStateFlow(true)
    val localAudioEnabled: StateFlow<Boolean> =
        _localAudioEnabled.asStateFlow()

    private val _localVideoEnabled = MutableStateFlow(false)
    val localVideoEnabled: StateFlow<Boolean> =
        _localVideoEnabled.asStateFlow()

    private val _screenSharing = MutableStateFlow(false)
    val screenSharing: StateFlow<Boolean> =
        _screenSharing.asStateFlow()

    private val _remoteStreams =
        MutableStateFlow<Map<String, MediaStream>>(emptyMap())

    val remoteStreams: StateFlow<Map<String, MediaStream>> =
        _remoteStreams.asStateFlow()

    private val disposeMutex = Mutex()
    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @Synchronized
    fun initialize() {
        if (factory != null) return

        if (!sdkInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                    .builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )

            sdkInitialized = true
        }

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )

        val decoderFactory = DefaultVideoDecoderFactory(
            eglBase.eglBaseContext
        )

        audioDeviceModule = JavaAudioDeviceModule.builder(context.applicationContext)
            .setAudioRecordErrorCallback(
                object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                    override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                        Log.e("WebRTC_ADM", "record init error: $errorMessage")
                    }

                    override fun onWebRtcAudioRecordStartError(
                        errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode,
                        errorMessage: String
                    ) {
                        Log.e("WebRTC_ADM", "record start error: $errorCode $errorMessage")
                    }

                    override fun onWebRtcAudioRecordError(errorMessage: String) {
                        Log.e("WebRTC_ADM", "record error: $errorMessage")
                    }
                }
            )
            .setAudioTrackErrorCallback(
                object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                    override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                        Log.e("WebRTC_ADM", "playout init error: $errorMessage")
                    }

                    override fun onWebRtcAudioTrackStartError(
                        errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode,
                        errorMessage: String
                    ) {
                        Log.e("WebRTC_ADM", "playout start error: $errorCode $errorMessage")
                    }

                    override fun onWebRtcAudioTrackError(errorMessage: String) {
                        Log.e("WebRTC_ADM", "playout error: $errorMessage")
                    }
                }
            )
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun setEventListener(listener: WebRtcEventListener?) {
        eventListener = listener
    }

    // -------------------------------------------------------------------------
    // Local audio
    // -------------------------------------------------------------------------

    @Synchronized
    fun createLocalAudioTrack(): AudioTrack? {
        initialize()

        audioTrack?.let { return it }

        val peerFactory = factory ?: return null

        val constraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googEchoCancellation",
                    "true"
                )
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googAutoGainControl",
                    "true"
                )
            )
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "googNoiseSuppression",
                    "true"
                )
            )
        }

        audioSource = peerFactory.createAudioSource(constraints)

        audioTrack = peerFactory.createAudioTrack(
            "audio_local",
            audioSource
        )

        audioTrack?.setEnabled(_localAudioEnabled.value)

        return audioTrack
    }

    fun setAudioEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
        _localAudioEnabled.value = enabled
    }

    fun getLocalAudioTrack(): AudioTrack? {
        return audioTrack
    }

    // -------------------------------------------------------------------------
    // Local video
    // -------------------------------------------------------------------------

    /**
     * Creates the VideoSource + VideoTrack WITHOUT opening the camera.
     * Called for every call (voice too) so the video m-line is part of the
     * first SDP exchange and camera on/off never needs renegotiation.
     */
    @Synchronized
    fun ensureLocalVideoTrack(): VideoTrack? {
        initialize()

        videoTrack?.let { return it }

        val peerFactory = factory ?: return null

        val source = peerFactory.createVideoSource(false)
        videoSource = source

        videoTrack = peerFactory.createVideoTrack("video_local", source)
        videoTrack?.setEnabled(_localVideoEnabled.value)

        return videoTrack
    }

    /** Ensures the track exists and the camera is capturing. */
    fun createLocalVideoTrack(): VideoTrack? {
        val track = ensureLocalVideoTrack() ?: return null
        return if (startCameraCapture()) track else null
    }

    @Synchronized
    private fun startCameraCapture(): Boolean {
        if (capturing) return true

        val source = videoSource ?: return false

        if (videoCapturer == null) {
            val enumerator = Camera2Enumerator(context)

            val cameraName =
                enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                    ?: enumerator.deviceNames.firstOrNull()
                    ?: return false

            val capturer = enumerator.createCapturer(cameraName, null)
                ?: return false

            val helper = SurfaceTextureHelper.create(
                "DootawasCamera",
                eglBase.eglBaseContext
            )

            capturer.initialize(helper, context, source.capturerObserver)

            videoCapturer = capturer
            surfaceTextureHelper = helper
        }

        return try {
            videoCapturer?.startCapture(1280, 720, 30)
            capturing = true
            true
        } catch (e: Exception) {
            Log.e("WebRTC", "startCapture failed", e)
            false
        }
    }

    @Synchronized
    private fun stopCameraCapture() {
        if (!capturing) return

        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.e("WebRTC", "stopCapture failed", e)
        }

        capturing = false
    }

    fun setVideoEnabled(enabled: Boolean) {

        if (enabled) {
            // Turning the camera on while sharing the screen replaces the share.
            if (screenCapturing) stopScreenCapture(restoreCamera = false)

            ensureLocalVideoTrack()
            val started = startCameraCapture()
            videoTrack?.setEnabled(started)
            _localVideoEnabled.value = started
        } else {
            if (screenCapturing) {
                // The track is carrying the screen, don't black it out.
                resumeCameraAfterShare = false
                _localVideoEnabled.value = false
                return
            }

            videoTrack?.setEnabled(false)
            // Releases the camera (and the privacy indicator).
            stopCameraCapture()
            _localVideoEnabled.value = false
        }
    }

    fun switchCamera(onResult: (Boolean) -> Unit = {}) {
        val capturer = videoCapturer as? CameraVideoCapturer ?: return

        capturer.switchCamera(
            object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    onResult(isFrontCamera)
                }

                override fun onCameraSwitchError(errorDescription: String) {
                    Log.e(
                        "CameraSwitch",
                        "Camera switch failed: $errorDescription"
                    )
                }
            }
        )
    }

    fun getLocalVideoTrack(): VideoTrack? {
        return videoTrack
    }

    // -------------------------------------------------------------------------
    // Peer connection
    // -------------------------------------------------------------------------

    @Synchronized
    fun createPeerConnection(
        participantId: String
    ): PeerConnection? {
        initialize()

        peerConnections[participantId]?.let {
            return it
        }

        val peerFactory = factory ?: return null

        val iceServers = iceServerProvider
            .getConfig()
            .iceServers
            .map { server ->
                PeerConnection.IceServer
                    .builder(server.urls)
                    .apply {
                        server.username?.let {
                            setUsername(it)
                        }

                        server.credential?.let {
                            setPassword(it)
                        }
                    }
                    .createIceServer()
            }

        val rtcConfig = PeerConnection.RTCConfiguration(
            iceServers
        ).apply {
            sdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN

            tcpCandidatePolicy =
                PeerConnection.TcpCandidatePolicy.ENABLED

            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            iceTransportsType =
                PeerConnection.IceTransportsType.ALL

            bundlePolicy =
                PeerConnection.BundlePolicy.MAXBUNDLE

            rtcpMuxPolicy =
                PeerConnection.RtcpMuxPolicy.REQUIRE

            keyType =
                PeerConnection.KeyType.ECDSA
        }

        var created: PeerConnection? = null

        val peerConnection =
            peerFactory.createPeerConnection(
                rtcConfig,
                createPeerConnectionObserver(participantId) { created }
            ) ?: return null

        created = peerConnection
        peerConnections[participantId] = peerConnection

        // Tracks must exist BEFORE the first SDP. Never create a PC first and
        // "hope" tracks get added later.
        createLocalAudioTrack()
        ensureLocalVideoTrack()
        addLocalTracks(peerConnection)

        return peerConnection
    }

    private fun addLocalTracks(
        peerConnection: PeerConnection
    ) {
        // ONE stream id for audio + video. With two different ids the remote
        // gets two MediaStreams and the second onAddTrack overwrites the first
        // in remoteStreams, so one of audio/video randomly goes missing.
        audioTrack?.let { track ->
            peerConnection.addTrack(track, listOf(LOCAL_STREAM_ID))
        }

        videoTrack?.let { track ->
            peerConnection.addTrack(track, listOf(LOCAL_STREAM_ID))
        }
    }

    fun getPeerConnection(
        participantId: String
    ): PeerConnection? {
        return peerConnections[participantId]
    }

    fun removePeerConnection(
        participantId: String
    ) {
        peerConnections.remove(participantId)?.let {
            it.close()
            it.dispose()
        }
        synchronized(iceLock) { pendingIceCandidates.remove(participantId) }
        removeRemoteStream(participantId)
    }

    fun removeAllPeerConnections() {
        val all = peerConnections.values.toList()
        peerConnections.clear() // detach first, then close

        all.forEach {
            it.close()
            it.dispose()
        }

        synchronized(iceLock) { pendingIceCandidates.clear() }
        _remoteStreams.value = emptyMap()
    }

    fun getPeerConnectionCount(): Int {
        return peerConnections.size
    }

    // -------------------------------------------------------------------------
    // SDP
    // -------------------------------------------------------------------------

    suspend fun createOffer(
        participantId: String
    ): SessionDescription? {
        val peerConnection =
            peerConnections[participantId] ?: return null

        return suspendCancellableCoroutine { continuation ->
            peerConnection.createOffer(
                object : SdpObserverAdapter() {

                    override fun onCreateSuccess(
                        description: SessionDescription
                    ) {
                        peerConnection.setLocalDescription(
                            object : SdpObserverAdapter() {

                                override fun onSetSuccess() {
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            description
                                        )
                                    }
                                }

                                override fun onSetFailure(
                                    error: String
                                ) {
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(
                                            IllegalStateException(error)
                                        )
                                    }
                                }
                            },
                            description
                        )
                    }

                    override fun onCreateFailure(
                        error: String
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(error)
                            )
                        }
                    }
                },
                MediaConstraints()
            )
        }
    }

    suspend fun restartIce(participantId: String): SessionDescription? {
        val peerConnection = peerConnections[participantId] ?: return null

        return suspendCancellableCoroutine { continuation ->
            val constraints = MediaConstraints().apply {
                mandatory.add(
                    MediaConstraints.KeyValuePair("IceRestart", "true")
                )
            }

            peerConnection.createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        peerConnection.setLocalDescription(
                            object : SdpObserver {
                                override fun onSetSuccess() {
                                    if (continuation.isActive) {
                                        continuation.resume(description)
                                    }
                                }

                                override fun onSetFailure(error: String) {
                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }

                                override fun onCreateSuccess(description: SessionDescription) = Unit
                                override fun onCreateFailure(error: String) = Unit
                            },
                            description
                        )
                    }

                    override fun onCreateFailure(error: String) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onSetSuccess() = Unit
                    override fun onSetFailure(error: String) = Unit
                },
                constraints
            )
        }
    }

    suspend fun createAnswer(
        participantId: String
    ): SessionDescription? {
        val peerConnection =
            peerConnections[participantId] ?: return null

        return suspendCancellableCoroutine { continuation ->
            peerConnection.createAnswer(
                object : SdpObserverAdapter() {

                    override fun onCreateSuccess(
                        description: SessionDescription
                    ) {
                        peerConnection.setLocalDescription(
                            object : SdpObserverAdapter() {

                                override fun onSetSuccess() {
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            description
                                        )
                                    }
                                }

                                override fun onSetFailure(
                                    error: String
                                ) {
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(
                                            IllegalStateException(error)
                                        )
                                    }
                                }
                            },
                            description
                        )
                    }

                    override fun onCreateFailure(
                        error: String
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(error)
                            )
                        }
                    }
                },
                MediaConstraints()
            )
        }
    }

    suspend fun setRemoteDescription(
        participantId: String,
        description: SessionDescription
    ): Boolean {
        val peerConnection =
            peerConnections[participantId] ?: return false

        return suspendCancellableCoroutine { continuation ->
            peerConnection.setRemoteDescription(
                object : SdpObserverAdapter() {

                    override fun onSetSuccess() {
                        flushPendingIceCandidates(
                            participantId,
                            peerConnection
                        )

                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onSetFailure(
                        error: String
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                },
                description
            )
        }
    }

    // -------------------------------------------------------------------------
    // ICE
    // -------------------------------------------------------------------------

    fun addIceCandidate(
        participantId: String,
        candidate: IceCandidate
    ): Boolean {
        val peerConnection = peerConnections[participantId]

        // Decide "apply now vs queue" under the same lock the flush uses, so a
        // candidate can't slip in between setRemoteDescription finishing and
        // the queue being drained.
        val readyConnection: PeerConnection? = synchronized(iceLock) {
            if (peerConnection == null || peerConnection.remoteDescription == null) {
                pendingIceCandidates
                    .getOrPut(participantId) { mutableListOf() }
                    .add(candidate)
                null
            } else {
                peerConnection
            }
        }

        return readyConnection?.addIceCandidate(candidate) ?: false
    }

    private fun flushPendingIceCandidates(
        participantId: String,
        peerConnection: PeerConnection
    ) {
        val candidates = synchronized(iceLock) {
            pendingIceCandidates.remove(participantId)
        } ?: return

        candidates.forEach { candidate ->
            peerConnection.addIceCandidate(candidate)
        }
    }

    // -------------------------------------------------------------------------
    // Remote streams
    // -------------------------------------------------------------------------

    private fun addRemoteStream(
        participantId: String,
        stream: MediaStream
    ) {
        _remoteStreams.value =
            _remoteStreams.value +
                    (participantId to stream)
    }

    private fun removeRemoteStream(
        participantId: String
    ) {
        _remoteStreams.value =
            _remoteStreams.value -
                    participantId
    }

    // -------------------------------------------------------------------------
    // Screen sharing
    // -------------------------------------------------------------------------

    /**
     * Starts sharing the screen.
     *
     * [permissionData] is the Intent returned by the system screen-capture
     * dialog. On Android 10+ a foreground service of type mediaProjection MUST
     * already be running (CallService does that before calling this), and on
     * Android 14 the token may only be used once.
     */
    @Synchronized
    fun startScreenCapture(permissionData: Intent): Boolean {
        if (screenCapturing) return true

        ensureLocalVideoTrack()
        val source = videoSource ?: return false

        // Same VideoSource as the camera: pause the camera first.
        resumeCameraAfterShare = _localVideoEnabled.value
        stopCameraCapture()

        val capturer = ScreenCapturerAndroid(
            permissionData,
            object : MediaProjection.Callback() {
                override fun onStop() {
                    // User stopped it from the system UI (status-bar chip /
                    // notification) or the projection was revoked.

                    // This callback runs on the capturer's own thread, while
                    // startScreenCapture() may be holding our lock and waiting
                    // for that thread. Hop to main to avoid a deadlock.
                    Handler(Looper.getMainLooper()).post { stopScreenCapture() }
                }

                // Android 14+ "share one app": what is captured is THAT APP'S
                // WINDOW, not the screen, and its size changes (rotation,
                // split-screen, resize). Follow it, or the picture is
                // scaled wrong / blank. Same thread rule as onStop: hop to main.
                override fun onCapturedContentResize(width: Int, height: Int) {
                    Handler(Looper.getMainLooper()).post {
                        resizeScreenCapture(width, height)
                    }
                }

                // The shared app went to the background / came back. While it
                // is hidden Android sends no frames.
                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                }
            }
        )

        val helper = SurfaceTextureHelper.create(
            "DootawasScreen",
            eglBase.eglBaseContext
        )

        return try {
            capturer.initialize(helper, context, source.capturerObserver)

            val (width, height) = screenCaptureSize()
            capturer.startCapture(width, height, SCREEN_CAPTURE_FPS)

            screenCapturer = capturer
            screenSurfaceHelper = helper
            screenCapturing = true

            videoTrack?.setEnabled(true)
            _localVideoEnabled.value = false // what we send is the screen, not the camera
            _screenSharing.value = true

            true
        } catch (e: Exception) {
            Log.e("WebRTC", "Screen capture failed", e)

            runCatching { capturer.dispose() }
            helper.dispose()

            restoreCameraAfterShare()
            false
        }
    }

    /** Matches the capture to the shared app's window (single-app sharing). */
    @Synchronized
    private fun resizeScreenCapture(contentWidth: Int, contentHeight: Int) {
        val capturer = screenCapturer ?: return
        if (!screenCapturing || contentWidth <= 0 || contentHeight <= 0) return

        val scale = minOf(
            1f,
            SCREEN_CAPTURE_MAX_SIDE / maxOf(contentWidth, contentHeight).toFloat()
        )

        val width = (contentWidth * scale).toInt() and 1.inv()
        val height = (contentHeight * scale).toInt() and 1.inv()

        if (width <= 0 || height <= 0) return

        runCatching {
            capturer.changeCaptureFormat(width, height, SCREEN_CAPTURE_FPS)
        }.onSuccess {
        }.onFailure {
            Log.e("WebRTC", "Screen capture resize failed", it)
        }
    }

    @Synchronized
    fun stopScreenCapture(restoreCamera: Boolean = true) {
        if (screenCapturer == null && !screenCapturing) {
            _screenSharing.value = false
            return
        }

        screenCapturing = false

        runCatching { screenCapturer?.stopCapture() }
        runCatching { screenCapturer?.dispose() }
        screenCapturer = null

        screenSurfaceHelper?.dispose()
        screenSurfaceHelper = null

        _screenSharing.value = false

        if (restoreCamera) {
            restoreCameraAfterShare()
        } else {
            resumeCameraAfterShare = false
        }

    }

    private fun restoreCameraAfterShare() {
        if (resumeCameraAfterShare) {
            resumeCameraAfterShare = false
            setVideoEnabled(true) // camera was on before sharing
        } else {
            videoTrack?.setEnabled(false)
        }
    }

    // Long side capped at 1280, even numbers (encoders need them).
    private fun screenCaptureSize(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val longSide = maxOf(metrics.widthPixels, metrics.heightPixels).toFloat()
        val scale = minOf(1f, SCREEN_CAPTURE_MAX_SIDE / longSide)

        val width = (metrics.widthPixels * scale).toInt() and 1.inv()
        val height = (metrics.heightPixels * scale).toInt() and 1.inv()

        return width to height
    }

    fun isScreenSharing(): Boolean {
        return _screenSharing.value
    }

    // -------------------------------------------------------------------------
    // PeerConnection observer
    // -------------------------------------------------------------------------

    private fun createPeerConnectionObserver(
        participantId: String,
        self: () -> PeerConnection?
    ): PeerConnection.Observer {
        // A PC that was removed/closed keeps firing callbacks (CLOSED, FAILED...)
        // on WebRTC's thread, possibly AFTER a new call has started. Without
        // this check those late events could end or fail the NEW call.
        fun isCurrent(): Boolean {
            val me = self() ?: return false
            return peerConnections[participantId] === me
        }

        return object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                if (!isCurrent()) return
                eventListener?.onIceCandidate(participantId, candidate)
            }

            override fun onAddStream(stream: MediaStream) {
                if (!isCurrent()) return

                addRemoteStream(participantId, stream)


                eventListener?.onRemoteStream(participantId, stream)

            }

            override fun onRemoveStream(stream: MediaStream) {
                removeRemoteStream(participantId)
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}

            override fun onIceConnectionChange(
                state: PeerConnection.IceConnectionState
            ) {
                if (!isCurrent()) return
                eventListener?.onIceConnectionStateChanged(participantId, state)
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}

            override fun onIceCandidatesRemoved(
                candidates: Array<out IceCandidate>
            ) {}

            override fun onAddTrack(
                receiver: RtpReceiver,
                mediaStreams: Array<out MediaStream>
            ) {
                if (!isCurrent()) return

                val track = receiver.track()


                mediaStreams
                    .firstOrNull()
                    ?.let { stream ->

                        addRemoteStream(participantId, stream)


                        eventListener?.onRemoteStream(
                            participantId,
                            stream
                        )

                    }
            }

            override fun onTrack(
                transceiver: RtpTransceiver
            ) {
                val receiver = transceiver.receiver
                val track = receiver.track()

            }

            override fun onDataChannel(dataChannel: DataChannel) {}

            override fun onRenegotiationNeeded() {}

            override fun onConnectionChange(
                newState: PeerConnection.PeerConnectionState
            ) {
                if (!isCurrent()) return
                eventListener?.onConnectionStateChanged(
                    participantId,
                    newState
                )
            }
        }
    }
    // -------------------------------------------------------------------------
    // EGL / local resources
    // -------------------------------------------------------------------------

    fun getEglBaseContext(): EglBase.Context {
        return eglBase.eglBaseContext
    }

    // -------------------------------------------------------------------------
    // Per-call cleanup
    // -------------------------------------------------------------------------

    suspend fun dispose() {
        disposeMutex.withLock {
            // Close PCs first so nothing is still reading the tracks.
            removeAllPeerConnections()

            // Stop the screen share BEFORE the video source is disposed.
            stopScreenCapture(restoreCamera = false)

            stopCameraCapture()

            videoCapturer?.let { capturer ->
                try {
                    capturer.dispose()
                } catch (_: Exception) {
                }
            }

            surfaceTextureHelper?.dispose()

            videoTrack?.dispose()
            audioTrack?.dispose()
            videoSource?.dispose()
            audioSource?.dispose()

            videoCapturer = null
            surfaceTextureHelper = null
            videoTrack = null
            audioTrack = null
            videoSource = null
            audioSource = null

            // The PeerConnectionFactory and audio device module are kept
            // alive between calls: recreating them per call is slow and was
            // racing with the next call's track creation (recall).
            _localAudioEnabled.value = true
            _localVideoEnabled.value = false
            _screenSharing.value = false
            _remoteStreams.value = emptyMap()
        }
    }

    // Final application shutdown only.
    // Do NOT call this after every call.
    suspend fun shutdown() {
        dispose()
        eventListener = null

        factory?.dispose()
        factory = null
        audioDeviceModule?.release()
        audioDeviceModule = null

        if (sdkInitialized) {
            eglBase.release()
        }
    }
}