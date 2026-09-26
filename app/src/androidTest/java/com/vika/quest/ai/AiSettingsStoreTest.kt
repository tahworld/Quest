package com.vika.quest.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiSettingsStoreTest {
    @Test
    fun selectedModelPersistsWithoutApiKey() {
        val store = AiSettingsStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.save(AiSettingsStore.DEFAULT_BASE_URL, "selected-model", null)
        assertEquals("selected-model", AiSettingsStore(InstrumentationRegistry.getInstrumentation().targetContext).readPublic().model)
        store.save(AiSettingsStore.DEFAULT_BASE_URL, AiSettingsStore.DEFAULT_MODEL, null)
    }
}
