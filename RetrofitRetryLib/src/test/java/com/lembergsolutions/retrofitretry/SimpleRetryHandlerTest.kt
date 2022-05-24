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

import okhttp3.Request
import org.assertj.core.api.Assertions
import org.junit.Test

class SimpleRetryHandlerTest {
    private val retryHandler = SimpleRetryHandler()

    @Test
    fun `Given DefaultRetryHandler When calling getNextRetryDelay Then request should be retried specified amount with specified delay`() {
        // ARRANGE
        val testRetryCount = 3

        val request = Request.Builder().url("https://test.local").build()
        val result = Result.failure<retrofit2.Response<out Any?>>(Exception())

        // ACT
        val delay = retryHandler.getRetryDelay(request, result, testRetryCount, 10)

        // ASSERT
        Assertions.assertThat(delay).isEqualTo(SimpleRetryHandler.RETRY_DELAY)
    }
}