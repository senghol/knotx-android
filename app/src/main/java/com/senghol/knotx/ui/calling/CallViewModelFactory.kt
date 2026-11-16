package com.senghol.knotx.ui.calling

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.ui.viewmodel.CallViewModel

class CallViewModelFactory(
    private val application: Application,
    private val knotx: Knotx,
    private val roomId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            return CallViewModel(application, knotx, roomId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
