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
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.get
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.torsdag
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
            søknadsperioder = listOf(SoknadsperiodeDTO(1.mandag, 1.fredag)),
            fravær = listOf(FravarDTO(1.torsdag, 1.fredag, PERMISJON))
        ).also {
            it.toString()
        }.sykdomstidslinje()

        assertType(Permisjonsdag::class, tidslinje[1.torsdag])
        assertType(Permisjonsdag::class, tidslinje[1.fredag])
    }

    inline fun assertType(expected: KClass<*>, actual: Any?) =
        assertEquals(expected, actual?.let { it::class })
}

