package net.saifs.reactivemigrations.storage

import aws.smithy.kotlin.runtime.net.url.Url
import net.saifs.reactivemigrations.storage.impl.S3Storage
import java.util.UUID

object StorageFactory {
    fun resolve(path: String): Storage {
        if (!path.startsWith("s3://")) {
            error("Unsupported storage path: $path. Only S3 storage is supported.")
        }

        // s3://<http://whatever:23213>/<accessKey>/<secretKey>/<bucket>

        val id = UUID.randomUUID().toString()
        val withoutPrefix = path.removePrefix("s3://")

        // Find the first '/' after the URL
        val firstSlashIndex = withoutPrefix.indexOf('/', startIndex = "http://".length)
        require(firstSlashIndex != -1) { "Missing accessKey/secretKey/bucket" }

        val url = withoutPrefix.substring(0, firstSlashIndex)

        val rest = withoutPrefix.substring(firstSlashIndex + 1)
        val parts = rest.split('/')

        require(parts.size >= 3) { "Expected at least accessKey/secretKey/bucket" }

        val accessKey = parts[0]
        val secretKey = parts[1]
        val bucket = parts[2]

        return S3Storage(
            id = id,
            url = Url.parse(url),
            bucket = bucket,
            accessKey = accessKey,
            secretKey = secretKey,
        )
    }
}