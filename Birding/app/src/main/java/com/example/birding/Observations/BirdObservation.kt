package com.example.birding.Observations

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import java.util.*

data class BirdObservation(
    val species: String,
    val dateTime: Date,
    val location: LatLng,
    val notes: String
) : Parcelable {
    constructor() : this("", Date(), LatLng(0.0, 0.0), "")

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        Date(parcel.readLong()), // Convert the long back to Date
        parcel.readParcelable(LatLng::class.java.classLoader) ?: LatLng(0.0, 0.0),
        parcel.readString() ?: ""
    )
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(species)
        parcel.writeLong(dateTime.time) // Convert Date to a long
        parcel.writeParcelable(location, flags)
        parcel.writeString(notes)
    }
    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<BirdObservation> {
        override fun createFromParcel(parcel: Parcel): BirdObservation {
            return BirdObservation(parcel)
        }
        override fun newArray(size: Int): Array<BirdObservation?> {
            return arrayOfNulls(size)
        }
    }
}
