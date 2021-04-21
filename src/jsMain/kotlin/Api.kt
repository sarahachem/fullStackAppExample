import io.ktor.client.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.browser.window

val endpoint = window.location.origin

val jsonClient = HttpClient {
    install(JsonFeature) { serializer = KotlinxSerializer() }
    install(WebSockets)
}

suspend fun addResult(empireData: String, fileName: String) {
    jsonClient.webSocket(method = HttpMethod.Get, port = 9095, path = "/data") {
        val dataWithName = empireData + "fileName:$fileName"
        send(dataWithName)
        val frame = incoming.receive()
        when (frame) {
            is Frame.Text -> println(frame.readText())
            is Frame.Binary -> println(frame.readBytes())
        }
    }
}