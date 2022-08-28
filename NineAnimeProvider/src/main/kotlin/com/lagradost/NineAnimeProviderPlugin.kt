package com.lagradost

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class NineAnimeProviderPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(NineAnimeProvider())
        registerMainAPI(WcoProvider())
        registerExtractorAPI(Mcloud())
        registerExtractorAPI(Vidstreamz())
        registerExtractorAPI(Vizcloud())
        registerExtractorAPI(Vizcloud2())
        registerExtractorAPI(VizcloudOnline())
        registerExtractorAPI(VizcloudXyz())
        registerExtractorAPI(VizcloudLive())
        registerExtractorAPI(VizcloudInfo())
        registerExtractorAPI(MwvnVizcloudInfo())
        registerExtractorAPI(VizcloudDigital())
        registerExtractorAPI(VizcloudCloud())
        registerExtractorAPI(VizcloudSite())
        registerExtractorAPI(WcoStream())
    }
}