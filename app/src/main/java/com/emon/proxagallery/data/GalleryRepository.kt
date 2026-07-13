package com.emon.proxagallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

class GalleryRepository(
    context: Context
) {
    private val contentResolver = context.applicationContext.contentResolver
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
}
