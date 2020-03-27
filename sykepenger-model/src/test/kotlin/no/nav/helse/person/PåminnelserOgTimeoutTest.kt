package no.nav.helse.person

import no.nav.helse.e2e.TestPersonInspektør
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class PåminnelserOgTimeoutTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "1234"
    }

    private lateinit var person: Person
    private lateinit var hendelse: ArbeidstakerHendelse
    private val inspektør get() = TestPersonInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `timeoutverdier`() {
        assertEquals(Duration.ofDays(30), Vedtaksperiode.Start.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSykmeldingFerdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSykmeldingUferdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSykmeldingFerdigForlengelse.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.MottattSykmeldingUferdigForlengelse.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerSøknadUferdigForlengelse.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerSøknadFerdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmeldingFerdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmeldingUferdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerUferdigForlengelse.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerUferdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmeldingFerdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmeldingFerdigGap.timeout)
        assertEquals(Duration.ofDays(30), Vedtaksperiode.AvventerInntektsmeldingFerdigGap.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.AvventerHistorikk.timeout)
        assertEquals(Duration.ofHours(1), Vedtaksperiode.AvventerVilkårsprøvingGap.timeout)
        assertEquals(Duration.ofDays(7), Vedtaksperiode.AvventerGodkjenning.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilUtbetaling.timeout)
        assertEquals(Duration.ZERO, Vedtaksperiode.TilInfotrygd.timeout)
    }

    @Test
    fun `påminnelse i mottatt sykmelding`() {
        person.håndter(sykmelding())
        person.håndter(påminnelse(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i mottatt søknad`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        assertTilstand(TilstandType.AVVENTER_GAP)
        assertEquals(2, hendelse.behov().size)
        person.håndter(påminnelse(TilstandType.AVVENTER_GAP))
        assertTilstand(TilstandType.AVVENTER_GAP)
        assertEquals(2, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Foreldrepenger))
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Sykepengehistorikk))
    }

    @Test
    fun `påminnelse i mottatt inntektsmelding`() {
        person.håndter(sykmelding())
        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP))
        assertEquals(0, hendelse.behov().size)
        assertTilstand(TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `påminnelse i vilkårsprøving`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        assertEquals(3, hendelse.behov().size)
        person.håndter(påminnelse(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP))
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP)
        assertEquals(3, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Inntektsberegning))
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.EgenAnsatt))
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Opptjening))
    }

    @Test
    fun `påminnelse i ytelser`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        assertEquals(2, hendelse.behov().size)
        person.håndter(påminnelse(TilstandType.AVVENTER_HISTORIKK))
        assertTilstand(TilstandType.AVVENTER_HISTORIKK)
        assertEquals(2, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Foreldrepenger))
        assertTrue(hendelse.etterspurteBehov(inspektør.vedtaksperiodeId(0), Behovtype.Sykepengehistorikk))
    }

    @Test
    fun `påminnelse i til godkjenning`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)
        assertEquals(2, hendelse.behov().size)
        assertTrue(hendelse.behov().any { it.type == Behovtype.Godkjenning })
        assertTrue(hendelse.behov().any { it.type == Behovtype.Simulering })
        person.håndter(påminnelse(TilstandType.AVVENTER_GODKJENNING))
        assertTilstand(TilstandType.TIL_INFOTRYGD)
        assertEquals(0, hendelse.behov().size)
    }

    @Test
    fun `påminnelse i til utbetaling`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(manuellSaksbehandling())
        assertEquals(1, hendelse.behov().size)
        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
        assertEquals(0, hendelse.behov().size)
    }

    @Test
    fun `ignorerer påminnelser på tidligere tilstander`() {
        person.håndter(sykmelding())
        person.håndter(påminnelse(TilstandType.TIL_INFOTRYGD))
        assertTilstand(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)

        person.håndter(søknad())
        person.håndter(påminnelse(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP))
        assertTilstand(TilstandType.AVVENTER_GAP)

        person.håndter(inntektsmelding())
        person.håndter(påminnelse(TilstandType.AVVENTER_GAP))
        assertTilstand(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP)

        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP))
        assertTilstand(TilstandType.AVVENTER_HISTORIKK)

        person.håndter(ytelser())
        person.håndter(påminnelse(TilstandType.AVVENTER_HISTORIKK))
        assertTilstand(TilstandType.AVVENTER_GODKJENNING)

        person.håndter(manuellSaksbehandling())
        person.håndter(påminnelse(TilstandType.AVVENTER_GODKJENNING))
        assertTilstand(TilstandType.TIL_UTBETALING)

        person.håndter(påminnelse(TilstandType.TIL_UTBETALING))
        assertTilstand(TilstandType.TIL_UTBETALING)
    }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(1.januar,  20.januar, 100, null)),
            harAndreInntektskilder = false,
            sendtTilNAV = 20.januar.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(1.januar, 20.januar, 100))
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.0, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.0,
            arbeidsgiverperioder = listOf(Periode(1.januar, 1.januar.plusDays(15))),
            ferieperioder = emptyList()
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2017, it), listOf(31000.0)
                )
            },
            erEgenAnsatt = false,
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017)))
        ).apply {
            hendelse = this
        }

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                    17.januar(2017),
                    20.januar(2017),
                    1000
                )
            ),
            inntektshistorikk = emptyList(),
            graderingsliste = emptyList(),
            aktivitetslogg = Aktivitetslogg()
        ),
        foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = null,
            svangerskapsytelse = null,
            aktivitetslogg = Aktivitetslogg()
        ),
        aktivitetslogg = Aktivitetslogg()
    ).apply {
        hendelse = this
    }

    private fun manuellSaksbehandling() = ManuellSaksbehandling(
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = true,
        godkjenttidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun påminnelse(tilstandType: TilstandType) = Påminnelse(
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun assertTilstand(expectedTilstand: TilstandType) {
        assertEquals(
            expectedTilstand,
            inspektør.sisteTilstand(0)
        )
    }
}
