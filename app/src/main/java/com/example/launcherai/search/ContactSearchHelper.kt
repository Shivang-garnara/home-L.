package com.example.launcherai.search

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

data class ContactResult(
    val id: Long,
    val name: String,
    val number: String
)

object ContactSearchHelper {

    fun searchContacts(context: Context, query: String, limit: Int = 5): List<ContactResult> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<ContactResult>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"

        try {
            val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val id = if (idCol >= 0) it.getLong(idCol) else 0L
                    val name = if (nameCol >= 0) it.getString(nameCol) ?: "" else ""
                    val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                    if (name.isNotBlank()) {
                        results.add(ContactResult(id, name, number))
                    }
                }
            }
        } catch (e: Exception) {
            // Handled safely if permission not granted
        }
        return results
    }

    fun dialContact(context: Context, number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {}
    }
}
