package no.nav.helse.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Simulering.*
import no.nav.helse.hendelser.Utbetaling
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.orgnummer
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal abstract class AbstractEndToEndTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        const val INNTEKT = 31000.00
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = TestPersonInspektør(person)
    protected lateinit var hendelselogg: ArbeidstakerHendelse
    protected var forventetEndringTeller = 0

    @BeforeEach
    internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    protected fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        val id = inspektør.vedtaksperiodeId(indeks)
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertNoErrors(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
    }

    protected fun assertNoWarnings(inspektør: TestPersonInspektør) {
        assertFalse(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    protected fun assertWarnings(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    protected fun assertMessages(inspektør: TestPersonInspektør) {
        assertTrue(inspektør.personLogg.hasMessages(), inspektør.personLogg.toString())
    }

    protected fun håndterSykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(sykmelding(*sykeperioder))
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeIndex: Int,
        vararg perioder: Søknad.Periode,
        harAndreInntektskilder: Boolean = false
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterSøknad(*perioder, harAndreInntektskilder = harAndreInntektskilder)
    }

    protected fun håndterSøknad(vararg perioder: Søknad.Periode, harAndreInntektskilder: Boolean = false, sendtTilNav: LocalDate = perioder.last().tom) {
        person.håndter(søknad(perioder = *perioder, harAndreInntektskilder = harAndreInntektskilder, sendtTilNav = sendtTilNav))
    }

    protected fun håndterSøknadArbeidsgiver(vararg perioder: SøknadArbeidsgiver.Periode) {
        person.håndter(søknadArbeidsgiver(perioder = *perioder))
    }

    protected fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeIndex: Int,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = 1.januar,
        ferieperioder: List<Periode> = emptyList()
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, ferieperioder)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = 1.januar,
        ferieperioder: List<Periode> = emptyList()
    ) {
        person.håndter(
            inntektsmelding(
                arbeidsgiverperioder,
                ferieperioder = ferieperioder,
                førsteFraværsdag = førsteFraværsdag
            )
        )
    }

    protected fun håndterVilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeIndex, inntekt))
    }

    protected fun håndterSimulering(vedtaksperiodeIndex: Int) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Simulering))
        person.håndter(simulering(vedtaksperiodeIndex))
    }

    protected fun håndterYtelser(vedtaksperiodeIndex: Int, vararg utbetalinger: Triple<LocalDate, LocalDate, Int>) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Sykepengehistorikk))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Foreldrepenger))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(ytelser(vedtaksperiodeIndex, utbetalinger.toList()))
    }

    protected fun håndterPåminnelse(vedtaksperiodeIndex: Int, påminnetTilstand: TilstandType) {
        person.håndter(påminnelse(vedtaksperiodeIndex, påminnetTilstand))
    }

    protected fun håndterManuellSaksbehandling(vedtaksperiodeIndex: Int, utbetalingGodkjent: Boolean) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(manuellSaksbehandling(vedtaksperiodeIndex, utbetalingGodkjent))
    }

    protected fun håndterUtbetalt(vedtaksperiodeIndex: Int, status: Utbetaling.Status) {
        person.håndter(utbetaling(vedtaksperiodeIndex, status))
    }

    private fun utbetaling(vedtaksperiodeIndex: Int, status: Utbetaling.Status) =
        Utbetaling(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        )


    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            hendelselogg = this
        }
    }

    private fun søknad(vararg perioder: Søknad.Periode, harAndreInntektskilder: Boolean, sendtTilNav: LocalDate = perioder.last().tom): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay()
        ).apply {
            hendelselogg = this
        }
    }

    private fun søknadArbeidsgiver(vararg perioder: SøknadArbeidsgiver.Periode): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder)
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder
        ).apply {
            hendelselogg = this
        }
    }

    private fun vilkårsgrunnlag(vedtaksperiodeIndex: Int, inntekt: Double): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsmåneder = (1..12).map {
                Vilkårsgrunnlag.Måned(
                    YearMonth.of(2017, it),
                    listOf(inntekt)
                )
            },
            erEgenAnsatt = false,
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 1.januar(2017)))
        ).apply {
            hendelselogg = this
        }
    }

    private fun påminnelse(
        vedtaksperiodeIndex: Int,
        påminnetTilstand: TilstandType
    ): Påminnelse {
        return Påminnelse(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            antallGangerPåminnet = 0,
            tilstand = påminnetTilstand,
            tilstandsendringstidspunkt = LocalDateTime.now(),
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Triple<LocalDate, LocalDate, Int>> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = listOf(
            Inntektsopplysning(
                1.desember(2017),
                INNTEKT.toInt() - 10000,
                ORGNUMMER
            )
        ),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        return Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                utbetalinger = utbetalinger.map {
                    Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                        it.first,
                        it.second,
                        it.third
                    )
                },
                inntektshistorikk = inntektshistorikk,
                graderingsliste = emptyList(),
                maksDato = null,
                aktivitetslogg = aktivitetslogg
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepenger,
                svangerskapspenger,
                aktivitetslogg
            ),
            aktivitetslogg = aktivitetslogg
        ).apply {
            hendelselogg = this
        }
    }

    private fun simulering(vedtaksperiodeIndex: Int, simuleringOK: Boolean = true) =
        no.nav.helse.hendelser.Simulering(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = simuleringOK,
            melding = "",
            simuleringResultat = SimuleringResultat(
                totalbeløp = 2000.toBigDecimal(),
                perioder = listOf(
                    SimulertPeriode(
                        fom = 17.januar,
                        tom = 20.januar,
                        utbetalinger = listOf(
                            SimulertUtbetaling(
                                forfallsdato = 21.januar,
                                utbetalesTil = Mottaker(
                                    id = orgnummer,
                                    navn = "Org Orgesen AS"
                                ),
                                feilkonto = false,
                                detaljer = listOf(
                                    Detaljer(
                                        fom = 17.januar,
                                        tom = 20.januar,
                                        konto = "81549300",
                                        beløp = 2000.toBigDecimal(),
                                        klassekode = Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Sats(
                                            sats = 1000.toBigDecimal(),
                                            antall = 2,
                                            type = "DAGLIG"
                                        ),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).apply {
            hendelselogg = this
        }

    private fun manuellSaksbehandling(
        vedtaksperiodeIndex: Int,
        utbetalingGodkjent: Boolean
    ): ManuellSaksbehandling {
        return ManuellSaksbehandling(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            saksbehandler = "Ola Nordmann",
            utbetalingGodkjent = utbetalingGodkjent,
            godkjenttidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }
}
