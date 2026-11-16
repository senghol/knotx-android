package com.senghol.knotx.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.senghol.knotx.data.database.Contact
import com.senghol.knotx.ui.viewmodel.ContactViewModel
import com.senghol.knotx.utils.data.ContactUiItem
import com.senghol.knotx.utils.enums.ContactButtonState
import com.senghol.knotx.R
@Composable
fun ContactItem(contact: ContactUiItem, contactViewModel: ContactViewModel, navController: NavController) {

    var showPreview by remember { mutableStateOf(false) }
    val buttonState = contact.buttonState
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                        if (buttonState == ContactButtonState.ADDED){
                            navController.navigate("sandesh_screen/${contact.number}")
                        }
                       },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    shadowElevation = 10.dp.toPx()     // GPU shadow
                    shape = CircleShape
                    clip = false
                }
                .clip(CircleShape)
                .background(Color(contact.backgroundColor), CircleShape)
                .clickable {
                    if (!contact.imageUrl.isNullOrBlank()) {
                        showPreview = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {

            if (!contact.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = contact.imageUrl,
                    contentDescription = "Profile Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = contact.name.first().uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,                    // ← BIGGER text
                    fontWeight = FontWeight.ExtraBold,   // ← BOLDER
                    lineHeight = 26.sp,                  // prevents clipping on tall letters (G, Q, etc.)
                    modifier = Modifier.offset(y = (-1).dp)  // optional: slight upward shift for perfect centering
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 📌 Contact Name & Number
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.scrim,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x55000000),
                        offset = Offset(2f, 3f),
                        blurRadius = 3f
                    )
                )
            )

            Text(
                text = contact.number,
                fontSize = 14.sp,
                color = Color.Gray,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x33000000),
                        offset = Offset(1.5f, 2.5f),
                        blurRadius = 2f
                    )
                )
            )

        }




        if (buttonState != ContactButtonState.ADDED) {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            when (buttonState) {
                                ContactButtonState.INVITE ->
                                    contactViewModel.updateContactState(contact, true)

                                ContactButtonState.INVITED ->
                                    Toast.makeText(context, "Already Invited.", Toast.LENGTH_SHORT).show()

                                ContactButtonState.ADD ->
                                    contactViewModel.updateContactState(contact, false)

                                ContactButtonState.ADDED -> {}
                            }
                        }
                        .background(
                            when (buttonState) {
                                ContactButtonState.INVITE -> Color(0xFF00BFA5)
                                ContactButtonState.INVITED -> Color(0xFF131A1E)
                                ContactButtonState.ADD -> Color(0xFF0091EA)
                                ContactButtonState.ADDED -> Color.Transparent
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .size(80.dp, 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            when (buttonState) {
                                ContactButtonState.INVITE -> R.string.invite
                                ContactButtonState.INVITED -> R.string.invited
                                ContactButtonState.ADD -> R.string.add
                                ContactButtonState.ADDED -> R.string.added
                            }
                        ),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    if (showPreview) {
        ProfileImagePreviewDialog(
            imageUrl = contact.imageUrl ?: "",
            onDismiss = { showPreview = false }
        )
    }

}
