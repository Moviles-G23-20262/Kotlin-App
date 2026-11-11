package com.campusswap.app.model.analytics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * BQ1 (type 1): "In which screens and features of the app do crashes happen most often, and on which
 * OS versions and device models?"
 *
 * Firebase Crashlytics already groups every crash by OS version and device model. This class adds
 * what Crashlytics can't know by itself: the screen and the feature the user was in, and the last
 * steps before the crash (breadcrumbs). In the Crashlytics console they show up as the custom keys
 * `screen` and `feature`, so crashes can be filtered and counted per screen and per feature.
 *
 * It does nothing when Firebase isn't configured (no `app/google-services.json`), so the app still
 * runs on every teammate's machine.
 */
object CrashTelemetry {
    private const val TAG = "CrashTelemetry"

    @Volatile
    private var crashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "Firebase is not configured (no google-services.json): crash reporting is off")
            return
        }
        crashlytics = FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            setCustomKey(KEY_SCREEN, "app_start")
            setCustomKey(KEY_FEATURE, "app_start")
        }
    }

    /** Called on every navigation, so a crash is attributed to the screen that was open. */
    fun screen(route: String) {
        val c = crashlytics ?: return
        c.setCustomKey(KEY_SCREEN, screenName(route))
        c.setCustomKey(KEY_FEATURE, featureOf(route))
        c.log("screen: ${screenName(route)}")
    }

    /** The analytics events the user triggered, kept as the steps that led to a crash. */
    fun breadcrumb(event: String) {
        crashlytics?.log("event: $event")
    }

    /** The account id (a UUID, never the email), so the team can tell one user crashing often from many. */
    fun user(userId: String?) {
        crashlytics?.setUserId(userId.orEmpty())
    }

    /** "chat/{productId}?room={room}" -> "chat". */
    internal fun screenName(route: String): String = route.substringBefore('/').substringBefore('?')

    /** Groups screens into the features of the app, which is how BQ1 is asked. */
    internal fun featureOf(route: String): String = when (screenName(route)) {
        "login", "register", "forgot-password", "verify-email" -> "auth"
        "home", "search", "product", "wishlist" -> "browse"
        "sell" -> "publish_listing"
        "cart", "checkout", "confirmation" -> "checkout"
        "messages", "chat" -> "chat"
        "meeting" -> "meeting_point"
        "complete" -> "exchange_and_rating"
        "notifications", "alerts" -> "smart_matching"
        "profile" -> "seller_hub"
        else -> "other"
    }

    private const val KEY_SCREEN = "screen"
    private const val KEY_FEATURE = "feature"
}
