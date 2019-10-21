package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.TestHendelser.egenmeldingFom
import no.nav.helse.hendelse.TestHendelser.egenmeldingTom
import no.nav.helse.hendelse.TestHendelser.ferieFom
import no.nav.helse.hendelse.TestHendelser.ferieTom
import no.nav.helse.hendelse.TestHendelser.nySøknad
import no.nav.helse.hendelse.TestHendelser.sendtSøknad
import no.nav.helse.hendelse.TestHendelser.sykeperiodeFOM
import no.nav.helse.hendelse.TestHendelser.sykeperiodeTOM
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import no.nav.syfo.kafka.sykepengesoknad.dto.FravarDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.FravarstypeDTO.PERMISJON
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SykepengesøknadTidslinjeTest {

    @Test
    fun `Tidslinjen får sykeperiodene (søknadsperiodene) fra søknaden`() {
        val tidslinje = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertType(Sykedag::class, tidslinje[sykeperiodeFOM])
        assertType(SykHelgedag::class, tidslinje[sykeperiodeTOM])
        assertEquals(sykeperiodeTOM, tidslinje.sluttdato())
    }

    @Test
    fun `Tidslinjen får egenmeldingsperiodene fra søknaden`() {
        val tidslinje = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertEquals(egenmeldingFom, tidslinje.startdato())
        assertType(Egenmeldingsdag::class, tidslinje[egenmeldingFom])
        assertType(Egenmeldingsdag::class, tidslinje[egenmeldingTom])
    }

    @Test
    fun `Tidslinjen får ferien fra søknaden`() {
        val tidslinje = (sendtSøknad().sykdomstidslinje() + nySøknad().sykdomstidslinje())

        assertType(Feriedag::class, tidslinje[ferieFom])
        assertType(Feriedag::class, tidslinje[ferieTom])
    }

    @Test
    fun `Tidslinjen får permisjon fra soknaden`() {
        val tidslinje = sendtSøknad(
            søknadsperioder = listOf(SoknadsperiodeDTO(Uke(1).mandag, Uke(1).fredag)),
            fravær = listOf(FravarDTO(Uke(1).torsdag, Uke(1).fredag, PERMISJON))
        ).also {
            it.toString()
        }.sykdomstidslinje()

        assertType(Permisjonsdag::class, tidslinje[Uke(1).torsdag])
        assertType(Permisjonsdag::class, tidslinje[Uke(1).fredag])
    }

    @Test
    fun `Tidslinjen får arbeidsdag resten av perioden hvis soknaden har arbeid gjenopptatt`() {
        val tidslinje = sendtSøknad(
            søknadsperioder = listOf(SoknadsperiodeDTO(Uke(1).mandag, Uke(1).fredag)),
            arbeidGjenopptatt = Uke(1).onsdag
        ).also {
            it.toString()
        }.sykdomstidslinje()

        assertType(Sykedag::class, tidslinje[Uke(1).mandag])
        assertType(Sykedag::class, tidslinje[Uke(1).tirsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).onsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).torsdag])
        assertType(Arbeidsdag::class, tidslinje[Uke(1).fredag])
    }

    inline fun assertType(expected: KClass<*>, actual: Any?) =
        assertEquals(expected, actual?.let { it::class })
}

