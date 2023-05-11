package no.nav.helse.spleis.monitorering

import no.nav.helse.spleis.db.PersonDao
import org.slf4j.event.Level

internal interface Sjekk {
    fun sjekk(): Pair<Level, String>?
}

internal class RegelmessigAvstemming(private val personDao: PersonDao): Sjekk {
    override fun sjekk(): Pair<Level, String>? {
        val manglerAvstemming = personDao.manglerAvstemming()
        if (manglerAvstemming == 0) return null
        return Level.ERROR to "Det er $manglerAvstemming personer som ikke er avstemt på over en måned!"
    }
}