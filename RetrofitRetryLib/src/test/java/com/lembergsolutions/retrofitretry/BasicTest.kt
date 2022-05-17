/*
 * Copyright (C) 2022 Lemberg Solutions.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lembergsolutions.retrofitretry

import com.google.gson.Gson
import com.google.gson.stream.MalformedJsonException
import com.lembergsolutions.retrofitretry.api.RetryOnError
import com.lembergsolutions.retrofitretry.implementation.RetrofitRetryCallAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class BasicTest : RetryTestBase() {

    private data class ResponseData(val data: Int)

    private interface ApiInterface {
        @RetryOnError(RETRY_COUNT, handlerClass = SimpleRetryHandler::class)
        @GET(API_PATH)
        suspend fun retryRequest(): Response<ResponseData>
    }

    private val api = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(createHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RetrofitRetryCallAdapterFactory.createCoroutineAdapter(testScope))
        .build()
        .create(ApiInterface::class.java)

    @Test
    fun `Given API with Retry (max, delay) specified When request failed on HTTP error Then request should be retried specified amount with specified delay`() {
        // ARRANGE
        // original request + repeats
        repeat(RETRY_COUNT + 1) {
            server.enqueue(MockResponse().apply {
                status = HTTP401_STATUS
            })
        }

        runBlocking {
            // ACT
            val (recordedRequests, result, execTime) = runRequests(RETRY_COUNT + 1)

            // ASSERT
            val response = result.getOrThrow()

            // check status code
            assertThat(response.code()).isEqualTo(401)

            // check requests count
            assertThat(server.requestCount).isEqualTo(RETRY_COUNT + 1)

            // check duration
            assertThat(execTime).isGreaterThanOrEqualTo(SimpleRetryHandler.RETRY_DELAY * RETRY_COUNT)

            // check API path
            val allRequestsValid = recordedRequests.map { it.path }.all { it == API_PATH }
            assertThat(allRequestsValid)
        }
    }

    @Test
    fun `Given API with Retry (max, delay) specified When request failed on data parsing Then request should be retried specified amount with specified delay`() {
        // ARRANGE
        // original request + repeats
        repeat(RETRY_COUNT + 1) {
            server.enqueue(MockResponse().apply {
                setBody("Error")
            })
        }

        runBlocking {
            // ACT
            val (_, result, execTime) = runRequests(RETRY_COUNT + 1)

            // ASSERT
            // check for failure
            assertThat(result.isFailure)

            // check exception type
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(MalformedJsonException::class.java)

            // check requests count
            assertThat(server.requestCount).isEqualTo(RETRY_COUNT + 1)

            // check duration
            assertThat(execTime).isGreaterThanOrEqualTo(SimpleRetryHandler.RETRY_DELAY * RETRY_COUNT)
        }
    }

    @Test
    fun `Given API with Retry (max, delay) specified When first request failed and second succeeded Then request should be succeeded for a client`() {
        // ARRANGE
        val testData = ResponseData(0x1234)

        // first request failed
        server.enqueue(MockResponse().apply {
            status = HTTP401_STATUS
        })

        // second request successful
        server.enqueue(MockResponse().apply {
            setBody(Gson().toJson(testData))
        })

        runBlocking {
            // ACT
            val (recordedRequests, result, execTime) = runRequests(2)

            // ASSERT
            // check result
            assertThat(result.isSuccess)

            val response = result.getOrThrow()

            // check response status code
            assertThat(response.isSuccessful)

            // check requests count
            assertThat(server.requestCount).isEqualTo(2)

            // check duration
            assertThat(execTime).isGreaterThanOrEqualTo(SimpleRetryHandler.RETRY_DELAY)

            // check API path
            val allRequestsValid = recordedRequests.map { it.path }.all { it == API_PATH }
            assertThat(allRequestsValid)

            // check response
            assertThat(response.body()).isEqualTo(testData)
        }
    }

    @Test
    fun `Given API with Retry (max, delay) specified When first request succeeded Then request should not be retried`() {
        // ARRANGE
        val testData = ResponseData(0x1234)

        // first request successful
        server.enqueue(MockResponse().apply {
            setBody(Gson().toJson(testData))
        })

        runBlocking {
            // ACT
            val (recordedRequests, result, execTime) = runRequests(1)

            // ASSERT
            // check result
            assertThat(result.isSuccess)

            val response = result.getOrThrow()

            // check response status code
            assertThat(response.isSuccessful)

            // check requests count
            assertThat(server.requestCount).isEqualTo(1)

            // check API path
            val allRequestsValid = recordedRequests.map { it.path }.all { it == API_PATH }
            assertThat(allRequestsValid)

            // check response
            assertThat(response.body()).isEqualTo(testData)
        }
    }

    private data class TestResult(
        val recordedRequests: List<RecordedRequest>,
        val result: Result<Response<ResponseData>>,
        val execTime: Long
    )

    private suspend fun runRequests(reqCount: Int): TestResult {
        val startTime = testDispatcher.scheduler.currentTime

        val responseJob = testScope.async {
            api.retryRequest()
        }
        testDispatcher.scheduler.runCurrent()

        val recordedRequests = recordRequests(reqCount)

        val result = runCatching {
            responseJob.await()
        }
        val execTime = testDispatcher.scheduler.currentTime - startTime

        return TestResult(recordedRequests, result, execTime)
    }

    companion object {
        private const val RETRY_COUNT = 5
        private const val API_PATH = "/retry"
    }
}