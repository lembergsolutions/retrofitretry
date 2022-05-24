[![Release](https://jitpack.io/v/com.lembergsolutions/retrofitretry.svg)](https://jitpack.io/#com.lembergsolutions/retrofitretry)

# RetrofitRetry

An Android AAR library that adds additional handler to [Retrofit](https://square.github.io/retrofit/) requests processing and allows to retry failed requests after a delay.

The library has build-in retry handler that implements [HTTP 429](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429) functionality. 
Also you can provide your own retry handler.

## Basic usage

### Add the library dependency

Edit `build.gradle` in project root and add additional repository:

```groovy
allprojects {
    repositories {
        // google(), jcenter()...
        maven { url 'https://jitpack.io' }
    }
}
```

Add library dependency in your module `build.gradle`:

```groovy
dependencies {
    implementation "com.lembergsolutions:retrofitretry:1.0.0"
    // ...
}
```

### Get transparent "Rate-limits" support by handling HTTP 429 status in the library

Add `RetrofitRetryCallAdapterFactory` adapter factory into Retrofit API builder:

```kotlin
private val api = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addCallAdapterFactory(RetrofitRetryCallAdapterFactory.createCoroutineAdapter())
    .build()
    .create(ApiInterface::class.java)
```

Add annotation to your Retrofit API interface:

```kotlin
interface ApiInterface {
    @RetryOnError
    @GET("/news")
    suspend fun fetchNews(): Response<ResponseData>
}
```

Now when you call `fetchNews()` method and the server return HTTP status 429, retry handler will automatically schedule request retry after delay specified in the response.

The delay can be specified in format of seconds:

> Retry-After: 5

or by date:

> Retry-After: Thu, 24 Feb 2022 04:30:00 EET

When subsequent request is completed, its result will be delivered to your code, so from the code side the request will look like running some time.

The library is using Kotlin coroutines for retries scheduling and does not use blocking wait thus it does not stuck IO threads.

## Advanced usage

`RetryOnError` annotation allows to specify custom retry handler and maximum retries count:

```kotlin
interface ApiInterface {
    @RetryOnError(maxRetries = 10, handlerClass = MyCustomRetryHandler::class)
    @GET("/news")
    suspend fun fetchNews(): Response<ResponseData>
}
```

`MyCustomRetryHandler` should implement `RetryHandler` interface with one single method:

```kotlin
/**
 * Get the delay to wait before retrying next request
 * @param request request object
 * @param result request result containing response or error
 * @param retryCount current retry count
 * @param maxRetries maximum retries set in annotation
 * @return >= 0 delay before retry or -1 to cancel retrying
 */
fun getRetryDelay(request: Request, result: Result<retrofit2.Response<out Any?>>, retryCount: Int, maxRetries: Int): Long
```

Retry handler class is instantiated for every API method declaration.

In case if `getRetryDelay` return positive value, the library schedules retry of a request using supplied `DelayRunner`.

This interface allows to schedule delayed run of a function:

```kotlin
interface DelayRunner {
    /**
     * Schedule function execution after specified delay.
     * Any previously scheduled function will be cancelled.
     * @param millis delay in milliseconds
     * @param fn function to call
     */
    fun scheduleDelayedRun(millis: Long, fn: () -> Unit)

    /**
     * Cancel previously scheduled delayed run.
     */
    fun cancelDelayedRun()
}
```

Default implementation uses coroutines framework, but you can supply custom `DelayRunner`:

```kotlin
private val api = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addCallAdapterFactory(RetrofitRetryCallAdapterFactory.createCustomAdapter(myDelayRunner))
    .build()
    .create(ApiInterface::class.java)
```

# License

    MIT License
    
    Copyright (c) 2022 Lemberg Solutions
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
