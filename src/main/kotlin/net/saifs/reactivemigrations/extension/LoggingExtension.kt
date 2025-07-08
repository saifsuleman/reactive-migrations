package net.saifs.reactivemigrations.extension

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Level.TRACE
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias JULLevel = java.util.logging.Level
typealias JULLogManager = java.util.logging.LogManager

fun logger() = DelegatedLogger

inline fun <reified T> logger() =
    ExtendedLogger(LogManager.getLogger(T::class.java))

class ExtendedLogger(
    @PublishedApi
    internal val logger: Logger,
    @PublishedApi
    internal val marker: Marker? = null
) {
    inline val debug get() = logger.isDebugEnabled
    inline val trace get() = logger.isTraceEnabled

    inline fun trace(exception: Throwable? = null, message: () -> String) = log(TRACE, exception, message)
    inline fun debug(exception: Throwable? = null, message: () -> String) = log(Level.DEBUG, exception, message)
    inline fun info(exception: Throwable? = null, message: () -> String) = log(Level.INFO, exception, message)
    inline fun warn(exception: Throwable? = null, message: () -> String) = log(Level.WARN, exception, message)
    inline fun error(exception: Throwable? = null, message: () -> String) = log(Level.ERROR, exception, message)
    inline fun fatal(exception: Throwable? = null, message: () -> String) = log(Level.FATAL, exception, message)

    inline fun log(level: JULLevel, exception: Throwable?, message: () -> String) =
        log(
            when (level) {
                JULLevel.FINEST -> TRACE
                JULLevel.FINER, JULLevel.FINE -> Level.DEBUG
                JULLevel.CONFIG, JULLevel.INFO -> Level.INFO
                JULLevel.WARNING -> Level.WARN
                JULLevel.SEVERE -> Level.ERROR
                JULLevel.OFF -> null
                else -> Level.DEBUG
            },
            exception, message
        )

    inline fun log(level: Level?, exception: Throwable? = null, message: () -> String) {
        if (level != null && logger.isEnabled(level)) {
            if (exception == null) logger.log(level, marker, message())
            else logger.log(level, marker, message(), exception)
        }
    }
}

object DelegatedLogger {
    operator fun provideDelegate(
        thisRef: Any,
        property: KProperty<*>
    ): ReadOnlyProperty<Any, ExtendedLogger> {
        val logger = LogManager.getLogger(thisRef::class.simpleName)
        val extended = ExtendedLogger(logger)
        return ReadOnlyProperty { _, _ -> extended }
    }
}