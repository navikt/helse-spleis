package no.nav.helse.unit.inntektsmelding

import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.Uke
import no.nav.helse.get
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = emptyList(),
                ferieperioder = listOf(
                        Periode(Uke(1).mandag, Uke(1).tirsdag),
                        Periode(Uke(1).torsdag, Uke(1).fredag)
                ))

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Uke(1).mandag, tidslinje.startdato())
    }

    @Test
    internal fun `ferieperioder i inntektsmelding kan være tom`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(
                Periode(Uke(1).mandag, Uke(1).tirsdag),
                Periode(Uke(1).torsdag, Uke(1).fredag)
        ), ferieperioder = emptyList())

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Uke(1).mandag, tidslinje.startdato())
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = emptyList(),
                ferieperioder = emptyList())

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(LocalDate.of(2019, 9, 10), tidslinje.startdato())
        assertEquals(LocalDate.of(2019, 9, 10), tidslinje.sluttdato())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = listOf(
                        Periode(Uke(1).mandag, Uke(3).tirsdag)
                ),
                ferieperioder = listOf(
                        Periode(Uke(3).onsdag, Uke(3).torsdag)
                ))

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Uke(1).mandag, tidslinje.startdato())
        assertEquals(Uke(3).torsdag, tidslinje.sluttdato())
        assertEquals(16, tidslinje.antallSykedagerHvorViTellerMedHelg())
        assertEquals(18, tidslinje.flatten().size)
    }
}
