package com.senghol.knotx.data.database


import androidx.room.*

@Dao
interface SignalDao {

    // ——— YOUR EXISTING METHODS (keep all of them) ———
    @Query("SELECT * FROM identity_keys WHERE id = 1")
    suspend fun getIdentity(): IdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveIdentity(entity: IdentityEntity)

    @Query("SELECT * FROM prekeys WHERE preKeyId = :id")
    suspend fun getPreKey(id: Int): PreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreKey(entity: PreKeyEntity)

    @Delete suspend fun deletePreKey(entity: PreKeyEntity)

    @Query("SELECT * FROM signed_prekeys WHERE signedPreKeyId = :id")
    suspend fun getSignedPreKey(id: Int): SignedPreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSignedPreKey(entity: SignedPreKeyEntity)

    @Query("SELECT * FROM sessions WHERE address = :address")
    suspend fun getSession(address: String): SessionEntity?

    @Query("""
    UPDATE sessions
    SET address = :newAddress
    WHERE address = :oldAddress
""")
    suspend fun updateSessionAddress(
        oldAddress: String,
        newAddress: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(entity: SessionEntity)



    // ==================== MESSAGE SESSION MAP (Table 1) ====================


    @Query("SELECT * FROM trusted_identities WHERE address = :address")
    suspend fun getTrustedIdentity(address: String): TrustedIdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrustedIdentity(entity: TrustedIdentityEntity)

    @Query("SELECT * FROM kyber_prekeys WHERE id = :id")
    suspend fun getKyberPreKey(id: Int): KyberPreKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKyberPreKey(entity: KyberPreKeyEntity)

    @Delete suspend fun deleteKyberPreKey(entity: KyberPreKeyEntity)
    @Query("DELETE FROM kyber_prekeys WHERE id = :id")
    suspend fun deleteKyberPreKey(id: Int)

    @Query("SELECT * FROM kyber_prekeys")
    suspend fun getAllKyberPreKeys(): List<KyberPreKeyEntity>

    // ——— NEW 6 METHODS REQUIRED FOR 0.86.4 ———
    @Query("SELECT signedPreKeyId, record FROM signed_prekeys")
    suspend fun getAllSignedPreKeys(): List<SignedPreKeyEntity>

    @Query("DELETE FROM sessions WHERE address = :address")
    suspend fun deleteSessionByAddress(address: String)
    @Query("DELETE FROM trusted_identities WHERE address = :address")
    suspend fun deleteTrustedIdentity(address: String)

    @Query("DELETE FROM sessions WHERE address LIKE :name || '.%'")
    suspend fun deleteAllSessionsForUser(name: String)

    @Query("DELETE FROM signed_prekeys WHERE signedPreKeyId = :id")
    suspend fun deleteSignedPreKey(id: Int)

    // Key state tracking (you already had these — keep them)
    @Query("SELECT lastPreKeyId FROM key_state WHERE id = 1")
    suspend fun getLastPreKeyId(): Int?

    @Query("UPDATE key_state SET lastPreKeyId = :id WHERE id = 1")
    suspend fun saveLastPreKeyId(id: Int)

    @Query("SELECT lastKyberId FROM key_state WHERE id = 1")
    suspend fun getLastKyberKeyId(): Int?

    @Query("UPDATE key_state SET lastKyberId = :id WHERE id = 1")
    suspend fun saveLastKyberKeyId(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyState(state: KeyStateEntity)

    @Query("SELECT * FROM key_state WHERE id = 1")
    suspend fun getKeyState(): KeyStateEntity?
}