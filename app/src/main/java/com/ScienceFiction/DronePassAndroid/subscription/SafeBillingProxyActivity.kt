package com.ScienceFiction.DronePassAndroid.subscription

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProxyBillingActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * BillingClient explicitly launches ProxyBillingActivity, which dereferences BUY_INTENT without
 * checking for null. The manifest alias routes that component here. A missing intent still goes
 * through the SDK's own result handling so the purchase listener receives an error.
 */
class SafeBillingProxyActivity : ProxyBillingActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && intent.hasExtra(BUY_INTENT)) {
            @Suppress("DEPRECATION")
            val buyIntent = runCatching { intent.getParcelableExtra<PendingIntent>(BUY_INTENT) }.getOrNull()
            if (buyIntent == null) {
                if (!BuildConfig.DEBUG) {
                    runCatching {
                        FirebaseCrashlytics.getInstance().recordException(
                            IllegalStateException("Billing proxy received null BUY_INTENT"),
                        )
                    }
                }
                val failure = Intent(this, BillingLaunchFailureActivity::class.java)
                val replacement = PendingIntent.getActivity(
                    this,
                    0,
                    failure,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                intent.putExtra(BUY_INTENT, replacement)
            }
        }
        super.onCreate(savedInstanceState)
    }

    private companion object {
        const val BUY_INTENT = "BUY_INTENT"
    }
}

/** Returns an SDK-readable launch error through ProxyBillingActivity.onActivityResult. */
class BillingLaunchFailureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(
            RESULT_CANCELED,
            Intent()
                .putExtra("RESPONSE_CODE", BillingClient.BillingResponseCode.ERROR)
                .putExtra("DEBUG_MESSAGE", "Billing flow launch unavailable"),
        )
        finish()
    }
}
