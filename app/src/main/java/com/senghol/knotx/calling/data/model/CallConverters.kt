package com.senghol.knotx.calling.data.model

import androidx.room.TypeConverter
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState

class CallConverters {

    @TypeConverter
    fun fromCallType(value: CallType?): String? = value?.name

    @TypeConverter
    fun toCallType(value: String?): CallType? =
        value?.let { CallType.valueOf(it) }

    @TypeConverter
    fun fromCallDirection(value: CallDirection?): String? = value?.name

    @TypeConverter
    fun toCallDirection(value: String?): CallDirection? =
        value?.let { CallDirection.valueOf(it) }

    @TypeConverter
    fun fromCallState(value: CallState?): String? = value?.name

    @TypeConverter
    fun toCallState(value: String?): CallState? =
        value?.let { CallState.valueOf(it) }

    @TypeConverter
    fun fromCallEndReason(value: CallEndReason?): String? = value?.name

    @TypeConverter
    fun toCallEndReason(value: String?): CallEndReason? =
        value?.let { CallEndReason.valueOf(it) }

    @TypeConverter
    fun fromParticipantConnectionState(value: ParticipantConnectionState?): String? =
        value?.name

    @TypeConverter
    fun toParticipantConnectionState(value: String?): ParticipantConnectionState? =
        value?.let { ParticipantConnectionState.valueOf(it) }
}