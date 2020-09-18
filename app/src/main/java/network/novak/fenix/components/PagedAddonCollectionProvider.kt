/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package network.novak.fenix.components

import android.content.Context
import android.util.AtomicFile
import androidx.annotation.VisibleForTesting
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonsProvider
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlin.sanitizeURL
import mozilla.components.support.ktx.util.readAndDeserialize
import mozilla.components.support.ktx.util.writeString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

internal const val API_VERSION = "api/v4"
internal const val DEFAULT_SERVER_URL = "https://addons.mozilla.org"
internal const val DEFAULT_COLLECTION_ACCOUNT = "mozilla"
internal const val DEFAULT_COLLECTION_NAME = "7e8d6dc651b54ab385fb8791bf9dac"
internal const val COLLECTION_FILE_NAME = "%s_components_addon_collection_%s.json"
internal const val MINUTE_IN_MS = 60 * 1000
internal const val DEFAULT_READ_TIMEOUT_IN_SECONDS = 20L

/**
 * Provide access to the collections AMO API.
 * https://addons-server.readthedocs.io/en/latest/topics/api/collections.html
 *
 * Unlike the android-components version, supports multiple-page responses and
 * custom collection accounts.
 *
 * Needs to extend AddonCollectionProvider because AddonsManagerAdapter won't
 * take just any AddonsProvider.
 *
 * @property serverURL The url of the endpoint to interact with e.g production, staging
 * or testing. Defaults to [DEFAULT_SERVER_URL].
 * @property collectionAccount The account owning the collection to access, defaults
 * to [DEFAULT_COLLECTION_ACCOUNT].
 * @property collectionName The name of the collection to access, defaults
 * to [DEFAULT_COLLECTION_NAME].
 * @property maxCacheAgeInMinutes maximum time (in minutes) the collection cache
 * should remain valid. Defaults to -1, meaning no cache is being used by default.
 * @property client A reference of [Client] for interacting with the AMO HTTP api.
 */
@Suppress("LongParameterList")
class PagedAddonCollectionProvider(
    private val context: Context,
    private val client: Client,
    private val serverURL: String = DEFAULT_SERVER_URL,
    private var collectionAccount: String = DEFAULT_COLLECTION_ACCOUNT,
    private var collectionName: String = DEFAULT_COLLECTION_NAME,
    private val maxCacheAgeInMinutes: Long = -1
) : AddonsProvider {

    private val logger = Logger("PagedAddonCollectionProvider")

    private val diskCacheLock = Any()

    fun setCollectionAccount(account: String) {
        collectionAccount = account
    }

    fun setCollectionName(collection: String) {
        collectionName = collection
    }

    /**
     * Interacts with the collections endpoint to provide a list of available
     * add-ons. May return a cached response, if available, not expired (see
     * [maxCacheAgeInMinutes]) and allowed (see [allowCache]).
     *
     * @param allowCache whether or not the result may be provided
     * from a previously cached response, defaults to true.
     * @param readTimeoutInSeconds optional timeout in seconds to use when fetching
     * available add-ons from a remote endpoint. If not specified [DEFAULT_READ_TIMEOUT_IN_SECONDS]
     * will be used.
     * @throws IOException if the request failed, or could not be executed due to cancellation,
     * a connectivity problem or a timeout.
     */
    @Throws(IOException::class)
    override suspend fun getAvailableAddons(allowCache: Boolean, readTimeoutInSeconds: Long?): List<Addon> {
        val cachedAddons = if (allowCache && !cacheExpired(context)) {
            readFromDiskCache()
        } else {
            null
        }

        if (cachedAddons != null) {
            return cachedAddons
        } else {
            return getAllPages(listOf(
                serverURL,
                API_VERSION,
                "accounts/account",
                collectionAccount,
                "collections",
                collectionName,
                "addons"
            ).joinToString("/"), readTimeoutInSeconds ?: DEFAULT_READ_TIMEOUT_IN_SECONDS).also {
                // Cache the JSON object before we parse out the addons
                if (maxCacheAgeInMinutes > 0) {
                    writeToDiskCache(it.toString())
                }
            }.getAddons()
        }
    }

    /**
     * Fetches all pages of add-ons from the given URL (following the "next"
     * field in the returned JSON) and combines the "results" arrays into that
     * of the first page. Returns that coalesced object.
     *
     * @param url URL of the first page to fetch
     * @param readTimeoutInSeconds timeout in seconds to use when fetching each page.
     * @throws IOException if the request failed, or could not be executed due to cancellation,
     * a connectivity problem or a timeout.
     */
    @Throws(IOException::class)
    suspend fun getAllPages(url: String, readTimeoutInSeconds: Long): JSONObject {
        // Fetch and compile all the pages into one object we can return
        var compiledResponse: JSONObject? = null
        // Each page tells us where to get the next page, if there is one
        var nextURL: String? = url
        while (nextURL != null) {
            client.fetch(
                Request(
                    url = nextURL,
                    readTimeout = Pair(readTimeoutInSeconds, TimeUnit.SECONDS)
                )
            )
            .use { response ->
                if (!response.isSuccess) {
                    val errorMessage = "Failed to fetch addon collection. Status code: ${response.status}"
                    logger.error(errorMessage)
                    throw IOException(errorMessage)
                }

                val currentResponse = try {
                    JSONObject(response.body.string(Charsets.UTF_8))
                } catch (e: JSONException) {
                    throw IOException(e)
                }
                if (compiledResponse == null) {
                    compiledResponse = currentResponse
                } else {
                    // Write the addons into the first response
                    compiledResponse!!.getJSONArray("results").concat(currentResponse.getJSONArray("results"))
                }
                nextURL = if (currentResponse.isNull("next")) null else currentResponse.getString("next")
            }
        }
        return compiledResponse!!
    }

    /**
     * Fetches given Addon icon from the url and returns a decoded Bitmap
     * @throws IOException if the request could not be executed due to cancellation,
     * a connectivity problem or a timeout.
     */
    @Throws(IOException::class)
    suspend fun getAddonIconBitmap(addon: Addon): Bitmap? {
        var bitmap: Bitmap? = null
        if (addon.iconUrl != "") {
            client.fetch(
                    Request(url = addon.iconUrl.sanitizeURL())
            ).use { response ->
                if (response.isSuccess) {
                    response.body.useStream {
                        bitmap = BitmapFactory.decodeStream(it)
                    }
                }
            }
        }

        return bitmap
    }

    @VisibleForTesting
    internal fun writeToDiskCache(collectionResponse: String) {
        synchronized(diskCacheLock) {
            getCacheFile(context).writeString { collectionResponse }
        }
    }

    @VisibleForTesting
    internal fun readFromDiskCache(): List<Addon>? {
        synchronized(diskCacheLock) {
            return getCacheFile(context).readAndDeserialize {
                JSONObject(it).getAddons()
            }
        }
    }

    @VisibleForTesting
    internal fun cacheExpired(context: Context): Boolean {
        return getCacheLastUpdated(context) < Date().time - maxCacheAgeInMinutes * MINUTE_IN_MS
    }

    @VisibleForTesting
    internal fun getCacheLastUpdated(context: Context): Long {
        val file = getBaseCacheFile(context)
        return if (file.exists()) file.lastModified() else -1
    }

    private fun getCacheFile(context: Context): AtomicFile {
        return AtomicFile(getBaseCacheFile(context))
    }

    private fun getBaseCacheFile(context: Context): File {
        return File(context.filesDir, COLLECTION_FILE_NAME.format(collectionAccount, collectionName))
    }

    fun deleteCacheFile(context: Context): Boolean {
        val file = getBaseCacheFile(context)
        return if (file.exists()) file.delete() else false
    }
}

internal fun JSONObject.getAddons(): List<Addon> {
    val addonsJson = getJSONArray("results")
    return (0 until addonsJson.length()).map { index ->
        addonsJson.getJSONObject(index).toAddons()
    }
}

internal fun JSONObject.toAddons(): Addon {
    return with(getJSONObject("addon")) {
        Addon(
            id = getSafeString("guid"),
            authors = getAuthors(),
            categories = getCategories(),
            createdAt = getSafeString("created"),
            updatedAt = getSafeString("last_updated"),
            downloadUrl = getDownloadUrl(),
            version = getCurrentVersion(),
            permissions = getPermissions(),
            translatableName = getSafeMap("name"),
            translatableDescription = getSafeMap("description"),
            translatableSummary = getSafeMap("summary"),
            iconUrl = getSafeString("icon_url"),
            siteUrl = getSafeString("url"),
            rating = getRating(),
            defaultLocale = getSafeString("default_locale").ifEmpty { Addon.DEFAULT_LOCALE }
        )
    }
}

internal fun JSONObject.getRating(): Addon.Rating? {
    val jsonRating = optJSONObject("ratings")
    return if (jsonRating != null) {
        Addon.Rating(
            reviews = jsonRating.optInt("count"),
            average = jsonRating.optDouble("average").toFloat()
        )
    } else {
        null
    }
}

internal fun JSONObject.getCategories(): List<String> {
    val jsonCategories = optJSONObject("categories")
    return if (jsonCategories == null) {
        emptyList()
    } else {
        val jsonAndroidCategories = jsonCategories.getSafeJSONArray("android")
        (0 until jsonAndroidCategories.length()).map { index ->
            jsonAndroidCategories.getString(index)
        }
    }
}

internal fun JSONObject.getPermissions(): List<String> {
    val fileJson = getJSONObject("current_version")
        .getSafeJSONArray("files")
        .getJSONObject(0)

    val permissionsJson = fileJson.getSafeJSONArray("permissions")
    return (0 until permissionsJson.length()).map { index ->
        permissionsJson.getString(index)
    }
}

internal fun JSONObject.getCurrentVersion(): String {
    return optJSONObject("current_version")?.getSafeString("version") ?: ""
}

internal fun JSONObject.getDownloadUrl(): String {
    return (getJSONObject("current_version")
        .optJSONArray("files")
        ?.getJSONObject(0))
        ?.getSafeString("url") ?: ""
}

internal fun JSONObject.getAuthors(): List<Addon.Author> {
    val authorsJson = getSafeJSONArray("authors")
    return (0 until authorsJson.length()).map { index ->
        val authorJson = authorsJson.getJSONObject(index)

        Addon.Author(
            id = authorJson.getSafeString("id"),
            name = authorJson.getSafeString("name"),
            username = authorJson.getSafeString("username"),
            url = authorJson.getSafeString("url")
        )
    }
}

internal fun JSONObject.getSafeString(key: String): String {
    return if (isNull(key)) {
        ""
    } else {
        getString(key)
    }
}

internal fun JSONObject.getSafeJSONArray(key: String): JSONArray {
    return if (isNull(key)) {
        JSONArray("[]")
    } else {
        getJSONArray(key)
    }
}

internal fun JSONObject.getSafeMap(valueKey: String): Map<String, String> {
    return if (isNull(valueKey)) {
        emptyMap()
    } else {
        val map = mutableMapOf<String, String>()
        val jsonObject = getJSONObject(valueKey)

        jsonObject.keys()
            .forEach { key ->
                map[key] = jsonObject.getSafeString(key)
            }
        map
    }
}

/**
 * Concatenates the given JSONArray onto this one.
 */
internal fun JSONArray.concat(other: JSONArray) {
    (0 until other.length()).map { index ->
        put(length(), other.getJSONObject(index))
    }
}
