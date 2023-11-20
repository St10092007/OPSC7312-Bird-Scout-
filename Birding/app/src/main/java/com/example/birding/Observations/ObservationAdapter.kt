package com.example.birding.Observation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.ParseException
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.Observations.BirdObservation
import com.example.birding.Observations.ObservationDeleteListener
import com.example.birding.Observations.ObservationsActivity
import com.example.birding.R
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

//Some Portions of this code are modifications based on work created and shared by geeksforgeeks
//Author: geeksforgeeks
//link : https://www.geeksforgeeks.org/android-recyclerview-in-kotlin/
class ObservationAdapter(private val observations: MutableList<BirdObservation>, private val geocoder: Geocoder, private val observationDeleteListener: ObservationDeleteListener) :
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

    fun removeItem(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            observations.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    fun deleteObservation(observationId: String) {
        val positionToDelete = observations.indexOfFirst { it.observationId == observationId }
        if (positionToDelete != -1) {
            observations.removeAt(positionToDelete)
            notifyItemRemoved(positionToDelete)
            notifyItemRangeChanged(positionToDelete, itemCount)
        }
    }

    inner class ObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.image_View)
        private val speciesTextView: TextView = itemView.findViewById(R.id.SpeciesDescriptionTextView)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.dateTimeTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val observationType: TextView = itemView.findViewById(R.id.observation_type)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)

        private val observationIdTextView: TextView = itemView.findViewById(R.id.observationIdTextView)
        private val notesTextView: TextView = itemView.findViewById(R.id.notesTextView)
        private val arrowButton: ImageButton = itemView.findViewById(R.id.arrow_button)
        private val hiddenLayout: LinearLayout = itemView.findViewById(R.id.hiddenLayout)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        init {
            btnDelete.setOnClickListener { onDeleteButtonClick() }
            // Set click listener for the arrow button
            arrowButton.setOnClickListener { onArrowButtonClick() }
        }



        fun bind(observation: BirdObservation) {

            Log.d("ObservationsActivity", "Binding observation: $observation")
            try {
                val imageBitmap: Bitmap? = if (!observation.image.isNullOrEmpty()) {
                    val byteArray = Base64.decode(observation.image, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                } else {
                    null
                }
                imageView.setImageBitmap(imageBitmap)
            } catch (e: IllegalArgumentException) {
                Log.e("ObservationsActivity", "Error decoding image: ${e.message}")
                imageView.setImageDrawable(null)
                // Log or display an error message as needed
            }catch (e: ParseException) {
                Log.e("ObservationsActivity", "Error parsing/formatting date: ${e.message}")
            }

            speciesTextView.text = "${observation.species}"
            val dateFormatter = SimpleDateFormat("dd EEE MMM yyyy HH:mm", Locale.US)

            // Parse the dateTime string into a Date
            val date = dateFormatter.parse(observation.dateTime)

            // Format the Date object back into a string
            val formattedDate = dateFormatter.format(date)
            dateTimeTextView.text = "$formattedDate"

            val location = observation.location
            val address = getAddress(location, geocoder)

            locationTextView.text = "$address"

            notesTextView.text = "${observation.notes}"
            observationType.text="${observation.observationType}"

            observationIdTextView.text="Observation: #${observation.observationId}"

            nameTextView.text = "${observation.fullName}"

            hiddenLayout.visibility = View.GONE
            arrowButton.setImageResource(R.drawable.down_arrow)

            Log.d("ObservationsActivity", "Binding complete")
        }

        private fun onDeleteButtonClick() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val observationId = observations[position].observationId
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(itemView.context, position, observationId)
            }
        }

        private fun showDeleteConfirmationDialog(context: Context, position: Int, observationId: String) {
            (context as ObservationsActivity).showDeleteConfirmationDialog(context, position, observationId)
        }

        private fun getAddress(location: LatLng, geocoder: Geocoder): String {
            return try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    addresses[0].getAddressLine(0) ?: "Address not available"
                } else {
                    "Address not available"
                }
            } catch (e: IOException) {
                e.printStackTrace()
                "Address not available"
            }
        }


        private fun onArrowButtonClick() {
            // Toggle visibility of the hidden layout
            if (hiddenLayout.visibility == View.VISIBLE) {
                hiddenLayout.visibility = View.GONE
                // Change the arrow icon to indicate collapse
                arrowButton.setImageResource(R.drawable.down_arrow)
            } else {
                hiddenLayout.visibility = View.VISIBLE
                // Change the arrow icon to indicate expansion
                arrowButton.setImageResource(R.drawable.up_arrow)
            }
        }


    }
}

