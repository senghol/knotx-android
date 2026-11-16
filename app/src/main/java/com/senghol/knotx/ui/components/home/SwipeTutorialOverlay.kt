package com.senghol.knotx.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SwipeTutorialOverlay(
    onDismiss: () -> Unit
) {

    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(Unit) {

        while (true) {

            // 👉 RIGHT = AUDIO CALL
            offsetX.animateTo(
                120f,
                animationSpec = tween(600)
            )

            delay(400)

            offsetX.animateTo(
                0f,
                animationSpec = tween(400)
            )

            delay(300)

            // 👈 LEFT = VIDEO CALL
            offsetX.animateTo(
                -120f,
                animationSpec = tween(600)
            )

            delay(400)

            offsetX.animateTo(
                0f,
                animationSpec = tween(400)
            )

            delay(800)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {}
    ) {

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Swipe to Call",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // 🔄 Directions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // 👈 LEFT = VIDEO
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {



                    Column {

                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF81C784)
                        )

                        Text(
                            "Video Call",
                            color = Color(0xFF81C784),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF81C784)
                    )


                }

                // 👉 RIGHT = AUDIO
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF4FC3F7)
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = Color(0xFF4FC3F7)
                        )

                        Text(
                            "Audio Call",
                            color = Color(0xFF4FC3F7),
                            fontSize = 12.sp
                        )
                    }


                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {

                // BACKGROUND ACTIONS
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    // 👈 LEFT = VIDEO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF81C784).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.CenterStart
                    ) {

                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // 👉 RIGHT = AUDIO
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF4FC3F7).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.CenterEnd
                    ) {

                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }

                // CHAT ITEM
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(
                                offsetX.value.roundToInt(),
                                0
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF37474F)),
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                "A",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {

                            Text(
                                "Aman",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                "Hey, what's up?",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {

                Text(
                    "Got it!",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
