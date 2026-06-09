package org.freedomsuite.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * OkHttp clients for Freedom Suite. All outbound HTTP must go through here so
 * we never accidentally inherit a library default that phones home.
 */
object PrivacyHttpClient {
    fun create(
        connectTimeoutSeconds: Long = 30,
        readTimeoutSeconds: Long = 60,
        writeTimeoutSeconds: Long = 60,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
        .proxy(java.net.Proxy.NO_PROXY)
        .addNetworkInterceptor(ForbiddenHostInterceptor)
        .build()

    private object ForbiddenHostInterceptor : Interceptor {
        private val blockedHostSuffixes = listOf(
            // Google telemetry / ads (not used by Freedom Suite; block misconfigured libs)
            "googleapis.com",
            "google-analytics.com",
            "app-measurement.com",
            "googleadservices.com",
            "doubleclick.net",
            "gstatic.com",
            "firebaseio.com",
            "crashlytics.com",
            // Crash / analytics SDKs
            "sentry.io",
            "bugsnag.com",
            "datadoghq.com",
            "segment.io",
            "mixpanel.com",
            "amplitude.com",
            "count.ly",
            "posthog.com",
            "newrelic.com",
            "appcenter.ms",
            "instabug.com",
            // Social / attribution
            "facebook.com",
            "graph.facebook.com",
            "adjust.com",
            "appsflyer.com",
        )

        override fun intercept(chain: Interceptor.Chain): Response {
            val host = chain.request().url.host.lowercase()
            val blocked = blockedHostSuffixes.any { host == it || host.endsWith(".$it") }
            require(!blocked) {
                "Blocked outbound request to non-approved host: $host"
            }
            return chain.proceed(chain.request())
        }
    }
}
