package com.example.birding.Observations

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.R
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*


class ObservationAdapter(private val observations: List<BirdObservation> , private val geocoder: Geocoder) :
    RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObservationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.observationcardview, parent, false)
        return ObservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObservationViewHolder, position: Int) {
        val observation = observations[position]
        holder.bind(observation)
    }

    override fun getItemCount(): Int {
        return observations.size
    }

    inner class ObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val speciesTextView: TextView = itemView.findViewById(R.id.speciesTextView)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.dateTimeTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val notesTextView: TextView = itemView.findViewById(R.id.notesTextView)

        fun bind(observation: BirdObservation) {
            speciesTextView.text = "Bird Species: ${observation.species}"
            val dateFormatter = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US)


            // Parse the dateTime string into a Date
            val date = dateFormatter.parse(observation.dateTime)

            // Format the Date object back into a string
            val formattedDate = dateFormatter.format(date)
            dateTimeTextView.text = "Date and Time: $formattedDate"


            val location = observation.location
            val address = getAddress(location, geocoder)
            locationTextView.text = "Location: $address"

            notesTextView.text = "Notes: ${observation.notes}"
        }

        private fun getAddress(location: LatLng, geocoder: Geocoder): String {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            return if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0) ?: "Address not available"
            } else {
                "Address not available"
            }
        }
    }
}
