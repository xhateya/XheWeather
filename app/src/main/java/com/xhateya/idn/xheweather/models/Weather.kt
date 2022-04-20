package com.xhateya.idn.xheweather.models

import android.util.EventLogTags
import com.google.gson.annotations.SerializedName

data class Weather (
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("main")
    val main: String,

    @field:SerializedName("description")
    val description: String,

    @field:SerializedName("icon")
    val icon: String



)
