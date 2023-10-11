package com.sdk.ads.consent

import com.google.android.ump.ConsentDebugSettings.DebugGeography.*

enum class ConsentGeography {
    DISABLED,
    EEA,
    NOT_EEA,
    ;

    // region Public Variables

    val debugGeography by lazy {
        when (this) {
            DISABLED -> DEBUG_GEOGRAPHY_DISABLED
            EEA -> DEBUG_GEOGRAPHY_EEA
            NOT_EEA -> DEBUG_GEOGRAPHY_NOT_EEA
        }
    }

    // endregion
}
