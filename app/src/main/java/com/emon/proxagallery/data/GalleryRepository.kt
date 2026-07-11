package com.emon.proxagallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class GalleryRepository(
    context: Context
) {
    private val contentResolver = context.applicationContext.contentResolver

    fun getPhotos(): List<Photo> {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.IS_PENDING} = ?"
        val selectionArgs = arrayOf("0")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        return buildList {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(Photo(id = id, uri = imageUri(id)))
                }
            }
        }
    }

    private fun imageUri(id: Long): Uri =
        ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )
}