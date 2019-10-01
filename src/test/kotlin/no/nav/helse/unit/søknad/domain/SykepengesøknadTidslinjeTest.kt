package no.nav.helse.unit.søknad.domain

import no.nav.helse.TestConstants.egenmeldingFom
import no.nav.helse.TestConstants.egenmeldingTom
import no.nav.helse.TestConstants.ferieFom
import no.nav.helse.TestConstants.ferieTom
import no.nav.helse.TestConstants.sykeperiodFOM
import no.nav.helse.TestConstants.sykeperiodeTOM
import no.nav.helse.TestConstants.søknad
import no.nav.helse.sykdomstidslinje.Feriedag
import no.nav.helse.sykdomstidslinje.SykHelgedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengesøknadTidslinjeTest {

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(Sykedag::class, sykdomstidslinje.syketilfeller().dagForDato(sykeperiodFOM)::class)
        assertEquals(SykHelgedag::class, sykdomstidslinje.syketilfeller().dagForDato(sykeperiodeTOM)::class)
        assertEquals(sykeperiodeTOM, sykdomstidslinje.syketilfeller().last().sluttdato())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(egenmeldingFom, sykdomstidslinje.syketilfeller().first().startdato())
        assertEquals(Sykedag::class, sykdomstidslinje.syketilfeller().dagForDato(egenmeldingFom)::class)
        assertEquals(SykHelgedag::class, sykdomstidslinje.syketilfeller().dagForDato(egenmeldingTom)::class)
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`(){
        val sykdomstidslinje = søknad.sykdomstidslinje

        assertEquals(Feriedag::class, sykdomstidslinje.syketilfeller().dagForDato(ferieFom)::class)
        assertEquals(Feriedag::class, sykdomstidslinje.syketilfeller().dagForDato(ferieTom)::class)
    }

    fun List<Sykdomstidslinje>.dagForDato(localDate: LocalDate) =
        flatMap { it.flatten() }
            .find { it.startdato() == localDate }!!
}

