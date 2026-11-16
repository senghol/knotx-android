package com.senghol.knotx.utils.data

import com.google.gson.annotations.SerializedName

// One-time prekeys (X3DH)
    data class PreKey(
        @SerializedName("preKeyId")       val preKeyId: Int,
        @SerializedName("preKeyPublic")   val preKeyPublic: String
    )

    // Kyber prekey (PQXDH)
    data class KyberPreKey(
        @SerializedName("kyberPreKeyId")       val kyberPreKeyId: Int,
        @SerializedName("kyberPreKeyPublic")  val kyberPreKeyPublic: String,
        @SerializedName("kyberPreKeySignature") val kyberPreKeySignature: String
    )

// Full prekey bundle (sent to server & received from ASK_KEY)
data class PreKeyBundleDto(
    @SerializedName("phoneNumber")          val phoneNumber: String,
    @SerializedName("registrationId")       val registrationId: Int,
    @SerializedName("identityKey")          val identityKey: String,
    @SerializedName("signedPreKeyId")       val signedPreKeyId: Int,
    @SerializedName("signedPreKeyPublic")   val signedPreKeyPublic: String,
    @SerializedName("signedPreKeySignature")val signedPreKeySignature: String,
)