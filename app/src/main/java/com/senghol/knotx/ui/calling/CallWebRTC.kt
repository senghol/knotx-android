package com.senghol.knotx.ui.calling

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.senghol.knotx.ui.calling.EglUtils.eglBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
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
import org.webrtc.NetworkMonitor
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class CallWebRTC(
    private val context: Context,
    private val eglBaseContext: EglBase.Context,
    private val isVideoCall: Boolean,
    private val signalingListener: SignalingListener
) {

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var localVideoSource: VideoSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var audioSource: AudioSource? = null



    fun initPeerConnectionFactory() {
        // Initialize WebRTC library
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
//                .setNativeLibraryName("webrtc") // Explicitly set library name (optional,
                // ensures correct loading)
                .createInitializationOptions()
        )

        // Configure factory options (optional, can customize if needed)
        val options = PeerConnectionFactory.Options()
        // Example: options.disableEncryption = false (default, keep enabled for security)

        // Video encoder/decoder factories, excluding H.264
        val encoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, false) // enableH264=false
        val decoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

        // Create PeerConnectionFactory
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("turn:coturn.api.knotx.com:3478?transport=udp")
                .setUsername("dakiya")
                .setPassword("dakiyapass")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:coturn.api.knotx.com:3478?transport=tcp")
                .setUsername("dakiya")
                .setPassword("dakiyapass")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            keyType = PeerConnection.KeyType.ECDSA // Default, but explicit

            // No need to set enableDtlsSrtp anymore; it is always enabled.
        }


        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                    signalingListener.onIceCandidateGenerated(candidate)

            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    //track.addSink(remoteRenderer)
                    signalingListener.onRemoteTrackReceived(track)
                }
            }

            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
                
                // Optional: If you want to handle audio tracks too
                val track = receiver.track()
                if (track is AudioTrack) {
                    // Add audio sinks or manage audio
                }
            }
            override fun onAddStream(stream: MediaStream?) {
//                stream?.videoTracks?.firstOrNull()?.let { videoTrack ->
//                    
//                    videoTrack.addSink(remoteRenderer)
//                    signalingListener.onRemoteTrackReceived(videoTrack)
//                }
            }


            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        
                        signalingListener.onCallConnected()
                    }

                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        
                        signalingListener.onCallDisconnectedTemporarily() // try reconnect
                    }

                    PeerConnection.IceConnectionState.CLOSED -> {
                        
// \                       signalingListener.onCallEnded() // no reconnect
                    }

                    else -> {
                        
                    }
                }
            }


            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onRemoveStream(stream: MediaStream) {} // Optional: can be removed
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
        })
    }


    fun startLocalMedia(isMicEnabled: Boolean) {
        audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("AUDIO", audioSource)

        // ✅ APPLY STATE HERE (CRITICAL)
        localAudioTrack?.setEnabled(isMicEnabled)

        // 🔥 IMPORTANT: sync with UI state
        if (isVideoCall) {
            videoCapturer = createVideoCapturer()
            localVideoSource = peerConnectionFactory.createVideoSource(false)
            videoCapturer?.initialize(
                SurfaceTextureHelper.create("CaptureThread", eglBaseContext),
                context,
                localVideoSource?.capturerObserver
            )
            videoCapturer?.startCapture(1280, 720, 30)
            if (localVideoSource == null) {
                Log.e("WebRTC", "❌ localVideoSource is null — skipping video track creation")
                return
            }
            localVideoTrack = peerConnectionFactory.createVideoTrack("VIDEO", localVideoSource)
            signalingListener.onLocalTrackCreated(localVideoTrack!!)
        }

        // ✅ Instead of creating and adding a stream:
        peerConnection?.addTrack(localAudioTrack, listOf("stream1"))
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("stream1"))
        }
    }



    fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        signalingListener.onOfferCreated(desc)
                    }
                    override fun onSetFailure(msg: String?) {}
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onCreateFailure(msg: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        signalingListener.onAnswerCreated(desc)
                    }
                    override fun onSetFailure(msg: String?) {}
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onCreateFailure(msg: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }
    var isPeerReady = false

    fun setRemoteDescription(sdp: SessionDescription, onSuccess: (() -> Unit)? = null) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                isPeerReady = true
                
                onSuccess?.invoke()
            }

            override fun onSetFailure(error: String?) {
                
            }

            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (peerConnection?.remoteDescription != null) {
            peerConnection?.addIceCandidate(candidate)
        }else{
            
        }
//        
//        peerConnection?.addIceCandidate(candidate)
    }

    fun setLocalDescription(sdp: SessionDescription) {
        peerConnection?.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sdp)
    }



    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        // Will be used when remote stream becomes available
    }

    fun toggleMic(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera(onResult: (Boolean) -> Unit) {
        val capturer = videoCapturer as? CameraVideoCapturer ?: return

        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {

                // 🔥 send result back
                onResult(isFrontCamera)
            }

            override fun onCameraSwitchError(errorDescription: String) {
                Log.e("WebRTC", "❌ Camera switch error: $errorDescription")
            }
        })
    }

    fun stopLocalMedia() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}

        try {
            videoCapturer?.dispose()
            videoCapturer = null
        } catch (_: Exception) {}

        try {
            localVideoSource?.dispose()
            localVideoSource = null
        } catch (_: Exception) {}

        try {
            localAudioTrack?.dispose()
            localAudioTrack = null
        } catch (_: Exception) {}

        try {
            audioSource?.dispose()
            audioSource = null
        } catch (_: Exception) {}
    }

    private inline fun safely(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            
        }
    }

    fun MediaStreamTrack?.safeDispose() {
        try {
            this?.dispose()
        } catch (e: IllegalStateException) {
            
        } catch (e: Exception) {
            
        }
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val releaseContext = newSingleThreadContext("WebRTCReleaseThread")
    var isReleased = false
    @OptIn(ExperimentalCoroutinesApi::class)
    fun releaseSafe() {
        
        if (isReleased) return
        isReleased = true
        
        isPeerReady = false

        CoroutineScope(releaseContext).launch {
            try {
                peerConnection?.close()
            } catch (e: Exception) {
                Log.w("WebRTC", "peerConnection.close failed: ${e.message}", e)
            } finally {
                peerConnection = null
            }

            try {
                videoCapturer?.stopCapture()
            } catch (e: Exception) {
                Log.w("WebRTC", "videoCapturer.stopCapture failed: ${e.message}", e)
            }

            try {
                videoCapturer?.dispose()
            } catch (e: Exception) {
                Log.w("WebRTC", "videoCapturer.dispose failed: ${e.message}", e)
            } finally {
                videoCapturer = null
            }

            try {
                localVideoSource?.dispose()
            } catch (e: Exception) {
                Log.w("WebRTC", "localVideoSource.dispose failed: ${e.message}", e)
            } finally {
                localVideoSource = null
            }

            try {
                localAudioTrack?.safeDispose()
            } catch (e: Exception) {
                Log.w("WebRTC", "localAudioTrack.safeDispose failed: ${e.message}", e)
            } finally {
                localAudioTrack = null
            }

            try {
                audioSource?.dispose()
            } catch (e: Exception) {
                Log.w("WebRTC", "audioSource.dispose failed: ${e.message}", e)
            } finally {
                audioSource = null
            }

            try {
                localRenderer?.release()
            } catch (e: Exception) {
                Log.w("WebRTC", "localRenderer.release failed: ${e.message}", e)
            } finally {
                localRenderer = null
            }

            try {
                remoteRenderer?.release()
            } catch (e: Exception) {
                Log.w("WebRTC", "remoteRenderer.release failed: ${e.message}", e)
            } finally {
                remoteRenderer = null
            }

            try {
                peerConnectionFactory.dispose()
            } catch (e: Exception) {
                Log.w("WebRTC", "peerConnectionFactory.dispose failed: ${e.message}", e)
            }

            // Optional EGL release (commented out in your version)
            // try {
            //     eglBase.release()
            // } catch (e: Exception) {
            //     Log.w("WebRTC", "eglBase.release failed: ${e.message}", e)
            // }
        }
    }
    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        return null
    }

    interface SignalingListener {
        fun onOfferCreated(sdp: SessionDescription)
        fun onAnswerCreated(sdp: SessionDescription)
        fun onIceCandidateGenerated(candidate: IceCandidate)
        fun onRemoteStreamAvailable()
        fun onRemoteTrackReceived(track: VideoTrack)
        fun onLocalTrackCreated(track: VideoTrack)
        fun onCallConnected()
        fun onCallDisconnectedTemporarily() // 🔄 new: auto-reconnect point
        fun onCallEnded()
    }

}
