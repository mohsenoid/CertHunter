package com.mohsenoid.certhunter.ui.list.widget

import android.content.pm.PackageManager
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Coil Fetcher that loads app icons by package name off the main thread.
 * Use [AppIconData] as the model in ImageRequest to trigger this fetcher.
 */
class AppIconFetcher(
    private val packageManager: PackageManager,
    private val packageName: String,
) : Fetcher {

    override suspend fun fetch(): FetchResult = DrawableResult(
        drawable = packageManager.getApplicationIcon(packageName),
        isSampled = false,
        dataSource = DataSource.MEMORY_CACHE,
    )

    class Factory : Fetcher.Factory<AppIconData> {
        override fun create(data: AppIconData, options: Options, imageLoader: ImageLoader) =
            AppIconFetcher(options.context.packageManager, data.packageName)
    }
}

/** Wrapper type used as the Coil ImageRequest model for app icon loading. */
data class AppIconData(val packageName: String)
