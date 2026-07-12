package com.emon.proxagallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class GalleryRepository(
    context: Context
) {
    private val contentResolver = context.applicationContext.contentResolver

    fun getPhotos(offset: Int = 0, limit: Int = Int.MAX_VALUE): List<Photo> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID
        )
        val selection = "${MediaStore.Images.Media.IS_PENDING} = ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        return queryPhotos(projection, selection, selectionArgs, sortOrder, offset, limit)
    }

    fun getPhotosForAlbum(bucketId: Long): List<Photo> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID
        )
        val selection = "${MediaStore.Images.Media.IS_PENDING} = ? AND ${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf("0", bucketId.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        return queryPhotos(projection, selection, selectionArgs, sortOrder, offset = 0, limit = Int.MAX_VALUE)
    }

    private fun queryPhotos(
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        offset: Int,
        limit: Int
    ): List<Photo> {
        return buildList {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.DISPLAY_NAME
                )
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

                var index = 0
                var taken = 0
                while (cursor.moveToNext() && taken < limit) {
                    if (index < offset) {
                        index++
                        continue
                    }
                    val id = cursor.getLong(idColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
                    val bucketId = cursor.getLong(bucketIdColumn)

                    add(
                        Photo(
                            id = id,
                            uri = imageUri(id),
                            displayName = cursor.getString(displayNameColumn).orEmpty(),
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
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID
        )
        val selection = "${MediaStore.Images.Media.IS_PENDING} = ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Images.Media.BUCKET_ID} ASC"

        val albumMap = linkedMapOf<Long, AlbumBuilder>()

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdColumn)
                val builder = albumMap.getOrPut(bucketId) {
                    AlbumBuilder(
                        id = bucketId,
                        displayName = cursor.getString(bucketNameColumn).orEmpty()
                    )
                }
                if (builder.firstPhotoId == -1L) {
                    builder.firstPhotoId = cursor.getLong(idColumn)
                }
                builder.photoCount++
            }
        }

        return albumMap.values.map { builder ->
            Album(
                id = builder.id,
                displayName = builder.displayName,
                coverPhotoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    builder.firstPhotoId
                ),
                photoCount = builder.photoCount
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    private fun imageUri(id: Long): Uri =
        ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )

    private data class AlbumBuilder(
        val id: Long,
        val displayName: String,
        var firstPhotoId: Long = -1L,
        var photoCount: Int = 0
    )
}
