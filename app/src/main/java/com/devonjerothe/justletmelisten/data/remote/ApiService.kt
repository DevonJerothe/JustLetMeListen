package com.devonjerothe.justletmelisten.data.remote

import android.util.Log
import com.devonjerothe.justletmelisten.BuildConfig
import com.devonjerothe.justletmelisten.data.remote.itunes.ItunesTopPodcast
import com.devonjerothe.justletmelisten.data.remote.itunes.ItunesTopPodcastResponse
import com.devonjerothe.justletmelisten.data.remote.itunes.toPodcastList
import com.devonjerothe.justletmelisten.data.remote.itunes.ItunesSearchResponse
import com.devonjerothe.justletmelisten.data.remote.itunes.ItunesPodcast
import com.devonjerothe.justletmelisten.data.remote.podcastIndex.PodcastIndexTrendingList
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import io.ktor.http.path
import io.ktor.util.sha1
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: ApiError) : ApiResult<Nothing>()
}

sealed class RssResult {
    data class Success(
        val channel: RssChannel,
        val etag: String?,
        val lastModified: String?
    ) : RssResult()

    object NotModified : RssResult()
    data class Error(val exception: ApiError) : RssResult()
}

sealed class ApiError {
    data class NetworkError(val exception: Exception) : ApiError()
    data class ServerError(val code: Int, val message: String) : ApiError()
    data class UnknownError(val message: String) : ApiError()
    object Timeout : ApiError()
    object SerializationError : ApiError()
}

class ApiService {

    // set up client
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
                contentType = ContentType("text", "javascript")
            )
        }

        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 15000L
            socketTimeoutMillis = 15000L
        }

        expectSuccess = true
    }

    /**
     * Search Itunes API for now. we should support other podcast providers later
     *  - podcastindex.org (free)
     */
    suspend fun searchPodcasts(
        query: String,
        limit: Int = 25
    ): ApiResult<ItunesSearchResponse> {
        return try {
            val response = client.request {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "itunes.apple.com"
                    path("search")
                    parameter("term", query)
                    parameter("limit", limit.toString())
                    parameter("media", "podcast")
                }
                this.method = HttpMethod.Get
            }
            ApiResult.Success(response.body())
        } catch (e: Exception) {
            val error = when (e) {
                is ResponseException -> ApiError.ServerError(
                    e.response.status.value,
                    e.response.status.description
                )
                // is SerializationException -> ApiError.SerializationError // This is specific to kotlinx.serialization
                is HttpRequestTimeoutException -> ApiError.Timeout
                else -> ApiError.UnknownError(e.message ?: "Unknown error")
            }
            ApiResult.Error(error)
        }
    }

    suspend fun getPodcast(
        id: String
    ): ApiResult<ItunesPodcast?> {
        return try {
            val response = client.request {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "itunes.apple.com"
                    path("lookup")
                    parameter("id", id)
                }
            }.body<ItunesSearchResponse>()

            val podcast = response.results.firstOrNull()
            ApiResult.Success(podcast)
        } catch (e: Exception) {
            val error = when (e) {
                is ResponseException -> ApiError.ServerError(
                    e.response.status.value,
                    e.response.status.description
                )
                is HttpRequestTimeoutException -> ApiError.Timeout
                else -> ApiError.UnknownError(e.message ?: "Unknown error")
            }
            ApiResult.Error(error)
        }
    }

    suspend fun getPopularPodcasts(
        country: String = "US"
    ): ApiResult<List<ItunesTopPodcast>> {
        return try {
            val response = client.request {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "itunes.apple.com"
                    appendPathSegments(
                        country,
                        "rss",
                        "toppodcasts",
                        "limit=15",
                        "explicit=true",
                        "json"
                    )
                }
                this.method = HttpMethod.Get
            }.body<ItunesTopPodcastResponse>()

            val podcasts = response.toPodcastList()
            ApiResult.Success(podcasts)
        } catch (e: Exception) {
            val error = when (e) {
                is ResponseException -> ApiError.ServerError(
                    e.response.status.value,
                    e.response.status.description
                )
                is HttpRequestTimeoutException -> ApiError.Timeout
                else -> ApiError.UnknownError(e.message ?: "Unknown error")
            }
            ApiResult.Error(error)
        }
    }

    suspend fun getPodcastEpisodes(
        feedUrl: String,
        etag: String? = null,
        lastModified: String? = null
    ): RssResult {
        return try {
            val rssClient = HttpClient(OkHttp)
            val response: HttpResponse = rssClient.get(feedUrl) {
                etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val channel = response.bodyAsText()
                    val newEtag = response.headers[HttpHeaders.ETag]
                    val lastModified = response.headers[HttpHeaders.LastModified]

                    val rssPodcast = RssParser().parse(channel)
                    RssResult.Success(
                        channel = rssPodcast,
                        etag = newEtag,
                        lastModified = lastModified
                    )
                }
                HttpStatusCode.NotModified -> {
                    RssResult.NotModified
                }
                else -> {
                    RssResult.Error(exception = ApiError.UnknownError(response.status.description))
                }
            }
        } catch (e: Exception) {
            RssResult.Error(exception = ApiError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    /**
     * Podcast Index API
     * - We can use https://github.com/mr3y-the-programmer/PodcastIndex-SDK if we want a full
     * migration to this API. For now we are just adding trending calls.
     *
     * TODO: support language selection
     */
    suspend fun getTrendingPodcasts(
        category: String? = "16"
    ): ApiResult<PodcastIndexTrendingList> {
        return try {
            val epochTime = (System.currentTimeMillis() / 1000)
            val response = client.request {
                headers {
                    val apiKey = BuildConfig.PODCAST_INDEX_API_KEY
                    val apiSecret = BuildConfig.PODCAST_INDEX_API_SECRET
                    val authToken = createAuthToken(apiKey, apiSecret, epochTime.toString())

                    Log.d("APIService", "pod index auth key: $apiKey")
                    Log.d("APIService", "pod index auth secret: $apiSecret")
                    Log.d("APIService", "pod index auth token: $authToken")

                    append(HttpHeaders.UserAgent, "JustLetMeListen/1.0")
                    append("X-Auth-Key", apiKey)
                    append("X-Auth-Date", epochTime.toString())
                    append("Authorization", authToken)
                }

                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.podcastindex.org"
                    path("api/1.0/podcasts/trending")
                    parameter("since", epochTime - 1000000)
                    parameter("max", "10")
                    parameter("lang", "en")
                    category?.let { parameter("cat", it) }
                }
                this.method = HttpMethod.Get
            }
            // return success body
            ApiResult.Success(response.body())
        } catch (e: Exception) {
            val error = when (e) {
                is ResponseException -> ApiError.ServerError(
                    e.response.status.value,
                    e.response.status.description
                )
                is HttpRequestTimeoutException -> ApiError.Timeout
                else -> ApiError.UnknownError(e.message ?: "Unknown error")
            }
            ApiResult.Error(error)
        }
    }

    /**
     * Creates the auth header value needed for PodcastIndex calls
     */
    private fun createAuthToken(apiKey: String, apiSecret: String, epoch: String): String {
        val hash = sha1("$apiKey$apiSecret$epoch".toByteArray())
        return hash.toHexString()
    }
}