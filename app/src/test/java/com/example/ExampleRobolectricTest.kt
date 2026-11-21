package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.integrations.UpiPaymentAdapter
import com.example.ui.screens.validatePalmBitmapQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name and firebase database id resources are properly configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bhakti Darshan", appName)

    val dbId = context.getString(R.string.firestore_database_id)
    assertTrue("Expected named Firestore database id", dbId.isNotBlank())
    assertFalse("Should not be empty or placeholder", dbId.contains("placeholder", ignoreCase = true))
  }

  @Test
  fun `upi payment adapter stays safely inactive without live merchant credentials`() {
    assertFalse("Live UPI must be inactive when UNCONFIGURED", UpiPaymentAdapter.isLiveUpiConfigured())
    assertNull(
      "UPI URI must be null when merchant VPA is unconfigured",
      UpiPaymentAdapter.buildUpiPaymentUri(amountInr = 101, purpose = "मंदिर सेवा", transactionNote = "Test")
    )
  }

  @Test
  fun `palm quality check rejects tiny and dark images and accepts well-lit clear images`() {
    val tinyBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    tinyBitmap.eraseColor(Color.rgb(180, 145, 120))
    assertFalse(validatePalmBitmapQuality(tinyBitmap).isAcceptable)

    val darkBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    darkBitmap.eraseColor(Color.rgb(10, 10, 10))
    assertFalse(validatePalmBitmapQuality(darkBitmap).isAcceptable)

    val goodBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
    goodBitmap.eraseColor(Color.rgb(195, 155, 125))
    assertTrue(validatePalmBitmapQuality(goodBitmap).isAcceptable)
  }
}
