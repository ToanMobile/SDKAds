package com.sdk.ads

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sdk.ads.ads.open.AdmobOpenSplash
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AdmobOpenSplash.show("ca-app-pub-3940256099942544/1033173712", 15000, nextAction = {})
    }
}