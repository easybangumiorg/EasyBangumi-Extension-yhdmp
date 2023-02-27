package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api.search.SearchSourceWrapper
import com.heyanle.bangumi_source_api.api.withResult
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/25 16:25.
 * https://github.com/heyanLE
 */

class YhdmSearchImpl(source: YhdmSource): SearchSourceWrapper(source){
    override fun getFirstSearchKey(keyword: String): Int {
        return 0
    }


    override suspend fun search(pageKey: Int, keyword: String): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult {
            val url = url("/search/$keyword?page=$pageKey")
            val doc = Jsoup.connect(url(url)).get()
            val r = arrayListOf<CartoonCover>()
            doc.select("div.fire.l div.lpic ul li").forEach {
                val detailUrl = url(it.child(0).attr("href"))
                val b = CartoonCoverImpl()
                    .apply {
                        id = "${pageKey}-$detailUrl"
                        source = this@YhdmSearchImpl.key
                        this.url = detailUrl
                        title = it.child(1).text()
                        intro = it.child(2).text()
                        coverUrl = url(it.child(0).child(0).attr("src"))
                    }
                r.add(b)
            }
            val pages = doc.select("div.pages")
            if (pages.isEmpty()) {
                Pair(null, r)
            } else {
                val p = pages.select("a#lastn")
                if (p.isEmpty()) {
                    Pair(null, r)
                } else {
                    Pair(pageKey + 1, r)
                }
            }
        }
    }
}