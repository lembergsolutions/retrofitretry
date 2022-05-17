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
package com.lembergsolutions.retrofitretry.api

import com.lembergsolutions.retrofitretry.retryhandler.Http429RetryHandler
import kotlin.reflect.KClass

/**
 * Default number of retries
 */
const val DEFAULT_MAX_RETRIES = 10

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RetryOnError(val maxRetries: Int = DEFAULT_MAX_RETRIES, val handlerClass: KClass<out RetryHandler> = Http429RetryHandler::class)
