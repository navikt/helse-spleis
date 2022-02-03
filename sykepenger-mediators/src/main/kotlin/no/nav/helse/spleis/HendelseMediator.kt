package no.nav.helse.spleis

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.utbetaling.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val hendelseRepository: HendelseRepository,
    private val lagrePersonDao: LagrePersonDao,
    private val versjonAvKode: String
) : IHendelseMediator {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    override fun behandle(message: HendelseMessage) {
        try {
            message.behandle(this)
        } catch (err: Aktivitetslogg.AktivitetException) {
            withMDC(err.kontekst()) {
                sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}\n\t${message.toJson()}", err)
            }
        }
    }

    override fun behandle(message: NySøknadMessage, sykmelding: Sykmelding) {
        håndter(message, sykmelding) { person ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: Søknad) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: SendtSøknadNavMessage, søknad: Søknad) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding) {
        håndter(message, inntektsmelding) { person ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay) {
        håndter(message, inntektsmelding) { person ->
            HendelseProbe.onInntektsmeldingReplay()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk) {
        håndter(message, utbetalingshistorikk) { person ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndter(utbetalingshistorikk)
        }
    }

    override fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger) {
        håndter(message, utbetalingshistorikkForFeriepenger) { person ->
            HendelseProbe.onUtbetalingshistorikkForFeriepenger()
            person.håndter(utbetalingshistorikkForFeriepenger)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser) {
        håndter(message, ytelser) { person ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag) {
        håndter(message, vilkårsgrunnlag) { person ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun behandle(message: UtbetalingsgrunnlagMessage, utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        håndter(message, utbetalingsgrunnlag) { person ->
            HendelseProbe.onUtbetalingsgrunnlag()
            person.håndter(utbetalingsgrunnlag)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering) {
        håndter(message, simulering) { person ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        håndter(message, utbetalingsgodkjenning) { person ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndter(utbetalingsgodkjenning)
        }
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført) {
        håndter(message, utbetaling) { person ->
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse) {
        håndter(message, utbetaling) { person ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse) {
        håndter(message, påminnelse) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse) {
        håndter(message, påminnelse) { person ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse) {
        håndter(message, påminnelse) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling) {
        håndter(message, annullerUtbetaling) { person ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndter(annullerUtbetaling)
        }
    }

    override fun behandle(message: AvstemmingMessage, avstemming: Avstemming) {
        håndter(message, avstemming) { person ->
            person.håndter(avstemming)
            lagrePersonDao.personAvstemt(avstemming)
        }
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje) {
        håndter(message, overstyrTidslinje) { person ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndter(overstyrTidslinje)
        }
    }

    override fun behandle(message: OverstyrInntektMessage, overstyrInntekt: OverstyrInntekt) {
        håndter(message, overstyrInntekt) { person ->
            HendelseProbe.onOverstyrInntekt()
            person.håndter(overstyrInntekt)
        }
    }

    override fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold) {
        håndter(message, overstyrArbeidsforhold) { person ->
            HendelseProbe.onOverstyrArbeidsforhold()
            person.håndter(overstyrArbeidsforhold)
        }
    }

    override fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering) {
        håndter(message, grunnbeløpsregulering) { person ->
            person.håndter(grunnbeløpsregulering)
        }
    }

    private fun <Hendelse : PersonHendelse> håndter(
        message: HendelseMessage,
        hendelse: Hendelse,
        handler: (Person) -> Unit
    ) {
        val jurist = MaskinellJurist()
        val person = person(hendelse, jurist)
        val personMediator = PersonMediator(person, message, hendelse, hendelseRepository)
        val subsumsjonMediator = SubsumsjonMediator(jurist, hendelse, message, versjonAvKode)
        person.addObserver(VedtaksperiodeProbe)
        handler(person)
        finalize(personMediator, subsumsjonMediator, message, hendelse)
    }

    private fun person(hendelse: PersonHendelse, jurist: MaskinellJurist): Person {
        return personRepository.hentPerson(hendelse.fødselsnummer().somFødselsnummer())
            ?.deserialize(jurist) { hendelseRepository.hentAlleHendelser(hendelse.fødselsnummer().somFødselsnummer()) }
            ?: Person(aktørId = hendelse.aktørId(), fødselsnummer = hendelse.fødselsnummer().somFødselsnummer(), jurist = jurist)
    }

    private fun finalize(personMediator: PersonMediator, subsumsjonMediator: SubsumsjonMediator, message: HendelseMessage, hendelse: PersonHendelse) {
        personMediator.finalize(rapidsConnection, lagrePersonDao)
        subsumsjonMediator.finalize(rapidsConnection)
        if (!hendelse.hasActivities()) return
        if (hendelse.hasErrorsOrWorse()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(message, hendelse)
    }

}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage)
    fun behandle(message: NySøknadMessage, sykmelding: Sykmelding)
    fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: Søknad)
    fun behandle(message: SendtSøknadNavMessage, søknad: Søknad)
    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding)
    fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay)
    fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse)
    fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk)
    fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger)
    fun behandle(message: YtelserMessage, ytelser: Ytelser)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag)
    fun behandle(message: UtbetalingsgrunnlagMessage, utbetalingsgrunnlag: Utbetalingsgrunnlag)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning)
    fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse)
    fun behandle(message: SimuleringMessage, simulering: Simulering)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling)
    fun behandle(message: AvstemmingMessage, avstemming: Avstemming)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje)
    fun behandle(message: OverstyrInntektMessage, overstyrInntekt: OverstyrInntekt)
    fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold)
    fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering)
}
