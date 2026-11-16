package com.senghol.knotx.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.preference.PreferenceManager
import java.util.Locale
import androidx.core.content.edit


object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    fun useSystemLocale(context: Context): Context {
        clearPersistedLanguage(context)

        val configuration = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(Locale.getDefault())
            configuration.setLayoutDirection(Locale.getDefault())
            return context.createConfigurationContext(configuration)
        }

        return updateResourcesLegacy(
            context,
            Locale.getDefault().language
        )
    }

    private fun persist(context: Context, language: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                putString(SELECTED_LANGUAGE, language)
            }
    }

    private fun clearPersistedLanguage(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                remove(SELECTED_LANGUAGE)
            }
    }

    private fun updateResources(
        context: Context,
        language: String
    ): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(
        context: Context,
        language: String
    ): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration

        configuration.locale = locale
        configuration.setLayoutDirection(locale)

        resources.updateConfiguration(
            configuration,
            resources.displayMetrics
        )

        return context
    }
}
