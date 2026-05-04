package com.couchraoke.tv.ui.theme

data class Surfaces(
    val allowsRuntimeBlur: Boolean = false,
    val allowsGlow: Boolean = false,
    val allowsShadow: Boolean = false,
    val allowsElevationFocusTreatment: Boolean = false,
)

val CouchraokeSurfacePolicy = Surfaces()
