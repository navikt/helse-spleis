package no.nav.helse.spleis.e2e.testcontext

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

internal class ArrangeContext(
    private val e2eTest: AbstractEndToEndTest,
    private val testhendelsefabrikk: Testhendelsefabrikk,
    private val person: Person,
    private val observatør: TestObservatør,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val actBlock: ArrangeContext.() -> Unit
) {

    private val inspektør get() = inspektør(person, organisasjonsnummer)

    private val Int.vedtaksperiode get() = this.vedtaksperiode(organisasjonsnummer)
    private val Int.utbetaling get() = this.utbetaling(organisasjonsnummer)

    private val assertContext = AssertContext(e2eTest, person, observatør, ikkeBesvarteBehov, fødselsnummer, aktørId, organisasjonsnummer, vedtaksperiodeId)

    internal companion object {
        private val ikkeBesvarteBehov = mutableListOf<EtterspurtBehov>()

        fun create(
            e2eTest: AbstractEndToEndTest,
            testhendelsefabrikk: Testhendelsefabrikk,
            sykmeldingsperiode: Sykmeldingsperiode,
            actBlock: ArrangeContext.() -> Unit
        ): ArrangeContext {
            val sykmelding = testhendelsefabrikk.sykmelding(sykmeldingsperiode)
            return create(e2eTest, testhendelsefabrikk, sykmelding, actBlock)
        }

        fun create(
            e2eTest: AbstractEndToEndTest,
            testhendelsefabrikk: Testhendelsefabrikk,
            sykmelding: Sykmelding,
            actBlock: ArrangeContext.() -> Unit
        ): ArrangeContext {
            val fødselsnummer = sykmelding.fødselsnummer()
            val aktørId = sykmelding.aktørId()
            val organisasjonsnummer = sykmelding.organisasjonsnummer()

            val person = Person(aktørId, fødselsnummer)
            val observatør = TestObservatør().also { person.addObserver(it) }

            val før = inspektør(person, organisasjonsnummer).vedtaksperioder()
            sykmelding.håndter(person, Person::håndter)
            val etter = inspektør(person, organisasjonsnummer).vedtaksperioder()
            if (før == etter || (før.size + 1) != etter.size) throw IllegalStateException("Ingen vedtaksperioder ble opprettet. ${før.size} perioder før, ${etter.size} perioder etterpå")
            val vedtaksperiodeId = etter.first { it !in før }
            return ArrangeContext(
                e2eTest,
                testhendelsefabrikk,
                person,
                observatør,
                fødselsnummer,
                aktørId,
                organisasjonsnummer,
                vedtaksperiodeId,
                actBlock
            )
        }

        private fun <T : PersonHendelse> T.håndter(person: Person, håndter: Person.(T) -> Unit): T {
            håndter(person, this)
            ikkeBesvarteBehov += EtterspurtBehov.finnEtterspurteBehov(behov())
            return this
        }

        private fun inspektør(person: Person, orgnummer: String) = TestArbeidsgiverInspektør(person, orgnummer)
    }

    private fun Int.vedtaksperiode(orgnummer: String) = vedtaksperiodeId(this - 1, orgnummer)
    private fun Int.utbetaling(orgnummer: String) = utbetalingId(this - 1, orgnummer)
    private fun vedtaksperiodeId(indeks: Int, orgnummer: String) = observatør.vedtaksperiode(orgnummer, indeks)
    private fun utbetalingId(indeks: Int, orgnummer: String) = inspektør(person, orgnummer).utbetalingId(indeks)

    internal fun arrange(): ArrangeContext {
        actBlock()
        return this
    }

    internal operator fun invoke(actBlock: ArrangeContext.() -> Unit): ArrangeContext {
        actBlock()
        return this
    }

    internal fun assert(assertBlock: AssertContext.(TestArbeidsgiverInspektør, TestObservatør) -> Unit) =
        assertContext(assertBlock)

    fun håndterSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID()
    ) =
        testhendelsefabrikk.søknad(*perioder, andreInntektskilder = andreInntektskilder, sendtTilNav = sendtTilNav, id = id)
            .håndter(person, Person::håndter)

    fun håndterInntektsmeldingMedValidering(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        beregnetInntekt: Inntekt = refusjon.second,
        harOpphørAvNaturalytelser: Boolean = false
    ) {
        assert { _, _ -> assertIkkeEtterspurt(Inntektsmelding::class, InntekterForSammenligningsgrunnlag) }
        håndterInntektsmelding(
            arbeidsgiverperioder,
            førsteFraværsdag,
            ferieperioder,
            refusjon,
            beregnetInntekt = beregnetInntekt,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
        )
    }

    internal fun håndterInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        ferieperioder: List<Periode> = emptyList(),
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, INNTEKT, emptyList()),
        id: UUID = UUID.randomUUID(),
        beregnetInntekt: Inntekt = refusjon.second,
        harOpphørAvNaturalytelser: Boolean = false
    ): UUID {
        testhendelsefabrikk.inntektsmelding(
            id,
            arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
        ).håndter(person, Person::håndter)
        return id
    }

    fun håndterYtelser(
        vararg utbetalinger: Utbetalingshistorikk.Infotrygdperiode,
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ) {
        assert { _, _ ->
            assertEtterspurt(
                Ytelser::class, Sykepengehistorikk, Foreldrepenger,
                Behovtype.Pleiepenger, Behovtype.Omsorgspenger, Behovtype.Opplæringspenger,
                Behovtype.Arbeidsavklaringspenger, Behovtype.Dagpenger,
                Behovtype.Institusjonsopphold, Behovtype.Dødsinfo
            )
        }
        testhendelsefabrikk.ytelser(
            vedtaksperiodeId,
            utbetalinger.toList(),
            inntektshistorikk = inntektshistorikk,
            foreldrepenger = foreldrepenger,
            pleiepenger = pleiepenger,
            omsorgspenger = omsorgspenger,
            opplæringspenger = opplæringspenger,
            institusjonsoppholdsperioder = institusjonsoppholdsperioder,
            dødsdato = dødsdato,
            statslønn = statslønn,
            arbeidskategorikoder = arbeidskategorikoder,
            arbeidsavklaringspenger = arbeidsavklaringspenger,
            dagpenger = dagpenger
        ).håndter(person, Person::håndter)
    }

    fun håndterVilkårsgrunnlag(
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurdering: Inntektsvurdering? = null
    ) {
        assert { _, _ -> assertEtterspurt(Vilkårsgrunnlag::class, InntekterForSammenligningsgrunnlag, Medlemskap) }
        testhendelsefabrikk.vilkårsgrunnlag(vedtaksperiodeId, arbeidsforhold, medlemskapstatus, inntektsvurdering)
            .håndter(person, Person::håndter)
    }

    fun håndterUtbetalingsgodkjenning(
        utbetalingGodkjent: Boolean = true,
        automatiskBehandling: Boolean = false
    ) {
        assert { _, _ -> assertEtterspurt(Utbetalingsgodkjenning::class, Godkjenning) }
        val utbetalingId = UUID.fromString(
            inspektør.sisteBehov(Godkjenning).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Godkjenning).kontekst()}")
        )
        testhendelsefabrikk.utbetalingsgodkjenning(vedtaksperiodeId, utbetalingId, utbetalingGodkjent, automatiskBehandling)
            .håndter(person, Person::håndter)
    }

    fun håndterUtbetalingshistorikk(utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode>, inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>?) {
        assert { _, _ -> assertEtterspurt(Utbetalingshistorikk::class, Sykepengehistorikk) }
        testhendelsefabrikk.utbetalingshistorikk(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk
        ).håndter(person, Person::håndter)
    }

    //fun simulering(simuleringOK: Boolean = true) =
     //   e2eTest.simulering(vedtaksperiodeId, simuleringOK, organisasjonsnummer)
}
