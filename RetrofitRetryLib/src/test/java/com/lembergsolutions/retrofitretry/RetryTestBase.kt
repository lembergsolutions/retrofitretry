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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
open class RetryTestBase {
    @ExperimentalCoroutinesApi
    protected val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    protected val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
    protected val server = MockWebServer()

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    protected fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    protected suspend fun recordRequests(count: Int = 1): List<RecordedRequest> {
        val result = mutableListOf<RecordedRequest>()
        repeat(count) {
            result.add(server.takeRequest())

            // wait some time for delivering and processing response by Retrofit and retrying request
            delay(RESPONSE_PROCESS_TIME)

            testDispatcher.scheduler.advanceUntilIdle()
        }
        return result
    }

    companion object {
        const val RESPONSE_PROCESS_TIME = 10L
        const val SOCKET_TIMEOUT = 10L
        const val HTTP401_STATUS = "HTTP/1.1 401 Auth error"
    }
}