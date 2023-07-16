package com.sdk.ads.demo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.txtText).setOnClickListener {
            com.sdk.ads.utils.logEvent("txtTextClick")
        }
        com.sdk.ads.ads.banner.AdmobBanner.show300x250(
            findViewById(R.id.viewBottom),
            adUnitId = "ca-app-pub-3940256099942544/2014213617",
            forceRefresh = true,
            callback = object : com.sdk.ads.utils.TAdCallback {
                override fun onAdLoaded(adUnit: String, adType: com.sdk.ads.utils.AdType) {
                    super.onAdLoaded(adUnit, adType)
                    com.sdk.ads.utils.logScreen(this::class.java.simpleName)
                }

                override fun onAdClicked(adUnit: String, adType: com.sdk.ads.utils.AdType) {
                    super.onAdClicked(adUnit, adType)
                }
            },
        )
    }
}
