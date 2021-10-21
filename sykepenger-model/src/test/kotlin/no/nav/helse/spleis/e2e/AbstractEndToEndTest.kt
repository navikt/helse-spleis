package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode
import no.nav.helse.hendelser.Simulering.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.InntektsmeldingDTO
import no.nav.helse.serde.api.v2.SykmeldingDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbstractEndToEndTest : AbstractPersonTest() {

    protected companion object {
        val INNTEKT = 31000.00.månedlig
        val DAGSINNTEKT = INNTEKT.reflection { _, _, _, dagligInt -> dagligInt }
        val MÅNEDLIG_INNTEKT = INNTEKT.reflection { _, månedlig, _, _ -> månedlig.toInt() }
    }

    fun speilApi(hendelser: List<HendelseDTO> = søknadDTOer + sykmeldingDTOer + inntektsmeldingDTOer) = serializePersonForSpeil(person, hendelser)
    protected lateinit var hendelselogg: IAktivitetslogg
    protected var forventetEndringTeller = 0
    private val sykmeldinger = mutableMapOf<UUID, Array<out Sykmeldingsperiode>>()
    private val søknader = mutableMapOf<UUID, Triple<LocalDate, List<Søknad.Inntektskilde>, Array<out Søknad.Søknadsperiode>>>()
    private val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()
    protected val søknadDTOer get() = søknader.map { (id, triple) ->
        val søknadsperiode = Søknad.Søknadsperiode.søknadsperiode(triple.third.toList())!!
        SøknadNavDTO(
            id = id.toString(),
            fom = søknadsperiode.first(),
            tom = søknadsperiode.last(),
            rapportertdato = triple.first.atStartOfDay(),
            sendtNav = triple.first.atStartOfDay()
        )
    }

    private val sykmeldingDTOer get() = sykmeldinger.map { (id, perioder) ->
        val sykmeldingsperiode = Sykmeldingsperiode.periode(perioder.toList())!!
        SykmeldingDTO(
            id = id.toString(),
            fom = sykmeldingsperiode.first(),
            tom = sykmeldingsperiode.last(),
            rapportertdato = sykmeldingsperiode.last().atStartOfDay()
        )
    }

    private val inntektsmeldingDTOer get() = inntektsmeldinger.map { (id, inntektsmeldingGetter) ->
        val im = inntektsmeldingGetter()
        InntektsmeldingDTO(
            id = id.toString(),
            mottattDato = LocalDateTime.now(),
            beregnetInntekt = im.beregnetInntekt.reflection { årlig, _, _, _ -> årlig }
        )
    }

    fun <T> sjekkAt(t: T, init: T.() -> Unit) {
        t.init()
    }

    @BeforeEach
    internal fun abstractSetup() {
        sykmeldinger.clear()
        søknader.clear()
        inntektsmeldinger.clear()
        ikkeBesvarteBehov.clear()
    }

    protected fun assertSisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType, orgnummer: String = ORGNUMMER) {
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last())
    }

    protected fun assertTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER) {
        assertTilstander(vedtaksperiodeIdInnhenter = (indeks + 1).vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
    }

    protected fun assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer), message: String? = null) {
        val id = vedtaksperiodeIdInnhenter(orgnummer)
        assertFalse(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
        assertTrue(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er forkastet med tilstander: ${observatør.tilstandsendringer[id]}" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[id], message)
    }

    protected fun TestArbeidsgiverInspektør.assertTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
        assertTilstander(vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter, tilstander = tilstander, orgnummer = arbeidsgiver.organisasjonsnummer(), inspektør = this)
    }

    protected fun assertForkastetPeriodeTilstander(indeks: Int, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER) {
        assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter = indeks.vedtaksperiode, tilstander = tilstander, orgnummer = orgnummer)
    }

    protected fun assertForkastetPeriodeTilstander(vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType, orgnummer: String = ORGNUMMER, inspektør: TestArbeidsgiverInspektør = inspektør(orgnummer)) {
        assertTrue(inspektør.periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør.periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)])
    }

    protected fun assertForkastetPeriodeTilstander(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, vararg tilstander: TilstandType) {
        assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertEquals(tilstander.asList(), observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)])
    }

    protected fun assertSisteForkastetPeriodeTilstand(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter, tilstand: TilstandType) {
        assertTrue(inspektør(orgnummer).periodeErForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertFalse(inspektør(orgnummer).periodeErIkkeForkastet(vedtaksperiodeIdInnhenter)) { "Perioden er ikke forkastet" }
        assertEquals(tilstand, observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last())
    }

    protected fun assertNoErrors(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun TestArbeidsgiverInspektør.assertHasNoErrors() = assertNoErrors(this)

    protected fun assertNoWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertFalse(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertWarnings(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasWarningsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertWarningTekst(inspektør: TestArbeidsgiverInspektør, vararg warnings: String) {
        val wantedWarnings = warnings.toMutableList()
        val actualWarnings:MutableList<String> = mutableListOf()
        inspektør.personLogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                wantedWarnings.remove(melding)
                actualWarnings.add(melding)
            }
        })
        assertTrue(wantedWarnings.isEmpty(), "forventede warnings mangler: $wantedWarnings, faktiske warnings: $actualWarnings")
    }

    protected fun assertErrorTekst(inspektør: TestArbeidsgiverInspektør, vararg errors: String) {
        val errorList = errors.toMutableList()
        val actualErrors: MutableList<String> = mutableListOf()
        inspektør.personLogg.accept(object : AktivitetsloggVisitor {
            override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
                errorList.remove(melding)
                actualErrors.add(melding)
            }
        })
        assertTrue(errorList.isEmpty(), "har ikke fått errors $errorList, faktiske errors: $actualErrors")
    }

    protected fun assertErrors(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
    }

    protected fun assertActivities(inspektør: TestArbeidsgiverInspektør) {
        assertTrue(inspektør.personLogg.hasActivities(), inspektør.personLogg.toString())
    }

    protected fun håndterSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER,
        fnr: String = UNG_PERSON_FNR_2018,
    ): UUID {
        sykmelding(
            id,
            *sykeperioder,
            sykmeldingSkrevet = sykmeldingSkrevet,
            mottatt = mottatt,
            orgnummer = orgnummer,
            fnr = fnr
        ).håndter(Person::håndter)
        sykmeldinger[id] = sykeperioder
        return id
    }

    protected fun håndterSøknadMedValidering(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        orgnummer: String = ORGNUMMER,
        fnr: String = UNG_PERSON_FNR_2018,
    ) {
        assertIkkeEtterspurt(Søknad::class, Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter, ORGNUMMER)
        håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, orgnummer = orgnummer, fnr = fnr)
    }

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null,
        fnr: String = UNG_PERSON_FNR_2018,
    ): UUID {
        søknad(
            id,
            *perioder,
            andreInntektskilder = andreInntektskilder,
            sendtTilNav = sendtTilNav,
            orgnummer = orgnummer,
            sykmeldingSkrevet = sykmeldingSkrevet,
            fnr = fnr
        ).håndter(Person::håndter)
        søknader[id] = Triple(sendtTilNav, andreInntektskilder, perioder)
        return id
    }

    protected fun håndterSøknadArbeidsgiver(
        vararg sykdomsperioder: SøknadArbeidsgiver.Sykdom,
        arbeidsperiode: SøknadArbeidsgiver.Arbeid? = null,
        orgnummer: String = ORGNUMMER
    ) = søknadArbeidsgiver(*sykdomsperioder, arbeidsperiode = arbeidsperiode, orgnummer = orgnummer).håndter(Person::håndter)

    protected fun håndterInntektsmeldingMedValidering(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        orgnummer: String = ORGNUMMER,
        fnr: String = UNG_PERSON_FNR_2018,
    ): UUID {
        assertIkkeEtterspurt(Inntektsmelding::class, Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter, orgnummer)
        return håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, beregnetInntekt = beregnetInntekt, refusjon, orgnummer = orgnummer, fnr = fnr)
    }

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        orgnummer: String = ORGNUMMER,
        id: UUID = UUID.randomUUID(),
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        fnr: String = UNG_PERSON_FNR_2018,
    ): UUID {
        inntektsmelding(
            id,
            arbeidsgiverperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            orgnummer = orgnummer,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
            arbeidsforholdId = arbeidsforholdId,
            fnr = fnr
        ).håndter(Person::håndter)
        return id
    }

    protected fun håndterInntektsmeldingReplay(
        inntektsmeldingId: UUID,
        vedtaksperiodeId: UUID
    ) {
        val inntektsmeldinggenerator = inntektsmeldinger[inntektsmeldingId] ?: fail { "Fant ikke inntektsmelding med id $inntektsmeldingId" }
        assertTrue(observatør.bedtOmInntektsmeldingReplay(vedtaksperiodeId)) { "Vedtaksperioden har ikke bedt om replay av inntektsmelding" }
        inntektsmeldingReplay(inntektsmeldinggenerator(), vedtaksperiodeId)
            .håndter(Person::håndter)
    }

    protected fun håndterVilkårsgrunnlag(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        inntekt: Inntekt = INNTEKT,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                val skjæringstidspunkt = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)
                skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                    orgnummer inntekt inntekt
                }
            }
        ),
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                ArbeidsgiverInntekt(orgnummer, (0..2).map {
                    val yearMonth = YearMonth.from(inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)).minusMonths(3L - it)
                    ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                        yearMonth = yearMonth,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        inntekt = INNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            )
        ),
        arbeidsforhold: List<Arbeidsforhold> = finnArbeidsgivere().map { Arbeidsforhold(it, LocalDate.EPOCH, null) },
        opptjening: Opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        fnr: String = UNG_PERSON_FNR_2018
    ) {
        fun assertEtterspurt(behovtype: Behovtype) =
            assertEtterspurt(Vilkårsgrunnlag::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

        assertEtterspurt(Behovtype.InntekterForSammenligningsgrunnlag)
        assertEtterspurt(Behovtype.InntekterForSykepengegrunnlag)
        assertEtterspurt(Behovtype.ArbeidsforholdV2)
        assertEtterspurt(Behovtype.Medlemskap)
        vilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            medlemskapstatus = medlemskapstatus,
            orgnummer = orgnummer,
            arbeidsforhold = arbeidsforhold,
            opptjening = opptjening,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            fnr = fnr
        ).håndter(Person::håndter)
    }

    protected fun håndterSimulering(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) {
        assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
        simulering(vedtaksperiodeIdInnhenter, simuleringOK, orgnummer).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingshistorikk(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        vararg utbetalinger: Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        orgnummer: String = ORGNUMMER,
        besvart: LocalDateTime = LocalDateTime.now()
    ) {
        val bedtOmSykepengehistorikk = inspektør(orgnummer).etterspurteBehov(vedtaksperiodeIdInnhenter, Behovtype.Sykepengehistorikk)
        if (bedtOmSykepengehistorikk) assertEtterspurt(Utbetalingshistorikk::class, Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer)
        utbetalingshistorikk(
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk,
            orgnummer = orgnummer,
            besvart = besvart
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingshistorikkUtenValidering(
        vararg utbetalinger: Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        orgnummer: String = ORGNUMMER,
        besvart: LocalDateTime = LocalDateTime.now()
    ) {
        utbetalingshistorikk(
            vedtaksperiodeIdInnhenter = { UUID.randomUUID() },
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk,
            orgnummer = orgnummer,
            besvart = besvart
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingshistorikkForFeriepenger(
        opptjeningsår: Year,
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
        feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
        skalBeregnesManuelt: Boolean = false
    ) {
        utbetalingshistorikkForFeriepenger(
            opptjeningsår = opptjeningsår,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            skalBeregnesManuelt = skalBeregnesManuelt
        ).håndter(Person::håndter)
    }

    protected fun Inntekt.repeat(antall: Int) = (0.until(antall)).map { this }

    private fun finnArbeidsgivere(): List<String> {
        val arbeidsgivere = mutableListOf<String>()
        person.accept(object : PersonVisitor {
            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                arbeidsgivere.add(organisasjonsnummer)
            }
        })

        return arbeidsgivere
    }

    protected fun håndterYtelser(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        vararg utbetalinger: Infotrygdperiode,
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        fnr: String = UNG_PERSON_FNR_2018
    ) {
        fun assertEtterspurt(behovtype: Behovtype) =
            assertEtterspurt(Ytelser::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

        assertEtterspurt(Behovtype.Foreldrepenger)
        assertEtterspurt(Behovtype.Pleiepenger)
        assertEtterspurt(Behovtype.Omsorgspenger)
        assertEtterspurt(Behovtype.Opplæringspenger)
        assertEtterspurt(Behovtype.Arbeidsavklaringspenger)
        assertEtterspurt(Behovtype.Dagpenger)
        assertEtterspurt(Behovtype.Institusjonsopphold)
        assertEtterspurt(Behovtype.Dødsinfo)

        ytelser(
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            utbetalinger = utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk,
            foreldrepenger = foreldrepenger,
            pleiepenger = pleiepenger,
            omsorgspenger = omsorgspenger,
            opplæringspenger = opplæringspenger,
            institusjonsoppholdsperioder = institusjonsoppholdsperioder,
            orgnummer = orgnummer,
            dødsdato = dødsdato,
            statslønn = statslønn,
            arbeidskategorikoder = arbeidskategorikoder,
            arbeidsavklaringspenger = arbeidsavklaringspenger,
            dagpenger = dagpenger,
            besvart = besvart,
            fnr = fnr
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingpåminnelse(
        utbetalingIndeks: Int,
        status: Utbetalingstatus,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) {
        utbetalingpåminnelse(inspektør.utbetalingId(utbetalingIndeks), status, tilstandsendringstidspunkt).håndter(Person::håndter)
    }

    protected fun håndterPåminnelse(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
        orgnummer: String = ORGNUMMER
    ) {
        påminnelse(
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer),
            påminnetTilstand = påminnetTilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            orgnummer = orgnummer
        ).håndter(Person::håndter)
    }

    protected fun håndterUtbetalingsgodkjenning(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        utbetalingGodkjent: Boolean = true,
        orgnummer: String = ORGNUMMER,
        automatiskBehandling: Boolean = false,
        utbetalingId: UUID = UUID.fromString(
            inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()}")
        ),
    ) {
        assertEtterspurt(Utbetalingsgodkjenning::class, Behovtype.Godkjenning, vedtaksperiodeIdInnhenter, orgnummer)
        utbetalingsgodkjenning(vedtaksperiodeIdInnhenter, utbetalingGodkjent, orgnummer, automatiskBehandling, utbetalingId).håndter(Person::håndter)
    }

    protected fun håndterUtbetalt(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        status: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        sendOverførtKvittering: Boolean = true,
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør(orgnummer).fagsystemId(vedtaksperiodeIdInnhenter),
        meldingsreferanseId: UUID = UUID.randomUUID()
    ): UtbetalingHendelse {
        if (sendOverførtKvittering) {
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = orgnummer,
                fagsystemId = fagsystemId,
                utbetalingId = inspektør.sisteBehov(Behovtype.Utbetaling).kontekst()["utbetalingId"]
                    ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Behovtype.Utbetaling).kontekst()}"),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            ).håndter(Person::håndter)
        }
        return utbetaling(
            fagsystemId = fagsystemId,
            status = status,
            orgnummer = orgnummer,
            meldingsreferanseId = meldingsreferanseId
        ).håndter(Person::håndter)
    }

    protected fun håndterGrunnbeløpsregulering(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
        gyldighetsdato: LocalDate
    ) {
        Grunnbeløpsregulering(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            gyldighetsdato = gyldighetsdato,
            fagsystemId = fagsystemId,
            aktivitetslogg = Aktivitetslogg()
        ).håndter(Person::håndter)
    }

    protected fun håndterAnnullerUtbetaling(
        orgnummer: String = ORGNUMMER,
        fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
        opprettet: LocalDateTime = LocalDateTime.now()
    ) {
        AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            fagsystemId = fagsystemId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = opprettet
        ).håndter(Person::håndter)
    }

    protected fun håndterOverstyrInntekt(
        inntekt: Inntekt = 31000.månedlig,
        orgnummer: String = ORGNUMMER,
        skjæringstidspunkt: LocalDate
    ) {
        OverstyrInntekt(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            organisasjonsnummer = orgnummer,
            inntekt = inntekt,
            skjæringstidspunkt = skjæringstidspunkt
        ).håndter(Person::håndter)
    }

    protected fun håndterOverstyrTidslinje(
        overstyringsdager: List<ManuellOverskrivingDag> = listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag, 100)),
        orgnummer: String = ORGNUMMER
    ) {
        OverstyrTidslinje(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            organisasjonsnummer = orgnummer,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        ).håndter(Person::håndter)
    }

    protected fun utbetaling(
        fagsystemId: String,
        status: UtbetalingHendelse.Oppdragstatus,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = inspektør.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId"),
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }

    protected fun håndterFeriepengerUtbetalt(
        status: UtbetalingHendelse.Oppdragstatus = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        orgnummer: String = ORGNUMMER,
        fagsystemId: String,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ): UtbetalingHendelse {
        return feriepengeutbetaling(
            fagsystemId = fagsystemId,
            status = status,
            orgnummer = orgnummer,
            meldingsreferanseId = meldingsreferanseId
        ).håndter(Person::håndter)
    }

    private fun feriepengeutbetaling(
        fagsystemId: String,
        status: UtbetalingHendelse.Oppdragstatus,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = inspektør.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId"),
            status = status,
            melding = "hey",
            avstemmingsnøkkel = 654321L,
            overføringstidspunkt = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }

    protected fun sykmelding(
        id: UUID,
        vararg sykeperioder: Sykmeldingsperiode,
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        fnr: String = UNG_PERSON_FNR_2018,
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = fnr,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            sykmeldingSkrevet = sykmeldingSkrevet ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = mottatt ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
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
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.plusMonths(7)?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    protected fun søknad(
        id: UUID,
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime? = null,
        fnr: String = UNG_PERSON_FNR_2018,
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = fnr,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay()
        ).apply {
            hendelselogg = this
        }
    }

    private fun søknadArbeidsgiver(
        vararg perioder: SøknadArbeidsgiver.Sykdom,
        arbeidsperiode: SøknadArbeidsgiver.Arbeid? = null,
        orgnummer: String
    ): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykdomsperioder = listOf(*perioder),
            arbeidsperiode = arbeidsperiode?.let(::listOf) ?: emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektsmeldingReplay(
        inntektsmelding: Inntektsmelding,
        vedtaksperiodeId: UUID
    ): InntektsmeldingReplay {
        return InntektsmeldingReplay(
            wrapped = inntektsmelding,
            vedtaksperiodeId = vedtaksperiodeId
        ).apply {
            hendelselogg = this
        }
    }

    protected fun inntektsmelding(
        id: UUID,
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        orgnummer: String = ORGNUMMER,
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        fnr: String = UNG_PERSON_FNR_2018,
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = refusjon,
                orgnummer = orgnummer,
                fødselsnummer = fnr,
                aktørId = AKTØRID,
                førsteFraværsdag = førsteFraværsdag,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arbeidsforholdId = arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt = null,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                mottatt = LocalDateTime.now()
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        EtterspurtBehov.fjern(ikkeBesvarteBehov, orgnummer, Behovtype.Sykepengehistorikk)
        return inntektsmeldinggenerator().apply { hendelselogg = this }
    }

    protected fun vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        arbeidsforhold: List<Arbeidsforhold> = listOf(Arbeidsforhold(orgnummer, 1.januar(2017))),
        opptjening: Opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        inntektsvurdering: Inntektsvurdering,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
        fnr: String = UNG_PERSON_FNR_2018
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            aktørId = AKTØRID,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = opptjening,
            arbeidsforhold = arbeidsforhold
        ).apply {
            hendelselogg = this
        }
    }

    private fun utbetalingpåminnelse(
        utbetalingId: UUID,
        status: Utbetalingstatus,
        tilstandsendringstidspunkt: LocalDateTime,
        orgnummer: String = ORGNUMMER
    ): Utbetalingpåminnelse {
        return Utbetalingpåminnelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            utbetalingId = utbetalingId,
            antallGangerPåminnet = 0,
            status = status,
            endringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun påminnelse(
        vedtaksperiodeId: UUID,
        påminnetTilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime,
        orgnummer: String = ORGNUMMER
    ): Påminnelse {
        return Påminnelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = påminnetTilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now()
        )
    }

    private fun utbetalingshistorikk(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        orgnummer: String = ORGNUMMER,
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now(),
    ): Utbetalingshistorikk {
        return Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            arbeidskategorikoder = emptyMap(),
            harStatslønn = harStatslønn,
            perioder = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            ugyldigePerioder = emptyList(),
            besvart = besvart
        ).apply {
            hendelselogg = this
        }
    }

    private fun utbetalingshistorikkForFeriepenger(
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
        feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
        arbeidskategorikoder: UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(listOf(KodePeriode(LocalDate.MIN til LocalDate.MAX, Arbeidstaker))),
        opptjeningsår: Year = Year.of(2017),
        skalBeregnesManuelt: Boolean
    ): UtbetalingshistorikkForFeriepenger {
        return UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = skalBeregnesManuelt
        ).apply {
            hendelselogg = this
        }
    }

    protected fun ytelser(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        fnr: String = UNG_PERSON_FNR_2018
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()

        val bedtOmSykepengehistorikk = erEtterspurt(Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer, AVVENTER_HISTORIKK)
            || erEtterspurt(Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer, AVVENTER_HISTORIKK_REVURDERING)
        if (bedtOmSykepengehistorikk) assertEtterspurt(Ytelser::class, Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer)
        val harSpesifisertSykepengehistorikk = utbetalinger.isNotEmpty() || arbeidskategorikoder.isNotEmpty()

        if (!bedtOmSykepengehistorikk && harSpesifisertSykepengehistorikk) {
            fail(
                "Vedtaksperiode ${vedtaksperiodeIdInnhenter(orgnummer)} har ikke bedt om Sykepengehistorikk" +
                    "\nfordi den har gjenbrukt Infotrygdhistorikk-cache." +
                    "\nTrenger ikke sende inn utbetalinger og inntektsopplysninger da." +
                    "\nEnten ta bort overflødig historikk, eller sett 'besvart'-tidspunktet tilbake i tid " +
                    "på forrige Ytelser-innsending" +
                    "\n\n${inspektør.personLogg}"
            )
        }

        val utbetalingshistorikk = if (!bedtOmSykepengehistorikk)
            null
        else
            Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = AKTØRID,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
                arbeidskategorikoder = arbeidskategorikoder,
                harStatslønn = statslønn,
                perioder = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                ugyldigePerioder = emptyList(),
                besvart = besvart
            )
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepenger,
                svangerskapsytelse = svangerskapspenger,
                aktivitetslogg = aktivitetslogg
            ),
            pleiepenger = Pleiepenger(
                perioder = pleiepenger,
                aktivitetslogg = aktivitetslogg
            ),
            omsorgspenger = Omsorgspenger(
                perioder = omsorgspenger,
                aktivitetslogg = aktivitetslogg
            ),
            opplæringspenger = Opplæringspenger(
                perioder = opplæringspenger,
                aktivitetslogg = aktivitetslogg
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = institusjonsoppholdsperioder,
                aktivitetslogg = aktivitetslogg
            ),
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger),
            aktivitetslogg = aktivitetslogg
        ).apply {
            hendelselogg = this
        }
    }

    protected fun tilGodkjenning(fom: LocalDate, tom: LocalDate, vararg organisasjonsnummere: String) {
        require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
        organisasjonsnummere.forEach {
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it)
        }
        organisasjonsnummere.forEach {
            håndterSøknad(Sykdom(fom, tom, 100.prosent), orgnummer = it)

        }
        organisasjonsnummere.forEach {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
                beregnetInntekt = 20000.månedlig,
                orgnummer = it
            )
        }
        val (første, _) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

        første.let { organisasjonsnummer ->
            håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterVilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = organisasjonsnummer,
                inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                    fom.minusYears(1) til fom.minusMonths(1) inntekter {
                        organisasjonsnummere.forEach {
                            it inntekt 20000.månedlig
                        }
                    }
                })
            )
            håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        }
    }

    protected fun nyeVedtak(
        fom: LocalDate,
        tom: LocalDate,
        vararg organisasjonsnummere: String,
        inntekterBlock: Inntektperioder.() -> Unit = {
            organisasjonsnummere.forEach {
                lagInntektperioder(it, fom, 20000.månedlig)
            }
        }
    ) {
        require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
        organisasjonsnummere.forEach {
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it)
        }
        organisasjonsnummere.forEach {
            håndterSøknad(Sykdom(fom, tom, 100.prosent), orgnummer = it)

        }
        organisasjonsnummere.forEach {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
                beregnetInntekt = 20000.månedlig,
                orgnummer = it
            )
        }
        val (første, resten) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

        første.let { organisasjonsnummer ->
            håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterVilkårsgrunnlag(
                vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
                orgnummer = organisasjonsnummer,
                inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                    inntekterBlock()
                })
            )
            håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        }

        resten.forEach { organisasjonsnummer ->
            håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
            håndterUtbetalt(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        }
    }

    protected fun forlengVedtak(fom: LocalDate, tom: LocalDate, vararg organisasjonsnummere: String) {
        require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
        organisasjonsnummere.forEach { håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it) }
        organisasjonsnummere.forEach { håndterSøknad(Sykdom(fom, tom, 100.prosent), orgnummer = it) }
        organisasjonsnummere.forEach { håndterYtelser(vedtaksperiodeIdInnhenter = { _ -> observatør.sisteVedtaksperiode(it) }, orgnummer = it) }

        val (første, resten) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

        første.let { organisasjonsnummer ->
            val vedtaksperiodeIdInnhenter: IdInnhenter = { observatør.sisteVedtaksperiode(organisasjonsnummer) }
            håndterYtelser(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterSimulering(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterUtbetalingsgodkjenning(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterUtbetalt(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
        }

        resten.forEach { organisasjonsnummer ->
            val vedtaksperiodeIdInnhenter: IdInnhenter = { observatør.sisteVedtaksperiode(organisasjonsnummer) }
            håndterYtelser(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterSimulering(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterUtbetalingsgodkjenning(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
            håndterUtbetalt(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
        }
    }


    protected fun nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = fom,
        orgnummer: String = ORGNUMMER,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
    ) {
        val id = tilGodkjent(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
        håndterUtbetalt({ id }, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, orgnummer = orgnummer)
    }

    protected fun tilGodkjent(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
    ): UUID {
        val id = tilGodkjenning(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
        håndterUtbetalingsgodkjenning({ id }, true, orgnummer = orgnummer)
        return id
    }

    protected fun tilGodkjenning(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
    ): UUID {
        val id = tilYtelser(fom, tom, grad, førsteFraværsdag, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
        håndterSimulering({ id }, orgnummer = orgnummer)
        return id
    }

    protected fun Inntektperioder.lagInntektperioder(orgnummer: String = ORGNUMMER, fom: LocalDate, inntekt: Inntekt = INNTEKT) =
        fom.minusYears(1) til fom.minusMonths(1) inntekter {
            orgnummer inntekt inntekt
        }

    protected fun tilYtelser(
        fom: LocalDate,
        tom: LocalDate,
        grad: Prosentdel,
        førsteFraværsdag: LocalDate,
        orgnummer: String = ORGNUMMER,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = inntektperioderForSykepengegrunnlag {
                fom.minusMonths(3) til fom.minusMonths(1) inntekter {
                    orgnummer inntekt INNTEKT
                }
            }
        )
    ): UUID {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
        håndterInntektsmeldingMedValidering(
            id,
            listOf(Periode(fom, fom.plusDays(15))),
            førsteFraværsdag = førsteFraværsdag,
            orgnummer = orgnummer,
            refusjon = refusjon
        )
        håndterSøknadMedValidering(id, Sykdom(fom, tom, grad), orgnummer = orgnummer)
        håndterYtelser(id, orgnummer = orgnummer)
        håndterVilkårsgrunnlag(
            id,
            INNTEKT,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag(inntekterBlock)
            ),
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag
        )
        håndterYtelser(id, orgnummer = orgnummer)
        return id(orgnummer)
    }

    protected fun forlengVedtak(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, orgnummer: String = ORGNUMMER, skalSimuleres: Boolean = true) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
        håndterSøknadMedValidering(id, Sykdom(fom, tom, grad), orgnummer = orgnummer)
        håndterYtelser(id, orgnummer = orgnummer)
        if (skalSimuleres) håndterSimulering(id, orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(id, true, orgnummer = orgnummer)
        håndterUtbetalt(id, status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT, orgnummer = orgnummer)
    }

    protected fun forlengPeriode(fom: LocalDate, tom: LocalDate, grad: Prosentdel = 100.prosent, orgnummer: String = ORGNUMMER) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), orgnummer = orgnummer)
        val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
        håndterSøknadMedValidering(id, Sykdom(fom, tom, grad), orgnummer = orgnummer)
    }

    protected fun simulering(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER
    ) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
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

    protected fun utbetalingsgodkjenning(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        utbetalingGodkjent: Boolean,
        orgnummer: String,
        automatiskBehandling: Boolean,
        utbetalingId: UUID = UUID.fromString(
            inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()}")
        ),
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    ).apply {
        hendelselogg = this
    }

    protected fun assertInntektForDato(forventetInntekt: Inntekt?, dato: LocalDate, inspektør: TestArbeidsgiverInspektør) {
        assertEquals(forventetInntekt, inspektør.inntektInspektør.grunnlagForSykepengegrunnlag(dato))
    }

    protected fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        hendelselogg = this
        person.håndter(this)
        ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(behov())
        return this
    }

    private fun erEtterspurt(type: Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String, tilstand: TilstandType): Boolean {
        return EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer, tilstand) != null
    }

    private fun <T : ArbeidstakerHendelse> assertEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
        assertTrue(ikkeBesvarteBehov.remove(etterspurtBehov)) {
            "Forventer at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last()
            }.\nAktivitetsloggen:\n${inspektør.personLogg}"
        }
    }

    private fun <T : ArbeidstakerHendelse> assertIkkeEtterspurt(løsning: KClass<T>, type: Behovtype, vedtaksperiodeIdInnhenter: IdInnhenter, orgnummer: String) {
        val etterspurtBehov = EtterspurtBehov.finnEtterspurtBehov(ikkeBesvarteBehov, type, vedtaksperiodeIdInnhenter, orgnummer)
        assertFalse(etterspurtBehov in ikkeBesvarteBehov) {
            "Forventer ikke at $type skal være etterspurt før ${løsning.simpleName} håndteres. Perioden er i ${
                observatør.tilstandsendringer[vedtaksperiodeIdInnhenter(orgnummer)]?.last()
            }"
        }
    }

    protected fun assertAlleBehovBesvart() {
        assertTrue(ikkeBesvarteBehov.isEmpty()) {
            "Ikke alle behov er besvart. Mangler fortsatt svar på behovene $ikkeBesvarteBehov"
        }
    }

    protected fun grunnlag(
        orgnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekt>
    ) = lagMånedsinntekter(orgnummer, skjæringstidspunkt, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sykepengegrunnlag)

    protected fun sammenligningsgrunnlag(
        orgnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekt>
    ) = lagMånedsinntekter(orgnummer, skjæringstidspunkt, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sammenligningsgrunnlag)

    private fun lagMånedsinntekter(
        orgnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekt>,
        creator: InntektCreator
    ) = ArbeidsgiverInntekt(
        orgnummer, inntekter.mapIndexed { index, inntekt ->
            val sluttMnd = YearMonth.from(skjæringstidspunkt)
            creator(
                sluttMnd.minusMonths((inntekter.size - index).toLong()),
                inntekt,
                ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                "Juidy inntekt",
                "Juidy fordel"
            )
        }
    )

    protected fun finnSkjæringstidspunkt(orgnummer: String, vedtaksperiodeIdInnhenter: IdInnhenter) = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)

    protected fun assertHendelseIder(
        vararg hendelseIder: UUID,
        orgnummer: String,
        vedtaksperiodeIndeks: Int = 2,
    ) {
        assertEquals(
            hendelseIder.toSet(),
            inspektør(orgnummer).hendelseIder(vedtaksperiodeIndeks.vedtaksperiode)
        )
    }

    protected fun assertTilstand(
        orgnummer: String,
        tilstand: TilstandType,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(tilstand, inspektør(orgnummer).sisteTilstand(vedtaksperiodeIndeks.vedtaksperiode)) {
            inspektør.personLogg.toString()
        }
    }

    protected fun assertInntektskilde(
        orgnummer: String,
        inntektskilde: Inntektskilde,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(inntektskilde, inspektør(orgnummer).inntektskilde(vedtaksperiodeIndeks.vedtaksperiode))
    }

    protected fun prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        gapPeriode(periode, orgnummer, sykedagstelling)
        historikk(orgnummer, sykedagstelling)
        betale(orgnummer)
    }

    protected fun gapPeriode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        nyPeriode(periode, orgnummer)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            førsteFraværsdag = periode.start,
            orgnummer = orgnummer
        )
        historikk(orgnummer, sykedagstelling)
        person.håndter(vilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        orgnummer inntekt INNTEKT
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        orgnummer inntekt INNTEKT
                    }
                }
            )
        ))
    }

    protected fun nyPeriode(periode: Periode, orgnummer: String) {
        person.håndter(
            sykmelding(
                UUID.randomUUID(),
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            søknad(
                UUID.randomUUID(),
                Sykdom(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
    }

    protected fun historikk(orgnummer: String, sykedagstelling: Int = 0) {
        person.håndter(
            ytelser(
                1.vedtaksperiode,
                utbetalinger = utbetalinger(sykedagstelling, orgnummer),
                orgnummer = orgnummer
            )
        )
    }

    protected fun betale(orgnummer: String) {
        person.håndter(simulering(1.vedtaksperiode, orgnummer = orgnummer))
        person.håndter(
            utbetalingsgodkjenning(
                1.vedtaksperiode,
                true,
                orgnummer = orgnummer,
                automatiskBehandling = false
            )
        )
        person.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = orgnummer,
                fagsystemId = inspektør(orgnummer).fagsystemId(1.vedtaksperiode),
                utbetalingId = hendelselogg.behov().first { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        person.håndter(
            utbetaling(
                inspektør(orgnummer).fagsystemId(1.vedtaksperiode),
                status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
                orgnummer = orgnummer
            )
        )
    }

    private fun utbetalinger(dagTeller: Int, orgnummer: String): List<ArbeidsgiverUtbetalingsperiode> {
        if (dagTeller == 0) return emptyList()
        val førsteDato = 2.desember(2017).minusDays(
            (
                (dagTeller / 5 * 7) + dagTeller % 5
                ).toLong()
        )
        return listOf(ArbeidsgiverUtbetalingsperiode(orgnummer, førsteDato, 1.desember(2017), 100.prosent, 100.daglig))
    }

    private val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

    private class EtterspurtBehov(
        private val type: Behovtype,
        private val tilstand: TilstandType,
        private val orgnummer: String,
        private val vedtaksperiodeId: UUID
    ) {
        companion object {
            internal fun fjern(liste: MutableList<EtterspurtBehov>, orgnummer: String, type: Behovtype) {
                liste.removeIf { it.orgnummer == orgnummer && it.type == type }
            }

            internal fun finnEtterspurteBehov(behovsliste: List<Aktivitetslogg.Aktivitet.Behov>) =
                behovsliste
                    .filter { "tilstand" in it.kontekst() }
                    .filter { "organisasjonsnummer" in it.kontekst() }
                    .filter { "vedtaksperiodeId" in it.kontekst() }
                    .map {
                        EtterspurtBehov(
                            type = it.type,
                            tilstand = enumValueOf(it.kontekst()["tilstand"] as String),
                            orgnummer = it.kontekst()["organisasjonsnummer"] as String,
                            vedtaksperiodeId = UUID.fromString(it.kontekst()["vedtaksperiodeId"] as String)
                        )
                    }

            internal fun finnEtterspurtBehov(
                ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
                type: Behovtype,
                vedtaksperiodeIdInnhenter: IdInnhenter,
                orgnummer: String
            ) =
                ikkeBesvarteBehov.firstOrNull { it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeIdInnhenter(orgnummer) }

            internal fun finnEtterspurtBehov(
                ikkeBesvarteBehov: MutableList<EtterspurtBehov>,
                type: Behovtype,
                vedtaksperiodeIdInnhenter: IdInnhenter,
                orgnummer: String,
                tilstand: TilstandType
            ) =
                ikkeBesvarteBehov.firstOrNull {
                    it.type == type && it.orgnummer == orgnummer && it.vedtaksperiodeId == vedtaksperiodeIdInnhenter(orgnummer) && it.tilstand == tilstand
                }
        }

        override fun toString() = "$type ($tilstand)"
    }

    fun assertWarn(message: String, aktivitetslogg: Aktivitetslogg) {
        var fant = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
                if (message == melding) fant = true
            }
        })
        assertTrue(fant)
    }

    protected fun tellArbeidsforholdhistorikkinnslag(orgnummer: String? = null): MutableList<UUID> {
        val arbeidsforholdIder = mutableListOf<UUID>()
        var erIRiktigArbeidsgiver = true
        person.accept(object : PersonVisitor {

            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                erIRiktigArbeidsgiver = orgnummer == null || orgnummer == organisasjonsnummer
            }

            override fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
                if (erIRiktigArbeidsgiver) {
                    arbeidsforholdIder.add(id)
                }
            }
        })

        return arbeidsforholdIder
    }

    protected fun tellArbeidsforholdINyesteHistorikkInnslag(orgnummer: String): Int {
        var antall = 0
        var erIRiktigArbeidsgiver = true
        var erIFørsteHistorikkinnslag = true

        person.accept(object : PersonVisitor {

            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                erIRiktigArbeidsgiver = orgnummer == organisasjonsnummer
            }

            override fun visitArbeidsforhold(orgnummer: String, fom: LocalDate, tom: LocalDate?) {
                if (erIRiktigArbeidsgiver && erIFørsteHistorikkinnslag) antall += 1
            }

            override fun postVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
                if (erIRiktigArbeidsgiver) erIFørsteHistorikkinnslag = false
            }
        })

        return antall
    }

    protected fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
    protected fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    protected fun manuellSykedag(dato: LocalDate, grad: Int = 100) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, grad)
    protected fun håndterOverstyringSykedag(periode: Periode) = håndterOverstyrTidslinje(periode.map { manuellSykedag(it) })
    protected fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)

    inline fun <reified R : Utbetalingsdag> assertUtbetalingsdag(dag: Utbetalingsdag, expectedDagtype: KClass<R>, expectedTotalgrad: Double = 100.0) {
        dag.let {
            it.økonomi.medData { _, _, _, _, totalGrad, _, _, _, _ ->
                assertEquals(expectedTotalgrad, totalGrad)
            }
            assertEquals(it::class, expectedDagtype)
        }
    }

    protected fun TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(førsteAG: Boolean = true): Array<out TilstandType> =
        if (førsteAG) arrayOf(
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        ) else arrayOf(
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

    protected fun TIL_AVSLUTTET_FORLENGELSE(førsteAG: Boolean = true) =
        if (førsteAG) arrayOf(
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        ) else arrayOf(
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )
}

infix fun <T> T?.er(expected: T?) =
    assertEquals(expected, this)

infix fun <T> T?.skalVære(expected: T?) =
    if (expected == null) {
        this == null
    } else {
        expected == this
    }

infix fun Boolean.ellers(message: String) {
    if (!this) fail(message)
}
