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