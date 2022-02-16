package no.nav.helse.person.filter

import no.nav.helse.Fødselsnummer
import no.nav.helse.Toggle
import no.nav.helse.hentInfo
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class BrukerutbetalingfilterTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `refusjon passer filter`() {
        assertTrue(brukerutbetalingfilter(utbetaling = lagUtbetaling()))
    }

    @Test
    fun `inntektsmelding er for gammel`() {
        assertFalse(brukerutbetalingfilter(inntektsmeldingtidsstempel = LocalDateTime.now().minusHours(25), utbetaling = lagUtbetaling()))
    }

    @Test
    fun `ingen refusjon passer filter`() {
        val ingenRefusjon = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
        assertTrue(brukerutbetalingfilter(utbetaling = ingenRefusjon))
    }

    @Test
    fun `feil periodetype`() {
        assertFalse(brukerutbetalingfilter(periodetype = Periodetype.INFOTRYGDFORLENGELSE, utbetaling = lagUtbetaling()))
    }

    @Test
    fun `spleis forlengelse passer`() {
        assertTrue(brukerutbetalingfilter(periodetype = Periodetype.FORLENGELSE, utbetaling = lagUtbetaling()))
    }

    @Test
    fun `feil fødselsdato passer ikke`() {
        assertFalse(brukerutbetalingfilter(fnr = "02108512345", utbetaling = lagUtbetaling()))
    }

    @Test
    fun `delvis refusjon passer ikke`() {
        val delvisRefusjon = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600)))
        assertFalse(brukerutbetalingfilter(utbetaling = delvisRefusjon))
    }

    @Test
    fun `inntektskilde fra flere arbeidsgivere passer ikke`() {
        assertFalse(brukerutbetalingfilter(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE, utbetaling = lagUtbetaling()))
    }

    @Test
    fun `en vedtaksperiode som har warnings passer ikke`() {
        assertFalse(brukerutbetalingfilter(vedtaksperiodeHarWarnings = true, utbetaling = lagUtbetaling()))
    }

    @Test
    fun `om hendelsen har warnings passer ikke`() {
        aktivitetslogg.warn("Warning")
        assertFalse(brukerutbetalingfilter(utbetaling = lagUtbetaling()))
    }

    @Test
    fun `om hendelsen har error passer ikke`() {
        aktivitetslogg.error("Error")
        assertFalse(brukerutbetalingfilter(utbetaling = lagUtbetaling()))
    }

    @Test
    fun `om perioden ikke inneholder brukerutbetaling passer ikke`() {
        assertFalse(brukerutbetalingfilter(harBrukerutbetaling = false, utbetaling = lagUtbetaling()))
    }

    @Test
    fun `ingen av kritereiene for brukerutbetaling passer`() {
        aktivitetslogg.warn("Warning")
        assertFalse(
            brukerutbetalingfilter(
                fnr = "02108512345",
                inntektsmeldingtidsstempel = LocalDateTime.now().minusHours(25),
                vedtaksperiodeHarWarnings = true,
                utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600))),
                periodetype = Periodetype.INFOTRYGDFORLENGELSE,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                harBrukerutbetaling = false
            )
        )

        val forventetMelding =
            "Ikke kandidat til brukerutbetaling fordi: Fødselsdag passer ikke, Perioden er ikke førstegangsbehandling eller forlengelse, Utbetalingen består av delvis refusjon, Inntektskilden er ikke for en arbeidsgiver, Inntektsmelding ble mottatt for mer enn 24 timer siden, Vedtaksperioden har warnings, Hendelsen har warnings, Inneholder ikke brukerutbetaling"

        assertEquals(1, aktivitetslogg.hentInfo().count { it == forventetMelding }) {
            "Forventet melding:\n$forventetMelding\n$aktivitetslogg"
        }
    }

    private fun assertTrue(filter: Featurefilter) {
        Toggle.BrukerutbetalingFilter.enable {
            assertTrue(filter.filtrer(aktivitetslogg)) { "Forventet at filteret skal være sant.\n$aktivitetslogg" }
            assertEquals(1, aktivitetslogg.hentInfo().count { it == "Plukket ut for brukerutbetaling" }) { "Forventet plukket ut for brukerutbetaling\n$aktivitetslogg" }
        }
        aktivitetslogg = Aktivitetslogg()
        Toggle.BrukerutbetalingFilter.disable {
            assertFalse(filter.filtrer(aktivitetslogg)) { "Forventet at filteret skal være usant.\n$aktivitetslogg" }
            assertEquals(1, aktivitetslogg.hentInfo().count { it == "Kandidat for brukerutbetaling" }) { "Forventet kandidat for brukerutbetaling\n$aktivitetslogg" }
        }
    }

    private fun assertFalse(filter: Featurefilter) {
        assertFalse(filter.filtrer(aktivitetslogg)) { "Forventet at filteret skal være usant.\n$aktivitetslogg" }
        assertEquals(1, aktivitetslogg.hentInfo().count { it.startsWith("Ikke kandidat til brukerutbetaling fordi:") })
    }

    private fun lagUtbetaling(tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV), forrige: Utbetaling? = null): Utbetaling {
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            tidslinje.first().dato
        ).betal()

        return Utbetaling.lagUtbetaling(
            forrige?.let { listOf(forrige) } ?: emptyList(),
            "fnr",
            UUID.randomUUID(),
            "orgnr",
            tidslinje,
            tidslinje.last().dato,
            aktivitetslogg,
            LocalDate.MAX,
            0,
            0,
            forrige
        )
    }

    internal companion object {
        internal fun brukerutbetalingfilter(
            fnr: String = "31108512345",
            inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
            periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            inntektsmeldingtidsstempel: LocalDateTime = LocalDateTime.now(),
            vedtaksperiodeHarWarnings: Boolean = false,
            utbetaling: Utbetaling,
            harBrukerutbetaling: Boolean = true
        ) = Brukerutbetalingfilter.Builder(Fødselsnummer.tilFødselsnummer(fnr))
            .inntektkilde(inntektskilde)
            .periodetype(periodetype)
            .utbetaling(utbetaling)
            .inntektsmeldingtidsstempel(inntektsmeldingtidsstempel)
            .vedtaksperiodeHarWarnings(vedtaksperiodeHarWarnings)
            .harBrukerutbetaling(harBrukerutbetaling)
            .build()
    }
}
