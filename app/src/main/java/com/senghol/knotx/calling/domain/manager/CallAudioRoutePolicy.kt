package com.senghol.knotx.calling.domain.manager

import com.senghol.knotx.calling.domain.model.CallType

/**
 * Pure function: "given this call and these devices, which output should we use?"
 * No Android classes, so it is trivially unit-testable.
 * Used only by CallAudioManager.
 *
 *  1. User manually picked a route and it is still available -> keep it.
 *  2. Bluetooth connected                 -> BLUETOOTH (voice and video)
 *  3. Wired / USB headset connected       -> WIRED_HEADSET (voice and video)
 *  4. Video call                          -> SPEAKER
 *  5. Voice call                          -> EARPIECE (SPEAKER if no earpiece, e.g. tablet)
 */
object CallAudioRoutePolicy {

    fun pick(
        callType: CallType,
        available: Set<CallAudioRoute>,
        userChoice: CallAudioRoute? = null
    ): CallAudioRoute {
        userChoice?.takeIf { it in available }?.let { return it }

        return when {
            CallAudioRoute.BLUETOOTH in available -> CallAudioRoute.BLUETOOTH
            CallAudioRoute.WIRED_HEADSET in available -> CallAudioRoute.WIRED_HEADSET
            callType == CallType.VIDEO -> CallAudioRoute.SPEAKER
            CallAudioRoute.EARPIECE in available -> CallAudioRoute.EARPIECE
            else -> CallAudioRoute.SPEAKER
        }
    }

    /** Where the speaker button goes when you turn the speaker OFF. */
    fun bestNonSpeaker(available: Set<CallAudioRoute>): CallAudioRoute = when {
        CallAudioRoute.BLUETOOTH in available -> CallAudioRoute.BLUETOOTH
        CallAudioRoute.WIRED_HEADSET in available -> CallAudioRoute.WIRED_HEADSET
        CallAudioRoute.EARPIECE in available -> CallAudioRoute.EARPIECE
        else -> CallAudioRoute.SPEAKER
    }
}