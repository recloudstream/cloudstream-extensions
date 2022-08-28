package com.lagradost

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class VidstreamBundlePlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerExtractorAPI(MultiQuality())
        registerExtractorAPI(XStreamCdn())
        registerExtractorAPI(LayarKaca())
        registerExtractorAPI(DBfilm())
        registerExtractorAPI(Luxubu())
        registerExtractorAPI(FEmbed())
        registerExtractorAPI(Fplayer())
        registerExtractorAPI(FeHD())
        registerMainAPI(VidEmbedProvider())
        registerMainAPI(OpenVidsProvider())
        registerMainAPI(KdramaHoodProvider())
        registerMainAPI(DramaSeeProvider())
        registerMainAPI(AsianLoadProvider())
        registerMainAPI(WatchAsianProvider())
    }
}