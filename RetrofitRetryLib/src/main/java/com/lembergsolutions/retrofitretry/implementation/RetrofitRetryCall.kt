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
                Log.i(TAG, "No retries left, throwing error up")
            }
            return false
        }
    }

    companion object {
        private val TAG = RetrofitRetryCall::class.java.simpleName
    }
}