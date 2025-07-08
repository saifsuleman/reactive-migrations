package net.saifs.reactivemigrations.storage.impl

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.saifs.reactivemigrations.extension.logger
import net.saifs.reactivemigrations.storage.Storage
import net.saifs.reactivemigrations.storage.StorageFile

class S3Storage(
    override val id: String,
    private val url: Url,
    private val bucket: String,
    private val accessKey: String,
    private val secretKey: String,
) : Storage {
    private val logger by logger()

    private val client = S3Client {
        region = "us-east-1"
        clientName = id
        endpointUrl = url
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }
        forcePathStyle = true
    }

    override suspend fun all(): Flow<StorageFile> {
        val request = ListObjectsV2Request { bucket = this@S3Storage.bucket }
        val response = client.listObjectsV2Paginated(request)
        return flow {
            response.collect { page ->
                page.contents?.forEach { obj ->
                    val path = obj.key
                    val checksum = obj.eTag
                    val size = obj.size

                    if (path == null) {
                        logger.warn { "Received null path in ListObjectsV2 API response" }
                        return@forEach
                    }

                    if (checksum == null) {
                        logger.warn { "Received null checksum in ListObjectsV2 API response" }
                        return@forEach
                    }

                    if (size == null) {
                        logger.warn { "Received null size in ListObjectsV2 API response" }
                        return@forEach
                    }

                    emit(StorageFile(path, size, checksum))
                }
            }
        }
    }

    override suspend fun upload(file: StorageFile, data: ByteArray) {
        logger.info { "Uploading ${file.path} (${data.size} bytes)" }
        val request = PutObjectRequest {
            bucket = this@S3Storage.bucket
            key = file.path
            contentLength = data.size.toLong()
            body = ByteStream.fromBytes(data)
        }
        client.putObject(request)
        logger.info { "Uploaded ${file.path} (${data.size} bytes)" }
    }

    override suspend fun download(file: StorageFile): ByteArray {
        logger.info { "Downloading ${file.path}" }

        val request = GetObjectRequest {
            bucket = this@S3Storage.bucket
            key = file.path
        }

        return client.getObject(request) {
            val content = it.body?.toByteArray()
            if (content == null) {
                logger.warn { "Failed to download file: ${file.path} (content is null)" }
                throw IllegalStateException("File not found: ${file.path}")
            }
            logger.info { "Downloaded ${file.path} (${content.size} bytes)" }
            content
        }
    }

    override suspend fun delete(file: StorageFile) {
        val request = DeleteObjectRequest {
            bucket = this@S3Storage.bucket
            key = file.path
        }
        client.deleteObject(request)
        logger.info { "Deleted ${file.path}" }
    }
}