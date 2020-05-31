package com.boswelja.contactringtonegenerator.contacts

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.database.getStringOrNull


object ContactManager {

    private val CONTACTS_PROJECTION = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME
    )

    private val CONTACT_NICKNAME_PROJECTION = arrayOf(
        ContactsContract.CommonDataKinds.Nickname._ID,
        ContactsContract.CommonDataKinds.Nickname.MIMETYPE,
        ContactsContract.CommonDataKinds.Nickname.NAME
    )

    fun getContacts(context: Context): List<Contact> {
        val contacts = ArrayList<Contact>()
        val cursor = context.contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
            CONTACTS_PROJECTION, null, null, null)
        if (cursor != null) {
            val idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val lookupKeyColumn = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val displayNameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val lookupKey = cursor.getString(lookupKeyColumn)
                val contact =
                    Contact(
                            id,
                            lookupKey,
                            getContactPhotoUri(context, id),
                            cursor.getString(displayNameColumn),
                            getContactNickname(
                                    context,
                                    lookupKey)
                    )
                if (!contacts.any { it.id == contact.id }) {
                    contacts.add(contact)
                }
            }
            cursor.close()
        }
        return contacts.sortedBy { it.contactNickname ?: it.contactName }
    }

    private fun getContactNickname(context: Context, lookupKey: String): String? {
        val cursor = context.contentResolver.query(ContactsContract.Data.CONTENT_URI,
            CONTACT_NICKNAME_PROJECTION, "${ContactsContract.Data.LOOKUP_KEY} = ? AND ${ContactsContract.CommonDataKinds.Nickname.MIMETYPE} = ?", arrayOf(lookupKey, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE), null)

        var nickname: String? = null
        if (cursor != null) {
            val nicknameColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
            if (cursor.moveToFirst()) {
                nickname = cursor.getStringOrNull(nicknameColumn)
            }
            cursor.close()
        }

        return nickname
    }

    fun getContactPhotoUri(context: Context, contactId: Long) : Uri? {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        val cursor = context.contentResolver.query(photoUri, arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null)
        if (cursor == null || !cursor.moveToFirst()) {
            return null
        }
        cursor.close()
        return photoUri
    }

    fun setContactRingtone(context: Context, contact: Contact, ringtoneUri: Uri) {
        val contactUri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
        val values = ContentValues()
        values.put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString())
        context.contentResolver.update(contactUri, values, null, null)
    }
}
