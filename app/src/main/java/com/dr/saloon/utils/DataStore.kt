package com.dr.saloon.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStore (private val context: Context){


    companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore("pref")
    }

    suspend fun setString(string: String, key: String) {
        context.dataStore.edit {
            it[stringPreferencesKey(key)] = string
        }
    }

    fun getString(key: String) = context.dataStore.data.map {
        it[stringPreferencesKey(key)] ?: ""
    }

    suspend fun setInt(int: Int, key: String) {
        context.dataStore.edit {
            it[intPreferencesKey(key)] = int
        }
    }

    fun getInt(key: String) = context.dataStore.data.map {
        it[intPreferencesKey(key)] ?: 0
    }

    suspend fun setFloat(float: Float, key: String) {
        context.dataStore.edit {
            it[floatPreferencesKey(key)] = float
        }
    }

    fun getFloat(key: String) = context.dataStore.data.map {
        it[floatPreferencesKey(key)] ?: 0f
    }

    suspend fun setDouble(double: Double, key: String) {
        context.dataStore.edit {
            it[doublePreferencesKey(key)] = double
        }
    }

    fun getDouble(key: String) = context.dataStore.data.map {
        it[doublePreferencesKey(key)] ?: .0
    }

     suspend fun setBoolean(boolean: Boolean, key: String) {
        context.dataStore.edit {
            it[booleanPreferencesKey(key)] = boolean
        }
    }

    fun getBoolean(key: String) = context.dataStore.data.map {
        it[booleanPreferencesKey(key)] ?: false
    }

    suspend fun deleteString(key: String) {
        context.dataStore.edit {
            it.remove(stringPreferencesKey(key))
        }
    }

    suspend fun deleteInt(key: String) {
        context.dataStore.edit {
            it.remove(intPreferencesKey(key))
        }
    }

    suspend fun deleteBoolean(key: String) {
        context.dataStore.edit {
            it.remove(booleanPreferencesKey(key))
        }
    }

    suspend fun deleteDouble(key: String) {
        context.dataStore.edit {
            it.remove(doublePreferencesKey(key))
        }
    }

    suspend fun deleteFloat(key: String) {
        context.dataStore.edit {
            it.remove(floatPreferencesKey(key))
        }
    }

    suspend fun logout() {
        context.dataStore.edit {
            it.clear()
        }
    }





}