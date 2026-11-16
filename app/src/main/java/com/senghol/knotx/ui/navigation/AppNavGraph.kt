package com.senghol.knotx.ui.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.senghol.knotx.data.sqldelight.SengholDB
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.ContactDao
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.GroupDao
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.data.database.UpbhogtaDao
import com.senghol.knotx.data.datastore.DootawasStore

import com.senghol.knotx.ui.calling.CallActivity2
import com.senghol.knotx.ui.calling.CallIntentBus
import com.senghol.knotx.ui.dialogs.ProfileSetupScreen
import com.senghol.knotx.ui.repository.HomeGroupRepository
import com.senghol.knotx.ui.repository.HomeSingleRepository
import com.senghol.knotx.ui.repository.StoryRepository


import com.senghol.knotx.ui.viewmodel.ContactViewModelFactory
import com.senghol.knotx.ui.screens.*
import com.senghol.knotx.ui.viewmodel.CallSessionRepository
import com.senghol.knotx.ui.viewmodel.ContactViewModel
import com.senghol.knotx.ui.viewmodel.HomeViewModel
import com.senghol.knotx.ui.viewmodel.HomeViewModelFactory
import com.senghol.knotx.ui.viewmodel.MediaViewModel
import com.senghol.knotx.ui.viewmodel.SandeshRepository

import com.senghol.knotx.ui.viewmodel.SandeshViewModelManager
import com.senghol.knotx.utils.enums.CallDataType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.SandeshType
import com.senghol.knotx.utils.objects.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    isRegistered: Boolean,
    upbhogtaDao: UpbhogtaDao,
    contactDao: ContactDao,
    sandeshDao: SandeshDao,
    knotx: Knotx,
    dootawasStore: DootawasStore,
    groupDao: GroupDao,
    sharedText: String?,
    chatSender: String?,
    sharedUris: List<Uri>?,
    onShareHandled: () -> Unit,
    onChatHandled: () -> Unit,
    dootawasDatabase: DootawasDatabase,// 👈 add this
    dootawasDB: SengholDB
) {


    val context = LocalContext.current.applicationContext // ✅ Application context (safe!)


    val startDestination = if (isRegistered) "home" else "register"

    val contactPermissionState = rememberPermissionState(permission = Manifest.permission.READ_CONTACTS)

    val sandeshRepository = remember { SandeshRepository(sandeshDao, knotx,groupDao, dootawasDatabase,context) }


    val directId by PrefsHelper.directIdFlow.collectAsState()

    val finalDirectId = chatSender ?: directId

    LaunchedEffect(finalDirectId) {
        finalDirectId?.let { id ->
            val route = if (id.contains("-")) "group_screen/$id" else "sandesh_screen/$id"

            navController.navigate(route) {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }

            PrefsHelper.setDirectId(context, null)   // always clear, no condition
            onChatHandled()                          // sets chatSenderState.value = null
        }
    }


    val manager: SandeshViewModelManager = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SandeshViewModelManager(sandeshRepository, knotx) as T
            }
        }
    )
    var preloaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preloaded) {

        if (!preloaded) {

            preloaded = true

            withContext(Dispatchers.IO) {
                val topUsers = upbhogtaDao.getTopUpbhogtas(5)

                topUsers.forEach { user ->
                    manager.get(user.userNumber, false)
                }
            }
        }
    }


    NavHost(navController = navController, startDestination = startDestination) {
        composable("register") {
            RegisterScreen(navController, knotx)
        }


            composable("home") {
                val callSessionRepository = CallSessionRepository(dootawasDB,dootawasDatabase)
                val singleRepository = HomeSingleRepository(dootawasDatabase)
                val groupRepository = HomeGroupRepository(dootawasDatabase)
                val storyRepository = StoryRepository(dootawasDatabase,knotx)
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(
                        singleRepository = singleRepository,
                        groupRepository = groupRepository,
                        callRepository = callSessionRepository,
                        storyRepository = storyRepository,
                        knotx = knotx,
                        dootawasStore = dootawasStore
                    )
                )
                LaunchedEffect(sharedText != null || sharedUris != null) {

                    if (sharedText != null || sharedUris != null) {
                        navController.navigate("share_screen")
    //                    onShareHandled()   // 👈 call callback instead
                    }
                }
                HomeScreen(homeViewModel, navController, knotx,
                        onSendSos = { messageSamagri, userNumbers ->
                    val sandeshViewModel = manager.get(userNumbers.first(), false)
                    sandeshViewModel.sendLocation(
                        userNumbers = userNumbers,
                        message = messageSamagri,
                        context = context
                    )
                }
                )

            }

//        composable("payment_screen") {
//            PaymentScreen(navController,knotx)
//        }
        composable("share_screen") {

            // 🔥 Copy once and freeze
            val initialText = remember { sharedText }
            val initialUris = remember { sharedUris }

            // Now clear global state
            LaunchedEffect(Unit) {
                onShareHandled()
            }

            val groupViewModel = manager.get("910123456789",false)

            ShareScreen(
                sharedText = initialText,
                sharedUris = initialUris,
                sandeshViewModel = groupViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("contacts") {
            // Check if the permission is granted when navigating to the contacts screen
            LaunchedEffect(contactPermissionState.status.isGranted) {
                // If permission is not granted, request permission
                if (!contactPermissionState.status.isGranted) {
                    contactPermissionState.launchPermissionRequest()
                }
            }
            // If the permission is granted, show the Contacts screen
            if (contactPermissionState.status.isGranted) {
                
                // dakiya.readContacts()
                val contactViewModel: ContactViewModel =
                    viewModel(factory = ContactViewModelFactory(contactDao, knotx))
                ContactScreen(contactViewModel,navController)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Contact permission is required to access the contacts.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                }
            }

            LaunchedEffect(sharedText != null || sharedUris != null) {

                if (sharedText != null || sharedUris != null) {
                    navController.navigate("share_screen")
//                    onShareHandled()   // 👈 call callback instead
                }
            }
        }

        composable(
            route = "profile_screen/{phoneNumber}",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            exitTransition = {
                fadeOut(tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 6 },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(180))
            }
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            ProfileSetupScreen(navController, phoneNumber, knotx)
        }

        composable(
            route = "sandesh_screen/{userNumber}",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(250))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 3 },
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(200))
            }
        ) { backStackEntry ->
            val userNumber = backStackEntry.arguments?.getString("userNumber")
                ?: return@composable

            val sandeshViewModel = manager.get(userNumber, false)

            SandeshScreen(
                navController = navController,
                sandeshViewModel = sandeshViewModel
            )

            LaunchedEffect(sharedText != null || sharedUris != null) {
                if (sharedText != null || sharedUris != null) {
                    navController.navigate("share_screen")
                }
            }
        }
        composable("group_screen/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            
            val group by produceState<GroupEntity?>(initialValue = null, groupId) {
                value = groupDao.getGroupById(groupId)
                // Optional: check group status here if needed
            }

            val groupViewModel = manager.get(groupId,true)

            group?.let {
                GroupScreen(
                    navController = navController,
                    groupViewModel = groupViewModel
                )
            }

            LaunchedEffect(sharedText != null || sharedUris != null) {

                if (sharedText != null || sharedUris != null) {
                    navController.navigate("share_screen")
//                    onShareHandled()   // 👈 call callback instead
                }
            }
        }


        composable(
            route = "media_screen/{userName}/{sandeshTypeValue}",
            arguments = listOf(
                navArgument("userName") { type = NavType.StringType },
                navArgument("sandeshTypeValue") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: return@composable
            val sandeshTypeValue = backStackEntry.arguments?.getInt("sandeshTypeValue") ?: return@composable
            val sandeshType = SandeshType.entries.find { it.value == sandeshTypeValue } ?: return@composable

            // Custom ViewModel factory to pass constructor params
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return MediaViewModel(
                            repository = sandeshRepository,
                            targetType = sandeshType,
                            userNumber = userName
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }

            val mediaViewModel: MediaViewModel = viewModel(factory = factory)

            MediaScreen(
                mediaViewModel = mediaViewModel,
                mediaType = sandeshType,
                navController = navController
            )

            LaunchedEffect(sharedText != null || sharedUris != null) {

                if (sharedText != null || sharedUris != null) {
                    navController.navigate("share_screen")
//                    onShareHandled()   // 👈 call callback instead
                }
            }
        }




        composable(
            route = "otp_screen?encryptedMessage={encryptedMessage}&phoneNumber={phoneNumber}",
            arguments = listOf(
                navArgument("encryptedMessage") { type = NavType.StringType },
                navArgument("phoneNumber") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            exitTransition = {
                fadeOut(tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 6 },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 6 },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(180))
            }
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(navController, knotx = knotx, phoneNumber = phoneNumber)
        }
    }

    LaunchedEffect(Unit) {
        CallIntentBus.flow.collectLatest { json ->
            
            val obj = Gson().fromJson(json, JsonObject::class.java)
            val sender = obj["sender"]?.asString ?: return@collectLatest
            val type = obj["callDataType"]?.asInt ?: return@collectLatest
            val roomId = obj[JsonKeys.PROFILE_ID]?.asString

            val isVideo = type == CallDataType.VIDEO_RECEIVED.value
            val isAudio = type == CallDataType.AUDIO_RECEIVED.value
            
            if (isVideo || isAudio) {
                
//                CallActivity2.finishIfRunning()  // 💥 Force-stop any running CallActivity2

                val intent = Intent(context, CallActivity2::class.java).apply {
                    putExtra("userNumber", sender)
                    putExtra("video", isVideo)
                    putExtra("outgoing", false)
                    putExtra("roomId",roomId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
        }
    }


}
