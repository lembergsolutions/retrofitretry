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
import com.lembergsolutions.retrofitretry.implementation.RetrofitRetryCallAdapterFactory
import kotlinx.coroutines.*
import mockwebserver3.MockResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET


@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class CancellationTest : RetryTestBase() {

    private interface ApiInterface {
        @RetryOnError(RETRY_COUNT, handlerClass = SimpleRetryHandler::class)
        @GET(API_PATH)
        suspend fun retryRequest(): Response<String>
    }

    private fun createApi(): ApiInterface {
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(createHttpClient())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RetrofitRetryCallAdapterFactory.createCoroutineAdapter(testScope))
            .build()
            .create(ApiInterface::class.java)
    }

    @Test
    fun `Given API with Retry specified When request failed, retry is scheduled and request is cancelled Then request should complete with error and no other retries should be scheduled`() {
        test { responseJob ->
            // cancel the request
            responseJob.cancel(CANCEL_MESSAGE)
        }
    }

    @Test
    fun `Given API with Retry specified When request failed, retry is scheduled and scope is cancelled Then request should complete with error and no other retries should be scheduled`() {
        test {
            // cancel the scope
            testScope.cancel(CANCEL_MESSAGE)
        }
    }

    private fun test(cancelFn: (Deferred<Response<String>>) -> Unit) {
        // ARRANGE
        val response = MockResponse().apply {
            status = HTTP401_STATUS
        }
        repeat(RETRY_COUNT + 1) {
            server.enqueue(response)
        }

        runBlocking {
            // ACT
            val responseJob = testScope.async {
                createApi().retryRequest()
            }
            testDispatcher.scheduler.runCurrent()

            // wait for request
            server.takeRequest()

            // wait some time for processing response and retrying request
            delay(RESPONSE_PROCESS_TIME)

            cancelFn(responseJob)

            // execute RetryingCall.delayAndRetryCall/scope.launch coroutine (which should fail due to cancellation)
            testDispatcher.scheduler.runCurrent()

            val result = runCatching {
                responseJob.await()
            }

            // ASSERT
            assertThat(result.isFailure)
            assertThat(result.exceptionOrNull()?.message).isEqualTo(CANCEL_MESSAGE)
        }
    }

    companion object {
        const val RETRY_COUNT = 5
        const val API_PATH = "/retry"
        const val CANCEL_MESSAGE = "Cancelled by test"
    }
}