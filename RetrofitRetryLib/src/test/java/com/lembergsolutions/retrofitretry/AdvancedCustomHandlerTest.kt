/*
 * MIT License
 *
 * Copyright (c) 2022 Lemberg Solutions
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.lembergsolutions.retrofitretry

import com.lembergsolutions.retrofitretry.api.RetryOnError
import com.lembergsolutions.retrofitretry.api.RetryHandler
import com.lembergsolutions.retrofitretry.implementation.RetrofitRetryCallAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class AdvancedCustomHandlerTest : RetryTestBase() {

    /**
     * This retry handler limits retry count to CUSTOM_RETRY_COUNT
     */
    class LimitRetryCountCustomRetryHandler : RetryHandler {
        override fun getRetryDelay(request: Request, result: Result<Response<out Any?>>, retryCount: Int, maxRetries: Int): Long {
            return if (retryCount <= CUSTOM_RETRY_COUNT) CUSTOM_RETRY_DELAY
            else -1
        }
    }

    /**
     * This retry handler do retry with a delay for CUSTOM_RETRY_COUNT only on 500 errors
     */
    class Http500CustomRetryHandler : RetryHandler {
        override fun getRetryDelay(request: Request, result: Result<Response<out Any?>>, retryCount: Int, maxRetries: Int): Long {
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error is HttpException && error.code() == 500) {
                    return CUSTOM_RETRY_DELAY
                }
            }
            return -1
        }
    }

    interface ApiInterface {
        // Use high "max" to not restrict custom handler in retries count
        @RetryOnError(maxRetries = Integer.MAX_VALUE, handlerClass = LimitRetryCountCustomRetryHandler::class)
        @GET(API_PATH)
        suspend fun retryLimitRetryCount(): Response<String>

        // Use high "max" to not restrict custom handler in retries count
        @RetryOnError(maxRetries = Integer.MAX_VALUE, handlerClass = Http500CustomRetryHandler::class)
        @GET(API_PATH)
        suspend fun retryHttp500(): Response<String>
    }

    private val api = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(createHttpClient())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addCallAdapterFactory(RetrofitRetryCallAdapterFactory.createCoroutineAdapter(testScope))
        .build()
        .create(ApiInterface::class.java)

    @Test
    fun `Given API with Retry (customHandler) specified When request failed on HTTP error Then request should be retried after specified delay`() {
        // first requests and repeats
        repeat(CUSTOM_RETRY_COUNT + 1) {
            server.enqueue(MockResponse().apply {
                status = HTTP401_STATUS
            })
        }

        runBlocking {
            // ACT
            val (recordedRequests, result, execTime) = runRequests(CUSTOM_RETRY_COUNT + 1)

            // ASSERT
            // check result
            assertThat(result.isFailure)

            val response = result.getOrThrow()

            // check status code
            assertThat(response.code()).isEqualTo(401)

            // check requests count
            assertThat(server.requestCount).isEqualTo(CUSTOM_RETRY_COUNT + 1)

            // check duration
            assertThat(execTime).isEqualTo(CUSTOM_RETRY_DELAY * CUSTOM_RETRY_COUNT)

            // check API path
            val allRequestsValid = recordedRequests.map { it.path }.all { it == API_PATH }
            assertThat(allRequestsValid)
        }
    }

    @Test
    fun `Given API with Retry (customHandler) specified When request failed on 500 HTTP error Then request should be retried after specified delay`() {
        // first request failed
        server.enqueue(MockResponse().apply {
            status = HTTP500_STATUS
        })

        // second request successful
        server.enqueue(MockResponse().apply {
            setBody("Test")
        })

        runBlocking {
            // ACT
            val (recordedRequests, result, execTime) = runRequests(2)

            // ASSERT
            // check result
            assertThat(result.isSuccess)

            val response = result.getOrThrow()

            // check status code
            assertThat(response.code()).isEqualTo(200)

            // check requests count
            assertThat(server.requestCount).isEqualTo(2)

            // check duration
            assertThat(execTime).isEqualTo(CUSTOM_RETRY_DELAY)

            // check API path
            val allRequestsValid = recordedRequests.map { it.path }.all { it == API_PATH }
            assertThat(allRequestsValid)
        }
    }

    private suspend fun runRequests(reqCount: Int): TestResult {
        val startTime = testDispatcher.scheduler.currentTime

        val responseJob = testScope.async {
            api.retryLimitRetryCount()
        }
        testDispatcher.scheduler.runCurrent()

        val recordedRequests = recordRequests(reqCount)

        val result = runCatching {
            responseJob.await()
        }
        val execTime = testDispatcher.scheduler.currentTime - startTime

        return TestResult(recordedRequests, result, execTime)
    }

    private data class TestResult(
        val recordedRequests: List<RecordedRequest>,
        val result: Result<Response<String>>,
        val execTime: Long
    )

    companion object {
        private const val CUSTOM_RETRY_COUNT = 5
        private const val CUSTOM_RETRY_DELAY = 10000L
        private const val API_PATH = "/retry_custom_handler"
        private const val HTTP500_STATUS = "HTTP/1.1 500 Server error"
    }
}