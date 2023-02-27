package com.heyanle.easybangumi_extension.yhdm

import com.heyanle.lib_anim.utils.SourceUtils
import com.heyanle.bangumi_source_api.api.Source
import com.heyanle.easybangumi_extension.R
import com.heyanle.extension_api.ExtensionIconSource

/**
 * Created by HeYanLe on 2023/2/25 15:19.
 * https://github.com/heyanLE
 */

fun url(source: String): String {
    return SourceUtils.urlParser(YhdmSource.ROOT_URL, source)
}
class YhdmSource: Source, ExtensionIconSource {

    companion object {
        const val ROOT_URL = "https://www.yhdmp.net/"


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


    /**
     * 拆分
     */
    override fun sources(): List<Source> {
        return listOf(
            YhdmSearchImpl(this),
            YhdmPageImpl(this)
        )
    }



}