package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class WifiBand(val displayName: String, val rangeText: String) {
    BAND_2_4_GHZ("2.4 GHz", "2400 - 2484 MHz (مدى واسع وتغطية جدران)"),
    BAND_5_GHZ("5 GHz", "5150 - 5850 MHz (سرعة عالية ونقاء تردد)")
}

data class ScannedWifiNetwork(
    val ssid: String,
    val bssid: String,
    val frequency: Int, // e.g. 2412 MHz
    val channel: Int,   // e.g. 1
    val rssi: Int,      // e.g. -65 dBm
    val band: WifiBand,
    val channelWidth: Int = 20, // 20, 40, 80, 160 MHz
    val security: String = "WPA2-PSK",
    val waveColor: Color = Color(0xFF0284C7)
) {
    val signalQualityPercent: Int
        get() = when {
            rssi <= -100 -> 0
            rssi >= -50 -> 100
            else -> 2 * (rssi + 100)
        }

    val signalLevelCategory: String
        get() = when {
            rssi >= -55 -> "ممتازة جداً"
            rssi >= -67 -> "جيدة ومستقرة"
            rssi >= -75 -> "متوسطة"
            else -> "ضعيفة ومشوشة"
        }
}

data class ChannelRating(
    val channelNumber: Int,
    val frequencyMhz: Int,
    val band: WifiBand,
    val cleanlinessScore: Int, // 0 to 100
    val competingApCount: Int,
    val coChannelCount: Int,
    val adjacentInterferenceLevel: Int, // 0 to 100
    val isRecommended: Boolean,
    val recommendationTitle: String,
    val adviceNotes: String,
    val routerOsCommand: String,
    val routerOsWifiWave2Command: String = "",
    val isCurrentConnectedChannel: Boolean = false
)

data class ConnectedWifiInfo(
    val isConnected: Boolean,
    val ssid: String? = null,
    val bssid: String? = null,
    val frequency: Int? = null,
    val channel: Int? = null,
    val band: WifiBand? = null,
    val rssi: Int? = null,
    val linkSpeedMbps: Int? = null,
    val ipAddress: String? = null,
    val gatewayIp: String? = null,
    val currentChannelCleanliness: Int? = null,
    val competingApsOnChannel: Int = 0,
    val channelWarningMessage: String? = null
)

data class BestChannelRecommendation(
    val band: WifiBand,
    val recommendedChannel: Int,
    val frequencyMhz: Int,
    val cleanlinessScore: Int,
    val ratingCategory: String,
    val reasoning: String,
    val comparisonWithCurrent: String?,
    val recommendedChannelWidth: String,
    val routerOsCommand: String,
    val routerOsWifiWave2Command: String
)

data class SpectrumAnalysisReport(
    val networks: List<ScannedWifiNetwork>,
    val channelRatings24: List<ChannelRating>,
    val channelRatings5: List<ChannelRating>,
    val bestChannel24: ChannelRating?,
    val bestChannel5: ChannelRating?,
    val totalApsDetected: Int,
    val isHardwareScan: Boolean,
    val connectedWifi: ConnectedWifiInfo? = null,
    val bestRecommendation24: BestChannelRecommendation? = null,
    val bestRecommendation5: BestChannelRecommendation? = null,
    val wifiEnabled: Boolean = true,
    val locationServicesEnabled: Boolean = true,
    val scanTimeMillis: Long = System.currentTimeMillis()
)
