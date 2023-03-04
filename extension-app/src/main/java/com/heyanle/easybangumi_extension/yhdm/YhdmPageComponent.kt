package com.heyanle.easybangumi_extension.yhdm

import android.util.Log
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.page.PageComponent
import com.heyanle.bangumi_source_api.api.component.page.SourcePage
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/27 23:17.
 * https://github.com/heyanLE
 */
class YhdmPageComponent(source: YhdmSource) : ComponentWrapper(source), PageComponent {

    override fun getPages(): List<SourcePage> {
        return listOf(
            // 首页
            SourcePage.Group(
                "首页",
                false,
            ) {
                withResult(Dispatchers.IO) {
                    homeListPages()
                }
            },

            // 新番时刻表
            SourcePage.Group(
                "每日更新列表",
                false,
            ) {
                withResult(Dispatchers.IO) {
                    homeTimelinePages()
                }
            },
        )
    }

    // 获取主页所有 ListPage
    private suspend fun homeListPages(): List<SourcePage.SingleCartoonPage> {
        val res = arrayListOf<SourcePage.SingleCartoonPage>()
        val doc = Jsoup.parse(
            networkHelper.cloudflareUserClient.newCall(GET(url(YhdmSource.ROOT_URL)))
                .execute().body?.string()!!
        )
        val children = doc.select("div.list div.listtit a.listtitle").iterator()
        children.forEach {
            val label = it.text()
            val ur = it.attr("href")
            val map = hashMapOf<String, String>()


            // 获取参数
            Log.d("YhdmSource", ur)
            ur.substringAfter("?").split("&").forEach {
                val dd = it.split("=")
                if (dd.size == 2) {
                    map[dd[0]] = dd[1]
                }
            }
            val page = SourcePage.SingleCartoonPage.WithCover(
                label = label,
                firstKey = { 0 },
            ) {
                withResult(Dispatchers.IO) {
                    source.listPage(
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
        val doc = Jsoup.parse(
            networkHelper.cloudflareUserClient.newCall(GET(url(YhdmSource.ROOT_URL)))
                .execute().body?.string()!!
        )
        val tags = doc.select("body div.tag span").iterator()
        val items = doc.select("body div.tlist ul").iterator()
        while (tags.hasNext() && items.hasNext()) {
            val tag = tags.next()
            val item = items.next()
            val r = arrayListOf<CartoonCover>()
            item.children().forEach {
                val detailUrl = url(it.child(1).attr("href"))
                val car = CartoonCoverImpl(
                    id = "${this@YhdmPageComponent.source.key}-$detailUrl",
                    source = this@YhdmPageComponent.source.key,
                    url = detailUrl,
                    title = it.child(1).text(),
                    intro = it.child(0).text(),
                    coverUrl = null,
                )
                r.add(car)
            }

            res.add(
                SourcePage.SingleCartoonPage.WithoutCover(
                    tag.text(),
                    firstKey = { 0 },
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