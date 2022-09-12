package no.nav.helse.dsl

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher

class DeferredLog: TestWatcher {

    private val info: MutableList<String> = mutableListOf()
    fun info(melding: String) {
        info.add(melding)
    }

    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        (context.testInstance.get() as AbstractDslTest).dumpLog()
    }

    fun dumpLog() {
        info.forEach { println(it) }
    }
}