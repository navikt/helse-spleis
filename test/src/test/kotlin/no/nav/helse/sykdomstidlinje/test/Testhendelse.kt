package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.KildeHendelse
import java.time.LocalDateTime

internal class Testhendelse(private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45)) :
    KildeHendelse {
    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun compareTo(other: KildeHendelse): Int {
        return this.rapportertdato().compareTo(other.rapportertdato())
    }
}
