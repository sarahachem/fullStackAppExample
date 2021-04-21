class Graph(private val size: Int, private val callBack: Callback) {

    interface Callback {
        fun newPathAcquired(path: Path)
    }

    lateinit var adjList: Array<ArrayList<Int>?>
    init {
        initAdjList()
    }

    private fun initAdjList() {
        adjList = arrayOfNulls(size)
        for (i in 0 until size) {
            adjList[i] = ArrayList()
        }
    }

    fun addEdge(start: Int, end: Int) {
        adjList[start]?.add(end) ?: let {
            adjList[start] = arrayListOf(end)
        }
    }

    fun getAllPossiblePaths(source: Int, destination: Int) {
        val isVisited = BooleanArray(size)
        val pathList = mutableListOf<Int>()
        pathList.add(source)
        printAllPathsOrReturnNew(source, destination, isVisited, pathList)
    }

    private fun printAllPathsOrReturnNew(
        u: Int, d: Int,
        isVisited: BooleanArray,
        localPathList: MutableList<Int>
    ) {
        if (u == d) {
            callBack.newPathAcquired(Path(localPathList))
            return
        }

        isVisited[u] = true
        for (i: Int in adjList[u]!!) {
            if (!isVisited[i]) {
                localPathList.add(i)
                printAllPathsOrReturnNew(i, d, isVisited, localPathList)
                localPathList.remove(i)
            }
        }
        isVisited[u] = false
    }
}

data class Path(var points: List<Int>)