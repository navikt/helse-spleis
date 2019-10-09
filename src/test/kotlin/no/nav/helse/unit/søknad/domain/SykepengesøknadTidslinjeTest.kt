package no.nav.helse.unit.søknad.domain

import no.nav.helse.TestConstants.egenmeldingFom
import no.nav.helse.TestConstants.egenmeldingTom
import no.nav.helse.TestConstants.ferieFom
import no.nav.helse.TestConstants.ferieTom
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.TestConstants.sykeperiodFOM
import no.nav.helse.TestConstants.sykeperiodeTOM
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
        val syketilfeller = sendtSøknad().sykdomstidslinje().syketilfeller()

        assertEquals(Sykedag::class, syketilfeller.dagForDato(sykeperiodFOM)::class)
        assertEquals(SykHelgedag::class, syketilfeller.dagForDato(sykeperiodeTOM)::class)
        assertEquals(sykeperiodeTOM, syketilfeller.last().sluttdato())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`(){
        val syketilfeller = sendtSøknad().sykdomstidslinje().syketilfeller()

        assertEquals(egenmeldingFom, syketilfeller.first().startdato())
        assertEquals(Sykedag::class, syketilfeller.dagForDato(egenmeldingFom)::class)
        assertEquals(SykHelgedag::class, syketilfeller.dagForDato(egenmeldingTom)::class)
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`(){
        val syketilfeller = sendtSøknad().sykdomstidslinje().syketilfeller()

        assertEquals(Feriedag::class, syketilfeller.dagForDato(ferieFom)::class)
        assertEquals(Feriedag::class, syketilfeller.dagForDato(ferieTom)::class)
    }

    fun List<Sykdomstidslinje>.dagForDato(localDate: LocalDate) =
        flatMap { it.flatten() }
            .find { it.startdato() == localDate }!!
}

