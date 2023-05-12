package no.nav.helse.spleis.monitorering

import java.time.LocalDateTime
import no.nav.helse.erHelg
import no.nav.helse.spleis.db.PersonDao
import org.slf4j.event.Level

internal interface Sjekk {
    fun sjekk(): Pair<Level, String>?
    fun skalSjekke(nå: LocalDateTime): Boolean
}

internal class RegelmessigAvstemming(private val personDao: PersonDao): Sjekk {
    override fun sjekk(): Pair<Level, String>? {
        val manglerAvstemming = personDao.manglerAvstemming()
        if (manglerAvstemming == 0) return null
        return Level.ERROR to "Det er $manglerAvstemming personer som ikke er avstemt på over en måned!"
    }

    override fun skalSjekke(nå: LocalDateTime): Boolean {
        if (nå.toLocalDate().erHelg()) return false
        return nå.minute % 15 == 0
    }
}