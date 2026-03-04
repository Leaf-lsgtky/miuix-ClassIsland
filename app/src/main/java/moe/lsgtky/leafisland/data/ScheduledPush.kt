package moe.lsgtky.leafisland.data

data class ScheduledPush(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val dismissMinutes: Int = 30,
)
