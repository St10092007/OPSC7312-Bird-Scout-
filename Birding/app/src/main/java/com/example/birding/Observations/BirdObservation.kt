package com.example.birding.Observations

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import java.util.*

data class BirdObservation(
    val species: String,
    val dateTime: String,
    val location: LatLng,
    val notes: String
) : Parcelable {
    constructor() : this("", "", LatLng(0.0, 0.0), "")

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(LatLng::class.java.classLoader) ?: LatLng(0.0, 0.0),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(species)
        parcel.writeString(dateTime)
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
