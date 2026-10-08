package com.goreecloud.launcher.core.launcher

data class LauncherHomeDragTarget(
    val key: String,
    val centerX: Float,
    val centerY: Float,
)

object LauncherHomeDragPolicy {
    fun nearestTargetKey(
        sourceKey: String,
        dropX: Float,
        dropY: Float,
        targets: List<LauncherHomeDragTarget>,
    ): String? {
        if (!dropX.isFinite() || !dropY.isFinite()) return null

        val nearest = targets
            .asSequence()
            .filter {
                it.key.isNotBlank() &&
                    it.centerX.isFinite() &&
                    it.centerY.isFinite()
            }
            .minByOrNull { target ->
                val dx = target.centerX - dropX
                val dy = target.centerY - dropY
                dx * dx + dy * dy
            }
            ?: return null

        return nearest.key.takeUnless { it == sourceKey }
    }
}
