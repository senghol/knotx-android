package com.senghol.knotx.ui.calling

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallEndedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(true)

        val activity = this

        val isVideoCall =
            intent.getBooleanExtra("video", false)

        val callDuration =
            intent.getLongExtra("duration", -1L)
                .takeIf { it >= 0 }

        val userName =
            intent.getStringExtra("name")

        val userNumber =
            intent.getStringExtra("number") ?: ""

        val userImageUrl =
            intent.getStringExtra("image")

        setContent {

            MaterialTheme {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.45f)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                MutableInteractionSource()
                            }
                        ) {
                            activity.finish()
                        },
                    contentAlignment = Alignment.Center
                ) {

                    CallEndedView(
                        isVideoCall = isVideoCall,
                        callDuration = callDuration,
                        userName = userName,
                        userNumber = userNumber,
                        userImageUrl = userImageUrl,

                        onBack = {
                            activity.finish()
                        },

                        onAudioRecall = {

                            val intent = Intent(
                                activity,
                                CallActivity2::class.java
                            ).apply {

                                putExtra(
                                    "userNumber",
                                    userNumber
                                )

                                putExtra(
                                    "outgoing",
                                    true
                                )

                                putExtra(
                                    "video",
                                    false
                                )

                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }

                            activity.startActivity(intent)

                            activity.finish()
                        },

                        onVideoRecall = {

                            val intent = Intent(
                                activity,
                                CallActivity2::class.java
                            ).apply {

                                putExtra(
                                    "userNumber",
                                    userNumber
                                )

                                putExtra(
                                    "outgoing",
                                    true
                                )

                                putExtra(
                                    "video",
                                    true
                                )

                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }

                            activity.startActivity(intent)

                            activity.finish()
                        }
                    )
                }
            }
        }

        // ⏳ Auto dismiss
        lifecycleScope.launch {

            delay(7_000)

            finish()
        }
    }
}