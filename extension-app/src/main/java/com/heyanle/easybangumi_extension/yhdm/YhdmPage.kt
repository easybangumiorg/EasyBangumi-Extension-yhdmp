package com.heyanle.easybangumi_extension.yhdm

import android.util.Log
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api.page.PageSourceWrapper
import com.heyanle.bangumi_source_api.api.page.SourcePage
import com.heyanle.bangumi_source_api.api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/27 23:17.
 * https://github.com/heyanLE
 */
class YhdmPageImpl(source: YhdmSource) : PageSourceWrapper(source) {

    override fun getPages(): List<SourcePage> {
        return listOf(
            // 首页
            SourcePage.Group (
                label,
                false,
            ){
                withResult(Dispatchers.IO) {
                    homeListPages()
                }
            },

            // 新番时刻表
            SourcePage.Group (
                label,
                true,
            ){
                withResult(Dispatchers.IO) {
                    homeTimelinePages()
                }
            },
        )
    }

    // 获取主页所有 ListPage
    private suspend fun homeListPages(): List<SourcePage.SingleCartoonPage> {
        val res = arrayListOf<SourcePage.SingleCartoonPage>()

        val doc = Jsoup.connect(url(YhdmSource.ROOT_URL)).get()
        val children = doc.select("div.firs.l div.dtit").iterator()
        children.forEach {
            val u = it.child(0).child(0)
            // 获取参数
            Log.d("YhdmSource", u.toString())
            val label = u.text()
            val ur = u.attr("href")
            val map = hashMapOf<String, String>()


            // 获取参数
            Log.d("YhdmSource", ur)
            ur.substringAfter("?").split("&").forEach {
                val dd = it.split("=")
                if(dd.size == 2){
                    map[dd[0]] = dd[1]
                }
            }
            val page = SourcePage.SingleCartoonPage.WithCover(
                label = label,
                firstKey = {0},
            ){
                withResult(Dispatchers.IO){
                    listPage(
                        map,
                        page = it
                    )
                }
            }
            res.add(page)
        }

        return res
    }

    // 获取新番时刻表 ListPage
    private suspend fun homeTimelinePages(): List<SourcePage.SingleCartoonPage> {
        val res = arrayListOf<SourcePage.SingleCartoonPage>()
        val doc = Jsoup.connect(url(YhdmSource.ROOT_URL)).get()
        val tags = doc.select("div.side.r div.dtit div.tag span").iterator()
        val items = doc.select("div.side.r div.dtit div.tlist ul").iterator()
        while(tags.hasNext() && items.hasNext()){
            val tag = tags.next()
            val item = items.next()
            val r = arrayListOf<CartoonCover>()
            item.children().forEach {
                val detailUrl = url(it.child(0).child(0).attr("href"))
                val car = CartoonCoverImpl()
                    .apply {
                        id = "${key}-$detailUrl"
                        source = key
                        this.url = detailUrl
                        title = it.child(1).text()
                        intro = it.child(0).text()
                        coverUrl = null
                    }
                r.add(car)
            }

            res.add(
                SourcePage.SingleCartoonPage.WithoutCover(
                tag.text(),
                firstKey = {1},
                load = {
                    withResult {
                        null to r
                    }
                }
            ))
        }
        return res
    }


}