import kotlinx.serialization.Serializable

@Serializable
data class EmpireData(var bounty_hunters: MutableList<BountyHunter>, val countdown: Int) {
    companion object {
        const val path = "/EmpireData"
    }
}

@Serializable
data class BountyHunter(val day: Int, val planet: String)