package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CallDirection
import com.example.data.model.CallRecord
import com.example.data.model.CallRecordStatus
import com.example.data.model.Contact

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey
    val id: String,
    val remotePhoneNumber: String,
    val remoteName: String?,
    val direction: String, // "INCOMING", "OUTGOING", "MISSED"
    val durationSeconds: Long,
    val status: String,
    val timestamp: Long,
    val twilioCallSid: String?,
    val notes: String?,
    val isRecorded: Boolean = false,
    val recordingDurationSeconds: Long = 0,
    val recordingUrl: String? = null
) {
    fun toCallRecord(): CallRecord {
        return CallRecord(
            id = id,
            remotePhoneNumber = remotePhoneNumber,
            remoteName = remoteName,
            direction = try {
                CallDirection.valueOf(direction)
            } catch (e: Exception) {
                CallDirection.OUTGOING
            },
            durationSeconds = durationSeconds,
            status = try {
                CallRecordStatus.valueOf(status)
            } catch (e: Exception) {
                CallRecordStatus.COMPLETED
            },
            timestamp = timestamp,
            twilioCallSid = twilioCallSid,
            notes = notes,
            isRecorded = isRecorded,
            recordingDurationSeconds = recordingDurationSeconds,
            recordingUrl = recordingUrl
        )
    }

    companion object {
        fun fromCallRecord(record: CallRecord): CallRecordEntity {
            return CallRecordEntity(
                id = record.id,
                remotePhoneNumber = record.remotePhoneNumber,
                remoteName = record.remoteName,
                direction = record.direction.name,
                durationSeconds = record.durationSeconds,
                status = record.status.name,
                timestamp = record.timestamp,
                twilioCallSid = record.twilioCallSid,
                notes = record.notes,
                isRecorded = record.isRecorded,
                recordingDurationSeconds = record.recordingDurationSeconds,
                recordingUrl = record.recordingUrl
            )
        }
    }
}

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String?,
    val organization: String?,
    val avatarUrl: String?,
    val isDeviceContact: Boolean,
    val isDnd: Boolean = false,
    val isBlacklisted: Boolean = false,
    val notes: String? = null,
    val createdAt: Long
) {
    fun toContact(): Contact {
        return Contact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            organization = organization,
            avatarUrl = avatarUrl,
            isDeviceContact = isDeviceContact,
            isDnd = isDnd,
            isBlacklisted = isBlacklisted,
            notes = notes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromContact(contact: Contact): ContactEntity {
            return ContactEntity(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                email = contact.email,
                organization = contact.organization,
                avatarUrl = contact.avatarUrl,
                isDeviceContact = contact.isDeviceContact,
                isDnd = contact.isDnd,
                isBlacklisted = contact.isBlacklisted,
                notes = contact.notes,
                createdAt = contact.createdAt
            )
        }
    }
}
