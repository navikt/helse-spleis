package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsledig
import no.nav.helse.person.Frilans
import no.nav.helse.person.Person
import no.nav.helse.person.Selvstendig
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.serde.assertPersonEquals
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

@Tag("e2e")
@ExtendWith(DeferredLogging::class)
internal abstract class AbstractDslTest {
    internal companion object {
        @JvmStatic
        protected val a1 = "a1"

        @JvmStatic
        protected val a2 = "a2"

        @JvmStatic
        protected val a3 = "a3"

        @JvmStatic
        protected val frilans = Frilans

        @JvmStatic
        protected val selvstendig = Selvstendig

        @JvmStatic
        protected val arbeidsledig = Arbeidsledig

        @JvmStatic
        @Deprecated("må bruke a1")
        protected val ORGNUMMER = a1

        @JvmStatic
        protected val personInspektør = { person: Person -> PersonInspektør(person) }

        @JvmStatic
        protected val personView = { person: Person -> person.view() }

        @JvmStatic
        protected val agInspektør = { orgnummer: String ->
            { person: Person ->
                TestArbeidsgiverInspektør(
                    person,
                    orgnummer
                )
            }
        }

        @JvmStatic
        protected infix fun String.og(annen: String) = listOf(this, annen)

        @JvmStatic
        protected infix fun List<String>.og(annen: String) = this.plus(annen)
    }

    protected lateinit var jurist: SubsumsjonsListLog
    protected lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson
    @Suppress("unused") private val person get() = testperson.person // Hen brukes av @OpenInSpanner
    private lateinit var deferredLog: DeferredLog

    protected fun Int.vedtaksperiode(orgnummer: String) = orgnummer { vedtaksperiode }
    protected val Int.vedtaksperiode get() = vedtaksperiode(bareÈnArbeidsgiver(a1))

    protected val String.inspektør get() = inspektør(this)
    protected val inspektør: TestArbeidsgiverInspektør get() = bareÈnArbeidsgiver(a1).inspektør

    private val TestPerson.TestArbeidsgiver.testArbeidsgiverAsserter
        get() = TestArbeidsgiverAssertions(
            observatør,
            inspektør,
            testperson.inspiser(personInspektør)
        )
    private val testPersonAsserter
        get() = TestPersonAssertions(
            testperson.inspiser(personInspektør),
            jurist
        )

    protected fun personView() = testperson.view()
    protected fun <INSPEKTØR> inspiser(inspektør: (Person) -> INSPEKTØR) =
        testperson.inspiser(inspektør)

    protected fun inspektør(orgnummer: String) = inspiser(agInspektør(orgnummer))
    protected fun inspektør(vedtaksperiodeId: UUID) =
        inspiser(personInspektør).vedtaksperiode(vedtaksperiodeId).inspektør

    protected fun inspektørForkastet(vedtaksperiodeId: UUID) =
        inspiser(personInspektør).forkastetVedtaksperiode(vedtaksperiodeId).inspektør

    protected operator fun <R> String.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        testperson.arbeidsgiver(this, testblokk)

    protected operator fun <R> List<String>.invoke(testblokk: TestPerson.TestArbeidsgiver.() -> R) =
        forEach { organisasjonsnummer -> organisasjonsnummer { testblokk() } }

    protected fun List<String>.forlengVedtak(periode: Periode, grad: Prosentdel = 100.prosent) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        (first()){
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach {
            it {
                håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalt()
            }
        }
    }

    protected fun List<String>.nyeVedtak(
        periode: Periode, grad: Prosentdel = 100.prosent, inntekt: Inntekt = 20000.månedlig,
        sykepengegrunnlagSkatt: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(
            map { it to inntekt },
            periode.start
        ),
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = map { orgnr ->
            Vilkårsgrunnlag.Arbeidsforhold(
                orgnr,
                LocalDate.EPOCH,
                null,
                Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
            )
        }
    ) {
        forEach {
            it {
                håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
                håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad))
            }
        }
        forEach {
            it {
                håndterInntektsmelding(
                    listOf(periode.start til periode.start.plusDays(15)),
                    beregnetInntekt = inntekt
                )
            }
        }
        (first()){
            håndterVilkårsgrunnlag(
                observatør.sisteVedtaksperiodeId(orgnummer),
                inntektsvurderingForSykepengegrunnlag = sykepengegrunnlagSkatt,
                arbeidsforhold = arbeidsforhold
            )
            håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
            håndterUtbetalt()
        }
        drop(1).forEach {
            it {
                håndterYtelser(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterSimulering(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiodeId(orgnummer))
                håndterUtbetalt()
            }
        }
    }

    protected fun <R> assertSubsumsjoner(block: SubsumsjonInspektør.() -> R): R {
        return testPersonAsserter.assertSubsumsjoner(block)
    }

    protected fun TestPerson.TestArbeidsgiver.assertTilstander(
        id: UUID,
        vararg tilstander: TilstandType,
        orgnummer: String = a1
    ) {
        testArbeidsgiverAsserter.assertTilstander(id, *tilstander)
    }

    protected fun TestPerson.TestArbeidsgiver.assertTilstand(
        id: UUID,
        tilstand: TilstandType,
        orgnummer: String = a1
    ) {
        assertSisteTilstand(id, tilstand)
    }

    protected fun TestPerson.TestArbeidsgiver.assertSisteTilstand(
        id: UUID,
        tilstand: TilstandType,
        orgnummer: String = a1
    ) {
        testArbeidsgiverAsserter.assertSisteTilstand(id, tilstand)
    }

    protected fun TestPerson.TestArbeidsgiver.assertUtbetalingsbeløp(
        vedtaksperiodeId: UUID,
        forventetArbeidsgiverbeløp: Int,
        forventetArbeidsgiverRefusjonsbeløp: Int,
        forventetPersonbeløp: Int = 0,
        subset: Periode? = null
    ) {
        testArbeidsgiverAsserter.assertUtbetalingsbeløp(
            vedtaksperiodeId,
            forventetArbeidsgiverbeløp,
            forventetArbeidsgiverRefusjonsbeløp,
            forventetPersonbeløp,
            subset
        )
    }

    protected fun TestPerson.TestArbeidsgiver.assertForkastetPeriodeTilstander(
        id: UUID,
        vararg tilstand: TilstandType,
        orgnummer: String = a1
    ) {
        testArbeidsgiverAsserter.assertForkastetPeriodeTilstander(id, *tilstand)
    }

    protected fun assertArbeidsgivereISykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        vararg arbeidsgivere: String
    ) =
        testPersonAsserter.assertArbeidsgivereISykepengegrunnlag(skjæringstidspunkt, *arbeidsgivere)

    protected fun TestPerson.TestArbeidsgiver.assertHarHendelseIder(
        vedtaksperiodeId: UUID,
        vararg hendelseIder: UUID
    ) =
        testArbeidsgiverAsserter.assertHarHendelseIder(vedtaksperiodeId, *hendelseIder)

    protected fun TestPerson.TestArbeidsgiver.assertHarIkkeHendelseIder(
        vedtaksperiodeId: UUID,
        vararg hendelseIder: UUID
    ) =
        testArbeidsgiverAsserter.assertHarIkkeHendelseIder(vedtaksperiodeId, *hendelseIder)

    protected fun TestPerson.TestArbeidsgiver.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenFunksjonelleFeil(*filtre)

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonelleFeil() =
        testArbeidsgiverAsserter.assertFunksjonelleFeil()

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonellFeil(
        funksjonellFeil: String,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertFunksjonellFeil(funksjonellFeil, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertFunksjonellFeil(
        funksjonellFeil: Varselkode,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertFunksjonellFeil(
            funksjonellFeil.funksjonellFeilTekst,
            *filtre
        )

    protected fun TestPerson.TestArbeidsgiver.ingenNyeFunksjonelleFeil(block: () -> Unit) =
        testArbeidsgiverAsserter.ingenNyeFunksjonelleFeil(block)

    protected fun TestPerson.TestArbeidsgiver.nyeFunksjonelleFeil(block: () -> Unit) =
        testArbeidsgiverAsserter.nyeFunksjonelleFeil(block)

    protected fun TestPerson.TestArbeidsgiver.assertVarsler(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertVarsler(*filtre)

    protected fun TestPerson.TestArbeidsgiver.assertVarsel(
        warning: String,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertVarsel(warning, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertVarsel(
        kode: Varselkode,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertVarsel(kode, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertIngenVarsel(
        kode: Varselkode,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertIngenVarsel(kode, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        testArbeidsgiverAsserter.assertIngenVarsler(*filtre)

    protected fun TestPerson.TestArbeidsgiver.assertInfo(
        forventet: String,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertInfo(forventet, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertIngenInfo(
        forventet: String,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertIngenInfo(forventet, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertIngenInfoSomInneholder(
        forventet: String,
        vararg filtre: AktivitetsloggFilter
    ) =
        testArbeidsgiverAsserter.assertIngenInfoSomInneholder(forventet, *filtre)

    protected fun TestPerson.TestArbeidsgiver.assertIngenBehov(
        vedtaksperiodeId: UUID,
        behovtype: Aktivitet.Behov.Behovtype
    ) =
        testArbeidsgiverAsserter.assertIngenBehov(vedtaksperiodeId, behovtype)

    protected fun TestPerson.TestArbeidsgiver.assertBehov(
        vedtaksperiodeId: UUID,
        behovtype: Aktivitet.Behov.Behovtype
    ) =
        testArbeidsgiverAsserter.assertBehov(vedtaksperiodeId, behovtype)

    protected fun nyPeriode(
        periode: Periode,
        vararg orgnummer: String,
        grad: Prosentdel = 100.prosent
    ) {
        testperson.nyPeriode(periode, *orgnummer, grad = grad)
    }

    /* alternative metoder fremfor å lage en arbeidsgiver-blokk hver gang */
    protected fun String.håndterSykmelding(
        vararg sykmeldingsperiode: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null
    ) =
        this {
            håndterSykmelding(
                *sykmeldingsperiode,
                sykmeldingSkrevet = sykmeldingSkrevet,
                mottatt = mottatt
            )
        }

    protected fun String.håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        arbeidUtenforNorge: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        sendTilGosys: Boolean = false,
        tilkomneInntekter: List<Søknad.TilkommenInntekt> = emptyList()
    ) =
        this {
            håndterSøknad(
                *perioder,
                andreInntektskilder = andreInntektskilder,
                arbeidUtenforNorge = arbeidUtenforNorge,
                sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
                sykmeldingSkrevet = sykmeldingSkrevet,
                sendTilGosys = sendTilGosys,
                tilkomneInntekter = tilkomneInntekter
            )
        }

    protected fun String.håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        harOpphørAvNaturalytelser: Boolean = false,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        mottatt: LocalDateTime = LocalDateTime.now()
    ) =
        this {
            håndterInntektsmelding(
                arbeidsgiverperioder,
                beregnetInntekt,
                førsteFraværsdag,
                refusjon,
                harOpphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id,
                mottatt = mottatt
            )
        }

    protected fun String.håndterInntektsmeldingPortal(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        vedtaksperiodeId: UUID,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        harOpphørAvNaturalytelser: Boolean = false,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID()
    ) =
        this {
            håndterInntektsmeldingPortal(
                arbeidsgiverperioder,
                beregnetInntekt,
                vedtaksperiodeId,
                refusjon,
                harOpphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id
            )
        }

    protected fun String.håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag? = null,
        inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering? = null,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>? = null
    ) =
        this {
            håndterVilkårsgrunnlag(
                vedtaksperiodeId,
                inntekt,
                medlemskapstatus,
                inntektsvurderingForSykepengegrunnlag,
                inntekterForOpptjeningsvurdering,
                arbeidsforhold
            )
        }

    protected fun String.håndterYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
    ) =
        this {
            håndterYtelser(
                vedtaksperiodeId,
                foreldrepenger,
                svangerskapspenger,
                pleiepenger,
                omsorgspenger,
                opplæringspenger,
                institusjonsoppholdsperioder,
                arbeidsavklaringspenger,
                dagpenger
            )
        }

    protected fun String.håndterSimulering(vedtaksperiodeId: UUID) =
        this { håndterSimulering(vedtaksperiodeId) }

    protected fun String.håndterUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        godkjent: Boolean = true
    ) =
        this { håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent) }

    protected fun String.håndterUtbetalingshistorikkEtterInfotrygdendring(vararg utbetalinger: Infotrygdperiode) =
        this { håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger) }

    protected fun String.håndterUtbetalt(status: Oppdragstatus) =
        this { håndterUtbetalt(status) }

    protected fun String.håndterAnnullering(utbetalingId: UUID) =
        this { håndterAnnullering(utbetalingId) }

    protected fun String.håndterIdentOpphørt(nyttFnr: Personidentifikator, nyAktørId: String) =
        this { håndterIdentOpphørt(nyttFnr) }

    protected fun String.håndterPåminnelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) =
        this { håndterPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt) }

    protected fun String.håndterGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        this { håndterGrunnbeløpsregulering(skjæringstidspunkt) }

    protected fun nullstillTilstandsendringer() = observatør.nullstillTilstandsendringer()
    protected fun String.assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        this { assertTilstander(id, *tilstander) }

    protected fun String.assertSisteTilstand(id: UUID, tilstand: TilstandType) =
        this { assertSisteTilstand(id, tilstand) }

    protected fun String.assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        this { assertIngenFunksjonelleFeil(*filtre) }

    protected fun String.assertVarsler(vararg filtre: AktivitetsloggFilter) =
        this { assertVarsler(*filtre) }

    protected fun String.assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) =
        this { assertVarsel(warning, *filtre) }

    protected fun String.assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        this { assertVarsel(kode, *filtre) }

    protected fun String.assertIngenVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        this { assertIngenVarsel(kode, *filtre) }

    protected fun String.assertFunksjonellFeil(
        kode: Varselkode,
        vararg filtre: AktivitetsloggFilter
    ) =
        this { assertFunksjonellFeil(kode, *filtre) }

    protected fun String.assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        this { assertIngenVarsler(*filtre) }

    protected fun String.nyttVedtak(
        periode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = periode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        sykepengegrunnlagSkatt: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(
            this,
            beregnetInntekt,
            førsteFraværsdag
        )
    ) =
        this {
            nyttVedtak(
                periode,
                grad,
                førsteFraværsdag,
                beregnetInntekt,
                refusjon,
                arbeidsgiverperiode,
                status,
                sykepengegrunnlagSkatt
            )
        }

    protected fun String.tilGodkjenning(
        periode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = periode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        sykepengegrunnlagSkatt: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(
            this,
            beregnetInntekt,
            førsteFraværsdag
        )
    ) =
        this {
            tilGodkjenning(
                periode,
                grad,
                førsteFraværsdag,
                beregnetInntekt,
                refusjon,
                arbeidsgiverperiode,
                status,
                sykepengegrunnlagSkatt
            )
        }

    protected fun migrerUbrukteRefusjonsopplysninger() {
        person.migrerUbrukteRefusjonsopplysninger(Aktivitetslogg())
    }

    protected fun migrerRefusjonsopplysningerPåBehandlinger() {
        person.migrerRefusjonsopplysningerPåBehandlinger(Aktivitetslogg())
    }

    /* dsl for å gå direkte på arbeidsgiver1, eksempelvis i tester for det ikke er andre arbeidsgivere */
    private fun bareÈnArbeidsgiver(orgnr: String): String {
        check(testperson.view().arbeidsgivere.size < 2) {
            "Kan ikke bruke forenklet API for én arbeidsgivere når det finnes flere! Det er ikke trygt og kommer til å lage feil!"
        }
        return orgnr
    }

    protected fun håndterSykmelding(periode: Periode) =
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))

    protected fun håndterSykmelding(
        vararg sykmeldingsperiode: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        mottatt: LocalDateTime? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterSykmelding(
            *sykmeldingsperiode,
            sykmeldingSkrevet = sykmeldingSkrevet,
            mottatt = mottatt
        )

    protected fun håndterSøknad(periode: Periode) =
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent))

    protected fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        arbeidUtenforNorge: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        orgnummer: String = a1,
        sendTilGosys: Boolean = false,
        tilkomneInntekter: List<Søknad.TilkommenInntekt> = emptyList()
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterSøknad(
            *perioder,
            andreInntektskilder = andreInntektskilder,
            arbeidUtenforNorge = arbeidUtenforNorge,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
            sykmeldingSkrevet = sykmeldingSkrevet,
            sendTilGosys = sendTilGosys,
            tilkomneInntekter = tilkomneInntekter
        )

    protected fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        harOpphørAvNaturalytelser: Boolean = false,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        orgnummer: String = a1,
        mottatt: LocalDateTime = LocalDateTime.now()
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterInntektsmelding(
            arbeidsgiverperioder,
            beregnetInntekt,
            førsteFraværsdag,
            refusjon,
            harOpphørAvNaturalytelser,
            begrunnelseForReduksjonEllerIkkeUtbetalt,
            id,
            mottatt = mottatt
        )

    internal fun håndterVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        inntekt: Inntekt = INNTEKT,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag? = null,
        inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering? = null,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>? = null,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterVilkårsgrunnlag(
            vedtaksperiodeId,
            inntekt,
            medlemskapstatus,
            inntektsvurderingForSykepengegrunnlag,
            inntekterForOpptjeningsvurdering,
            arbeidsforhold
        )

    internal fun håndterYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterYtelser(
            vedtaksperiodeId,
            foreldrepenger,
            svangerskapspenger,
            pleiepenger,
            omsorgspenger,
            opplæringspenger,
            institusjonsoppholdsperioder,
            arbeidsavklaringspenger,
            dagpenger
        )

    internal fun håndterSimulering(vedtaksperiodeId: UUID, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).håndterSimulering(vedtaksperiodeId)

    internal fun håndterUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        godkjent: Boolean = true,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterUtbetalingsgodkjenning(vedtaksperiodeId, godkjent)

    internal fun håndterUtbetalt(
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        orgnummer: String = a1
    ) =
        bareÈnArbeidsgiver(orgnummer).håndterUtbetalt(status)

    protected fun håndterAnnullering(utbetalingId: UUID) =
        bareÈnArbeidsgiver(a1).håndterAnnullering(utbetalingId)

    protected fun håndterIdentOpphørt(nyttFnr: Personidentifikator, nyAktørId: String) =
        bareÈnArbeidsgiver(a1).håndterIdentOpphørt(nyttFnr, nyAktørId)

    protected fun håndterPåminnelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
    ) =
        bareÈnArbeidsgiver(a1).håndterPåminnelse(
            vedtaksperiodeId,
            tilstand,
            tilstandsendringstidspunkt
        )

    protected fun håndterGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        bareÈnArbeidsgiver(a1).håndterGrunnbeløpsregulering(skjæringstidspunkt)

    protected fun håndterOverstyrArbeidsforhold(
        skjæringstidspunkt: LocalDate,
        vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
    ) =
        testperson { håndterOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold) }

    protected fun håndterSkjønnsmessigFastsettelse(
        skjæringstidspunkt: LocalDate,
        arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>,
        meldingsreferanseId: UUID = UUID.randomUUID(),
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) =
        testperson {
            håndterSkjønnsmessigFastsettelse(
                skjæringstidspunkt,
                arbeidsgiveropplysninger,
                meldingsreferanseId,
                tidsstempel
            )
        }

    protected fun håndterOverstyrArbeidsgiveropplysninger(
        skjæringstidspunkt: LocalDate,
        arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        testperson {
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt,
                arbeidsgiveropplysninger,
                meldingsreferanseId
            )
        }

    protected fun assertTilstander(id: UUID, vararg tilstander: TilstandType) =
        bareÈnArbeidsgiver(a1).assertTilstander(id, *tilstander)

    protected fun assertSisteTilstand(id: UUID, tilstand: TilstandType, orgnummer: String = a1) =
        bareÈnArbeidsgiver(orgnummer).assertSisteTilstand(id, tilstand)

    protected fun assertIngenFunksjonelleFeil(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertIngenFunksjonelleFeil(*filtre)

    protected fun assertVarsler(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsler(*filtre)

    protected fun assertVarsel(warning: String, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(warning, *filtre)

    protected fun assertVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertVarsel(kode, *filtre)

    protected fun assertIngenVarsel(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertIngenVarsel(kode, *filtre)

    protected fun assertFunksjonellFeil(kode: Varselkode, vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertFunksjonellFeil(kode, *filtre)

    protected fun assertIngenVarsler(vararg filtre: AktivitetsloggFilter) =
        bareÈnArbeidsgiver(a1).assertIngenVarsler(*filtre)

    protected fun assertActivities() {
        val inspektør = inspiser(personInspektør)
        assertTrue(inspektør.aktivitetslogg.aktiviteter.isNotEmpty()) { inspektør.aktivitetslogg.toString() }
    }

    protected fun assertGjenoppbygget(dto: PersonUtDto) {
        val serialisertPerson = dto.tilPersonData().tilSerialisertPerson()
        val gjenoppbyggetPersonViaPersonData =
            Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto())
        val gjenoppbyggetPersonViaPersonDto =
            Person.gjenopprett(EmptyLog, dto.tilPersonData().tilPersonDto())

        val dtoFraPersonViaPersonData = gjenoppbyggetPersonViaPersonData.dto()
        val dtoFraPersonViaPersonDto = gjenoppbyggetPersonViaPersonDto.dto()

        assertEquals(dto, dtoFraPersonViaPersonData)
        assertEquals(dto, dtoFraPersonViaPersonDto)
        assertPersonEquals(testperson.person, gjenoppbyggetPersonViaPersonData)
        assertPersonEquals(testperson.person, gjenoppbyggetPersonViaPersonDto)
    }

    protected fun håndterDødsmelding(dødsdato: LocalDate) {
        testperson.håndterDødsmelding(dødsdato)
    }

    protected fun nyttVedtak(
        vedtaksperiode: Periode,
        grad: Prosentdel = 100.prosent,
        førsteFraværsdag: LocalDate = vedtaksperiode.start,
        beregnetInntekt: Inntekt = INNTEKT,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            beregnetInntekt,
            null,
            emptyList()
        ),
        arbeidsgiverperiode: List<Periode> = emptyList(),
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
        sykepengegrunnlagSkatt: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(
            a1,
            beregnetInntekt,
            førsteFraværsdag
        )
    ) =
        bareÈnArbeidsgiver(a1).nyttVedtak(
            vedtaksperiode,
            grad,
            førsteFraværsdag,
            beregnetInntekt,
            refusjon,
            arbeidsgiverperiode,
            status,
            sykepengegrunnlagSkatt
        )

    protected fun dto() = testperson.dto()

    protected fun medFødselsdato(fødselsdato: LocalDate) {
        testperson = TestPerson(
            observatør = observatør,
            fødselsdato = fødselsdato,
            deferredLog = deferredLog,
            jurist = jurist
        )
    }

    @BeforeEach
    fun setup() {
        jurist = SubsumsjonsListLog()
        observatør = TestObservatør()
        deferredLog = DeferredLog()
        testperson = TestPerson(observatør = observatør, deferredLog = deferredLog, jurist = jurist)
    }

    @AfterEach
    fun verify() {
        testperson.bekreftBehovOppfylt()
    }

    fun dumpLog() = deferredLog.dumpLog()
}
