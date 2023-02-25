package com.heyanle.easybangumi_extension.yhdm

import android.util.Log
import com.heyanle.bangumi_source_api.api.ISourceParser
import com.heyanle.bangumi_source_api.api.entity.Bangumi
import com.heyanle.bangumi_source_api.api2.Source
import com.heyanle.bangumi_source_api.api2.component.Component
import com.heyanle.bangumi_source_api.api2.component.ComponentBuilderScope
import com.heyanle.bangumi_source_api.api2.component.page.ListPage
import com.heyanle.bangumi_source_api.api2.component.page.SingleListPage
import com.heyanle.bangumi_source_api.api2.entity.CartoonCover
import com.heyanle.bangumi_source_api.api2.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api2.withResult
import com.heyanle.easybangumi_extension.R
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.lib_anim.utils.SourceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/2/25 15:19.
 * https://github.com/heyanLE
 */
class YhdmSource: Source, ExtensionIconSource {

    companion object {
        const val ROOT_URL = "https://www.yhdmp.net/"
    }

    fun url(source: String): String {
        return SourceUtils.urlParser(ROOT_URL, source)
    }

    override fun getIconResourcesId(): Int? {
        return R.drawable.yhdm
    }

    override val describe: String?
        get() = null
    override val key: String
        get() = "yhdmp"
    override val label: String
        get() = "樱花动漫P"
    override val version: String
        get() = "1.0"
    override val versionCode: Int
        get() = 0



    override fun components(): List<Component> {
        return ComponentBuilderScope(this).apply {
            // 搜索
            search(1) { keyword, pageKey ->
                withResult(Dispatchers.IO){
                    search(keyword, pageKey)
                }
            }

            // 主页
            listPageGroup("主页"){
                withResult(Dispatchers.IO){
                    homeListPages()
                }
            }

            // 新番时刻表
            listPageGroupTab("新番时刻表"){
                withResult(Dispatchers.IO){
                    homeTimelinePages()
                }
            }
        }.components
    }

    // 获取主页所有 ListPage
    private suspend fun homeListPages(): List<ListPage> {
        val res = arrayListOf<ListPage>()

        val doc = Jsoup.connect(url(ROOT_URL)).get()
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
            val page = ListPage(
                label = label,
                source = this,
                newScreen = false,
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
    private suspend fun homeTimelinePages(): List<ListPage> {
        val res = arrayListOf<ListPage>()
        val doc = Jsoup.connect(url(ROOT_URL)).get()
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

            res.add(SingleListPage(
                tag.text(),
                this,
                false,
                r
            ))
        }
        return res
    }

}