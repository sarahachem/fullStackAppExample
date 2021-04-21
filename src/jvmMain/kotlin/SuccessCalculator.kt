import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

class SuccessCalculator : Graph.Callback {

    data class RoutePath(
        val routes: MutableList<Route>,
        val travelTime: Int,
        var numberOfRefuels: Int = 0,
        var extraDays: Int = 0
    )

    lateinit var routes: List<Route>

    private lateinit var distinctLocations: List<String>
    private lateinit var falcon: Falcon

    private val finalPathsList = mutableListOf<RoutePath>()

    suspend fun computeProbabilityOfSuccess(empireData: EmpireData, falcon: Falcon, routes: List<Route>): Float {
        this.routes = routes
        this.falcon = falcon
        val allLocations = routes.map { it.origin }.toMutableList()
        allLocations.addAll(routes.map { it.destination }.toMutableList())
        distinctLocations = allLocations.distinct()
        val routesGraph = Graph(distinctLocations.size, this)
        routes.forEach {
            routesGraph.addEdge(distinctLocations.indexOf(it.origin), distinctLocations.indexOf(it.destination))
        }
        withContext(Dispatchers.IO) {
            routesGraph.getAllPossiblePaths(
                distinctLocations.indexOf(falcon.departure),
                distinctLocations.indexOf(falcon.arrival)
            )
        }
        val potentialPaths = finalPathsList.filter { it.travelTime <= empireData.countdown }
        if (potentialPaths.isEmpty()) return 0F
        val pathsAfterRefuel = potentialPaths.filter {
            if (it.travelTime > falcon.autonomy) {
                it.numberOfRefuels = it.travelTime / falcon.autonomy
            }
            it.travelTime + it.numberOfRefuels <= empireData.countdown
        }.toMutableList()
        if (pathsAfterRefuel.isEmpty()) return 0F
        val pathsAfterRefuelWithExtraDays = pathsAfterRefuel.onEach {
            it.extraDays = empireData.countdown - (it.travelTime + it.travelTime / falcon.autonomy)
        }.toMutableList()
        pathsAfterRefuel.addAll(generateAllPossiblePathsWithExtraDays(pathsAfterRefuelWithExtraDays))
        val probability = pathsAfterRefuel.map { probabilityOfFailure(it, empireData) }.minOrNull() ?: 0
        return 100.minus(probability.toFloat() * 100)
    }

    private fun generateAllPossiblePathsWithExtraDays(pathsAfterRefuelWithExtraDays: List<RoutePath>): List<RoutePath> {
        val newPaths = mutableListOf<RoutePath>()
        pathsAfterRefuelWithExtraDays.forEach { initialPath ->
            initialPath.routes.forEachIndexed { index, route ->
                val routes = mutableListOf<Route>()
                initialPath.routes.forEach {
                    routes.add(it.copy(waitDay = 0))
                }
                val path = initialPath.copy(routes = routes)
                path.routes[index].apply { waitDay = initialPath.extraDays }
                newPaths.add(RoutePath(path.routes, initialPath.travelTime))
            }
        }
        return newPaths
    }

    private fun probabilityOfFailure(path: RoutePath, empireData: EmpireData): Float {
        var bustedCounter = 0
        var time = 0
        var autonomy = falcon.autonomy
        path.routes.forEach { route ->
            if (hasEnoughFuel(autonomy, route.travel_time)) autonomy -= route.travel_time else {
                autonomy = falcon.autonomy
                time++
                empireData.bounty_hunters.firstOrNull { it.day == time && it.planet == route.origin }?.let {
                    bustedCounter++
                }
            }
            time += route.travel_time + route.waitDay
            empireData.bounty_hunters.firstOrNull { it.day == time && it.planet == route.destination }?.let {
                bustedCounter++
            }
        }
        return computationResult(bustedCounter)
    }

    private fun hasEnoughFuel(autonomy: Int, nextTravelTime: Int): Boolean {
        return autonomy >= nextTravelTime
    }

    private fun computationResult(bustedCounter: Int): Float {
        var probability = 0F
        for (i in 1..bustedCounter) {
            probability += 9.0.pow((i.toDouble().minus(1.0))).div(10.0.pow(i.toDouble())).toFloat()
        }
        return probability
    }

    override fun newPathAcquired(path: Path) {
        val locations = path.points.map { distinctLocations[it] }
        val finalRoutes = locations.mapIndexed { index, location ->
            if (index < locations.size - 1) {
                routes.find {
                    it.origin == location && it.destination == locations[index + 1]
                }
            } else null
        }.filterNotNull().toMutableList()
        val pathWithDuration = RoutePath(finalRoutes, finalRoutes.sumBy { it.travel_time })
        finalPathsList.add(pathWithDuration)
    }
}