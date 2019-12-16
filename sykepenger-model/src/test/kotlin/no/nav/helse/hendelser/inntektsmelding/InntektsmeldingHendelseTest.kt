package no.nav.helse.hendelser.inntektsmelding

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.Uke
import no.nav.helse.get
import no.nav.helse.sak.UtenforOmfangException
import no.nav.helse.september
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

        assertEquals(Uke(1).mandag, tidslinje.førsteDag())
        assertThrows<IllegalStateException> {
            assertEquals(Uke(1).mandag, tidslinje.utgangspunktForBeregningAvYtelse())
        }
    }

    @Test
    internal fun `ferieperioder i inntektsmelding kan være tom`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(
                Periode(Uke(1).mandag, Uke(1).tirsdag),
                Periode(Uke(1).torsdag, Uke(1).fredag)
        ), ferieperioder = emptyList())

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(Uke(1).mandag.minusDays(16), tidslinje.førsteDag())
        assertEquals(Uke(1).torsdag, tidslinje.utgangspunktForBeregningAvYtelse())
    }

    @Test
    internal fun `arbeidsgiverperioden kan ikke ha overlappende perioder`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(arbeidsgiverperioder = listOf(
                Periode(Uke(1).mandag, Uke(1).tirsdag),
                Periode(Uke(1).torsdag, Uke(1).fredag),
                Periode(Uke(1).onsdag, Uke(1).torsdag)
        ))

        assertThrows<UtenforOmfangException> {
            inntektsmeldingHendelse.sykdomstidslinje()
        }
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        val inntektsmeldingHendelse = inntektsmeldingHendelse(
                arbeidsgiverperioder = emptyList(),
                ferieperioder = emptyList())

        val tidslinje = inntektsmeldingHendelse.sykdomstidslinje()

        assertEquals(10.september, tidslinje.førsteDag())
        assertEquals(10.september, tidslinje.utgangspunktForBeregningAvYtelse())
        assertEquals(10.september, tidslinje.sisteDag())
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

        assertEquals(Uke(1).mandag.minusDays(16), tidslinje.førsteDag())
        assertEquals(Uke(1).mandag, tidslinje.utgangspunktForBeregningAvYtelse())
        assertEquals(Uke(3).torsdag, tidslinje.sisteDag())
        assertEquals(34, tidslinje.flatten().size)
    }

    @Test
    internal fun `inntektsmelding uten mottattDato er ikke gyldig`() {
        val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
            (it as ObjectNode).remove("mottattDato")
        }
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertFalse(inntektsmeldingHendelse.kanBehandles())
    }

    @Test
    internal fun `inntektsmelding uten foersteFravaersdag er ikke gyldig`() {
        val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
            (it as ObjectNode).remove("foersteFravaersdag")
        }
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertFalse(inntektsmeldingHendelse.kanBehandles())
    }

    @Test
    internal fun `inntektsmelding uten virksomhetsnummer er ikke gyldig`() {
        val inntektsmeldingJson = inntektsmeldingDTO().toJsonNode().also {
            (it as ObjectNode).remove("virksomhetsnummer")
        }
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertFalse(inntektsmeldingHendelse.kanBehandles())
    }

    @Test
    internal fun `inntektsmelding med refusjon beløp == null er ikke gyldig`() {
        val inntektsmeldingJson = inntektsmeldingDTO(refusjon = Refusjon(null)).toJsonNode()
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertFalse(inntektsmeldingHendelse.kanBehandles())
    }

    @Test
    internal fun `inntektsmelding med refusjon lik beregnet inntekt gir gyldig hendelse`() {
        val inntektsmeldingJson = inntektsmeldingDTO(
                beregnetInntekt = 700.toBigDecimal(),
                refusjon = Refusjon(beloepPrMnd = 700.toBigDecimal())
        ).toJsonNode()
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertTrue(inntektsmeldingHendelse.kanBehandles())
    }

    @Test
    internal fun `inntektsmelding med refusjon forskjellig fra beregnetInntekt er ikke gyldig`() {
        val inntektsmeldingJson = inntektsmeldingDTO(
                beregnetInntekt = 700.toBigDecimal(),
                refusjon = Refusjon(beloepPrMnd = 321.toBigDecimal())
        ).toJsonNode()
        val inntektsmeldingHendelse = InntektsmeldingHendelse(inntektsmeldingJson)

        assertFalse(inntektsmeldingHendelse.kanBehandles())
    }
}
