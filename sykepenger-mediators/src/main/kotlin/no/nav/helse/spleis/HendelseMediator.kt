package no.nav.helse.spleis

import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingsgrunnlag
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.serde.migration.JsonMigrationObserver
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.db.SlettetVedtaksperiodeDao
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.EtterbetalingMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage
import no.nav.helse.spleis.meldinger.model.OverstyrInntektMessage
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingOverførtMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingpåminnelseMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingsgodkjenningMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingsgrunnlagMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkForFeriepengerMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage
import no.nav.helse.spleis.meldinger.model.YtelserMessage
import org.slf4j.LoggerFactory

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val hendelseRepository: HendelseRepository,
    private val lagrePersonDao: LagrePersonDao,
    private val slettetVedtaksperiodeDao: SlettetVedtaksperiodeDao,
    private val versjonAvKode: String
) : IHendelseMediator {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val behovMediator = BehovMediator(sikkerLogg)

    override fun behandle(message: HendelseMessage, context: MessageContext) {
        try {
            message.behandle(this, context)
        } catch (err: Aktivitetslogg.AktivitetException) {
            withMDC(err.kontekst()) {
                sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}\n\t${message.toJson()}", err)
            }
        }
    }

    override fun behandle(message: NySøknadMessage, sykmelding: Sykmelding, context: MessageContext) {
        håndter(message, sykmelding, context) { person ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: Søknad, context: MessageContext) {
        håndter(message, søknad, context) { person ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: SendtSøknadNavMessage, søknad: Søknad, context: MessageContext) {
        håndter(message, søknad, context) { person ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext) {
        håndter(message, inntektsmelding, context) { person ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay, context: MessageContext) {
        håndter(message, inntektsmelding, context) { person ->
            HendelseProbe.onInntektsmeldingReplay()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext) {
        håndter(message, utbetalingshistorikk, context) { person ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndter(utbetalingshistorikk)
        }
    }

    override fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext) {
        håndter(message, utbetalingshistorikkForFeriepenger, context) { person ->
            HendelseProbe.onUtbetalingshistorikkForFeriepenger()
            person.håndter(utbetalingshistorikkForFeriepenger)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext) {
        håndter(message, ytelser, context) { person ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext) {
        håndter(message, vilkårsgrunnlag, context) { person ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun behandle(message: UtbetalingsgrunnlagMessage, utbetalingsgrunnlag: Utbetalingsgrunnlag, context: MessageContext) {
        håndter(message, utbetalingsgrunnlag, context) { person ->
            HendelseProbe.onUtbetalingsgrunnlag()
            person.håndter(utbetalingsgrunnlag)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext) {
        håndter(message, simulering, context) { person ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext) {
        håndter(message, utbetalingsgodkjenning, context) { person ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndter(utbetalingsgodkjenning)
        }
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført, context: MessageContext) {
        håndter(message, utbetaling, context) { person ->
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext) {
        håndter(message, utbetaling, context) { person ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse, context: MessageContext) {
        håndter(message, påminnelse, context) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext) {
        håndter(message, påminnelse, context) { person ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext) {
        håndter(message, påminnelse, context) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext) {
        håndter(message, annullerUtbetaling, context) { person ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndter(annullerUtbetaling)
        }
    }

    override fun behandle(message: AvstemmingMessage, avstemming: Avstemming, context: MessageContext) {
        håndter(message, avstemming, context) { person ->
            person.håndter(avstemming)
            lagrePersonDao.personAvstemt(avstemming)
        }
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext) {
        håndter(message, overstyrTidslinje, context) { person ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndter(overstyrTidslinje)
        }
    }

    override fun behandle(message: OverstyrInntektMessage, overstyrInntekt: OverstyrInntekt, context: MessageContext) {
        håndter(message, overstyrInntekt, context) { person ->
            HendelseProbe.onOverstyrInntekt()
            person.håndter(overstyrInntekt)
        }
    }

    override fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext) {
        håndter(message, overstyrArbeidsforhold, context) { person ->
            HendelseProbe.onOverstyrArbeidsforhold()
            person.håndter(overstyrArbeidsforhold)
        }
    }

    override fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext) {
        håndter(message, grunnbeløpsregulering, context) { person ->
            person.håndter(grunnbeløpsregulering)
        }
    }

    private fun <Hendelse : PersonHendelse> håndter(
        message: HendelseMessage,
        hendelse: Hendelse,
        context: MessageContext,
        handler: (Person) -> Unit
    ) {
        val jurist = MaskinellJurist()
        val observer = MigrationObserver(slettetVedtaksperiodeDao, hendelse)
        val person = person(hendelse, jurist, observer)
        val personMediator = PersonMediator(person, message, hendelse, hendelseRepository)
        val subsumsjonMediator = SubsumsjonMediator(jurist, hendelse.fødselsnummer(), message, versjonAvKode)
        person.addObserver(VedtaksperiodeProbe)
        handler(person)
        finalize(context, personMediator, subsumsjonMediator, hendelse)
    }

    private fun person(hendelse: PersonHendelse, jurist: MaskinellJurist, observer: JsonMigrationObserver): Person {
        return personRepository.hentPerson(hendelse.fødselsnummer().somFødselsnummer())
            ?.deserialize(
                jurist = jurist,
                meldingerSupplier = { hendelseRepository.hentAlleHendelser(hendelse.fødselsnummer().somFødselsnummer()) },
                observer = observer
            )
            ?: Person(aktørId = hendelse.aktørId(), fødselsnummer = hendelse.fødselsnummer().somFødselsnummer(), jurist = jurist)
    }

    private fun finalize(
        context: MessageContext,
        personMediator: PersonMediator,
        subsumsjonMediator: SubsumsjonMediator,
        hendelse: PersonHendelse
    ) {
        personMediator.finalize(rapidsConnection, context, lagrePersonDao)
        subsumsjonMediator.finalize(context)
        if (!hendelse.hasActivities()) return
        if (hendelse.hasErrorsOrWorse()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(context, hendelse)
    }
}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage, context: MessageContext)
    fun behandle(message: NySøknadMessage, sykmelding: Sykmelding, context: MessageContext)
    fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: Søknad, context: MessageContext)
    fun behandle(message: SendtSøknadNavMessage, søknad: Søknad, context: MessageContext)
    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext)
    fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay, context: MessageContext)
    fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse, context: MessageContext)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext)
    fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext)
    fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext)
    fun behandle(message: UtbetalingsgrunnlagMessage, utbetalingsgrunnlag: Utbetalingsgrunnlag, context: MessageContext)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext)
    fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført, context: MessageContext)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext)
    fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext)
    fun behandle(message: AvstemmingMessage, avstemming: Avstemming, context: MessageContext)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext)
    fun behandle(message: OverstyrInntektMessage, overstyrInntekt: OverstyrInntekt, context: MessageContext)
    fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext)
    fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext)
}
