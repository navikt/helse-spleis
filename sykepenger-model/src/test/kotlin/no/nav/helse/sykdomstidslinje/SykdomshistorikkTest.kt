package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding.BitAvInntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Personopplysninger
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.TestHendelse
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomshistorikkTest {
    private lateinit var historikk: Sykdomshistorikk

    @BeforeEach
    fun setup() {
        historikk = Sykdomshistorikk()
        resetSeed()
    }

    @Test
    fun `fjerner ingenting`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(emptyList())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals(tidslinje, historikk.sykdomstidslinje())
    }

    @Test
    fun `fjerner hel periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(tidslinje.periode()!!))
        assertEquals(2, historikk.inspektør.elementer())
        assertFalse(historikk.inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(historikk.inspektør.tidslinje(1).iterator().hasNext())
    }

    @Test
    fun `fjerner del av periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(tidslinje.førsteDag() til tidslinje.sisteDag().minusDays(1)))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(1, historikk.inspektør.tidslinje(0).count())
    }

    @Test
    fun `fjerner flere perioder`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(1.januar til 2.januar, 5.januar til 10.januar))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(3.januar til 4.januar, historikk.sykdomstidslinje().periode())
    }

    @Test
    fun `håndterer kun hendelser èn gang`() {
        val tidslinje = 10.S
        val hendelse = TestHendelse(tidslinje)
        historikk.håndter(hendelse)
        assertEquals(1, historikk.inspektør.elementer())
        historikk.håndter(hendelse)
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `Inntektsmeldingen skal kunne håndteres i biter uten å lage flere historikkinnslag 😊`() {
        val inntektsmelding = inntektsmelding(1.januar til 16.januar)
        val bit1 = BitAvInntektsmelding(inntektsmelding, 1.januar til 8.januar)
        val bit2 = BitAvInntektsmelding(inntektsmelding, 9.januar til 16.januar)
        historikk.håndter(bit1)
        assertEquals(1, historikk.inspektør.elementer())
        //val nyesteId = historikk.nyesteId()
        assertEquals("UUUUUGG U", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2)
        assertEquals(1, historikk.inspektør.elementer())
        //assertEquals(historikk.nyesteId(), nyesteId)
        assertEquals("UUUUUGG UUUUUGG UU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Overlappende biter fra samme inntektsmelding`(){
        val inntektsmelding = inntektsmelding(1.januar til 16.januar)
        val bit1 = BitAvInntektsmelding(inntektsmelding, 1.januar til 8.januar)
        val bit2 = BitAvInntektsmelding(inntektsmelding, 8.januar til 16.januar)
        historikk.håndter(bit1)
        assertEquals(1, historikk.inspektør.elementer())
        //val nyesteId = historikk.nyesteId()
        assertEquals("UUUUUGG U", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2)
        assertEquals(1, historikk.inspektør.elementer())
        //assertEquals(historikk.nyesteId(), nyesteId)
        assertEquals("UUUUUGG UUUUUGG UU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Hele inntektsmeldingen er håndtert for en hendelse siden`(){
        val inntektsmelding = inntektsmelding(11.januar til 26.januar)
        val heleBiten = BitAvInntektsmelding(inntektsmelding, 11.januar til 26.januar)
        historikk.håndter(heleBiten)
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(heleBiten)
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `En bit av inntektsmeldingen håndteres før annen hendelse, og den andre biten etterpå`(){
        val inntektsmelding = inntektsmelding(11.januar til 26.januar)
        val bit1 = BitAvInntektsmelding(inntektsmelding, 11.januar til 18.januar)
        val bit2 = BitAvInntektsmelding(inntektsmelding, 19.januar til 26.januar)
        historikk.håndter(bit1)
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUGG UUUU", historikk.sykdomstidslinje().toShortString())
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2)
        assertEquals(3, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
    }

    private companion object {
        private fun inntektsmelding(
            vararg arbeidsgiverperiode: Periode
        ) = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null),
            orgnummer = "12345678",
            fødselsnummer = "12345678910",
            aktørId = "1",
            førsteFraværsdag = arbeidsgiverperiode.first().start,
            arbeidsgiverperioder = arbeidsgiverperiode.toList(),
            beregnetInntekt = 31000.månedlig,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harFlereInntektsmeldinger = false,
            mottatt = LocalDateTime.now(),
            personopplysninger = Personopplysninger(
                Personidentifikator.somPersonidentifikator("12345678910"),
                "1",
                LocalDate.now()
            )
        )
    }
}
