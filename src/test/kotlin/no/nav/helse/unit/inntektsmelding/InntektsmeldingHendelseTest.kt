package no.nav.helse.unit.inntektsmelding

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.Uke
import no.nav.helse.get
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingHendelseTest {

    @Test
    internal fun `mellomrom mellom arbeidsgiverperioder skal være arbeidsdager`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(
                Periode(Uke(1).mandag, Uke(1).tirsdag),
                Periode(Uke(1).torsdag, Uke(1).fredag)
        ))

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Arbeidsdag::class, tidslinje[Uke(1).onsdag]!!::class)
    }


    @Test
    internal fun `arbeidsgiverperioden skal være egenmeldingsdager`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(
                Periode(Uke(1).mandag, Uke(1).tirsdag),
                Periode(Uke(1).torsdag, Uke(1).fredag)
        ))

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Egenmeldingsdag::class, tidslinje[Uke(1).mandag]!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje[Uke(1).tirsdag]!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje[Uke(1).torsdag]!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje[Uke(1).fredag]!!::class)
    }
}
