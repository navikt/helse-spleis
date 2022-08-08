package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Dødsinfo
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.lagInntektperioder
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.fail

internal class TestPerson(
    private val observatør: PersonObserver,
    private val aktørId: String = AKTØRID,
    private val fødselsnummer: Fødselsnummer = UNG_PERSON_FNR_2018,
    private val fødselsdato: LocalDate = UNG_PERSON_FDATO_2018,
    jurist: MaskinellJurist = MaskinellJurist()
) {
    internal companion object {
        private val fnrformatter = DateTimeFormatter.ofPattern("ddMMyy")
        internal val UNG_PERSON_FDATO_2018 = 12.februar(1992)
        internal val UNG_PERSON_FNR_2018: Fødselsnummer = "${UNG_PERSON_FDATO_2018.format(fnrformatter)}40045".somFødselsnummer()
        internal const val AKTØRID = "42"

        internal val INNTEKT = 31000.00.månedlig

        internal operator fun <R> String.invoke(testPerson: TestPerson, testblokk: TestArbeidsgiver.() -> R) =
            testPerson.arbeidsgiver(this, testblokk)
    }

    private lateinit var forrigeHendelse: IAktivitetslogg

    private val behovsamler = Behovsamler()
    private val vedtaksperiodesamler = Vedtaksperiodesamler()

    private val person = Person(aktørId, fødselsnummer, fødselsdato.alder, jurist).also {
        it.addObserver(vedtaksperiodesamler)
        it.addObserver(behovsamler)
        it.addObserver(observatør)
    }
    private val arbeidsgivere = mutableMapOf<String, TestArbeidsgiver>()

    internal fun <INSPEKTØR : PersonVisitor> inspiser(inspektør: (Person) -> INSPEKTØR) = inspektør(person)

    internal fun arbeidsgiver(orgnummer: String) =
        arbeidsgivere.getOrPut(orgnummer) { TestArbeidsgiver(orgnummer) }

    internal fun <R> arbeidsgiver(orgnummer: String, block: TestArbeidsgiver.() -> R) =
        arbeidsgiver(orgnummer)(block)

    internal operator fun <R> String.invoke(testblokk: TestArbeidsgiver.() -> R) =
        arbeidsgiver(this, testblokk)

    private fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        forrigeHendelse = this
        person.håndter(this)
        behovsamler.registrerBehov(forrigeHendelse)
        return this
    }

    internal fun forkastAlle() {
        person.invaliderAllePerioder(forrigeHendelse, null)
    }

    internal fun bekreftBehovOppfylt() {
        behovsamler.bekreftBehovOppfylt()
    }

    inner class TestArbeidsgiver(internal val orgnummer: String) {
        private val fabrikk = Hendelsefabrikk(aktørId, fødselsnummer, orgnummer, fødselsdato)

        internal val inspektør get() = TestArbeidsgiverInspektør(person, orgnummer)

        internal val Int.vedtaksperiode get() = vedtaksperiodesamler.vedtaksperiodeId(orgnummer, this - 1)

        internal fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode,
                                       sykmeldingSkrevet: LocalDateTime? = null,
                                       mottatt: LocalDateTime? = null,) =
            fabrikk.lagSykmelding(*sykmeldingsperiode, sykmeldingSkrevet = sykmeldingSkrevet, mottatt = mottatt).håndter(Person::håndter)

        internal fun håndterSøknad(
            vararg perioder: Søknad.Søknadsperiode,
            andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
            sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
            sykmeldingSkrevet: LocalDateTime? = null,
            orgnummer: String = ""
        ) =
            vedtaksperiodesamler.fangVedtaksperiode {
                fabrikk.lagSøknad(*perioder, andreInntektskilder = andreInntektskilder, sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver, sykmeldingSkrevet = sykmeldingSkrevet).håndter(Person::håndter)
            }

        internal fun håndterInntektsmelding(
            arbeidsgiverperioder: List<Periode>,
            beregnetInntekt: Inntekt = INNTEKT,
            førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            harOpphørAvNaturalytelser: Boolean = false,
            arbeidsforholdId: String? = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
            id: UUID = UUID.randomUUID(),
            orgnummer: String = ""
        ): UUID {
            fabrikk.lagInntektsmelding(
                arbeidsgiverperioder,
                beregnetInntekt,
                førsteFraværsdag,
                refusjon,
                harOpphørAvNaturalytelser,
                arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id
            ).håndter(Person::håndter)
            return id
        }

        internal fun håndterInntektsmeldingReplay(inntektsmeldingId: UUID, vedtaksperiodeId: UUID) {
            behovsamler.bekreftOgKvitterReplay(vedtaksperiodeId)
            fabrikk.lagInntektsmeldingReplay(inntektsmeldingId, vedtaksperiodeId)
                .håndter(Person::håndter)
        }

        internal fun håndterUtbetalingshistorikk(
            vedtaksperiodeId: UUID,
            utbetalinger: List<Infotrygdperiode> = listOf(),
            inntektshistorikk: List<Inntektsopplysning> = emptyList(),
            harStatslønn: Boolean = false,
            besvart: LocalDateTime = LocalDateTime.now()
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Sykepengehistorikk)
            fabrikk.lagUtbetalingshistorikk(vedtaksperiodeId, utbetalinger, inntektshistorikk, harStatslønn, besvart)
                .håndter(Person::håndter)
        }

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            inntekt: Inntekt = INNTEKT,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntektsvurdering: Inntektsvurdering? = null,
            inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag? = null,
            arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>? = null,
            orgnummer: String = "aa"
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, InntekterForSammenligningsgrunnlag, InntekterForSykepengegrunnlag, ArbeidsforholdV2, Medlemskap)
            fabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId,
                medlemskapstatus,
                arbeidsforhold ?: arbeidsgivere.map { (orgnr, _) -> Vilkårsgrunnlag.Arbeidsforhold(orgnr, LocalDate.EPOCH, null) },
                inntektsvurdering ?: lagStandardSammenligningsgrunnlag(
                    this.orgnummer,
                    inntekt,
                    inspektør.skjæringstidspunkt(vedtaksperiodeId)
                ),
                inntektsvurderingForSykepengegrunnlag ?: lagStandardSykepengegrunnlag(this.orgnummer, inntekt, inspektør.skjæringstidspunkt(vedtaksperiodeId))
            )
                .håndter(Person::håndter)
        }

        internal fun håndterYtelser(
            vedtaksperiodeId: UUID,
            utbetalinger: List<Infotrygdperiode> = listOf(),
            inntektshistorikk: List<Inntektsopplysning> = emptyList(),
            foreldrepenger: Periode? = null,
            svangerskapspenger: Periode? = null,
            pleiepenger: List<Periode> = emptyList(),
            omsorgspenger: List<Periode> = emptyList(),
            opplæringspenger: List<Periode> = emptyList(),
            institusjonsoppholdsperioder: List<no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
            dødsdato: LocalDate? = null,
            statslønn: Boolean = false,
            arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
            arbeidsavklaringspenger: List<Periode> = emptyList(),
            dagpenger: List<Periode> = emptyList(),
            besvart: LocalDateTime = LocalDateTime.now(),
            orgnummer: String = "aa"
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Dagpenger, Arbeidsavklaringspenger, Dødsinfo, Institusjonsopphold, Opplæringspenger, Pleiepenger, Omsorgspenger, Foreldrepenger)

            val hendelse = if (!behovsamler.harBehov(vedtaksperiodeId, Sykepengehistorikk)) {
                fabrikk.lagYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, arbeidsavklaringspenger, dagpenger)
            } else {
                fabrikk.lagYtelser(vedtaksperiodeId, utbetalinger, inntektshistorikk, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, statslønn, arbeidskategorikoder, arbeidsavklaringspenger, dagpenger, besvart)
            }
            hendelse.håndter(Person::håndter)
        }

        internal fun håndterSimulering(vedtaksperiodeId: UUID, simuleringOK: Boolean = true) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Simulering)
            behovsamler.detaljerFor(vedtaksperiodeId, Simulering).forEach { (detaljer, kontekst) ->
                val fagsystemId = detaljer.getValue("fagsystemId") as String
                val fagområde = detaljer.getValue("fagområde") as String
                val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))

                fabrikk.lagSimulering(vedtaksperiodeId, utbetalingId, fagsystemId, fagområde, simuleringOK, standardSimuleringsresultat(orgnummer)).håndter(Person::håndter)
            }
        }

        internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true, automatiskBehandling: Boolean = true) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Godkjenning)
            val (_, kontekst) = behovsamler.detaljerFor(vedtaksperiodeId, Godkjenning).single()
            val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))
            fabrikk.lagUtbetalingsgodkjenning(vedtaksperiodeId, godkjent, automatiskBehandling, utbetalingId)
                .håndter(Person::håndter)
        }

        internal fun håndterUtbetalt(status: Oppdragstatus = Oppdragstatus.AKSEPTERT) {
            behovsamler.bekreftBehov(orgnummer, Utbetaling)
            behovsamler.detaljerFor(orgnummer, Utbetaling).forEach { (detaljer, kontekst) ->
                val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))
                val fagsystemId = detaljer.getValue("fagsystemId") as String
                fabrikk.lagUtbetalingOverført(utbetalingId, fagsystemId)
                    .håndter(Person::håndter)
                fabrikk.lagUtbetalinghendelse(utbetalingId, fagsystemId, status)
                    .håndter(Person::håndter)
            }
        }

        internal fun håndterAnnullering(fagsystemId: String) {
            fabrikk.lagAnnullering(fagsystemId).håndter(Person::håndter)
        }

        internal fun håndterPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()) {
            fabrikk.lagPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt)
                .håndter(Person::håndter)
        }

        internal fun håndterOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: ArbeidsforholdOverstyrt) {
            fabrikk.lagOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold)
                .håndter(Person::håndter)
        }

        operator fun <R> invoke(testblokk: TestArbeidsgiver.() -> R): R {
            return testblokk(this)
        }
    }
}

private fun lagStandardSammenligningsgrunnlag(orgnummer: String, inntekt: Inntekt, skjæringstidspunkt: LocalDate) =
    Inntektsvurdering(
        inntekter = inntektperioderForSammenligningsgrunnlag {
            skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                orgnummer inntekt inntekt
            }
        }
    )

private fun lagStandardSykepengegrunnlag(orgnummer: String, inntekt: Inntekt, skjæringstidspunkt: LocalDate) =
    InntektForSykepengegrunnlag(
        inntekter = listOf(
            ArbeidsgiverInntekt(orgnummer, (0..2).map {
                val yearMonth = YearMonth.from(skjæringstidspunkt).minusMonths(3L - it)
                ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                    yearMonth = yearMonth,
                    type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    inntekt = inntekt,
                    fordel = "fordel",
                    beskrivelse = "beskrivelse"
                )
            })
        ), arbeidsforhold = emptyList()
    )

internal fun standardSimuleringsresultat(orgnummer: String) = no.nav.helse.hendelser.Simulering.SimuleringResultat(
    totalbeløp = 2000,
    perioder = listOf(
        no.nav.helse.hendelser.Simulering.SimulertPeriode(
            periode = Periode(17.januar, 20.januar),
            utbetalinger = listOf(
                no.nav.helse.hendelser.Simulering.SimulertUtbetaling(
                    forfallsdato = 21.januar,
                    utbetalesTil = no.nav.helse.hendelser.Simulering.Mottaker(
                        id = orgnummer,
                        navn = "Org Orgesen AS"
                    ),
                    feilkonto = false,
                    detaljer = listOf(
                        no.nav.helse.hendelser.Simulering.Detaljer(
                            periode = Periode(17.januar, 20.januar),
                            konto = "81549300",
                            beløp = 2000,
                            klassekode = no.nav.helse.hendelser.Simulering.Klassekode(
                                kode = "SPREFAG-IOP",
                                beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                            ),
                            uføregrad = 100,
                            utbetalingstype = "YTEL",
                            tilbakeføring = false,
                            sats = no.nav.helse.hendelser.Simulering.Sats(
                                sats = 1000.toDouble(),
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

internal fun TestPerson.TestArbeidsgiver.nyttVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    førsteFraværsdag: LocalDate = fom,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode> = emptyList(),
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom, beregnetInntekt) }
) {
    val vedtaksperiode = nyPeriode(fom til tom, grad)
    håndterInntektsmelding(arbeidsgiverperiode, beregnetInntekt, førsteFraværsdag, refusjon)
    håndterYtelser(vedtaksperiode)
    håndterVilkårsgrunnlag(vedtaksperiode, beregnetInntekt, inntektsvurdering = Inntektsvurdering(
        inntekter = inntektperioderForSammenligningsgrunnlag(inntekterBlock)
    ))
    håndterYtelser(vedtaksperiode)
    håndterSimulering(vedtaksperiode)
    håndterUtbetalingsgodkjenning(vedtaksperiode)
    håndterUtbetalt(status)
}

internal fun TestPerson.TestArbeidsgiver.nyPeriode(periode: Periode, grad: Prosentdel = 100.prosent): UUID {
    håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, grad))
    return håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad)) ?: fail { "Det ble ikke opprettet noen vedtaksperiode." }
}

internal fun TestPerson.nyPeriode(periode: Periode, vararg orgnummer: String, grad: Prosentdel = 100.prosent) {
    orgnummer.forEach { it { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, grad)) } }
    orgnummer.forEach { it { håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad)) } }
}