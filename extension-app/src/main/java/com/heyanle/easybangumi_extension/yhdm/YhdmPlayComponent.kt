package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.play.PlayComponent
import com.heyanle.bangumi_source_api.api.entity.CartoonSummary
import com.heyanle.bangumi_source_api.api.entity.PlayLine
import com.heyanle.bangumi_source_api.api.entity.PlayerInfo
import com.heyanle.bangumi_source_api.api.withResult

/**
 * Created by HeYanLe on 2023/3/4 14:42.
 * https://github.com/heyanLE
 */
class YhdmPlayComponent(
    source: YhdmSource
): ComponentWrapper(source), PlayComponent {

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episodeIndex: Int
    ): SourceResult<PlayerInfo> {

        return withResult {
            PlayerInfo(PlayerInfo.DECODE_TYPE_HLS, "https://kh4.psdcat.com/playm3u8/1676475262_aabbcc05_patengdwS206.m3u8")
        }
        // TODO("Not yet implemented")
    }

}