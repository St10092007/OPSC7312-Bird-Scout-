package com.example.birding.Observations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.R
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

class ObservationAdapter(private val observations: List<BirdObservation>) :
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
            val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormatter.format(observation.dateTime)
            dateTimeTextView.text = "Date and Time: $formattedDate"
            val location = observation.location
            locationTextView.text = "Location (Lat, Long): ${location.latitude}, ${location.longitude}"
            notesTextView.text = "Notes: ${observation.notes}"
        }
    }
}
