package com.dr.saloon.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dr.saloon.utils.AppConstaints.APPLIED_REFER_BALANCE
import com.dr.saloon.utils.AppConstaints.APPLY_REFERRAL_INCOME
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS
import com.dr.saloon.utils.AppConstaints.DATA_TYPE
import com.dr.saloon.utils.AppConstaints.DATE_TIME
import com.dr.saloon.utils.AppConstaints.DISTANCE
import com.dr.saloon.utils.AppConstaints.DOCUMENT_ID
import com.dr.saloon.utils.AppConstaints.FIELD_RATING
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.RESPONSE
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_COST
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.SLIDER_TAG
import com.dr.saloon.utils.AppConstaints.THE_TITLE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT_PAID

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
    (name = DATE_TIME) val dateTime : Long,
    @ColumnInfo
    (name =  SERVICES) val services : String,
    @ColumnInfo
    (name = SALON_ID) val salonId : String,
    @ColumnInfo
    (name = TOTAL_AMOUNT_PAID) val amountPaid : Float,
    @ColumnInfo
    (name = TOTAL_AMOUNT) val totalAmount: Float,
    @ColumnInfo
    (name = APPLIED_REFER_BALANCE) val appliedReferBonus: Float,
    @ColumnInfo
    (name = APPOINTMENT_STATUS) val appointmentStatus: String,
    @ColumnInfo
        (name = RESPONSE) val response : String,
    @ColumnInfo
    (name = DOCUMENT_ID) val documentId : String
)

@Entity(tableName = "login_slider")
data class LoginSliderEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
    (name = THE_TITLE) val title : String,
    @ColumnInfo
    (name = SLIDER_TAG) val tag : String,
    @ColumnInfo
    (name = IMAGE_URL) val imageUrl : String
)

@Entity(tableName = "wish_list")
data class WishlistEntity(
    @ColumnInfo
    (name = TIMESTAMP_FIELD) val timestamp : Long,
    @ColumnInfo
    (name = SALON_ID) val salonId : String,
    @ColumnInfo
    (name = SERVICE_ID) val serviceId : String,
    @ColumnInfo
    (name = DATA_TYPE) val dataType : String,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
    (name = DOCUMENT_ID) val id : String,
)

@Entity(tableName = "nearby_salon")
data class NearBySalonEntity(
    @ColumnInfo
    (name = SALON_NAME) val salonName : String,
    @ColumnInfo
    (name = IMAGE_URL) val imageUrl: String,
    @ColumnInfo
    (name = FIELD_RATING) val rating: Float,
    @ColumnInfo
    (name = SERVICES) val services : String,
    @ColumnInfo
    (name = DISTANCE) val distance : Double,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
    (name = DOCUMENT_ID) val id : String,
)

@Entity(tableName = "selectedServices")
data class SelectedServicesEntity (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo
        (name = SERVICE_ID) val serviceId : String,
    @ColumnInfo
        (name = SERVICE_COST) val serviceCost: Float,
    @ColumnInfo
        (name = SERVICE_NAME) val serviceName: String,
    @ColumnInfo
        (name = IMAGE_URL) val imageUrl: String,
    @ColumnInfo
        (name = APPLY_REFERRAL_INCOME) val isApplyReferIncome: Boolean,
    )
// val salonName : String, val imageUrl : String, val rating : Float, val services : String ,  val distance : Double , val id : String





