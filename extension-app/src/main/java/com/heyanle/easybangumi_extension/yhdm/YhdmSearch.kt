package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.search.SearchComponent
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/25 16:25.
 * https://github.com/heyanLE
 */

class YhdmSearchImpl(source: YhdmSource): ComponentWrapper(source), SearchComponent{
    override fun getFirstSearchKey(keyword: String): Int {
        return 0
    }


    override suspend fun search(pageKey: Int, keyword: String): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult(Dispatchers.IO) {
            val url = url("/s_all?pageindex=${pageKey}&kw=$keyword&pagesize=24")
            val d = networkHelper.cloudflareUserClient.newCall(GET(url(url))).execute().body?.string()!!
            val doc = Jsoup.parse(d)
            val r = arrayListOf<CartoonCover>()
            doc.select("li").forEach {
                val detailUrl = url(it.child(0).child(0).attr("href"))
                val coverStyle = it.select("div.imgblock")[0].attr("style")
                val coverPattern = Regex("""(?<=url\(').*(?='\))""")
                val cover = coverPattern.find(coverStyle)?.value ?: ""

                val b = CartoonCoverImpl().apply {
                    id = "${this@YhdmSearchImpl.source.key}-$detailUrl"
                    title = it.child(1).text()
                    this.url = detailUrl
                    intro = it.select("div.itemimgtext")[0].text()
                    coverUrl = "https:${cover}"
                    source = this@YhdmSearchImpl.source.key
                }
                r.add(b)
            }
            val pages = doc.select("div.pages a")
            return@withResult if (pages.isEmpty()) {
                Pair(null, r)
            } else {
                var hasNext = false
                for(p in pages){
                    if(p.text() == (pageKey+2).toString() || p.text() == "下一页"){
                        hasNext = true
                        break
                    }
                }
                if (!hasNext) {
                    Pair(null, r)
                } else {
                    Pair(pageKey + 1, r)
                }
            }
        }
    }
}