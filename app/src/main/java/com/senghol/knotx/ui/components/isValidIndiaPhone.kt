package com.senghol.knotx.ui.components

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.i18n.phonenumbers.NumberParseException

private val phoneUtil = PhoneNumberUtil.getInstance()

fun isValidIndianPhone(raw: String): Boolean {
    

    val cleaned = raw.trim().replace(Regex("[\\s\\-()]"), "")
    

    val normalized = when {
        cleaned.startsWith("+91") -> {
            
            cleaned
        }
        cleaned.startsWith("91") -> {
            
            "+$cleaned"
        }
        cleaned.startsWith("0") && cleaned.length == 11 -> {
            
            "+91${cleaned.drop(1)}"
        }
        cleaned.length == 10 && cleaned.all { it.isDigit() } -> {
            
            "+91$cleaned"
        }
        else -> {
            
            return false
        }
    }
    

    return try {
        val number: PhoneNumber = phoneUtil.parse(normalized, "IN")
        

        val isValidRegion = phoneUtil.isValidNumberForRegion(number, "IN")
        

        val numberType = phoneUtil.getNumberType(number)
        

//        val isMobile = numberType == PhoneNumberUtil.PhoneNumberType.MOBILE
//        

        isValidRegion
    } catch (e: NumberParseException) {
        
        false
    }
}

fun formatToE164Indian(raw: String): String? {
    val cleaned = raw.trim().replace(Regex("[\\s\\-()]"), "")

    val candidate = when {
        cleaned.startsWith("+91") -> cleaned
        cleaned.startsWith("0") -> "+91${cleaned.drop(1)}"   // remove leading 0 if present
        cleaned.startsWith("91") -> "+$cleaned"
        cleaned.length == 10 && cleaned.all { it.isDigit() } -> "+91$cleaned"
        else -> cleaned
    }

    return try {
        val number = phoneUtil.parse(candidate, "IN")
        if (phoneUtil.isValidNumberForRegion(number, "IN")) {
            phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } else null
    } catch (e: NumberParseException) {
        null
    }
}