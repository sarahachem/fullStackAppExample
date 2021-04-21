import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileReader
import java.util.*

fun main() {
    val falcon: Falcon = Gson().fromJson(FileReader("millenium-falcon.json"), Falcon::class.java)
    val db = falcon.routes_db
    val routes = Database.getRoutes(db)
    embeddedServer(Netty, 9095) {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            anyHost()
        }
        install(Compression) {
            gzip()
        }
        install(WebSockets)

        routing {
            val connections = Collections.synchronizedList<Connection?>(LinkedList())
            webSocket("/data") {
                // connections.clear()
                val thisConnection = Connection(this)
                connections += thisConnection
                val frame = incoming.receive()
                //    for (frame in incoming) {
                (frame as? Frame.Text)?.let {
                    val receivedText = frame.readText()
                    if (receivedText.contains("fileName")) {
                        val content = receivedText.split("fileName:")[0]
                        val name = receivedText.split("fileName:")[1]
                        saveJsonFile(name, content)
                        val result = SuccessCalculator().computeProbabilityOfSuccess(
                            Gson().fromJson(
                                content,
                                EmpireData::class.java
                            ), falcon, routes
                        )
                        connections.forEach {
                            it.session.send(getCustomMessage(result).toString())
                            it.session.close()
                        }
                        connections.clear()
                    }
                }
            }

            get("/") {
                this::class.java.classLoader.getResource("index.html")?.let {
                    call.respondText(
                        it.readText(),
                        ContentType.Text.Html
                    )
                } ?: call.respondText("oups!")
            }
            post("/probability") {
                val post = call.receiveParameters()
                val milleniumParam = post["millenium"]
                val empireParam = post["empire"]
                val falcon = Gson().fromJson(FileReader(milleniumParam), Falcon::class.java)
                val empire = Gson().fromJson(FileReader(empireParam), EmpireData::class.java)
                val result = SuccessCalculator().computeProbabilityOfSuccess(empire, falcon, routes)
                call.respondText(result.toString())
            }
            static("/") {
                resources("")
            }
        }
    }.start(wait = true)
}

fun getCustomMessage(result: Float): String {
    return when {
        result == 100F -> "Your weapons, you will not need them. The force is with you, your probability of success is $result%"
        result >= 90 -> "Do. Or do not. There is no try. And you can win with a probability of $result%"
        result >= 80 -> "Destroy the Sith we must, with a chance of success $result%"
        result >= 50 -> "Put a shield on my saber I must!, your chance of success is $result%"
        result >= 20 -> "Oh. Great warrior. Wars not make one great. your chances of success are only $result%"
        result >= 0 -> "Control, control, you must learn control!, you will fail and your chances of success are $result%"
        else -> "I dont know what happened, my internal system failed you my warriors"
    }
}

fun saveJsonFile(name: String, content: String) {
    File(name).writeText(content)
}


