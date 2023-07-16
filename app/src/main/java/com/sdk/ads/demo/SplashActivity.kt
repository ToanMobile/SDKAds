package com.sdk.ads.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sdk.ads.R
import com.sdk.ads.ads.open.AdmobOpenSplash

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AdmobOpenSplash.show("ca-app-pub-3940256099942544/1033173712", 15000, nextAction = {})
    }
}
