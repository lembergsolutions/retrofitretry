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