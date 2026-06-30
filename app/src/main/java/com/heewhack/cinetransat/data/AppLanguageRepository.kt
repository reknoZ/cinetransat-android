package com.heewhack.cinetransat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.appLanguageDataStore by preferencesDataStore(name = "app_language")

private val LanguageTagKey = stringPreferencesKey("language_tag")

class AppLanguageRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _language = MutableStateFlow(loadInitialLanguage())

    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        scope.launch {
            appContext.appLanguageDataStore.data.collect { prefs ->
                _language.value = prefs.toAppLanguage()
            }
        }
    }

    fun currentLanguage(): AppLanguage = _language.value

    suspend fun setLanguage(language: AppLanguage) {
        appContext.appLanguageDataStore.edit { prefs ->
            prefs[LanguageTagKey] = language.storageTag
        }
    }

    private fun loadInitialLanguage(): AppLanguage =
        runBlocking {
            appContext.appLanguageDataStore.data.first().toAppLanguage()
        }

    private fun androidx.datastore.preferences.core.Preferences.toAppLanguage(): AppLanguage =
        when (this[LanguageTagKey]) {
            AppLanguage.En.storageTag -> AppLanguage.En
            AppLanguage.Fr.storageTag -> AppLanguage.Fr
            else -> defaultFromSystem()
        }

    companion object {
        fun defaultFromSystem(): AppLanguage =
            when (Locale.getDefault().language) {
                "en" -> AppLanguage.En
                else -> AppLanguage.Fr
            }
    }
}

private val AppLanguage.storageTag: String
    get() =
        when (this) {
            AppLanguage.En -> "en"
            AppLanguage.Fr -> "fr"
        }
