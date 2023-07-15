package com.sdk.ads

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdLoadCallback
import com.sdk.ads.ads.banner.AdmobBanner
import com.sdk.ads.utils.AdType
import com.sdk.ads.utils.TAdCallback
import com.sdk.ads.utils.logEvent
import com.sdk.ads.utils.logScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.txtText).setOnClickListener {
            logEvent("txtTextClick")
        }
        AdmobBanner.show300x250(
            findViewById(R.id.viewBottom),
            adUnitId = "ca-app-pub-3940256099942544/2014213617",
            forceRefresh = true,
            callback = object : TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: AdType) {
                    super.onAdLoaded(adUnit, adType)
                    logScreen(this::class.java.simpleName)
                }

                override fun onAdClicked(adUnit: String, adType: AdType) {
                    super.onAdClicked(adUnit, adType)
                }
            }
        )
    }
}
