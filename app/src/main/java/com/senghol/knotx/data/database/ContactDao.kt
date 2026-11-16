package com.senghol.knotx.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.senghol.knotx.utils.data.ContactPhotoProjection
import com.senghol.knotx.utils.enums.ContactButtonState
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: Contact)

    @Update
    suspend fun update(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)

    @Query("DELETE FROM contact")
    suspend fun deleteAllContacts()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<Contact>)

    @Query("SELECT * FROM contact ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contact ORDER BY name ASC")
    fun getAllSortedContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contact")
    fun getAllLiveContacts(): Flow<List<Contact>>

    @Query("SELECT COUNT(*) FROM contact WHERE number = :number AND name = :name")
    suspend fun isContactExists(name: String, number: String): Int

    @Query("SELECT * FROM contact WHERE number = :phoneNumber LIMIT 1")
    suspend fun getUserContactByNumber(phoneNumber: String): Contact?

    @Query("UPDATE contact SET imageUrl = :newUrl WHERE number = :number")
    suspend fun updateImageUrlByNumber(newUrl: String, number: String)

    @Query("SELECT imageUrl FROM contact WHERE number = :number")
    suspend fun getImageUrlByNumber(number: String): String?

    @Query("SELECT name FROM contact WHERE number = :number")
    suspend fun getUserNameByNumber(number: String): String?

    @Query("SELECT * FROM contact ORDER BY isInstalled DESC, name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>> // ✅ Live updates

    @Query("UPDATE contact SET buttonState = :buttonState WHERE number = :number")
    suspend fun updateContactStateByNumber(number: String, buttonState: ContactButtonState)

    @Query("UPDATE contact SET imageTime = :newImageTime WHERE number = :userNumber")
    fun updateImageTimeByNumber(userNumber: String, newImageTime: Long)

    @Query(
        """
    SELECT number, imageUrl
    FROM contact
    WHERE imageUrl IS NOT NULL
    """
    )
    suspend fun getAllContactPhotos(): List<ContactPhotoProjection>


    @Query("""
    UPDATE contact
    SET isInstalled = 1
    WHERE number = :userNumber
""")
    suspend fun setInstalled(userNumber: String)
}
