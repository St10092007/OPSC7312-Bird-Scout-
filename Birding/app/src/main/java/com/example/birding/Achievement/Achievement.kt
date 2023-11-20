package com.example.birding.Achievement

import com.example.birding.R

data class Achievement
    (val title: String,
     val description: String,
     val iconResourceId: Int,
     val completionCount: Int,
     var isComplete: Boolean = false )


