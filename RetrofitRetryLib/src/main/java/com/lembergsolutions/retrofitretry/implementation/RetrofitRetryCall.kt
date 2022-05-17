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

import android.util.Log
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

internal class RetrofitRetryCall<R>(
    private val delayRunner: DelayRunner,
    private val call: Call<R>,
    private val retryParams: RetryParams
    ) : Call<R> {

    @Throws(IOException::class)
    override fun execute(): Response<R> {
        return call.execute()
    }

    override fun enqueue(callback: retrofit2.Callback<R>) {
        call.enqueue(Callback(callback))
    }

    override fun isExecuted(): Boolean {
        return call.isExecuted
    }

    override fun cancel() {
        delayRunner.cancelDelayedRun()
        call.cancel()
    }

    override fun isCanceled(): Boolean {
        return call.isCanceled
    }

    override fun clone(): Call<R> {
        return RetrofitRetryCall(delayRunner, call.clone(), retryParams)
    }

    override fun request(): Request {
        return call.request()
    }

    override fun timeout(): Timeout {
        return call.timeout()
    }

    internal inner class Callback(private val callback: retrofit2.Callback<R>) : retrofit2.Callback<R> {

        private var retryCount = 0

        override fun onResponse(call: Call<R>, response: Response<R>) {
            if (!response.isSuccessful) {
                Log.e(TAG, "Request ${call.request().url()} failed, status code: ${response.code()}")

                if (checkAndRetry(call, Result.success(response))) return
            }

            // default processing
            callback.onResponse(call, response)
        }

        override fun onFailure(call: Call<R>, error: Throwable) {
            Log.e(TAG, "Request ${call.request().url()} failed", error)

            if (checkAndRetry(call, Result.failure(error))) return

            // default processing
            callback.onFailure(call, error)
        }

        private fun checkAndRetry(call: Call<R>, result: Result<Response<out Any?>>): Boolean {
            if (!call.isCanceled && retryCount < retryParams.maxRetries) {
                retryCount++
                val delay = retryParams.retryHandler.getRetryDelay(call.request(), result, retryCount, retryParams.maxRetries)
                if (delay >= 0) {
                    Log.i(TAG, "Retrying $retryCount/${retryParams.maxRetries} ...")
                    delayAndRetryCall(delay, this@Callback)
                    return true
                }
            } else {
                Log.i(TAG, "No retries left, run default processing")
            }
            return false
        }

        private fun delayAndRetryCall(delayMillis: Long, callback: Callback) {
            delayRunner.cancelDelayedRun()
            if (delayMillis > 0) {
                Log.i(TAG, "Delaying retry for $delayMillis ms")
                delayRunner.scheduleDelayedRun(delayMillis) {
                    call.clone().enqueue(callback)
                }
            } else {
                // execute immediately
                Log.i(TAG, "Executing retry immediately")
                call.clone().enqueue(callback)
            }
        }
    }

    companion object {
        private val TAG = RetrofitRetryCall::class.java.simpleName
    }
}