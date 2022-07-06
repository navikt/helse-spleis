package no.nav.helse.dsl

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Dødsinfo
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import no.nav.helse.økonomi.Inntekt

internal class TestPerson(
    private val observatør: PersonObserver,
    private val aktørId: String = AKTØRID,
    private val fødselsnummer: Fødselsnummer = UNG_PERSON_FNR_2018,
    alder: Alder = UNG_PERSON_FDATO_2018.alder,
    private val jurist: MaskinellJurist = MaskinellJurist()
) {
    internal companion object {
        private val fnrformatter = DateTimeFormatter.ofPattern("ddMMyy")
        internal val UNG_PERSON_FDATO_2018 = 12.februar(1992)
        internal val UNG_PERSON_FNR_2018: Fødselsnummer = "${UNG_PERSON_FDATO_2018.format(fnrformatter)}40045".somFødselsnummer()
        internal const val AKTØRID = "42"

        internal operator fun <R> String.invoke(testPerson: TestPerson, testblokk: TestArbeidsgiver.() -> R) =
            testPerson.arbeidsgiver(this, testblokk)
    }

    private lateinit var forrigeHendelse: IAktivitetslogg

    private val behovsamler = Behovsamler()
    private val vedtaksperiodesamler = Vedtaksperiodesamler()

    private val person = Person(aktørId, fødselsnummer, alder, jurist).also {
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

    private fun <T : PersonHendelse> T.håndter(håndter: Person.(T) -> Unit): T {
        forrigeHendelse = this
        person.håndter(this)
        behovsamler.registrerBehov(forrigeHendelse)
        return this
    }

    inner class TestArbeidsgiver(private val orgnummer: String) {
        private val fabrikk = Hendelsefabrikk(aktørId, fødselsnummer, orgnummer)

        internal val inspektør get() = TestArbeidsgiverInspektør(person, orgnummer)

        internal val Int.vedtaksperiode get() = vedtaksperiodesamler.vedtaksperiodeId(orgnummer, this - 1)

        internal fun håndterSykmelding(vararg sykmeldingsperiode: Sykmeldingsperiode) =
            fabrikk.lagSykmelding(*sykmeldingsperiode).håndter(Person::håndter)

        internal fun håndterSøknad(vararg perioder: Søknad.Søknadsperiode) =
            fabrikk.lagSøknad(*perioder).håndter(Person::håndter)

        internal fun håndterInntektsmelding(arbeidsgiverperioder: List<Periode>, inntekt: Inntekt) =
            fabrikk.lagInntektsmelding(arbeidsgiverperioder, inntekt).håndter(Person::håndter)

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            inntekt: Inntekt = INNTEKT,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntektsvurdering: Inntektsvurdering = lagStandardSammenligningsgrunnlag(orgnummer, inntekt, inspektør.skjæringstidspunkt(vedtaksperiodeId)),
            inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = lagStandardSykepengegrunnlag(orgnummer, inntekt, inspektør.skjæringstidspunkt(vedtaksperiodeId)),
            arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = arbeidsgivere.map { (orgnr, _) -> Vilkårsgrunnlag.Arbeidsforhold(orgnr, LocalDate.EPOCH, null) }
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, InntekterForSammenligningsgrunnlag, InntekterForSykepengegrunnlag, ArbeidsforholdV2, Medlemskap)
            fabrikk.lagVilkårsgrunnlag(vedtaksperiodeId, medlemskapstatus, arbeidsforhold, inntektsvurdering, inntektsvurderingForSykepengegrunnlag)
                .håndter(Person::håndter)
        }

        internal fun håndterYtelser(vedtaksperiodeId: UUID) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Dagpenger, Arbeidsavklaringspenger, Dødsinfo, Institusjonsopphold, Opplæringspenger, Pleiepenger, Omsorgspenger, Foreldrepenger)
            fabrikk.lagYtelser(vedtaksperiodeId).håndter(Person::håndter)
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
