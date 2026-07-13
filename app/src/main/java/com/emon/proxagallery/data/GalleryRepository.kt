package com.emon.proxagallery.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class GalleryRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val externalContentUri = MediaStore.Files.getContentUri("external")

    private val mediaProjection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.DURATION
    )

    fun getPhotos(offset: Int = 0, limit: Int = Int.MAX_VALUE): List<MediaItem> {
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?))"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200" // 300 KB in bytes
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return queryMediaItems(mediaProjection, selection, selectionArgs, sortOrder, offset, limit)
    }

    fun getPhotosForAlbum(bucketId: Long, offset: Int = 0, limit: Int = Int.MAX_VALUE): List<MediaItem> {
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?)) AND " +
            "${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200",
            bucketId.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return queryMediaItems(mediaProjection, selection, selectionArgs, sortOrder, offset, limit)
    }

    fun getPhotosForSearch(query: String, offset: Int = 0, limit: Int = Int.MAX_VALUE): List<MediaItem> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return getPhotos(offset, limit)
        }
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?)) AND (" +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} LIKE ?)"
        
        val likeArg = "%$cleanQuery%"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200",
            likeArg,
            likeArg,
            likeArg
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return queryMediaItems(mediaProjection, selection, selectionArgs, sortOrder, offset, limit)
    }

    private fun queryMediaItems(
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        offset: Int,
        limit: Int
    ): List<MediaItem> {
        return buildList {
            val queryArgs = android.os.Bundle().apply {
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            }

            // Local cache pools for highly repeated string values to reuse references and avoid allocations
            val mimeTypeCache = HashMap<String, String>()
            val bucketDisplayNameCache = HashMap<String, String>()

            contentResolver.query(
                externalContentUri,
                projection,
                queryArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DISPLAY_NAME
                )
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.MIME_TYPE
                )
                val mediaTypeColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DATE_TAKEN
                )
                val dateAddedColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DATE_ADDED
                )
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                )
                val bucketIdColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.BUCKET_ID
                )
                val bucketDisplayNameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
                )
                val durationColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DURATION
                )

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val mediaType = cursor.getInt(mediaTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val bucketId = cursor.getLong(bucketIdColumn)
                    
                    val rawMimeType = cursor.getString(mimeTypeColumn).orEmpty()
                    val mimeType = mimeTypeCache.getOrPut(rawMimeType) { rawMimeType }
                    
                    val rawBucketDisplayName = cursor.getString(bucketDisplayNameColumn)
                    val bucketDisplayName = rawBucketDisplayName?.let {
                        bucketDisplayNameCache.getOrPut(it) { it }
                    }
                    
                    val duration = cursor.getLong(durationColumn)

                    add(
                        MediaItem(
                            id = id,
                            uri = mediaItemUri(id, mediaType),
                            displayName = cursor.getString(displayNameColumn).orEmpty(),
                            mimeType = mimeType,
                            width = width.takeIf { it > 0 },
                            height = height.takeIf { it > 0 },
                            fileSize = size.takeIf { it > 0L },
                            dateTakenMs = dateTaken.takeIf { it > 0L } ?: dateAdded.takeIf { it > 0L },
                            bucketId = bucketId.takeIf { it > 0L },
                            bucketDisplayName = bucketDisplayName,
                            durationMs = duration.takeIf { it > 0L },
                            dateModifiedSec = dateModified.takeIf { it > 0L }
                        )
                    )
                }
            }
        }
    }

    fun getAlbums(): List<Album> {
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?))"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200"
        )
        val sortOrder = "${MediaStore.Files.FileColumns.BUCKET_ID} ASC"

        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
        )

        val queryArgs = android.os.Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_GROUP_COLUMNS,
                    arrayOf(MediaStore.Files.FileColumns.BUCKET_ID)
                )
            }
        }

        val uniqueBuckets = mutableListOf<Pair<Long, String>>()

        contentResolver.query(
            externalContentUri,
            projection,
            queryArgs,
            null
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            var lastBucketId = -1L
            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdColumn)
                if (bucketId != lastBucketId) {
                    lastBucketId = bucketId
                    val bucketName = cursor.getString(bucketNameColumn).orEmpty()
                    uniqueBuckets.add(Pair(bucketId, bucketName))
                }
            }
        }

        return uniqueBuckets.mapNotNull { (bucketId, displayName) ->
            getAlbumDetails(bucketId, selection, selectionArgs)?.let { (count, coverUri) ->
                Album(
                    id = bucketId,
                    displayName = displayName,
                    coverPhotoUri = coverUri,
                    itemCount = count
                )
            }
        }.sortedBy { it.displayName.lowercase() }
    }

    private fun getAlbumDetails(
        bucketId: Long,
        baseSelection: String,
        baseSelectionArgs: Array<String>
    ): Pair<Int, Uri>? {
        val selection = "($baseSelection) AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        val selectionArgs = baseSelectionArgs + arrayOf(bucketId.toString())
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val queryArgs = android.os.Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
        }

        contentResolver.query(
            externalContentUri,
            arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE),
            queryArgs,
            null
        )?.use { cursor ->
            val count = cursor.count
            if (count > 0 && cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(mediaTypeColumn)
                return Pair(count, mediaItemUri(id, mediaType))
            }
        }
        return null
    }

    private fun mediaItemUri(id: Long, mediaType: Int): Uri {
        return when (mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
            else ->
                ContentUris.withAppendedId(externalContentUri, id)
        }
    }

    fun getPhotoById(id: Long): MediaItem? {
        val selection = "${MediaStore.Files.FileColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return queryMediaItems(mediaProjection, selection, selectionArgs, "", 0, 1).firstOrNull()
    }

    fun getPhotosByIds(ids: List<Long>): List<MediaItem> {
        if (ids.isEmpty()) return emptyList()
        val selection = "${MediaStore.Files.FileColumns._ID} IN (${ids.joinToString()})"
        val selectionArgs = emptyArray<String>()
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        return queryMediaItems(mediaProjection, selection, selectionArgs, sortOrder, 0, ids.size)
    }

    fun getAllPhotoIds(): List<Long> {
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?))"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200"
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        return queryMediaIds(selection, selectionArgs, sortOrder)
    }

    fun getPhotoIdsForAlbum(bucketId: Long): List<Long> {
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?)) AND " +
            "${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200",
            bucketId.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        return queryMediaIds(selection, selectionArgs, sortOrder)
    }

    fun getPhotoIdsForSearch(query: String): List<Long> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return getAllPhotoIds()
        }
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} >= ?)) AND (" +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} LIKE ?)"
        
        val likeArg = "%$cleanQuery%"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "307200",
            likeArg,
            likeArg,
            likeArg
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        return queryMediaIds(selection, selectionArgs, sortOrder)
    }

    private fun queryMediaIds(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String
    ): List<Long> {
        return buildList {
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val queryArgs = android.os.Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            }
            contentResolver.query(
                externalContentUri,
                projection,
                queryArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                while (cursor.moveToNext()) {
                    add(cursor.getLong(idColumn))
                }
            }
        }
    }

    /**
     * Attempt to delete the media item identified by [uri].
     * On Android 11+ uses the batch createDeleteRequest path.
     * On Android 10 catches [RecoverableSecurityException] and returns [DeleteResult.RequiresPermission].
     */
    fun deletePhoto(uri: Uri): DeleteResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                DeleteResult.RequiresPermission(
                    intentSender = pendingIntent.intentSender,
                    retryUri = null
                )
            } else {
                val deletedRows = contentResolver.delete(uri, null, null)
                if (deletedRows > 0) DeleteResult.Success
                else DeleteResult.Error("File not found or already deleted.")
            }
        } catch (e: RecoverableSecurityException) {
            DeleteResult.RequiresPermission(
                intentSender = e.userAction.actionIntent.intentSender,
                retryUri = uri
            )
        } catch (e: Exception) {
            DeleteResult.Error(e.localizedMessage ?: "Unknown error during deletion.")
        }
    }

    /**
     * Retry the actual content-resolver delete after the user granted permission
     * via a [RecoverableSecurityException] dialog (Android 10 only).
     */
    fun retryDelete(uri: Uri): DeleteResult {
        return try {
            val deletedRows = contentResolver.delete(uri, null, null)
            if (deletedRows > 0) DeleteResult.Success
            else DeleteResult.Error("Deletion failed after user confirmation.")
        } catch (e: Exception) {
            DeleteResult.Error(e.localizedMessage ?: "Unknown error during retry deletion.")
        }
    }

    fun getMediaDetails(id: Long): MediaDetails? {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DURATION,
            "orientation"
        )

        val selection = "${MediaStore.Files.FileColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        contentResolver.query(
            externalContentUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val mediaType = cursor.getInt(
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                )
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val idCol = cursor.getLong(
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                )

                val orientationCol = cursor.getColumnIndex("orientation")
                val orientationDeg = if (orientationCol >= 0) {
                    cursor.getInt(orientationCol).takeIf { it > 0 }
                } else null

                val relPath = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
                )

                var details = MediaDetails(
                    id = idCol,
                    uri = if (isVideo) {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, idCol
                        )
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, idCol
                        )
                    },
                    displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    ).orEmpty(),
                    mimeType = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    ).orEmpty(),
                    width = cursor.getInt(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                    ).takeIf { it > 0 },
                    height = cursor.getInt(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                    ).takeIf { it > 0 },
                    fileSize = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    ).takeIf { it > 0 },
                    isVideo = isVideo,
                    dateTakenMs = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                    ).takeIf { it > 0 },
                    dateAddedMs = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    ) * 1000L,
                    dateModifiedSec = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    ).takeIf { it > 0 },
                    bucketId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                    ).takeIf { it > 0 },
                    bucketDisplayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                    ),
                    relativePath = relPath,
                    durationMs = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                    ).takeIf { it > 0 },
                    orientationDegrees = orientationDeg,
                    storageType = computeStorageType(relPath)
                )

                details = if (isVideo) {
                    enrichWithVideoMetadata(details)
                } else {
                    enrichWithExif(details)
                }

                return details
            }
        }
        return null
    }

    private fun computeStorageType(relativePath: String?): String? {
        if (relativePath == null) return null
        return when {
            relativePath.contains("emulated", ignoreCase = true) ||
                relativePath.startsWith("DCIM") ||
                relativePath.startsWith("Download") ||
                relativePath.startsWith("Pictures") ||
                relativePath.startsWith("Movies") ||
                relativePath.startsWith("Music") -> "Internal storage"
            relativePath.startsWith("..") ||
                relativePath.contains("sdcard", ignoreCase = true) ||
                relativePath.contains("external", ignoreCase = true) -> "SD card"
            else -> null
        }
    }

    private fun enrichWithVideoMetadata(details: MediaDetails): MediaDetails {
        val videoProjection = arrayOf(
            MediaStore.Video.VideoColumns.BITRATE
        )
        val selection = "${MediaStore.Video.VideoColumns._ID} = ?"
        val selectionArgs = arrayOf(details.id.toString())

        var bitrate: Long? = null

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.BITRATE)
                bitrate = cursor.getLong(bitrateCol).takeIf { it > 0 }
            }
        }

        var rotation: Int? = null

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(appContext, details.uri)

            rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )?.toIntOrNull()?.takeIf { it != 0 && it != -1 }

            retriever.release()
        } catch (_: Exception) {
            // Leave rotation as null
        }

        return details.copy(
            codec = null,
            frameRate = null,
            bitrate = bitrate,
            rotation = rotation
        )
    }

    private fun enrichWithExif(details: MediaDetails): MediaDetails {
        return try {
            contentResolver.openInputStream(details.uri)?.use { stream ->
                val exif = ExifInterface(stream)

                val exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, -1
                ).let { value ->
                    when (value) {
                        1 -> 0
                        3 -> 180
                        6 -> 90
                        8 -> 270
                        else -> details.orientationDegrees
                    }
                }

                details.copy(
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE)
                        ?.takeIf { it.isNotBlank() },
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                        ?.takeIf { it.isNotBlank() },
                    lensModel = exif.getAttribute("LensModel")
                        ?.takeIf { it.isNotBlank() },
                    aperture = formatAperture(exif),
                    shutterSpeed = formatShutterSpeed(exif),
                    iso = exif.getAttribute(ExifInterface.TAG_ISO)
                        ?.takeIf { it.isNotBlank() },
                    focalLength = formatFocalLength(exif),
                    flash = formatFlash(exif),
                    whiteBalance = formatWhiteBalance(exif),
                    exposureMode = exif.getAttribute("ExposureMode")
                        ?.let { mode ->
                            when (mode.toIntOrNull()) {
                                0 -> "Auto"
                                1 -> "Manual"
                                2 -> "Auto bracket"
                                else -> null
                            }
                        },
                    exposureCompensation = exif.getAttribute(
                        ExifInterface.TAG_EXPOSURE_BIAS_VALUE
                    )?.takeIf { it.isNotBlank() && it != "0/1" && it != "0" },
                    meteringMode = readMeteringMode(exif),
                    digitalZoom = exif.getAttribute(
                        ExifInterface.TAG_DIGITAL_ZOOM_RATIO
                    )?.takeIf { it.isNotBlank() && it != "0/1" && it != "0" },
                    colorSpace = readColorSpace(exif),
                    software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
                        ?.takeIf { it.isNotBlank() },
                    artist = exif.getAttribute(ExifInterface.TAG_ARTIST)
                        ?.takeIf { it.isNotBlank() },
                    copyright = exif.getAttribute(ExifInterface.TAG_COPYRIGHT)
                        ?.takeIf { it.isNotBlank() },
                    bitsPerPixel = exif.getAttributeInt(
                        ExifInterface.TAG_BITS_PER_SAMPLE, -1
                    ).takeIf { it > 0 },
                    orientationDegrees = exifOrientation,
                    latitude = readGpsCoordinate(
                        exif,
                        ExifInterface.TAG_GPS_LATITUDE,
                        ExifInterface.TAG_GPS_LATITUDE_REF
                    ),
                    longitude = readGpsCoordinate(
                        exif,
                        ExifInterface.TAG_GPS_LONGITUDE,
                        ExifInterface.TAG_GPS_LONGITUDE_REF
                    )
                )
            } ?: details
        } catch (_: Exception) {
            details
        }
    }

    private fun formatAperture(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: return null
        val parts = raw.split("/")
        if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull()
            val den = parts[1].toDoubleOrNull()
            if (num != null && den != null && den > 0) {
                return "f/${"%.1f".format(num / den)}"
            }
        }
        return "f/$raw"
    }

    private fun formatShutterSpeed(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: return null
        val parts = raw.split("/")
        if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull()
            val den = parts[1].toDoubleOrNull()
            if (num != null && den != null && den > 0) {
                val seconds = num / den
                return if (seconds >= 1) {
                    "${"%.0f".format(seconds)}s"
                } else {
                    "1/${(den / num).toInt()}"
                }
            }
        }
        return "${raw}s"
    }

    private fun formatFocalLength(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: return null
        val parts = raw.split("/")
        if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull()
            val den = parts[1].toDoubleOrNull()
            if (num != null && den != null && den > 0) {
                return "${"%.1f".format(num / den)} mm"
            }
        }
        return "$raw mm"
    }

    private fun formatFlash(exif: ExifInterface): String? {
        val raw = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
        if (raw < 0) return null
        return when (raw and 0x07) {
            0x00 -> "No Flash"
            0x01 -> "Fired"
            0x05 -> "Fired (strobe return detected)"
            0x07 -> "Fired (no strobe return)"
            else -> "Unknown ($raw)"
        }
    }

    private fun formatWhiteBalance(exif: ExifInterface): String? {
        val raw = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
        if (raw < 0) return null
        return when (raw) {
            0 -> "Auto"
            1 -> "Manual"
            else -> null
        }
    }

    private fun readGpsCoordinate(
        exif: ExifInterface,
        coordTag: String,
        refTag: String
    ): Double? {
        val rawArray = exif.getAttribute(coordTag) ?: return null
        val ref = exif.getAttribute(refTag) ?: return null

        val parts = rawArray.split(",").map { it.trim() }
        if (parts.size != 3) return null

        val degrees = parseRational(parts[0]) ?: return null
        val minutes = parseRational(parts[1]) ?: return null
        val seconds = parseRational(parts[2]) ?: return null

        var result = degrees + minutes / 60.0 + seconds / 3600.0
        if (ref == "S" || ref == "W") result = -result
        return result
    }

    private fun parseRational(value: String): Double? {
        val parts = value.split("/")
        if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull()
            val den = parts[1].toDoubleOrNull()
            if (num != null && den != null && den > 0) return num / den
        }
        return value.toDoubleOrNull()
    }

    private fun readMeteringMode(exif: ExifInterface): String? {
        val raw = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1)
        if (raw < 0) return null
        return when (raw) {
            1 -> "Average"
            2 -> "Center-weighted average"
            3 -> "Spot"
            4 -> "Multi-spot"
            5 -> "Pattern"
            6 -> "Partial"
            255 -> "Other"
            else -> null
        }
    }

    private fun readColorSpace(exif: ExifInterface): String? {
        val raw = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
        if (raw < 0) return null
        return when (raw) {
            1 -> "sRGB"
            2 -> "Adobe RGB"
            65535 -> "Uncalibrated"
            else -> null
        }
    }
}
