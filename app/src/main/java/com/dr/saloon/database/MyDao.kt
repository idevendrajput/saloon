package com.dr.saloon.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppointmentsDao {
    @Insert
    suspend fun insertAppointment(entity: AppointmentEntity)

    @Update
    suspend fun updateAppointment(entity: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(entity: AppointmentEntity)

    @Query("DELETE FROM appointments")
    fun delete()

    @Query("Select * From appointments ORDER BY dateTime DESC")
    fun getAllAppointments(): LiveData<List<AppointmentEntity>>
}

@Dao
interface LoginSliderDao {
    @Insert
    suspend fun insertSlider(entity: LoginSliderEntity)

    @Update
    suspend fun updateSlider(entity: LoginSliderEntity)

    @Delete
    suspend fun deleteSlider(entity: LoginSliderEntity)

    @Query("DELETE FROM login_slider")
    fun delete()

    @Query("Select * From login_slider")
    fun getAllSlider():List<LoginSliderEntity>
}

@Dao
interface WishListDao {
    @Insert
    suspend fun insertWishlist(entity: WishlistEntity)

    @Update
    suspend fun updateWishlist(entity: WishlistEntity)

    @Delete
    suspend fun deleteWishlist(entity: WishlistEntity)

    @Query("DELETE FROM wish_list")
    fun delete()

    @Query("DELETE FROM wish_list WHERE documentId ==:id")
    fun delete(id: String)

    @Query("Select * From wish_list ORDER BY timestamp DESC")
    fun getAllWishList():List<WishlistEntity>

    @Query("Select * From wish_list WHERE documentId ==:id")
    fun getWishListById(id : String):List<WishlistEntity>

}

@Dao
interface NearBySalonDao {
    @Insert
    suspend fun insertWishlist(entity: NearBySalonEntity)

    @Update
    suspend fun updateWishlist(entity: NearBySalonEntity)

    @Delete
    suspend fun deleteWishlist(entity: NearBySalonEntity)

    @Query("DELETE FROM nearby_salon")
    fun delete()

    @Query("DELETE FROM nearby_salon WHERE documentId ==:id")
    fun delete(id: String)

    @Query("Select * From nearby_salon ORDER BY distance ASC")
    fun getAllNearBy():List<NearBySalonEntity>

    @Query("Select * From nearby_salon WHERE documentId ==:id")
    fun getAllNearByById(id : String):List<NearBySalonEntity>

}

@Dao
interface SelectedServicesDao {
    @Insert
    suspend fun insertService(entity: SelectedServicesEntity)

    @Update
    suspend fun updateService(entity: SelectedServicesEntity)

    @Delete
    suspend fun deleteService(entity: SelectedServicesEntity)

    @Query("DELETE FROM selectedServices")
    fun delete()

    @Query("DELETE FROM selectedServices WHERE serviceId ==:id")
    fun delete(id: String)

    @Query("Select * From selectedServices WHERE serviceId ==:id")
    fun getServiceById(id : String): List<SelectedServicesEntity>

   @Query("Select * From selectedServices")
    fun getAllSelectedService(): List<SelectedServicesEntity>

    @Query("Select * From selectedServices")
    fun getAllSelectedServiceLive(): LiveData<List<SelectedServicesEntity>>

}

