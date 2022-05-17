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