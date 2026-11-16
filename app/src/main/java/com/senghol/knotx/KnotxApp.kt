package com.senghol.knotx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.FirebaseApp
import com.razorpay.Checkout
import com.senghol.knotx.connection.Knotx
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.senghol.knotx.data.sqldelight.SengholDB
import com.senghol.knotx.calling.CallContainer
import com.senghol.knotx.data.datastore.DootawasStore
import com.senghol.knotx.connection.NetworkMonitor
import com.senghol.knotx.connection.SocketManager
import com.senghol.knotx.connection.TimeTracker
import com.senghol.knotx.data.database.DootawasDatabase
import com.senghol.knotx.data.database.DootawasDatabase.Companion.getDatabase
import com.senghol.knotx.data.sqldelight.DatabaseDriverFactory
import com.senghol.knotx.utils.objects.DLog
import com.senghol.knotx.utils.objects.EndTone
import kotlin.getValue


class KnotxApp : Application() {

    lateinit var knotx: Knotx private set

    lateinit var dootawasStore: DootawasStore private set
    private lateinit var analytics: FirebaseAnalytics
    // 40-second grace period before we actually disconnect on background
    private val disconnectRunnable = Runnable {
        DLog.i("⏱ Grace period elapsed → setAppActive(false)")
        SocketManager.setAppActive(false)
    }
    private val lifecycleHandler = Handler(Looper.getMainLooper())
    private var backgroundedAt = 0L
    val tracker = TimeTracker()

    lateinit var callContainer: CallContainer
        private set
  val dootawasDatabase: DootawasDatabase by lazy {
        getDatabase(this)
    }

    private val sqlDelightDriver by lazy {
        DatabaseDriverFactory(this).createDriver()
    }

    val dootawasDB: SengholDB by lazy {
        SengholDB(sqlDelightDriver)
    }

    override fun onCreate() {
        super.onCreate()

        val driver = sqlDelightDriver

        dootawasStore = DootawasStore(this)
        knotx = Knotx(this,dootawasDatabase)
        FirebaseApp.initializeApp(this)

         analytics = FirebaseAnalytics.getInstance(this)
        NetworkMonitor(this).register()
        Checkout.preload(applicationContext)
        callContainer = CallContainer(
            context = this,
            dootawasDatabase = dootawasDatabase,
            sqlDelightDatabase = dootawasDB,
            knotx = knotx
        )
//        createNotificationChannel()
        EndTone.init(applicationContext)
        // -----------------------------
        // 🔥 PROCESS LIFECYCLE
        // -----------------------------
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {

                    Lifecycle.Event.ON_START -> {
                        DLog.d("Process ON_START")
                        // Cancel any pending delayed disconnect
                        lifecycleHandler.removeCallbacks(disconnectRunnable)
                        val awayMs = if (backgroundedAt == 0L) 0L
                        else SystemClock.elapsedRealtime() - backgroundedAt
                        SocketManager.onForeground(awayMs)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        DLog.d("Process ON_STOP → starting 40 s grace timer")
                        Knotx.activeScreenUser = ""
                        backgroundedAt = SystemClock.elapsedRealtime()
                        // Delay the real disconnect so quick app-switch doesn't flap
                        lifecycleHandler.postDelayed(disconnectRunnable, 40_000L)
                    }

                    else -> {}
                }
            }
        )

        // -----------------------------
        // 🔥 ACTIVITY LIFECYCLE
        // -----------------------------
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: Activity) {


//                SocketManager.ensureConnected()
            }

            override fun onActivityDestroyed(activity: Activity) {

                // ❌ DO NOT disconnect here
                // activities can be destroyed during rotation etc.
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
    }

    // -----------------------------
    // 🔥 TIMER CONTROL
    // -----------------------------


}