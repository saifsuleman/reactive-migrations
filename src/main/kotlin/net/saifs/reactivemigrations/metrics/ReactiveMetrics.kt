package net.saifs.reactivemigrations.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ReactiveMetrics private constructor(
    override val registry: MeterRegistry,
    private val tags: List<String>,
) : Metrics {
    @Autowired
    constructor(registry: MeterRegistry) : this(registry, emptyList())

    override fun tag(vararg tags: String) = ReactiveMetrics(
        registry,
        this.tags + tags.toList()
    )

    override fun counter(counter: String, vararg tags: String): Counter =
        registry.counter(counter, *(this.tags + tags).toTypedArray())

    override fun increment(counter: String, vararg tags: String, amount: Double) {
        counter(counter, *tags).increment(amount)
    }

    override fun timer(name: String, vararg tags: String): Timer =
        registry.timer(name, *(this.tags + tags).toTypedArray())

    override fun <T> gauge(name: String, value: T, measure: (T) -> Number) {
        registry.gauge(
            name,
            tags.chunked(2).map { (key, value) -> Tag.of(key, value) },
            value
        ) { measure(it).toDouble() }
    }
}