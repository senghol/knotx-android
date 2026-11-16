package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.ContactDao

class ContactViewModelFactory(private val contactDao: ContactDao, private val knotx: Knotx) :
    ViewModelProvider
    .Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactViewModel(contactDao, knotx) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
