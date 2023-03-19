package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.detailed.DetailedComponent
import com.heyanle.bangumi_source_api.api.component.page.PageComponent
import com.heyanle.bangumi_source_api.api.entity.Cartoon
import com.heyanle.bangumi_source_api.api.entity.CartoonImpl
import com.heyanle.bangumi_source_api.api.entity.CartoonSummary
import com.heyanle.bangumi_source_api.api.entity.PlayLine
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by HeYanLe on 2023/3/4 14:39.
 * https://github.com/heyanLE
 */
class YhdmDetailedComponent(
    source: YhdmSource
) : ComponentWrapper(source), DetailedComponent {


    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary)
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {
            playLine(getDoc(summary), summary)
        }
    }

    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary) to playLine(getDoc(summary), summary)
        }
    }

    private fun getDoc(summary: CartoonSummary): Document {
        val d = networkHelper.cloudflareUserClient.newCall(GET(url(summary.url)))
            .execute().body?.string() ?: throw NullPointerException()
        return Jsoup.parse(d)
    }

    private fun playLine(doc: Document, summary: CartoonSummary) : List<PlayLine> {
        val tabs = doc.select("body div.tabs")[0]
        val title = tabs.child(0).children().iterator()
        val epRoot = doc.select("body div.tabs div.movurl ul").iterator()
        val res = arrayListOf<PlayLine>()
        var index = 0
        while (title.hasNext() && epRoot.hasNext()){
            val tit = title.next()
            val ep = epRoot.next()
            val es = arrayListOf<String>()
            ep.children().forEach {
                es.add(it.text())
            }
            val playLine = PlayLine(
                id = index.toString(),
                label = tit.text(),
                episode = es
            )
            res.add(playLine)
            index ++
        }
        return res
    }


    private fun detailed(doc: Document, summary: CartoonSummary): Cartoon {
        val show = doc.select("body div.list div.show")[0]
        val title = show.child(1).text()
        val cover = url(show.child(0).attr("src"))

        val infoSub = show.child(3)
        val intro = infoSub.child(2).text().substringAfter(":")
        val status =
            if (intro.startsWith("连载")) Cartoon.STATUS_ONGOING
            else if (intro.startsWith("完结")) Cartoon.STATUS_COMPLETED
            else Cartoon.STATUS_UNKNOWN

        val isTheater = infoSub.child(0).text().contains("剧场版")

        val desc = doc.select("body div.info").text()

        val update =
            if (isTheater) {
                if (status == Cartoon.STATUS_COMPLETED) {
                    Cartoon.UPDATE_STRATEGY_NEVER
                } else {
                    Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                }
            } else {
                if (status == Cartoon.STATUS_COMPLETED) {
                    Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                } else {
                    Cartoon.UPDATE_STRATEGY_ALWAYS
                }
            }

        val genreRoot = infoSub.child(4)
        var genre = buildString {
            genreRoot.children().iterator().forEach {
                append(it.text())
                append(", ")
            }
        }
        if (genre.endsWith(", ")) {
            genre = genre.subSequence(0, genre.length - 2).toString()
        }

        return CartoonImpl(
            id = summary.id,
            url = summary.url,
            source = summary.source,

            title = title,
            coverUrl = cover,

            intro = intro,
            description = desc,

            genre = genre,

            status = status,
            updateStrategy = update,
        )

    }
}