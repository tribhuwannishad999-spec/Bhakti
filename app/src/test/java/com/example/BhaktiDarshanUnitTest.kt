package com.example

import com.example.data.integrations.ConfigValidator
import com.example.data.model.MantraItem
import com.example.data.repository.DevotionalSeedCatalog
import com.example.game.MantraBubbleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BhaktiDarshanUnitTest {

    @Test
    fun `all 10 required Hindi mantras are present with exact speech mapping`() {
        val expectedMantras = listOf(
            "ॐ नमः शिवाय",
            "जय श्रीराम",
            "जय श्रीकृष्ण",
            "ॐ हनुमते नमः",
            "ॐ गं गणपतये नमः",
            "जय माता दी",
            "ॐ नमो नारायणाय",
            "ॐ सूर्याय नमः",
            "ॐ शनैश्चराय नमः",
            "ॐ नमो भगवते वासुदेवाय"
        )
        assertEquals(10, MantraItem.entries.size)
        val actualTexts = MantraItem.entries.map { it.mantraHindi }
        assertEquals(expectedMantras, actualTexts)

        // Verify exact 1-to-1 text-to-speech mapping and unique IDs
        val ids = MantraItem.entries.map { it.id }.toSet()
        assertEquals(10, ids.size)
        for (item in MantraItem.entries) {
            assertEquals("Mantra display text must match spoken text exactly", item.mantraHindi, item.speechTextHindi)
            assertEquals(item, MantraItem.fromId(item.id))
        }
    }

    @Test
    fun `bubble engine spawns rising bubbles, handles tap scoring, slow motion, pause and restart`() {
        val engine = MantraBubbleEngine(reducedMotion = false, random = Random(42))
        engine.onViewportSized(width = 1080f, height = 2200f)

        assertEquals("Starter bubbles should spawn immediately on first layout", 5, engine.bubbles.size)
        val firstBubble = engine.bubbles.first()
        val initialY = firstBubble.y

        // Stepping moves bubbles upward (smaller Y)
        assertTrue(engine.step(0.03f))
        assertTrue("Bubbles must rise bottom-to-top", firstBubble.y < initialY)

        // Tapping directly on a bubble pops it and returns the EXACT mantra shown on that bubble
        val targetBubble = engine.bubbles.last()
        val expectedMantra = targetBubble.mantra
        val popped = engine.handleTap(targetBubble.x, targetBubble.y)
        assertEquals(expectedMantra, popped)
        assertEquals(expectedMantra.punyaPoints.toLong(), engine.hudState.punyaScore)
        assertEquals(1L, engine.hudState.totalChants)
        assertEquals(expectedMantra.punyaPoints.toLong(), engine.hudState.bestScore)
        assertTrue("Burst particles should spawn on pop", engine.particles.isNotEmpty())
        assertTrue("Floating score popup should spawn on pop", engine.popups.isNotEmpty())

        // Slow motion activates and ticks down
        assertTrue(engine.activateSlowMotion())
        assertTrue(engine.hudState.slowMotionActive)
        assertFalse("Cannot re-activate while already active", engine.activateSlowMotion())

        // Pause prevents stepping and tapping
        engine.pause()
        assertTrue(engine.hudState.isPaused)
        assertFalse(engine.step(0.03f))
        val remainingBubble = engine.bubbles.first()
        assertNull(engine.handleTap(remainingBubble.x, remainingBubble.y))

        // Resume restores stepping
        engine.resume()
        assertFalse(engine.hudState.isPaused)
        assertTrue(engine.step(0.03f))

        // Restart resets session score while preserving bestScore
        val bestBeforeRestart = engine.hudState.bestScore
        engine.restartSession()
        assertEquals(0L, engine.hudState.punyaScore)
        assertEquals(0L, engine.hudState.totalChants)
        assertEquals(1, engine.hudState.sadhanaLevel)
        assertEquals(bestBeforeRestart, engine.hudState.bestScore)
        assertEquals(5, engine.bubbles.size)
    }

    @Test
    fun `config validator rejects unconfigured placeholders and non-https urls`() {
        assertFalse(ConfigValidator.isConfigured(null))
        assertFalse(ConfigValidator.isConfigured(""))
        assertFalse(ConfigValidator.isConfigured("UNCONFIGURED"))
        assertFalse(ConfigValidator.isConfigured("YOUR_API_KEY"))
        assertFalse(ConfigValidator.isConfigured("MY_SECRET"))
        assertTrue(ConfigValidator.isConfigured("merchant@okaxis"))

        assertFalse(ConfigValidator.isValidHttpsUrl("http://insecure.example.com"))
        assertFalse(ConfigValidator.isValidHttpsUrl("UNCONFIGURED"))
        assertTrue(ConfigValidator.isValidHttpsUrl("https://api.navamsha.example"))
    }

    @Test
    fun `devotional seed catalog provides complete traditional references and exact thank you text`() {
        assertEquals(12, DevotionalSeedCatalog.zodiacSigns.size)
        assertEquals(6, DevotionalSeedCatalog.referenceVrats.size)
        assertTrue(DevotionalSeedCatalog.kathaCategories.contains("सभी"))
        assertTrue(DevotionalSeedCatalog.bhajanCategories.contains("All"))
        assertTrue(DevotionalSeedCatalog.referenceTemples.isNotEmpty())
        assertTrue(DevotionalSeedCatalog.referenceFestivals.isNotEmpty())
        assertTrue(DevotionalSeedCatalog.referenceBhajans.isNotEmpty())
        assertNotNull(DevotionalSeedCatalog.THANK_YOU_MESSAGE_HI)
        assertTrue(DevotionalSeedCatalog.THANK_YOU_MESSAGE_HI.contains("Bhakti Darshan परिवार"))
    }
}
