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

import com.lembergsolutions.retrofitretry.api.RetryHandler
import okhttp3.Request

class SimpleRetryHandler : RetryHandler {
    override fun getRetryDelay(request: Request, result: Result<retrofit2.Response<out Any?>>, retryCount: Int, maxRetries: Int): Long {
        return RETRY_DELAY
    }

    companion object {
        const val RETRY_DELAY = 1000L
    }
}