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
package com.lembergsolutions.retrofitretry.implementation

import com.lembergsolutions.retrofitretry.api.RetryOnError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.Type
import kotlin.reflect.full.createInstance

class RetrofitRetryCallAdapterFactory(private val delayRunner: DelayRunner) : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val retryAnnotation = getRetryAnnotation(annotations) ?: return null
        if (retryAnnotation.maxRetries <= 0) return null

        val maxRetries = retryAnnotation.maxRetries
        val retryHandler = retryAnnotation.handlerClass.createInstance()

        return RetrofitRetryCallAdapter(
            delayRunner,
            retrofit.nextCallAdapter(this, returnType, annotations),
            RetryParams(maxRetries, retryHandler)
        )
    }

    private fun getRetryAnnotation(annotations: Array<Annotation>): RetryOnError? {
        return annotations.filterIsInstance(RetryOnError::class.java).firstOrNull()
    }

    companion object {
        @JvmStatic
        fun createCoroutineAdapter(scope: CoroutineScope = GlobalScope): RetrofitRetryCallAdapterFactory {
            return RetrofitRetryCallAdapterFactory(CoroutineDelayRunner(scope))
        }

        @Suppress("unused")
        @JvmStatic
        fun createCustomAdapter(delayRunner: DelayRunner): RetrofitRetryCallAdapterFactory {
            return RetrofitRetryCallAdapterFactory(delayRunner)
        }
    }
}