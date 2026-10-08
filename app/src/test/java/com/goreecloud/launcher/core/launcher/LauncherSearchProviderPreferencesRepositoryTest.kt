package com.goreecloud.launcher.core.launcher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSearchProviderPreferencesRepositoryTest {
    @Test
    fun absenceAndExplicitEmptySelectionRemainDistinct() = runBlocking {
        val store = InMemoryPreferencesDataStore()
        val repository = LauncherSearchProviderPreferencesRepository(store)
        val key = stringPreferencesKey("search_provider_preferences_v1")

        assertTrue(repository.read() is LauncherSearchProviderPreferenceDecodeResult.Absent)

        val explicitEmpty = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = emptySet(),
            providerOrder = emptyList(),
        )
        repository.set(explicitEmpty)

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Loaded(explicitEmpty),
            repository.read(),
        )
        assertNotNull(store.data.first()[key])

        repository.clear()

        assertTrue(repository.read() is LauncherSearchProviderPreferenceDecodeResult.Absent)
    }

    @Test
    fun providerEnablementAndOrderRoundTripThroughDataStore() = runBlocking {
        val store = InMemoryPreferencesDataStore()
        val repository = LauncherSearchProviderPreferencesRepository(store)
        val snapshot = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = setOf("apps", "settings"),
            providerOrder = listOf("settings", "apps", "future:handoff"),
        )

        repository.set(snapshot)

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Loaded(snapshot),
            repository.read(),
        )
    }

    @Test
    fun malformedStoredProviderPreferenceFailsClosed() = runBlocking {
        val store = InMemoryPreferencesDataStore()
        val repository = LauncherSearchProviderPreferencesRepository(store)
        val key = stringPreferencesKey("search_provider_preferences_v1")

        store.edit { values ->
            values[key] = buildString {
                append("goreecloud-launcher-search-provider-preferences-v1\n")
                append("enabled=%%%\n")
                append("order=")
            }
        }

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_PROVIDER_ID_ENCODING,
            ),
            repository.read(),
        )
    }

    @Test
    fun persistenceWritesOnlyTheVersionedProviderControlKey() = runBlocking {
        val store = InMemoryPreferencesDataStore()
        val repository = LauncherSearchProviderPreferencesRepository(store)

        repository.set(
            LauncherSearchProviderPreferenceSnapshot(
                enabledProviderIds = setOf("apps"),
                providerOrder = listOf("apps"),
            ),
        )

        assertEquals(
            setOf("search_provider_preferences_v1"),
            store.data.first().asMap().keys.map { key -> key.name }.toSet(),
        )
    }

    private class InMemoryPreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }
}
