package com.devonjerothe.justletmelisten.network
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: ApiError) : ApiResult<Nothing>()
}

sealed class RssResult {
    data class Success(
        val channel: RssChannel,
        val etag: String?
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
    ): ApiResult<SearchResponse> {
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

    suspend fun getPodcastEpisodes(
        feedUrl: String,
        etag: String? = null
    ): RssResult {
        return try {
            val rssClient = HttpClient(OkHttp)
            val response: HttpResponse = rssClient.get(feedUrl) {
                etag?.let { header(HttpHeaders.IfNoneMatch, it) }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val channel = response.bodyAsText()
                    val newEtag = response.headers[HttpHeaders.ETag]

                    val rssPodcast = RssParser().parse(channel)
                    RssResult.Success(
                        channel = rssPodcast,
                        etag = newEtag
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
}

/**
 * Simple response data for search results
 */
@Serializable
data class SearchResponse(
    val resultCount: Int,
    val results: List<SearchResult>
)

@Serializable
data class SearchResult(
    val wrapperType: String? = null,
    val kind: String? = null,
    val artistId: Long? = null,
    val collectionId: Long,
    val trackId: Long,
    val artistName: String? = null,
    val collectionName: String,
    val trackName: String,
    val collectionCensoredName: String? = null,
    val trackCensoredName: String? = null,
    val artistViewUrl: String? = null,
    val collectionViewUrl: String? = null,
    val feedUrl: String? = null,
    val trackViewUrl: String? = null,
    val artworkUrl30: String? = null,
    val artworkUrl60: String? = null,
    val artworkUrl100: String? = null,
    val collectionPrice: Double? = null,
    val trackPrice: Double? = null,
    val collectionHdPrice: Double? = null,
    val releaseDate: String? = null, // You might want to parse this to a Date object later
    val collectionExplicitness: String? = null,
    val trackExplicitness: String? = null,
    val trackCount: Int? = null,
    val trackTimeMillis: Long? = null,
    val country: String? = null,
    val currency: String? = null,
    val primaryGenreName: String? = null,
    val contentAdvisoryRating: String? = null,
    val artworkUrl600: String? = null,
    val genreIds: List<String>? = null,
    val genres: List<String>? = null
)
