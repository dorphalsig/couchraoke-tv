package com.couchraoke.tv.ui.theme

enum class MotionBudget {
    V0,
    V1,
    V2,
}

data class MotionPolicy(
    val songListSettled: MotionBudget = MotionBudget.V2,
    val songListActiveNavigation: MotionBudget = MotionBudget.V1,
    val joinOverlay: MotionBudget = MotionBudget.V1,
    val selectPlayers: MotionBudget = MotionBudget.V1,
    val loading: MotionBudget = MotionBudget.V0,
    val countdown: MotionBudget = MotionBudget.V2,
    val singing: MotionBudget = MotionBudget.V0,
    val interruptionOverlay: MotionBudget = MotionBudget.V1,
    val allowsRuntimeBlur: Boolean = false,
    val allowsBloom: Boolean = false,
    val allowsGlow: Boolean = false,
    val allowsFullScreenShaderEffects: Boolean = false,
    val allowsGameplayParticles: Boolean = false,
    val allowsBackgroundAnimationDuringSinging: Boolean = false,
)

val CouchraokeMotionPolicy = MotionPolicy()
