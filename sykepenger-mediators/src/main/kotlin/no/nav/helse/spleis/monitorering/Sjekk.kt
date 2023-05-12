package no.nav.helse.spleis.monitorering

import java.time.LocalDateTime
import no.nav.helse.erHelg
import org.slf4j.event.Level

internal interface Sjekk {
    fun sjekk(): Pair<Level, String>?
    fun skalSjekke(nå: LocalDateTime): Boolean
}

internal class RegelmessigAvstemming(private val manglerAvstemming: () -> Int): Sjekk {
    override fun sjekk(): Pair<Level, String>? {
        val mangler = manglerAvstemming()
        if (mangler == 0) return null
        return Level.ERROR to "Det er $mangler personer som ikke er avstemt på over en måned!"
    }

    override fun skalSjekke(nå: LocalDateTime): Boolean {
        if (nå.toLocalDate().erHelg()) return false
        return nå.minute % 15 == 0
    }
}