package no.nav.helse.rapids_rivers

abstract class RapidsConnection {

    protected val listeners = mutableListOf<MessageListener>()

    fun register(listener: MessageListener) {
        listeners.add(listener)
    }

    abstract fun start()
    abstract fun stop()

    interface MessageContext {
        fun send(message: String)
        fun send(key: String, message: String)
    }

    interface MessageListener {
        fun onMessage(message: String, context: MessageContext)
    }
}
