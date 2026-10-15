package com.ScienceFiction.DronePassAndroid.subscription

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SafeBillingProxyActivityTest {
    @Test
    fun nullBuyIntentReturnsBillingErrorWithoutCrashing() {
        assertEquals(BillingClient.BillingResponseCode.ERROR, launchAndReadResponse(null))
    }

    @Test
    fun validBuyIntentStillUsesSdkResultHandling() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val stub = PendingIntent.getActivity(
            context,
            1,
            Intent(context, BillingStubResultActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        assertEquals(BillingClient.BillingResponseCode.USER_CANCELED, launchAndReadResponse(stub))
    }

    @Test
    fun nullBuyIntentReachesBillingClientPurchaseListener() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connected = CountDownLatch(1)
        val purchaseUpdate = CountDownLatch(1)
        val setupCode = AtomicInteger(Int.MIN_VALUE)
        val callbackCode = AtomicInteger(Int.MIN_VALUE)
        val client = BillingClient.newBuilder(context)
            .setListener { result, _ ->
                callbackCode.set(result.responseCode)
                purchaseUpdate.countDown()
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    setupCode.set(result.responseCode)
                    connected.countDown()
                }

                override fun onBillingServiceDisconnected() = Unit
            })
            assertTrue("BillingClient did not connect", connected.await(10, TimeUnit.SECONDS))
            assertEquals(BillingClient.BillingResponseCode.OK, setupCode.get())
            assertEquals(BillingClient.BillingResponseCode.ERROR, launchAndReadResponse(null))
            assertTrue("Purchase listener was not called", purchaseUpdate.await(10, TimeUnit.SECONDS))
            assertEquals(BillingClient.BillingResponseCode.ERROR, callbackCode.get())
        } finally {
            client.endConnection()
        }
    }

    private fun launchAndReadResponse(buyIntent: PendingIntent?): Int {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val received = CountDownLatch(1)
        val responseCode = AtomicInteger(Int.MIN_VALUE)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                responseCode.set(intent.getIntExtra("RESPONSE_CODE", Int.MIN_VALUE))
                received.countDown()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("com.android.vending.billing.LOCAL_BROADCAST_PURCHASES_UPDATED"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        try {
            val billingIntent = Intent()
                .setClassName(context, "com.android.billingclient.api.ProxyBillingActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("BUY_INTENT", buyIntent as Parcelable?)
            instrumentation.runOnMainSync { context.startActivity(billingIntent) }
            assertTrue("Billing result broadcast was not received", received.await(10, TimeUnit.SECONDS))
            return responseCode.get()
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}
