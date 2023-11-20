package com.example.birding.Achievement

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.birding.Observations.BirdObservation
import com.example.birding.Observations.ObservationManager
import com.example.birding.Observations.ObservationManager.Companion.getObservationCount
import com.example.birding.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AchievementsAdapter(private val achievements: List<Achievement>, private val observationCount: Int) :
    RecyclerView.Adapter<AchievementsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val achievement = achievementsList[position]

        val userId = getCurrentUserUid()
        // Get the total observation count
        val totalObservationCount = getObservationCount()

        Log.d("AchievementActivity", "totalObservationCount start: $totalObservationCount")

        // Check if the achievement should be marked as complete
        if (totalObservationCount >= achievement.completionCount && !achievement.isComplete) {
            // Mark achievement as complete
            achievement.isComplete = true

            // Update achievement status in Firebase Realtime Database
            val database = FirebaseDatabase.getInstance()
            val achievementsRef = database.getReference("achievements")

            achievementsRef.child(position.toString()).apply {
                child("isComplete").setValue(true)
            }

            // Optionally, you can show a toast or perform other actions to indicate achievement completion
            Toast.makeText(holder.itemView.context, "Achievement unlocked: ${achievement.title}", Toast.LENGTH_SHORT).show()
        }

        // Toggle trophy image based on completion status
        val trophyImage = holder.itemView.findViewById<ImageView>(R.id.ivTrophy)
        trophyImage.setImageResource(if (achievement.isComplete) R.drawable.trophy else R.drawable.trophy_2)

        // Bind achievement data to the ViewHolder
        holder.itemView.findViewById<TextView>(R.id.tvTitle).text = achievement.title
        holder.itemView.findViewById<TextView>(R.id.tvDescription).text = achievement.description
        holder.itemView.findViewById<ImageView>(R.id.ivIcon).setImageResource(achievement.iconResourceId)

        // Set completion status
        val completionStatus = holder.itemView.findViewById<TextView>(R.id.tvCompletionStatus)
        completionStatus.text = if (achievement.isComplete) "Completed" else "Not Complete"

        Log.d("AchievementAdapter", "Setting progressCount start: $totalObservationCount")

        // Set progress count
        val progressCount = holder.itemView.findViewById<TextView>(R.id.tvProgressCount)
        progressCount.text =
            if (!achievement.isComplete) {
                "${totalObservationCount}/${achievement.completionCount}"
            } else {
                "${achievement.completionCount}/${achievement.completionCount}"
            }

        Log.d("AchievementAdapter", "Setting progressCount set: ${progressCount.text}")

        // Set text color based on completion status
        if (achievement.isComplete) {
            // Set color to green for completed achievements
            progressCount.setTextColor(holder.itemView.context.getColor(R.color.primary_green))
        } else {
            // Set color to red for incomplete achievements
            progressCount.setTextColor(holder.itemView.context.getColor(R.color.primary_red))
        }
    }

    fun markAchievementAsComplete(position: Int, totalObservationCount: Int) {
        if (position in achievementsList.indices) {
            val achievement = achievementsList[position]
            if (totalObservationCount >= achievement.completionCount && !achievement.isComplete) {
                // Mark achievement as complete
                achievement.isComplete = true

                // Notify the adapter that the item has changed
                notifyItemChanged(position)
            }
        }
    }


private fun getCurrentUserUid(): String {
    val user = FirebaseAuth.getInstance().currentUser
    return user?.uid ?: ""
}

    override fun getItemCount(): Int {
        return achievements.size
    }

    companion object {
        // Example achievements list with dynamic completion counts
        val achievementsList = listOf(
            Achievement("Novice Birder", "Completed 1st Observation", R.drawable.achievment9, 1),
            Achievement("Scholar", "Completed 5 Observations", R.drawable.achievment7, 5),
            Achievement("Bird Watcher", "Observed 10 different birds", R.drawable.achievment8, 10),
            Achievement("Expert Ornithologist", "Identified 20 different birds", R.drawable.achievment4, 20),
            Achievement("Feather Collector", "Identified 30 bird species", R.drawable.achievment5, 30),
            Achievement("Avian Explorer", "50 Observation", R.drawable.achievment6, 50),
            Achievement("Photograph chaser", "Identified 70 bird species", R.drawable.achievment2, 70),
            Achievement("Chirp Detective", "Identified 100 species", R.drawable.achievment3, 100),
            Achievement("Conservationist", "Identified 150 species", R.drawable.achievment1, 150),
            Achievement("Falconer", "Identified 200 bird species", R.drawable.achievment10, 200),
        )

    }
}


