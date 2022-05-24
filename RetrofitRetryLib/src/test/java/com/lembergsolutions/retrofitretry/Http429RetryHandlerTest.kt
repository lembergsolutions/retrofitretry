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

import com.lembergsolutions.retrofitretry.retryhandler.Http429RetryHandler
import com.lembergsolutions.retrofitretry.retryhandler.Http429RetryHandler.Companion.RETRY_AFTER_HEADER
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import okhttp3.Headers
import okhttp3.Request
import okhttp3.internal.http.RealResponseBody
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class Http429RetryHandlerTest {
    private val retryHandler = Http429RetryHandler()

    @Before
    fun setup() {
        val calendar = mockk<Calendar>()
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendar
        every { calendar.timeInMillis } returns 0
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Given Http429RetryHandler and failed HTTP request with status 429 but without Retry-After header When calling getNextRetryDelay Then request should not be retried`() {
        // ARRANGE
        val request = mockk<Request>(relaxed = true)
        val responseBody = mockk<RealResponseBody>(relaxed = true)
        val response = Response.error<String>(Http429RetryHandler.HTTP_TOO_MANY_REQUESTS_STATUS, responseBody)
        val result = Result.success<Response<out Any?>>(response)

        // ACT
        val delay = retryHandler.getRetryDelay(request, result, RETRY_COUNT, MAX_RETRIES)

        // ASSERT
        Assertions.assertThat(delay).isEqualTo(-1)
    }

    @Test
    fun `Given Http429RetryHandler and failed HTTP request with status 429 and with Retry-After header in seconds format When calling getNextRetryDelay Then request should be retried after specified delay`() {
        // ARRANGE
        val retryAfterSeconds = 100L

        // ACT
        val actualDelay = test(retryAfterSeconds.toString())

        // ASSERT
        val expectedDelayMillis = TimeUnit.SECONDS.toMillis(retryAfterSeconds)
        Assertions.assertThat(actualDelay).isEqualTo(expectedDelayMillis)
    }

    @Test
    fun `Given Http429RetryHandler and failed HTTP request with status 429 and with Retry-After in date format header When calling getNextRetryDelay Then request should be retried after specified delay`() {
        // ARRANGE
        val retryAfterSeconds = 100L

        // ACT
        val actualDelay = test(formatDate(Calendar.getInstance().timeInMillis + TimeUnit.SECONDS.toMillis(retryAfterSeconds)))

        // ASSERT
        val expectedDelayMillis = TimeUnit.SECONDS.toMillis(retryAfterSeconds)
        Assertions.assertThat(actualDelay).isEqualTo(expectedDelayMillis)
    }

    private fun test(retryAfterValue: String): Long {
        val request = mockk<Request>(relaxed = true)
        val responseBody = mockk<RealResponseBody>(relaxed = true)
        val rawResponse = mockk<okhttp3.Response>(relaxed = true)
        val headers = Headers.headersOf(RETRY_AFTER_HEADER, retryAfterValue)
        every { rawResponse.headers } returns headers
        every { rawResponse.code } returns Http429RetryHandler.HTTP_TOO_MANY_REQUESTS_STATUS
        val response = Response.error<String>(responseBody, rawResponse)
        val result = Result.success<Response<out Any?>>(response)

        return retryHandler.getRetryDelay(request, result, RETRY_COUNT, MAX_RETRIES)
    }

    private fun formatDate(timeMillis: Long): String {
        return SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).format(Date(timeMillis))
    }

    companion object {
        private const val RETRY_COUNT = 3
        private const val MAX_RETRIES = 10
    }
}