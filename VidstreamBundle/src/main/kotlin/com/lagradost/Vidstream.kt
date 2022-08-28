package com.lagradost

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * overrideMainUrl is necessary for for other vidstream clones like vidembed.cc
 * If they diverge it'd be better to make them separate.
 * */
class Vidstream(val mainUrl: String) {
    val name: String = "Vidstream"

    companion object {
        data class GogoSources(
            @JsonProperty("source") val source: List<GogoSource>?,
            @JsonProperty("sourceBk") val sourceBk: List<GogoSource>?,
            //val track: List<Any?>,
            //val advertising: List<Any?>,
            //val linkiframe: String
        )

        data class GogoSource(
            @JsonProperty("file") val file: String,
            @JsonProperty("label") val label: String?,
            @JsonProperty("type") val type: String?,
            @JsonProperty("default") val default: String? = null
        )

        // https://github.com/saikou-app/saikou/blob/3e756bd8e876ad7a9318b17110526880525a5cd3/app/src/main/java/ani/saikou/anime/source/extractors/GogoCDN.kt#L60
        // No Licence on the function
        private fun cryptoHandler(
            string: String,
            iv: String,
            secretKeyString: String,
            encrypt: Boolean = true
        ): String {
            //println("IV: $iv, Key: $secretKeyString, encrypt: $encrypt, Message: $string")
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            val secretKey = SecretKeySpec(secretKeyString.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            return if (!encrypt) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
                String(cipher.doFinal(base64DecodeArray(string)))
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
                base64Encode(cipher.doFinal(string.toByteArray()))
            }
        }

        /**
         * @param iframeUrl something like https://gogoplay4.com/streaming.php?id=XXXXXX
         * @param mainApiName used for ExtractorLink names and source
         * @param iv secret iv from site, required non-null if isUsingAdaptiveKeys is off
         * @param secretKey secret key for decryption from site, required non-null if isUsingAdaptiveKeys is off
         * @param secretDecryptKey secret key to decrypt the response json, required non-null if isUsingAdaptiveKeys is off
         * @param isUsingAdaptiveKeys generates keys from IV and ID, see getKey()
         * @param isUsingAdaptiveData generate encrypt-ajax data based on $("script[data-name='episode']")[0].dataset.value
         * */
        suspend fun extractVidstream(
            iframeUrl: String,
            mainApiName: String,
            callback: (ExtractorLink) -> Unit,
            iv: String?,
            secretKey: String?,
            secretDecryptKey: String?,
            // This could be removed, but i prefer it verbose
            isUsingAdaptiveKeys: Boolean,
            isUsingAdaptiveData: Boolean,
            // If you don't want to re-fetch the document
            iframeDocument: Document? = null
        ) = safeApiCall {
            // https://github.com/saikou-app/saikou/blob/3e756bd8e876ad7a9318b17110526880525a5cd3/app/src/main/java/ani/saikou/anime/source/extractors/GogoCDN.kt
            // No Licence on the following code
            // Also modified of https://github.com/jmir1/aniyomi-extensions/blob/master/src/en/gogoanime/src/eu/kanade/tachiyomi/animeextension/en/gogoanime/extractors/GogoCdnExtractor.kt
            // License on the code above  https://github.com/jmir1/aniyomi-extensions/blob/master/LICENSE

            if ((iv == null || secretKey == null || secretDecryptKey == null) && !isUsingAdaptiveKeys)
                return@safeApiCall

            val id = Regex("id=([^&]+)").find(iframeUrl)!!.value.removePrefix("id=")

            var document: Document? = iframeDocument
            val foundIv =
                iv ?: (document ?: app.get(iframeUrl).document.also { document = it })
                    .select("""div.wrapper[class*=container]""")
                    .attr("class").split("-").lastOrNull() ?: return@safeApiCall
            val foundKey = secretKey ?: getKey(base64Decode(id) + foundIv) ?: return@safeApiCall
            val foundDecryptKey = secretDecryptKey ?: foundKey

            val uri = URI(iframeUrl)
            val mainUrl = "https://" + uri.host

            val encryptedId = cryptoHandler(id, foundIv, foundKey)
            val encryptRequestData = if (isUsingAdaptiveData) {
                // Only fetch the document if necessary
                val realDocument = document ?: app.get(iframeUrl).document
                val dataEncrypted =
                    realDocument.select("script[data-name='episode']").attr("data-value")
                val headers = cryptoHandler(dataEncrypted, foundIv, foundKey, false)
                "id=$encryptedId&alias=$id&" + headers.substringAfter("&")
            } else {
                "id=$encryptedId&alias=$id"
            }

            val jsonResponse =
                app.get(
                    "$mainUrl/encrypt-ajax.php?$encryptRequestData",
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                )
            val dataencrypted =
                jsonResponse.text.substringAfter("{\"data\":\"").substringBefore("\"}")
            val datadecrypted = cryptoHandler(dataencrypted, foundIv, foundDecryptKey, false)
            val sources = AppUtils.parseJson<GogoSources>(datadecrypted)

            fun invokeGogoSource(
                source: GogoSource,
                sourceCallback: (ExtractorLink) -> Unit
            ) {
                sourceCallback.invoke(
                    ExtractorLink(
                        mainApiName,
                        mainApiName,
                        source.file,
                        mainUrl,
                        getQualityFromName(source.label),
                        isM3u8 = source.type == "hls" || source.label?.contains(
                            "auto",
                            ignoreCase = true
                        ) == true
                    )
                )
            }

            sources.source?.forEach {
                invokeGogoSource(it, callback)
            }
            sources.sourceBk?.forEach {
                invokeGogoSource(it, callback)
            }
        }
    }


    private fun getExtractorUrl(id: String): String {
        return "$mainUrl/streaming.php?id=$id"
    }

    private fun getDownloadUrl(id: String): String {
        return "$mainUrl/download?id=$id"
    }

    private val normalApis = arrayListOf(MultiQuality())

    // https://gogo-stream.com/streaming.php?id=MTE3NDg5
    suspend fun getUrl(
        id: String,
        isCasting: Boolean = false,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val extractorUrl = getExtractorUrl(id)
        argamap(
            {
                normalApis.apmap { api ->
                    val url = api.getExtractorUrl(id)
                    api.getSafeUrl(
                        url,
                        callback = callback,
                        subtitleCallback = subtitleCallback
                    )
                }
            }, {
                /** Stolen from GogoanimeProvider.kt extractor */
                val link = getDownloadUrl(id)
                println("Generated vidstream download link: $link")
                val page = app.get(link, referer = extractorUrl)

                val pageDoc = Jsoup.parse(page.text)
                val qualityRegex = Regex("(\\d+)P")

                //a[download]
                pageDoc.select(".dowload > a")?.apmap { element ->
                    val href = element.attr("href") ?: return@apmap
                    val qual = if (element.text()
                            .contains("HDP")
                    ) "1080" else qualityRegex.find(element.text())?.destructured?.component1()
                        .toString()

                    if (!loadExtractor(href, link, subtitleCallback, callback)) {
                        callback.invoke(
                            ExtractorLink(
                                this.name,
                                name = this.name,
                                href,
                                page.url,
                                getQualityFromName(qual),
                                element.attr("href").contains(".m3u8")
                            )
                        )
                    }
                }
            }, {
                with(app.get(extractorUrl)) {
                    val document = Jsoup.parse(this.text)
                    val primaryLinks = document.select("ul.list-server-items > li.linkserver")
                    //val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()

                    // All vidstream links passed to extractors
                    primaryLinks.distinctBy { it.attr("data-video") }.forEach { element ->
                        val link = element.attr("data-video")
                        //val name = element.text()

                        // Matches vidstream links with extractors
                        extractorApis.filter { !it.requiresReferer || !isCasting }.apmap { api ->
                            if (link.startsWith(api.mainUrl)) {
                                api.getSafeUrl(link, extractorUrl, subtitleCallback, callback)
                            }
                        }
                    }
                }
            }
        )
        return true
    }
}