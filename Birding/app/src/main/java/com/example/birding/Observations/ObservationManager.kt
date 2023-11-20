package com.example.birding.Observations

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ObservationManager {

    companion object {
        private var observationCount: Int = 0

        fun incrementObservationCount() {
            observationCount++
        }
        fun getObservationCount(): Int {
            return observationCount
        }
        // Assume the observations are stored under the "Observations" node
        private const val OBSERVATIONS_NODE = "Observations"

        // This method fetches the observation count from the Firebase Realtime Database
        fun getObservationCount(userUid: String): Int {
            val databaseReference = FirebaseDatabase.getInstance().reference
                .child(OBSERVATIONS_NODE)
                .child(userUid)

            // Use addListenerForSingleValueEvent to fetch data only once
            var observationCount = 0
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Count the number of children (observations) under the user's node
                    observationCount = dataSnapshot.childrenCount.toInt()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors or show a message
                    Log.e("FirebaseError", databaseError.message)
                }
            })

            return observationCount
        }

        fun setObservationCount(count: Int) {
            observationCount = count
        }


        // New function to broadcast when an observation is added
        fun broadcastObservationAdded(context: Context) {
            val intent = Intent("com.example.birding.Observations.ObservationAdded")
            context.sendBroadcast(intent)
        }
    }
}
