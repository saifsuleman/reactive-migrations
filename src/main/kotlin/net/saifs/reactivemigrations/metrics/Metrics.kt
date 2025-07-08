package net.saifs.reactivemigrations.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit
import kotlin.time.toJavaDuration

interface Metrics {
    val registry: MeterRegistry

    fun tag(vararg tags: String): Metrics
    fun counter(counter: String, vararg tags: String): Counter
    fun increment(counter: String, vararg tags: String, amount: Double = 1.0)
    fun timer(name: String, vararg tags: String): Timer
    fun <T> gauge(name: String, value: T, measure: (T) -> Number)
    fun gauge(name: String, measure: () -> Number) = gauge(name, Unit) { measure() }

    suspend fun <T> timer(name: String, vararg tags: String, block: suspend () -> T): T {
        val timer = timer(name, *tags)
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            val duration = System.nanoTime() - start
            timer.record(duration, TimeUnit.NANOSECONDS)
        }
    }

    companion object {
        fun Timer.record(time: kotlin.time.Duration) {
            record(time.toJavaDuration())
        }
    }
}