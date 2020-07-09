package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Simulering.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import no.nav.helse.hendelser.UtbetalingHendelse as UtbetalingHendelse1

internal abstract class AbstractEndToEndTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        const val INNTEKT = 31000.00
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør
    protected val inspektør get() = TestArbeidsgiverInspektør(person)
    protected lateinit var hendelselogg: ArbeidstakerHendelse
    protected var forventetEndringTeller = 0

    @BeforeEach
    internal fun abstractSetup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    protected fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertTilstander(inspektør.vedtaksperiodeId(indeks), *tilstander)
    }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType) {
        val id = inspektør.forkastetVedtaksperiodeId(indeks)
        assertEquals(tilstander.asList(), observatør.tilstander[id])
    }

    protected fun assertNoErrors(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
    }

    protected fun assertNoWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    protected fun assertWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasWarnings(), inspektør.personLogg.toString())
    }

    protected fun assertErrors(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
    }

    protected fun assertMessages(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasMessages(), inspektør.personLogg.toString())
    }

    protected fun håndterSykmelding(vararg sykeperioder: Sykmeldingsperiode) {
        person.håndter(sykmelding(*sykeperioder))
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeIndex: Int,
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterSøknad(*perioder, harAndreInntektskilder = harAndreInntektskilder)
    }

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false,
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive
    ) {
        person.håndter(
            søknad(
                perioder = *perioder,
                harAndreInntektskilder = harAndreInntektskilder,
                sendtTilNav = sendtTilNav
            )
        )
    }

    protected fun håndterSøknadArbeidsgiver(
        vararg perioder: SøknadArbeidsgiver.Søknadsperiode,
        orgnummer: String = ORGNUMMER
    ) {
        person.håndter(søknadArbeidsgiver(perioder = *perioder, orgnummer = orgnummer))
    }

    protected fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeIndex: Int,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Double, List<LocalDate>> = Triple(null, INNTEKT, emptyList())
    ) {
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, ferieperioder, refusjon)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Double, List<LocalDate>> = Triple(null, INNTEKT, emptyList())
    ) {
        person.håndter(
            inntektsmelding(
                arbeidsgiverperioder,
                ferieperioder = ferieperioder,
                førsteFraværsdag = førsteFraværsdag,
                refusjon = refusjon
            )
        )
    }

    protected fun håndterVilkårsgrunnlag(
        vedtaksperiodeIndex: Int,
        inntekt: Double,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        egenAnsatt: Boolean = false,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Inntektsberegning))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, EgenAnsatt))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Behovtype.Dagpenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Behovtype.Arbeidsavklaringspenger))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Medlemskap))
        person.håndter(vilkårsgrunnlag(
            vedtaksperiodeIndex,
            inntekt,
            arbeidsforhold,
            egenAnsatt,
            medlemskapstatus,
            orgnummer
        ))
    }

    protected fun håndterSimulering(vedtaksperiodeIndex: Int) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Simulering))
        person.håndter(simulering(vedtaksperiodeIndex))
    }

    protected fun håndterUtbetalingshistorikk(
        vedtaksperiodeIndex: Int,
        vararg utbetalinger: Utbetalingshistorikk.Periode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ) {
        person.håndter(utbetalingshistorikk(
            vedtaksperiodeIndex,
            utbetalinger.toList(),
            inntektshistorikk(inntektshistorikk, orgnummer)
        ))
    }

    protected fun håndterYtelser(
        vedtaksperiodeIndex: Int,
        vararg utbetalinger: Utbetalingshistorikk.Periode,
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Sykepengehistorikk))
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Foreldrepenger))
        assertFalse(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(ytelser(
            vedtaksperiodeIndex,
            utbetalinger.toList(),
            inntektshistorikk(inntektshistorikk, orgnummer)
        ))
    }

    protected fun håndterPåminnelse(
        vedtaksperiodeIndex: Int,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        person.håndter(påminnelse(vedtaksperiodeIndex, påminnetTilstand, tilstandsendringstidspunkt))
    }

    protected fun håndterUtbetalingsgodkjenning(
        vedtaksperiodeIndex: Int,
        utbetalingGodkjent: Boolean,
        orgnummer: String = ORGNUMMER
    ) {
        assertTrue(inspektør.etterspurteBehov(vedtaksperiodeIndex, Godkjenning))
        person.håndter(utbetalingsgodkjenning(vedtaksperiodeIndex, utbetalingGodkjent, orgnummer))
    }

    protected fun håndterUtbetalt(vedtaksperiodeIndex: Int, status: UtbetalingHendelse1.Oppdragstatus) {
        person.håndter(utbetaling(vedtaksperiodeIndex, status))
    }

    protected fun håndterKansellerUtbetaling(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId()
    ) {
        person.håndter(KansellerUtbetaling(
            AKTØRID,
            UNG_PERSON_FNR_2018,
            orgnummer,
            fagsystemId,
            "Ola Nordmann"
        ))
    }

    private fun utbetaling(
        vedtaksperiodeIndex: Int,
        status: UtbetalingHendelse1.Oppdragstatus,
        orgnummer: String = ORGNUMMER
    ) = utbetaling(
        inspektør.vedtaksperiodeId(vedtaksperiodeIndex),
        status,
        orgnummer
    )

    protected fun utbetaling(
        vedtaksperiodeId: UUID,
        status: UtbetalingHendelse1.Oppdragstatus,
        orgnummer: String = ORGNUMMER
    ) =
        UtbetalingHendelse1(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        )

    protected fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = ORGNUMMER): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    internal fun sentSykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = ORGNUMMER): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map { it.fom }.min()?.plusYears(2)?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    protected fun søknad(
        vararg perioder: Søknad.Søknadsperiode,
        harAndreInntektskilder: Boolean = false,
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        orgnummer: String = ORGNUMMER
    ): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false
        ).apply {
            hendelselogg = this
        }
    }

    private fun søknadArbeidsgiver(vararg perioder: SøknadArbeidsgiver.Søknadsperiode, orgnummer: String): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder)
        ).apply {
            hendelselogg = this
        }
    }

    protected fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjon: Triple<LocalDate?, Double, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        orgnummer: String = ORGNUMMER
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjon.first, refusjon.second, refusjon.third),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelselogg = this
        }
    }

    protected fun vilkårsgrunnlag(
        vedtaksperiodeIndex: Int,
        inntekt: Double,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        egenAnsatt: Boolean = false,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER
    ) = vilkårsgrunnlag(
        inspektør.vedtaksperiodeId(vedtaksperiodeIndex),
        inntekt,
        arbeidsforhold,
        egenAnsatt,
        medlemskapstatus,
        orgnummer
    )

    protected fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Double,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        egenAnsatt: Boolean = false,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                perioder = (1..12).map {
                    YearMonth.of(2017, it) to (orgnummer to inntekt)
                }.groupBy({ it.first }) { it.second }
            ),
            erEgenAnsatt = egenAnsatt,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(if (arbeidsforhold.isEmpty()) listOf(
                Opptjeningvurdering.Arbeidsforhold(orgnummer, 1.januar(2017))
            )
            else arbeidsforhold),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelselogg = this
        }
    }

    private fun påminnelse(
        vedtaksperiodeIndex: Int,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime,
        orgnummer: String = ORGNUMMER
    ): Påminnelse {
        return Påminnelse(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            antallGangerPåminnet = 0,
            tilstand = påminnetTilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun utbetalingshistorikk(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        orgnummer: String = ORGNUMMER
    ): Utbetalingshistorikk {
        return Utbetalingshistorikk(
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(vedtaksperiodeIndex).toString(),
            utbetalinger = utbetalinger,
            inntektshistorikk =
            inntektshistorikk(inntektshistorikk, orgnummer)
        ).apply {
            hendelselogg = this
        }
    }

    private fun ytelser(
        vedtaksperiodeIndex: Int,
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        orgnummer: String = ORGNUMMER
    ) = ytelser(
        inspektør.vedtaksperiodeId(vedtaksperiodeIndex),
        utbetalinger,
        inntektshistorikk,
        foreldrepenger,
        svangerskapspenger,
        orgnummer
    )

    protected fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        orgnummer: String = ORGNUMMER
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        return Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk(inntektshistorikk, orgnummer),
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

    private fun inntektshistorikk(
        inntektshistorikk: List<Inntektsopplysning>?,
        orgnummer: String
    ) = if (inntektshistorikk == null) listOf(
        Inntektsopplysning(
            1.desember(2017),
            INNTEKT,
            orgnummer,
            true
        )
    )
    else inntektshistorikk

    private fun simulering(
        vedtaksperiodeIndex: Int,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) = simulering(
        inspektør.vedtaksperiodeId(vedtaksperiodeIndex),
        simuleringOK,
        orgnummer
    )
    protected fun simulering(
        vedtaksperiodeId: UUID,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) =
        no.nav.helse.hendelser.Simulering(
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = simuleringOK,
            melding = "",
            simuleringResultat = SimuleringResultat(
                totalbeløp = 2000,
                perioder = listOf(
                    SimulertPeriode(
                        periode = Periode(17.januar, 20.januar),
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
                                        periode = Periode(17.januar, 20.januar),
                                        konto = "81549300",
                                        beløp = 2000,
                                        klassekode = Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTEL",
                                        tilbakeføring = false,
                                        sats = Sats(
                                            sats = 1000,
                                            antall = 2,
                                            type = "DAG"
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

    private fun utbetalingsgodkjenning(
        vedtaksperiodeIndex: Int,
        utbetalingGodkjent: Boolean,
        orgnummer: String
    ) = utbetalingsgodkjenning(
        inspektør.vedtaksperiodeId(vedtaksperiodeIndex),
        utbetalingGodkjent,
        orgnummer
    )

    protected fun utbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingGodkjent: Boolean,
        orgnummer: String
    ) = Utbetalingsgodkjenning(
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now()
    ).apply {
        hendelselogg = this
    }

    private val vedtaksperioderIder = mutableMapOf<Pair<String, Int>, UUID>()

    private inner class VedtaksperioderFinder(person: Person): PersonVisitor {
        private lateinit var orgnummer: String
        private var indeks = 0
        init {
            person.accept(this)
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            this.orgnummer = organisasjonsnummer
            indeks = 0
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode
        ) {
            vedtaksperioderIder[orgnummer to indeks] = id
            indeks++
        }
    }

    internal fun String.id(indeks: Int): UUID {
        if (vedtaksperioderIder[this to indeks] == null) VedtaksperioderFinder(person)
        return requireNotNull(vedtaksperioderIder[this to indeks])
    }

}

