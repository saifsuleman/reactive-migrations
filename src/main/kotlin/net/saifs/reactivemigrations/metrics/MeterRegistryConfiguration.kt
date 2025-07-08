package net.saifs.reactivemigrations.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class MeterRegistryConfiguration {
    @Bean
    open fun influxConfig(): InfluxConfig = object : InfluxConfig {
        override fun get(key: String): String? = null // fallback for unspecified properties

        override fun uri(): String = "http://saifs.xyz:8086"
        override fun bucket(): String = "metrics"
        override fun org(): String = "local"
        override fun token(): String = "secrettoken"
        override fun step(): Duration = Duration.ofSeconds(10)
    }

    @Bean
    open fun meterRegistry(config: InfluxConfig): MeterRegistry {
        return InfluxMeterRegistry
            .builder(config)
            .build()
    }
}