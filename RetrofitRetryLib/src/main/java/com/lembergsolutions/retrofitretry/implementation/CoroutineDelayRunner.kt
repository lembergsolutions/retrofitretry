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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * [DelayRunner] implementation based on Kotlin Coroutines.
 */
class CoroutineDelayRunner(private val scope: CoroutineScope) : DelayRunner {
    @Volatile
    private var delayJob: Job? = null

    override fun scheduleDelayedRun(millis: Long, fn: () -> Unit) {
        cancelDelayedRun()
        delayJob = scope.launch {
            Log.i(TAG, "Delaying for $millis ms")
            delay(millis)
            fn()
        }.apply {
            invokeOnCompletion {
                delayJob = null
            }
        }
    }

    override fun cancelDelayedRun() {
        delayJob?.let {
            Log.i(TAG, "Cancelling delay")
            it.cancel()
        }
    }

    companion object {
        private val TAG = CoroutineDelayRunner::class.java.simpleName
    }
}