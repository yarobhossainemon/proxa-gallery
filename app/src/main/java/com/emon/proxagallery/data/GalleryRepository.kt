package com.emon.proxagallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class GalleryRepository(
    context: Context
) {
    private val contentResolver = context.applicationContext.contentResolver
    private val externalContentUri = MediaStore.Files.getContentUri("external")

    fun getPhotos(offset: Int = 0, limit: Int = Int.MAX_VALUE): List<MediaItem> {
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
            MediaStore.Files.FileColumns.BUCKET_ID
        )
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return queryMediaItems(projection, selection, selectionArgs, sortOrder, offset, limit)
    }

    fun getPhotosForAlbum(bucketId: Long): List<MediaItem> {
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
            MediaStore.Files.FileColumns.BUCKET_ID
        )
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND " +
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND " +
            "${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            bucketId.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        return queryMediaItems(projection, selection, selectionArgs, sortOrder, offset = 0, limit = Int.MAX_VALUE)
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
            contentResolver.query(
                externalContentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
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
                val bucketIdColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.BUCKET_ID
                )

                var index = 0
                var taken = 0
                while (cursor.moveToNext() && taken < limit) {
                    if (index < offset) {
                        index++
                        continue
                    }
                    val id = cursor.getLong(idColumn)
                    val mediaType = cursor.getInt(mediaTypeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                    val bucketId = cursor.getLong(bucketIdColumn)

                    add(
                        MediaItem(
                            id = id,
                            uri = mediaItemUri(id, mediaType),
                            displayName = cursor.getString(displayNameColumn).orEmpty(),
                            mimeType = cursor.getString(mimeTypeColumn).orEmpty(),
                            width = width.takeIf { it > 0 },
                            height = height.takeIf { it > 0 },
                            fileSize = size.takeIf { it > 0L },
                            dateTakenMs = dateTaken.takeIf { it > 0L } ?: dateAdded.takeIf { it > 0L },
                            bucketId = bucketId.takeIf { it > 0L }
                        )
                    )
                    taken++
                    index++
                }
            }
        }
    }

    fun getAlbums(): List<Album> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.IS_PENDING} = ? AND (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
        val selectionArgs = arrayOf(
            "0",
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.BUCKET_ID} ASC"

        val albumMap = linkedMapOf<Long, AlbumBuilder>()

        contentResolver.query(
            externalContentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.BUCKET_ID
            )
            val bucketNameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME
            )
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdColumn)
                val builder = albumMap.getOrPut(bucketId) {
                    AlbumBuilder(
                        id = bucketId,
                        displayName = cursor.getString(bucketNameColumn).orEmpty()
                    )
                }
                if (builder.firstMediaId == -1L) {
                    builder.firstMediaId = cursor.getLong(idColumn)
                    builder.firstMediaType = cursor.getInt(mediaTypeColumn)
                }
                builder.itemCount++
            }
        }

        return albumMap.values.map { builder ->
            Album(
                id = builder.id,
                displayName = builder.displayName,
                coverPhotoUri = mediaItemUri(builder.firstMediaId, builder.firstMediaType),
                itemCount = builder.itemCount
            )
        }.sortedBy { it.displayName.lowercase() }
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

    private data class AlbumBuilder(
        val id: Long,
        val displayName: String,
        var firstMediaId: Long = -1L,
        var firstMediaType: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
        var itemCount: Int = 0
    )
}
