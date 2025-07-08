package net.saifs.reactivemigrations.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MeterRegistryConfiguration {
    @Bean
    open fun influxConfig(): InfluxConfig = object : InfluxConfig {
        override fun get(key: String): String? = null // fallback for unspecified properties
        override fun db(): String = "metrics" // default database name
        override fun step(): java.time.Duration = java.time.Duration.ofSeconds(10) // default reporting interval
        override fun uri(): String = "http://saifs.xyz:8086" // default InfluxDB URI
        override fun userName(): String = "admin" // default username
        override fun password(): String = "admin123" // default password
    }

    @Bean
    open fun meterRegistry(config: InfluxConfig): MeterRegistry {
        return InfluxMeterRegistry
            .builder(config)
            .build()
    }
}