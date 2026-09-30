package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM app_users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM app_users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @androidx.room.Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM app_users WHERE role = :role ORDER BY id ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("DELETE FROM app_users")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM app_users WHERE isGoogleUser = 0")
    suspend fun deleteNonGoogleUsers()
}
