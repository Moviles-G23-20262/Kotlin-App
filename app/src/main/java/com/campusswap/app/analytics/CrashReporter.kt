package com.campusswap.app.analytics

/** Records uncaught exceptions synchronously to the event store, then defers to the system handler. */
object CrashReporter {
    fun install(store: FileEventStore, buildEvent: (Map<String, Any?>) -> AnalyticsEvent) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val frame = throwable.stackTrace.firstOrNull()
                store.append(
                    buildEvent(
                        mapOf(
                            "exception" to throwable::class.java.name,
                            "message" to throwable.message,
                            "top_frame" to frame?.toString(),
                            "thread" to thread.name,
                        ),
                    ),
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
