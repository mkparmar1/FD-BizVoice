package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE direction = :direction ORDER BY timestamp DESC")
    fun getCallsByDirection(direction: String): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE remotePhoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByPhoneNumber(phoneNumber: String): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE id = :id LIMIT 1")
    suspend fun getCallById(id: String): CallRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallRecordEntity) = insertCall(call)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCalls(calls: List<CallRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(calls: List<CallRecordEntity>) = insertAllCalls(calls)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteCall(id: String)

    @Query("DELETE FROM call_records")
    suspend fun clearAllCalls()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByPhone(phoneNumber: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity) = insertContact(contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>) = insertAllContacts(contacts)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String) = deleteContact(id)

    @Query("DELETE FROM contacts WHERE isDeviceContact = 0")
    suspend fun clearAppContacts()

    @Query("DELETE FROM contacts WHERE isDeviceContact = 1")
    suspend fun clearDeviceContacts()
}
