package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDateTime

internal class Testhendelse(private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45)) :
    Sykdomshendelse {
    override fun organisasjonsnummer(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun aktørId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun compareTo(other: Sykdomshendelse): Int {
        return this.rapportertdato().compareTo(other.rapportertdato())
    }
}
