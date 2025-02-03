package com.test

import android.content.ContentResolver
import android.provider.ContactsContract
import com.facebook.react.bridge.*
import android.database.Cursor
import android.util.Log

class ContactsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "ContactsModule"

    @ReactMethod
    fun getContacts(promise: Promise) {
        try {
            val contacts = WritableNativeArray()
            val contentResolver = reactApplicationContext.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contact = WritableNativeMap()
                    val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    
                    contact.putString("name", name)
                    contact.putString("phoneNumber", number?.replace(Regex("[^0-9]"), ""))
                    contacts.pushMap(contact)
                }
            }
            
            promise.resolve(contacts)
        } catch (e: Exception) {
            promise.reject("CONTACTS_ERROR", e.message)
        }
    }
} 