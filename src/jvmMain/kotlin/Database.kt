import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

object Database {
    fun getRoutes(databaseName: String): List<Route> {
        val routes = mutableListOf<Route>()
        val url = "jdbc:sqlite:/Users/admin/Desktop/StarWarsFullStack/$databaseName"
        val conn = DriverManager.getConnection(url);

        val statement: Statement = conn.createStatement()
        val queryString = "select * from routes"
        val rs: ResultSet = statement.executeQuery(queryString)
        while (rs.next()) {
            routes.add(
                Route(
                    origin = rs.getString(1),
                    destination = rs.getString(2),
                    travel_time = Integer.valueOf(rs.getString(3))
                )
            )
        }
        return routes
    }
}