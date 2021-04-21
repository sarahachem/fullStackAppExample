import react.child
import react.dom.render
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun main() {
    render(document.getElementById("root")) {
        child(App)
    }
}

@JsName("sendData")
fun sendData(data: String, fileName: String) {
    MainScope().launch {
        document.getElementById("result")?.innerHTML = "Thank you fellow rebels for the information. May the force be with you!!"
        addResult(data, fileName)
    }
}