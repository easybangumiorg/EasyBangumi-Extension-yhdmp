package com.heyanle.easybangumi_extension

import com.heyanle.bangumi_source_api.api2.Source
import com.heyanle.bangumi_source_api.api2.SourceFactory
import com.heyanle.easybangumi_extension.yhdm.YhdmSource

/**
 * Created by HeYanLe on 2023/2/19 23:23.
 * https://github.com/heyanLE
 */
class EasySourceFactory: SourceFactory {

    override fun create(): List<Source> {
        return listOf(
            YhdmSource()
        )
    }
}