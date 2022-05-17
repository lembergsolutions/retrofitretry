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