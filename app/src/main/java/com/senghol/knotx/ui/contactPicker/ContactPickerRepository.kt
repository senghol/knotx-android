package com.senghol.knotx.ui.contactPicker

import com.senghol.knotx.utils.enums.ContactButtonState

import com.senghol.knotx.data.database.Contact
import com.senghol.knotx.data.database.GroupEntity
import com.senghol.knotx.data.database.GroupMemberEntity
import com.senghol.knotx.data.database.Upbhogta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map



class ContactPickerRepository(
    private val contacts: Flow<List<Contact>>,
    private val upbhogtas: Flow<List<Upbhogta>>,
    private val groups: Flow<List<GroupEntity>>,
    private val groupMembers: Flow<List<GroupMemberEntity>>
) {
    fun numberContacts(): Flow<List<PickedContact>> =
        upbhogtaPicked { !it.userNumber.startsWith("@") }

    fun usernameContacts(): Flow<List<PickedContact>> =
        upbhogtaPicked { it.userNumber.startsWith("@") }

    fun groups(): Flow<List<PickedContact>> = groups.map { list ->
        list.filter { it.isGroupActive != false }.map {
            PickedContact(
                userNumber = it.groupId,
                userName = it.groupName,
                profileUri = it.groupImageUrl,
                isGroup = true
            )
        }
    }

    fun groupMembers(groupId: String? = null): Flow<List<PickedContact>> =
        groupMembers.map { members ->
            members.asSequence()
                .filter { groupId == null || it.groupId == groupId }
                .distinctBy { it.userNumber }
                .map {
                    PickedContact(
                        userNumber = it.userNumber,
                        userName = it.contactName.ifBlank { it.userNumber },
                        profileUri = it.profileUrl,
                        isGroup = false
                    )
                }
                .toList()
        }

    fun deviceContacts(): Flow<List<PickedContact>> = contacts.map { list ->
        list.filter { it.isInstalled && it.buttonState == ContactButtonState.ADD }
            .distinctBy { it.number }
            .map {
                PickedContact(
                    userNumber = it.number,
                    userName = it.name.ifBlank { it.number },
                    profileUri = it.imageUrl,
                    isGroup = false
                )
            }
    }

    private fun upbhogtaPicked(
        filter: (Upbhogta) -> Boolean
    ): Flow<List<PickedContact>> = combine(upbhogtas, contacts) { ups, cons ->
        val byNumber = cons.associateBy { it.number }
        ups.filter(filter).map { u ->
            val saved = byNumber[u.userNumber]
            PickedContact(
                userNumber = u.userNumber,
                userName = saved?.name?.takeIf { it.isNotBlank() }
                    ?: u.contactName?.takeIf { it.isNotBlank() }
                    ?: u.onlineName?.takeIf { it.isNotBlank() }
                    ?: u.userNumber,
                profileUri = u.photoUrl ?: saved?.imageUrl,
                isGroup = false
            )
        }
    }
}
