package com.ScienceFiction.DronePassAndroid.subscription

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.android.billingclient.api.BillingClient

/** Only present in debug builds to verify that the billing proxy delegates valid intents. */
class BillingStubResultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(
            RESULT_CANCELED,
            Intent().putExtra("RESPONSE_CODE", BillingClient.BillingResponseCode.USER_CANCELED),
        )
        finish()
    }
}
