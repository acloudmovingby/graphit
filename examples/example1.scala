import com.whatever.{HttpClient, HttpRequest}

/**
 * This code is nonsense. It's just an example.
 */

class MessageManager {
    def sendMessage(m: String): Unit = {
        val maker = new MessageMaker
        val politeMessage = maker.makePolite()
        val request = maker.makeHttpRequest(politeMessage)
        MessageSender.sendRequest(request)
    }
}

object MessageSender {
    def sendRequest(r: HttpRequest): Unit =  {
        val client = getClient().setConnectTimeout(300L)
        client.send(m)
    }

    def getClient(): HttpClient = new HttpClient()
}

class MessageMaker {
    def makePolite(m: String): String = m + ", please."

    def makeHttpRequest(m: String): HttpRequest = new HttpRequest(m)
}
