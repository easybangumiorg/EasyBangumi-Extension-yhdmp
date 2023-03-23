package com.heyanle.easybangumi_extension.yhdm

import com.google.gson.JsonParser
import com.heyanle.bangumi_source_api.api.ParserException
import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.play.PlayComponent
import com.heyanle.bangumi_source_api.api.entity.CartoonSummary
import com.heyanle.bangumi_source_api.api.entity.PlayLine
import com.heyanle.bangumi_source_api.api.entity.PlayerInfo
import com.heyanle.bangumi_source_api.api.entity.PlayerInfo.Companion.DECODE_TYPE_HLS
import com.heyanle.bangumi_source_api.api.entity.PlayerInfo.Companion.DECODE_TYPE_OTHER
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import okhttp3.Headers
import java.net.URLDecoder
import java.util.Date

/**
 * Created by HeYanLe on 2023/3/4 14:42.
 * https://github.com/heyanLE
 */
class YhdmPlayComponent(
    source: YhdmSource
) : ComponentWrapper(source), PlayComponent {

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episodeIndex: Int
    ): SourceResult<PlayerInfo> {

        return withResult(Dispatchers.IO) {
            if (episodeIndex < 0) {
                throw IndexOutOfBoundsException()
            }

            val playID = Regex("""(?<=showp/).*(?=.html)""").find(summary.url)?.value ?: ""

            if (playID.isEmpty())
                throw ParserException("playID Error")

            val url = url("/showp/${playID}-${playLine.id}-${episodeIndex}.html")

            val playSecret = runCatching {
                k1 = null
                getPlayInfoRequest(playID, playLine.id.toInt(), episodeIndex, url)
            }.getOrElse {
                it.printStackTrace()
                throw ParserException(it.message ?: it.toString())
            }

            val jsonObject = runCatching {
                JsonParser.parseString(playSecret).asJsonObject
            }.getOrElse {
                it.printStackTrace()
                throw it
            }

            val vurl = jsonObject.get("vurl").asString

            val result = decodeByteCrypt(vurl)
            if (result.isNotEmpty()) {
                if (result.indexOf(".mp4") != -1){
                    PlayerInfo(
                        decodeType = DECODE_TYPE_OTHER,
                        uri = url(result)
                    )
                }else{
                    PlayerInfo(
                        decodeType = DECODE_TYPE_HLS,
                        uri = url(result)
                    )
                }
            }else{
                throw ParserException("Unknown")
            }
        }
    }

    private var t1: Long? = null
    private var t2: Long? = null
    private var k1: Long? = null
    private var k2: Long? = null
    private var errCount = 0

    private fun getPlayInfoRequest(
        playID: String,
        playIndex: Int,
        epIndex: Int,
        referer: String
    ): String {
        val target =
            url("/_getplay?aid=${playID}&playindex=${playIndex}&epindex=${epIndex}&r=${Math.random()}")
        val clint = networkHelper.client

        val header = Headers.Builder().add("Referer", referer)
        if (k1 != null && t1 != null) {
            val t = t1!!.div(0x3e8) shr 5
            k2 = (t * (t % 0x1000) + 0x99d6) * (t % 0x1000) + t
            t2 = Date().time

            header.add("Cookie", "t1=${t1}; k1=${k1}; k2=${k2}; t2=${t2};")
        }

        val request = GET(target, header.build())
        val exReq = clint.newCall(request).execute()

        val body = exReq.body!!.string()

        if (body == "err:timeout" || body.isEmpty()) {
            val cookies: List<String> = exReq.headers.values("Set-Cookie")

            cookies.forEach { session ->
                if (session.isNotEmpty()) {
                    val size = session.length
                    val i = session.indexOf(";")
                    if (i in 0 until size) {
                        val cookie = session.substring(0, i).split("=")
                        when (cookie[0]) {
                            "k1" -> k1 = cookie[1].toLong()
                            "t1" -> t1 = cookie[1].toLong()
                        }
                    }
                }
            }

            if (errCount == 10) {
                errCount = 0
                throw Error("Too many failures")
            }
            errCount++

            return getPlayInfoRequest(playID, playIndex, epIndex, referer)
        }

        return body
    }

    private fun decodeByteCrypt(rawData: String): String {
        if (rawData.indexOf('{') < 0) {
            var hfPanurl = ""
            val keyMP = 1048576
            val panurlLen = rawData.length

            for (i in 0 until panurlLen step 2) {
                val byte = rawData.substring(i, i + 2)
                var mn = byte.toInt(16)
                mn = (mn + keyMP - (panurlLen / 2 - 1 - i / 2)) % 256
                hfPanurl = Char(mn) + hfPanurl
            }
            return URLDecoder.decode(hfPanurl, "utf-8")
        }
        return rawData
    }

}