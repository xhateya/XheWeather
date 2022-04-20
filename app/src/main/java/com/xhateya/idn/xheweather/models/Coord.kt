package com.xhateya.idn.xheweather.models

import com.google.gson.annotations.SerializedName

data class Coord (
    @field:SerializedName("lont")
    val lon: Double,

    @field:SerializedName("lan")

    val lan: Double,

    )