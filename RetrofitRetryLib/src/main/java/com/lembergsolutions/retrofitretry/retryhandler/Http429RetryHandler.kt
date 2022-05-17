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
package com.lembergsolutions.retrofitretry.retryhandler

import com.lembergsolutions.retrofitretry.api.RetryHandler
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Retry handler that follows HTTP 429 status code and corresponding "Retry-After" header.
 * It repeats the request after delay specified in the response.
 */
class Http429RetryHandler : RetryHandler {
    private val httpDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)

    override fun getRetryDelay(request: Request, result: Result<retrofit2.Response<out Any?>>, retryCount: Int, maxRetries: Int): Long {
        if (result.isSuccess) {
            result.getOrNull()?.let { response ->
                if (!response.isSuccessful && response.code() == HTTP_TOO_MANY_REQUESTS_STATUS) {
                    val delayMillis = response.headers()[RETRY_AFTER_HEADER]?.let { tryParseDelayBySeconds(it) ?: tryParseDelayByHttpDate(it) }
                    if (delayMillis != null) return delayMillis
                }
            }
        }
        // don't repeat
        return -1
    }

    private fun tryParseDelayBySeconds(str: String): Long? {
        return try {
            TimeUnit.SECONDS.toMillis(str.toLong())
        } catch (e: Exception) {
            null
        }
    }

    private fun tryParseDelayByHttpDate(str: String): Long? {
        val repeatTimeMillis = tryParseHttpDate(str) ?: return null
        val currentTimeMillis = Calendar.getInstance().timeInMillis
        if (repeatTimeMillis >= currentTimeMillis) return repeatTimeMillis - currentTimeMillis
        return null
    }

    private fun tryParseHttpDate(str: String): Long? {
        return try {
            httpDateFormat.parse(str).time
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val HTTP_TOO_MANY_REQUESTS_STATUS = 429
        const val RETRY_AFTER_HEADER = "Retry-After"
    }
}