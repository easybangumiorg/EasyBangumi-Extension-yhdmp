package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.detailed.DetailedComponent
import com.heyanle.bangumi_source_api.api.component.page.PageComponent
import com.heyanle.bangumi_source_api.api.entity.Cartoon
import com.heyanle.bangumi_source_api.api.entity.CartoonImpl
import com.heyanle.bangumi_source_api.api.entity.CartoonSummary
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by HeYanLe on 2023/3/4 14:39.
 * https://github.com/heyanLE
 */
class YhdmDetailedComponent(
    source: YhdmSource
) : ComponentWrapper(source), DetailedComponent {

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            detailed(summary)
        }
    }


    private fun detailed(summary: CartoonSummary): Cartoon {
        val d = networkHelper.cloudflareUserClient.newCall(GET(url(summary.url)))
            .execute().body?.string() ?: throw NullPointerException()
        val doc = Jsoup.parse(d)
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
                    Cartoon.UPDATE_STRATEGY_ONLY_MANUAL
                }
            } else {
                if (status == Cartoon.STATUS_COMPLETED) {
                    Cartoon.UPDATE_STRATEGY_ONLY_MANUAL
                } else {
                    Cartoon.UPDATE_STRATEGY_ALWAYS
                }
            }

        val genreRoot = infoSub.child(4)
        var genre = buildString {
            genreRoot.children().iterator().forEach {
                append(it.text())
                append(",")
            }
        }
        if (genre.endsWith(",")) {
            genre = genre.subSequence(0, genre.length - 1).toString()
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
        ).apply {


        }

    }
}