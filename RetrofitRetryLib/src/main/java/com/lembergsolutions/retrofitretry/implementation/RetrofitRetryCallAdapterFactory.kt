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
            return createCustomAdapter(CoroutineDelayRunner(scope))
        }

        @JvmStatic
        fun createCustomAdapter(delayRunner: DelayRunner): RetrofitRetryCallAdapterFactory {
            return RetrofitRetryCallAdapterFactory(delayRunner)
        }
    }
}