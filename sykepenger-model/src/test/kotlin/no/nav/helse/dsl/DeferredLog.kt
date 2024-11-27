package no.nav.helse.dsl

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher

class DeferredLog : TestWatcher {
    private val meldinger: MutableList<String> = mutableListOf()
    fun log(melding: String) {
        meldinger.add(melding)
    }

    fun dumpLog() {
        meldinger.forEach { println(it) }
    }
}

class DeferredLogging : TestWatcher {
    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        (context.testInstance.get() as AbstractDslTest).dumpLog()
    }
}
