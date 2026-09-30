package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.data.model.BestChannelRecommendation
import com.example.data.model.ChannelRating
import com.example.data.model.ConnectedWifiInfo
import com.example.data.model.ScannedWifiNetwork
import com.example.data.model.SpectrumAnalysisReport
import com.example.data.model.WifiBand
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SpectrumAnalysisEngine {

    // Distinct palette for visual spectrum waves
    val SPECTRUM_COLORS = listOf(
        Color(0xFF38BDF8), // Cyan
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFFA855F7), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6), // Blue
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6), // Teal
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4)  // Sky
    )

    fun frequencyToChannel(frequencyMhz: Int): Pair<Int, WifiBand> {
        return when {
            frequencyMhz in 2412..2484 -> {
                val channel = if (frequencyMhz == 2484) 14 else ((frequencyMhz - 2412) / 5) + 1
                Pair(channel, WifiBand.BAND_2_4_GHZ)
            }
            frequencyMhz in 5170..5825 -> {
                val channel = ((frequencyMhz - 5000) / 5)
                Pair(channel, WifiBand.BAND_5_GHZ)
            }
            else -> Pair(1, WifiBand.BAND_2_4_GHZ)
        }
    }

    fun channelToFrequency(channel: Int, band: WifiBand): Int {
        return when (band) {
            WifiBand.BAND_2_4_GHZ -> {
                if (channel == 14) 2484 else 2412 + ((channel - 1) * 5)
            }
            WifiBand.BAND_5_GHZ -> {
                5000 + (channel * 5)
            }
        }
    }

    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled ?: false
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun hasWifiScanPermissions(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val wifiState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return (fineLocation || nearbyGranted) && wifiState
    }

    /**
     * Reads REAL live connection details from the device's Wi-Fi hardware:
     * SSID, BSSID (MAC), Frequency, Operating Channel, RSSI, Link Speed, and Gateway IP.
     */
    fun getRealConnectedWifiInfo(context: Context): ConnectedWifiInfo {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ConnectedWifiInfo(isConnected = false)
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // Check if Wi-Fi transport is actually active
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isWifiTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        @Suppress("DEPRECATION")
        val info: WifiInfo? = wifiManager.connectionInfo
        val networkId = info?.networkId ?: -1

        val isConnected = isWifiTransport && (networkId != -1 || (info?.bssid != null && info.bssid != "02:00:00:00:00:00"))

        if (!isConnected || info == null) {
            return ConnectedWifiInfo(isConnected = false)
        }

        val rawSsid = info.ssid ?: ""
        val ssid = rawSsid.trim('"').takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "شبكة واي فاي متصلة"
        val bssid = info.bssid?.takeIf { it != "02:00:00:00:00:00" } ?: "متصل"
        val freq = info.frequency
        val rssi = info.rssi
        val linkSpeed = info.linkSpeed

        val (channel, band) = if (freq > 0) {
            frequencyToChannel(freq)
        } else {
            Pair(1, WifiBand.BAND_2_4_GHZ)
        }

        // Real IP Address
        val ipInt = info.ipAddress
        val ipAddress = if (ipInt != 0) {
            "${ipInt and 0xFF}.${ipInt shr 8 and 0xFF}.${ipInt shr 16 and 0xFF}.${ipInt shr 24 and 0xFF}"
        } else null

        // Real Gateway IP (MikroTik Router IP)
        val dhcp = wifiManager.dhcpInfo
        val gatewayInt = dhcp?.gateway ?: 0
        val gatewayIp = if (gatewayInt != 0) {
            "${gatewayInt and 0xFF}.${gatewayInt shr 8 and 0xFF}.${gatewayInt shr 16 and 0xFF}.${gatewayInt shr 24 and 0xFF}"
        } else null

        return ConnectedWifiInfo(
            isConnected = true,
            ssid = ssid,
            bssid = bssid,
            frequency = freq,
            channel = channel,
            band = band,
            rssi = rssi,
            linkSpeedMbps = linkSpeed,
            ipAddress = ipAddress,
            gatewayIp = gatewayIp
        )
    }

    /**
     * Executes a 100% REAL Wi-Fi spectrum scan via Android WifiManager:
     * - Collects surrounding Access Points with actual RSSI and frequencies.
     * - Merges the current connected AP if active.
     * - Performs algorithmic channel rating based on Co-Channel (CCI) and Adjacent-Channel (ACI) interference.
     * - Formulates expert recommendations for 2.4 GHz and 5 GHz, with ready-to-run MikroTik RouterOS commands.
     */
    fun performSpectrumScan(context: Context): SpectrumAnalysisReport {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiOn = isWifiEnabled(context)
        val isLocOn = isLocationEnabled(context)
        val hasPerm = hasWifiScanPermissions(context)

        val connectedWifi = getRealConnectedWifiInfo(context)
        val realNetworks = mutableListOf<ScannedWifiNetwork>()
        var isHardware = false

        if (wifiManager != null && hasPerm && isWifiOn) {
            try {
                @Suppress("DEPRECATION")
                wifiManager.startScan()
                @Suppress("DEPRECATION")
                val results = wifiManager.scanResults
                if (!results.isNullOrEmpty()) {
                    realNetworks.addAll(mapScanResultsToModels(results))
                    isHardware = true
                }
            } catch (e: Exception) {
                // Fallback to connection info if scan is throttled by Android OS
            }
        }

        // If connected, ensure the connected network is present in the list
        if (connectedWifi.isConnected && connectedWifi.frequency != null && connectedWifi.frequency > 0) {
            val exists = realNetworks.any { it.bssid.equals(connectedWifi.bssid, ignoreCase = true) }
            if (!exists) {
                realNetworks.add(
                    0,
                    ScannedWifiNetwork(
                        ssid = "${connectedWifi.ssid ?: "شبكة متصلة"} (متصل بها)",
                        bssid = connectedWifi.bssid ?: "00:00:00:00:00:00",
                        frequency = connectedWifi.frequency,
                        channel = connectedWifi.channel ?: 1,
                        rssi = connectedWifi.rssi ?: -55,
                        band = connectedWifi.band ?: WifiBand.BAND_2_4_GHZ,
                        channelWidth = 20,
                        security = "متصل حالياً",
                        waveColor = Color(0xFF38BDF8)
                    )
                )
                isHardware = true
            }
        }

        val channelRatings24 = evaluateChannelsForBand(
            channels = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
            band = WifiBand.BAND_2_4_GHZ,
            networks = realNetworks,
            connectedChannel = if (connectedWifi.band == WifiBand.BAND_2_4_GHZ) connectedWifi.channel else null
        )

        val channelRatings5 = evaluateChannelsForBand(
            channels = listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 132, 136, 140, 149, 153, 157, 161, 165),
            band = WifiBand.BAND_5_GHZ,
            networks = realNetworks,
            connectedChannel = if (connectedWifi.band == WifiBand.BAND_5_GHZ) connectedWifi.channel else null
        )

        val best24Rating = selectBestChannel(channelRatings24, WifiBand.BAND_2_4_GHZ)
        val best5Rating = selectBestChannel(channelRatings5, WifiBand.BAND_5_GHZ)

        val bestRec24 = generateBestRecommendation(WifiBand.BAND_2_4_GHZ, best24Rating, connectedWifi, channelRatings24)
        val bestRec5 = generateBestRecommendation(WifiBand.BAND_5_GHZ, best5Rating, connectedWifi, channelRatings5)

        // Enhance connectedWifi info with current channel cleanliness
        val enrichedConnectedWifi = if (connectedWifi.isConnected && connectedWifi.channel != null) {
            val rating = if (connectedWifi.band == WifiBand.BAND_2_4_GHZ) {
                channelRatings24.find { it.channelNumber == connectedWifi.channel }
            } else {
                channelRatings5.find { it.channelNumber == connectedWifi.channel }
            }

            val warning = when {
                rating == null -> null
                rating.cleanlinessScore < 60 -> "تحذير: قناتك الحالية تعاني من تشويش وتداخل مرتفع (${rating.competingApCount} شبكات متداخلة). نوصي بتغيير التردد."
                rating.cleanlinessScore < 80 -> "ملاحظة: قناتك الحالية مقبولة ولكن هناك قنوات أخرى أكثر نقاءً واستقراراً."
                else -> "ممتاز: شبكتك تعمل على قناة نقية جداً وخالية من التداخل."
            }

            connectedWifi.copy(
                currentChannelCleanliness = rating?.cleanlinessScore,
                competingApsOnChannel = rating?.competingApCount ?: 0,
                channelWarningMessage = warning
            )
        } else connectedWifi

        return SpectrumAnalysisReport(
            networks = realNetworks,
            channelRatings24 = channelRatings24,
            channelRatings5 = channelRatings5,
            bestChannel24 = best24Rating,
            bestChannel5 = best5Rating,
            totalApsDetected = realNetworks.size,
            isHardwareScan = isHardware,
            connectedWifi = enrichedConnectedWifi,
            bestRecommendation24 = bestRec24,
            bestRecommendation5 = bestRec5,
            wifiEnabled = isWifiOn,
            locationServicesEnabled = isLocOn
        )
    }

    private fun mapScanResultsToModels(results: List<ScanResult>): List<ScannedWifiNetwork> {
        return results.mapIndexed { index, item ->
            val (channel, band) = frequencyToChannel(item.frequency)
            val ssid = if (item.SSID.isNullOrBlank()) "شبكة مخفية (Hidden AP)" else item.SSID
            val color = SPECTRUM_COLORS[index % SPECTRUM_COLORS.size]
            val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (item.channelWidth) {
                    ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                    ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                    ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                    else -> 20
                }
            } else 20

            ScannedWifiNetwork(
                ssid = ssid,
                bssid = item.BSSID ?: "00:00:00:00:00:00",
                frequency = item.frequency,
                channel = channel,
                rssi = item.level,
                band = band,
                channelWidth = width,
                security = item.capabilities ?: "WPA2",
                waveColor = color
            )
        }.sortedByDescending { it.rssi }
    }

    /**
     * Evaluates channel quality based on RF interference physics:
     * - Co-Channel Interference (CCI): Transmitters on identical frequency sharing airtime.
     * - Adjacent Channel Interference (ACI): 2.4GHz spectral leakage from adjacent channels.
     */
    private fun evaluateChannelsForBand(
        channels: List<Int>,
        band: WifiBand,
        networks: List<ScannedWifiNetwork>,
        connectedChannel: Int?
    ): List<ChannelRating> {
        val bandNetworks = networks.filter { it.band == band }

        return channels.map { channel ->
            val freq = channelToFrequency(channel, band)
            var totalPenalty = 0.0
            var coChannelCount = 0
            var adjacentCount = 0
            var competingApCount = 0

            for (net in bandNetworks) {
                val channelDiff = abs(net.channel - channel)
                // Normalize RSSI: -35 dBm is very strong (1.0), -90 dBm is weak (0.1)
                val signalFactor = max(0.08, (100.0 + net.rssi.toDouble()) / 70.0)

                if (band == WifiBand.BAND_2_4_GHZ) {
                    when (channelDiff) {
                        0 -> {
                            coChannelCount++
                            competingApCount++
                            totalPenalty += 28.0 * signalFactor
                        }
                        1 -> {
                            adjacentCount++
                            competingApCount++
                            totalPenalty += 22.0 * signalFactor
                        }
                        2 -> {
                            adjacentCount++
                            competingApCount++
                            totalPenalty += 15.0 * signalFactor
                        }
                        3 -> {
                            adjacentCount++
                            totalPenalty += 8.0 * signalFactor
                        }
                        4 -> {
                            adjacentCount++
                            totalPenalty += 3.0 * signalFactor
                        }
                    }
                } else {
                    // 5 GHz: Non-overlapping at 20MHz
                    if (channelDiff == 0) {
                        coChannelCount++
                        competingApCount++
                        totalPenalty += 30.0 * signalFactor
                    } else if (channelDiff <= 2 && net.channelWidth >= 40) {
                        adjacentCount++
                        competingApCount++
                        totalPenalty += 14.0 * signalFactor
                    }
                }
            }

            val cleanlinessScore = max(5, min(100, (100.0 - totalPenalty).toInt()))

            val isStandardTriad = if (band == WifiBand.BAND_2_4_GHZ) {
                channel == 1 || channel == 6 || channel == 11
            } else true

            val recommendationTitle = when {
                cleanlinessScore >= 85 -> "ممتازة جداً (نقاء فائق)"
                cleanlinessScore >= 70 -> "جيدة ومستقرة"
                cleanlinessScore >= 50 -> "متوسطة - تشويش طفيف"
                else -> "مزدحمة بشدة - تجنبها"
            }

            val advice = buildString {
                if (band == WifiBand.BAND_2_4_GHZ) {
                    if (!isStandardTriad) {
                        append("تنبيه: القناة غير قياسية (يفضل دائماً 1 أو 6 أو 11 لمنع التداخل). ")
                    }
                }
                if (competingApCount == 0) {
                    append("طيف التردد نظيف 100% وخالٍ من أي بث منافس.")
                } else {
                    append("تم رصد $coChannelCount بث على نفس القناة و $adjacentCount بث في القنوات المجاورة.")
                }
            }

            // Commands for MikroTik RouterOS
            val routerOsCmd = when (band) {
                WifiBand.BAND_2_4_GHZ ->
                    "/interface wireless set [find default-name=wlan1] frequency=$freq band=2ghz-b/g/n channel-width=20mhz wireless-protocol=802.11 comment=\"Configured by SamNet Spectrum Advisor\""
                WifiBand.BAND_5_GHZ ->
                    "/interface wireless set [find default-name=wlan2] frequency=$freq band=5ghz-a/n/ac channel-width=20/40/80mhz-XXXX wireless-protocol=802.11 comment=\"Configured by SamNet Spectrum Advisor\""
            }

            val wifiWave2Cmd = when (band) {
                WifiBand.BAND_2_4_GHZ ->
                    "/interface wifi set [find band=2ghz] channel.frequency=$freq channel.width=20mhz comment=\"SamNet 2.4G Best Channel\""
                WifiBand.BAND_5_GHZ ->
                    "/interface wifi set [find band=5ghz] channel.frequency=$freq channel.width=20/40/80mhz comment=\"SamNet 5G Best Channel\""
            }

            val isCurrent = connectedChannel != null && connectedChannel == channel

            ChannelRating(
                channelNumber = channel,
                frequencyMhz = freq,
                band = band,
                cleanlinessScore = cleanlinessScore,
                competingApCount = competingApCount,
                coChannelCount = coChannelCount,
                adjacentInterferenceLevel = min(100, (totalPenalty * 0.8).toInt()),
                isRecommended = cleanlinessScore >= 80 && (band != WifiBand.BAND_2_4_GHZ || isStandardTriad),
                recommendationTitle = recommendationTitle,
                adviceNotes = advice,
                routerOsCommand = routerOsCmd,
                routerOsWifiWave2Command = wifiWave2Cmd,
                isCurrentConnectedChannel = isCurrent
            )
        }
    }

    private fun selectBestChannel(ratings: List<ChannelRating>, band: WifiBand): ChannelRating? {
        if (ratings.isEmpty()) return null
        return if (band == WifiBand.BAND_2_4_GHZ) {
            // Strictly prioritize standard non-overlapping channels: 1, 6, 11
            val standardRatings = ratings.filter { it.channelNumber == 1 || it.channelNumber == 6 || it.channelNumber == 11 }
            standardRatings.maxByOrNull { it.cleanlinessScore } ?: ratings.maxByOrNull { it.cleanlinessScore }
        } else {
            ratings.maxByOrNull { it.cleanlinessScore }
        }
    }

    private fun generateBestRecommendation(
        band: WifiBand,
        bestRating: ChannelRating?,
        connectedWifi: ConnectedWifiInfo,
        allRatings: List<ChannelRating>
    ): BestChannelRecommendation? {
        if (bestRating == null) return null

        val isConnectedOnThisBand = connectedWifi.isConnected && connectedWifi.band == band && connectedWifi.channel != null
        val currentChannel = connectedWifi.channel

        val comparison = if (isConnectedOnThisBand && currentChannel != null) {
            if (currentChannel == bestRating.channelNumber) {
                "✅ رائع! أنت متصل بالفعل بالقناة الأفضل (قناة $currentChannel) بنقاء ${bestRating.cleanlinessScore}%. إعدادات ترددك مثالية ولا تحتاج تعديل."
            } else {
                val currentRating = allRatings.find { it.channelNumber == currentChannel }
                val currentScore = currentRating?.cleanlinessScore ?: 50
                "⚠️ شبكتك الحالية تعمل على (قناة $currentChannel) بنقاء $currentScore% وتواجه ${currentRating?.competingApCount ?: 0} شبكات متداخلة. نوصي بتغيير التردد فوراً إلى القناة الأفضل (${bestRating.channelNumber}) لرفع سرعة البث بنسبة ملحوظة."
            }
        } else {
            "نوصي بضبط راوتر البث أو الأكسس بوينت على هذه القناة لتحقيق أعلى استقرار وبث خالٍ من التشويش."
        }

        val reasoning = if (band == WifiBand.BAND_2_4_GHZ) {
            "القناة (${bestRating.channelNumber}) هي قناة قياسية غير متداخلة، وسجلت أعلى نسبة نقاء (${bestRating.cleanlinessScore}%) مع ${bestRating.competingApCount} شبكات متداخلة فقط في محيطك."
        } else {
            "القناة (${bestRating.channelNumber} - ${bestRating.frequencyMhz} MHz) توفر طيفاً نقياً بنسبة (${bestRating.cleanlinessScore}%) وتدعم دمج قنوات عريض حتى 80MHz لسرعة قصوى."
        }

        val recommendedWidth = if (band == WifiBand.BAND_2_4_GHZ) "20 MHz (إلزامي لمنع التداخل)" else "20/40/80 MHz (لأقصى سرعة)"

        return BestChannelRecommendation(
            band = band,
            recommendedChannel = bestRating.channelNumber,
            frequencyMhz = bestRating.frequencyMhz,
            cleanlinessScore = bestRating.cleanlinessScore,
            ratingCategory = bestRating.recommendationTitle,
            reasoning = reasoning,
            comparisonWithCurrent = comparison,
            recommendedChannelWidth = recommendedWidth,
            routerOsCommand = bestRating.routerOsCommand,
            routerOsWifiWave2Command = bestRating.routerOsWifiWave2Command
        )
    }
}
