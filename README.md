# Reactive Blob Storage Migrator

A high-performance, coroutine-based tool for migrating data between blob storage backends. Designed to handle large-scale transfers with fine-grained control over concurrency, rate limiting, and error handling.

## Features

- Reactive, non-blocking architecture using Kotlin coroutines and Flow
- Concurrent ingestion and diffing of source and target storage
- Configurable transfer parameters (batch size, concurrency, TPS)
- Optional deletion of orphaned files in the target
- Integrated metrics with Micrometer (e.g. InfluxDB, Prometheus)
- Pluggable storage interface with support for custom backends

## Architecture

The system works in three phases:

1. **Ingestion**
    - Metadata from both source and target is ingested concurrently via the `StorageTable`.

2. **Diffing**
    - The `StorageTable` computes the delta between the two storage backends.
    - Diffs are emitted as a flow of `PaginatedDiff` objects.

3. **Transfer**
    - Files that are added or modified are downloaded from the source and uploaded to the target.
    - Files that are removed (and if `allowDelete` is enabled) are deleted from the target.
    - All operations are rate-limited using a token bucket model.

## Configuration

The tool is configured via a `TransferConfig` object:

```kotlin
data class TransferConfig(
    val source: Storage,
    val target: Storage,
    val allowDelete: Boolean = false,
    val syncMetadata: Boolean = false,
    val concurrency: Int = 200,
    val tps: Int = 2000,
    val batchSize: Int = 1000
)
