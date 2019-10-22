package no.nav.helse.utbetalingstidslinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.egenmeldingsdager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ferie
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.sykedager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.utenlandsdag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.JULY

internal class EnkelUtbetalingsTest {

    companion object {
        private val testKildeHendelse = Testhendelse(rapportertdato = LocalDateTime.of(2019, JULY, 1, 0, 0))
    }

}
