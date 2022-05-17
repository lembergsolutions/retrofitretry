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
package com.lembergsolutions.retrofitretry.retryhandler

import com.lembergsolutions.retrofitretry.api.RetryHandler
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Retry handler that follows HTTP 429 status code and corresponding "Retry-After" header.
 * It repeat the request after specified in the response delay.
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