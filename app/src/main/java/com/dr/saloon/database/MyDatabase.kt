package com.dr.saloon.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppointmentEntity::class, LoginSliderEntity::class, WishlistEntity::class, NearBySalonEntity::class, SelectedServicesEntity::class], version = 12)
abstract class MyDatabase : RoomDatabase() {

	abstract fun appointmentsDao() : AppointmentsDao
	abstract fun loginSliderDao(): LoginSliderDao
	abstract fun wishlistDao(): WishListDao
	abstract fun nearBySalonDao(): NearBySalonDao
	abstract fun selectedServices(): SelectedServicesDao
	
	companion object {
		private var INSTANCE: MyDatabase? = null
		fun getDatabase(context: Context): MyDatabase {
			if (INSTANCE == null) {
				synchronized(this) {
					INSTANCE =
						Room.databaseBuilder(context,MyDatabase::class.java, "user_database_dr_salon")
							.fallbackToDestructiveMigration()
							.allowMainThreadQueries()
							.build()
				}
			}
			return INSTANCE!!
		}
	}

}




