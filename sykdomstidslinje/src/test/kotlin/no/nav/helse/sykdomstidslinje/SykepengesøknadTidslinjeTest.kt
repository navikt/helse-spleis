package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.TestHendelser.egenmeldingFom
import no.nav.helse.hendelse.TestHendelser.egenmeldingTom
import no.nav.helse.hendelse.TestHendelser.ferieFom
import no.nav.helse.hendelse.TestHendelser.ferieTom
import no.nav.helse.hendelse.TestHendelser.nySøknad
import no.nav.helse.hendelse.TestHendelser.sendtSøknad
import no.nav.helse.hendelse.TestHendelser.sykeperiodeFOM
import no.nav.helse.hendelse.TestHendelser.sykeperiodeTOM
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengesøknadTidslinjeTest {

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`() {
        val syketilfeller = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje()).syketilfeller()

        assertEquals(Sykedag::class, syketilfeller.dagForDato(sykeperiodeFOM)::class)
        assertEquals(SykHelgedag::class, syketilfeller.dagForDato(sykeperiodeTOM)::class)
        assertEquals(sykeperiodeTOM, syketilfeller.last().sluttdato())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`() {
        val syketilfeller = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje()).syketilfeller()

        assertEquals(egenmeldingFom, syketilfeller.first().startdato())
        assertEquals(Egenmeldingsdag::class, syketilfeller.dagForDato(egenmeldingFom)::class)
        assertEquals(Egenmeldingsdag::class, syketilfeller.dagForDato(egenmeldingTom)::class)
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`() {
        val syketilfeller = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje()).syketilfeller()

        assertEquals(Feriedag::class, syketilfeller.dagForDato(ferieFom)::class)
        assertEquals(Feriedag::class, syketilfeller.dagForDato(ferieTom)::class)
    }

    fun List<Sykdomstidslinje>.dagForDato(localDate: LocalDate) =
        flatMap { it.flatten() }
            .find { it.startdato() == localDate }!!
}

