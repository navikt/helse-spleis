package no.nav.helse.testhelpers

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

class LogCollector : AppenderBase<ILoggingEvent>(), Iterable<ILoggingEvent> {
    private val messages = mutableListOf<ILoggingEvent>()

    override fun append(eventObject: ILoggingEvent) {
        messages.add(eventObject)
    }

    fun clear() {
        messages.removeIf { it.threadName == Thread.currentThread().name }
    }

    override fun iterator(): Iterator<ILoggingEvent> = messages
        .filter { it.threadName == Thread.currentThread().name }
        .iterator()
}
