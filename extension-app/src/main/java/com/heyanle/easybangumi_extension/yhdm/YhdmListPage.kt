package com.heyanle.easybangumi_extension.yhdm

import android.util.Log
import com.heyanle.bangumi_source_api.api.Source
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/25 16:26.
 * https://github.com/heyanLE
 */

// 获取 樱花动漫 筛选页面 某一页的数据
suspend fun Source.listPage(
    order: String? = null,
    genre: String? = null,
    letter: String? = null,
    status: String? = null,
    season: String? = null,
    year: String? = null,
    region: String? = null,
    label: String? = null,
    page: Int,
): Pair<Int?, List<CartoonCover>> {
    val args = buildString {
        order?.let {
            append("order=${it}&")
        }
        genre?.let {
            append("genre=${it}&")
        }
        letter?.let {
            append("letter=${it}&")
        }
        status?.let {
            append("status=${it}&")
        }
        season?.let {
            append("season=${it}&")
        }
        year?.let {
            append("year=${it}&")
        }
        region?.let {
            append("region=${it}&")
        }
        label?.let {
            append("label=${it}&")
        }
        if (endsWith("&")) {
            setLength(length - 1)
        }

    }
    val u = url("/list/?${args}&pageindex=${page}")
    val doc =
        Jsoup.parse(networkHelper.cloudflareUserClient.newCall(GET(u)).execute().body?.string()!!)
    val r = arrayListOf<CartoonCover>()
    doc.select("body div.list ul li").forEach {
        val detailUrl = url(it.child(0).child(0).attr("href"))
        val coverStyle = it.child(0).child(0).child(0).child(0).attr("style")
        val coverPattern = Regex("""(?<=url\(').*(?='\))""")
        var cover = coverPattern.find(coverStyle)?.value ?: ""
        if (cover.startsWith("//")) {
            cover = "https:${cover}"
        }
        val b = CartoonCoverImpl(
            id = "${key}-$detailUrl",
            source = key,
            url = detailUrl,
            title = it.child(1).text(),
            intro = it.child(0).child(0).child(0).child(1).text(),
            coverUrl = cover,
        )
        r.add(b)
    }

    val pages = doc.select("div.pages a")
    return if (pages.isEmpty()) {
        Pair(null, r)
    } else {
        var hasNext = false
        for (p in pages) {
            if (p.text() == (page + 2).toString() || p.text() == "下一页") {
                hasNext = true
                break
            }
        }
        if (!hasNext) {
            Pair(null, r)
        } else {
            Pair(page + 1, r)
        }
    }

}

suspend fun Source.listPage(
    map: Map<String, String>,
    page: Int,
): Pair<Int?, List<CartoonCover>> {
    val args = buildString {
        map.iterator().forEach {
            append(it.key)
            append("=")
            append(it.value)
            append("&")
        }
        if (endsWith("&")) {
            setLength(length - 1)
        }
    }
    val u = url("/list/?${args}&pageindex=${page}")
    Log.d("YhdmListPage", " listPage ${u}")
    val doc =
        Jsoup.parse(networkHelper.cloudflareUserClient.newCall(GET(u)).execute().body?.string()!!)
    val r = arrayListOf<CartoonCover>()
    doc.select("body div.list ul li").forEach {
        val detailUrl = url(it.child(0).child(0).attr("href"))
        val coverStyle = it.child(0).child(0).child(0).child(0).attr("style")
        val coverPattern = Regex("""(?<=url\(').*(?='\))""")
        var cover = coverPattern.find(coverStyle)?.value ?: ""
        if (cover.startsWith("//")) {
            cover = "https:${cover}"
        }
        val b = CartoonCoverImpl(
            id = "${key}-$detailUrl",
            source = key,
            url = detailUrl,
            title = it.child(1).text(),
            intro = it.child(0).child(0).child(0).child(1).text(),
            coverUrl = cover
        )

        r.add(b)
    }
    val pages = doc.select("div.pages a")
    return if (pages.isEmpty()) {
        Pair(null, r)
    } else {
        var hasNext = false
        for (p in pages) {
            if (p.text() == (page + 2).toString() || p.text() == "下一页") {
                hasNext = true
                break
            }
        }
        if (!hasNext) {
            Pair(null, r)
        } else {
            Pair(page + 1, r)
        }
    }

}

