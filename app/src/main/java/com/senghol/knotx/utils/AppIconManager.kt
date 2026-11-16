package com.senghol.knotx.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.senghol.knotx.MainActivity

object AppIconManager {

    private val aliasPackage = MainActivity::class.java.`package`!!.name

    private val icons = listOf(
        "DefaultIcon",
        "GreenIcon",
        "CyanIcon",
        "BlueIcon",
        "SlateIcon",
        "OrangeIcon"
    )

    fun changeAppIcon(context: Context, icon: String) {
        val pm = context.packageManager
        val selected = icon.replaceFirstChar { it.uppercase() } + "Icon"
        val selectedAlias = if (selected in icons) selected else "DefaultIcon"

        fun component(alias: String) =
            ComponentName(context.packageName, "$aliasPackage.$alias")

        // enable the new one first
        pm.setComponentEnabledSetting(
            component(selectedAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // then disable the others
        icons.filter { it != selectedAlias }.forEach { alias ->
            pm.setComponentEnabledSetting(
                component(alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}