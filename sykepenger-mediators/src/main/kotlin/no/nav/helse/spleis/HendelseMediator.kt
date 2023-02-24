package no.nav.helse.spleis

import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.hendelser.Migrate
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.serde.serialize
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.PersonDao
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.EtterbetalingMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayUtførtMessage
import no.nav.helse.spleis.meldinger.model.MigrateMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
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
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkEtterInfotrygdendringMessage
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
    private val hendelseRepository: HendelseRepository,
    private val personDao: PersonDao,
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

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NySøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, sykmelding, context, historiskeFolkeregisteridenter) { person ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsgiverMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, søknad, context, historiskeFolkeregisteridenter) { person ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadNavMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, søknad, context, historiskeFolkeregisteridenter) { person ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun behandle(personopplysninger: Personopplysninger, message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext) {
        opprettPersonOgHåndter(personopplysninger, message, inntektsmelding, context) { person ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay, context: MessageContext) {
        hentPersonOgHåndter(message, inntektsmelding, context) { person ->
            HendelseProbe.onInntektsmeldingReplay()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(
        message: InntektsmeldingReplayUtførtMessage,
        replayUtført: InntektsmeldingReplayUtført,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, replayUtført, context) { person ->
            person.håndter(replayUtført)
        }
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext) {
        hentPersonOgHåndter(message, utbetalingshistorikk, context) { person ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndter(utbetalingshistorikk)
        }
    }

    override fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext) {
        hentPersonOgHåndter(message, utbetalingshistorikkForFeriepenger, context) { person ->
            HendelseProbe.onUtbetalingshistorikkForFeriepenger()
            person.håndter(utbetalingshistorikkForFeriepenger)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext) {
        hentPersonOgHåndter(message, ytelser, context) { person ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext) {
        hentPersonOgHåndter(message, vilkårsgrunnlag, context) { person ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext) {
        hentPersonOgHåndter(message, simulering, context) { person ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext) {
        hentPersonOgHåndter(message, utbetalingsgodkjenning, context) { person ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndter(utbetalingsgodkjenning)
        }
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført, context: MessageContext) {
        hentPersonOgHåndter(message, utbetaling, context) { person ->
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext) {
        hentPersonOgHåndter(message, utbetaling, context) { person ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse, context: MessageContext) {
        hentPersonOgHåndter(message, påminnelse, context) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext) {
        hentPersonOgHåndter(message, påminnelse, context) { person ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext) {
        hentPersonOgHåndter(message, påminnelse, context) { person ->
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext) {
        hentPersonOgHåndter(message, annullerUtbetaling, context) { person ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndter(annullerUtbetaling)
        }
    }

    override fun behandle(message: AvstemmingMessage, avstemming: Avstemming, context: MessageContext) {
        hentPersonOgHåndter(message, avstemming, context) { person ->
            person.håndter(avstemming)
        }
    }

    override fun behandle(message: MigrateMessage, migrate: Migrate, context: MessageContext) {
        hentPersonOgHåndter(message, migrate, context) { /* intentionally left blank */ }
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext) {
        hentPersonOgHåndter(message, overstyrTidslinje, context) { person ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndter(overstyrTidslinje)
        }
    }

    override fun behandle(message: OverstyrInntektMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, overstyrArbeidsgiveropplysninger, context) { person ->
            HendelseProbe.onOverstyrInntekt()
            person.håndter(overstyrArbeidsgiveropplysninger)
        }
    }

    override fun behandle(message: OverstyrArbeidsgiveropplysningerMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, overstyrArbeidsgiveropplysninger, context) { person ->
            HendelseProbe.onOverstyrArbeidsgiveropplysninger()
            person.håndter(overstyrArbeidsgiveropplysninger)
        }
    }

    override fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext) {
        hentPersonOgHåndter(message, overstyrArbeidsforhold, context) { person ->
            HendelseProbe.onOverstyrArbeidsforhold()
            person.håndter(overstyrArbeidsforhold)
        }
    }

    override fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext) {
        hentPersonOgHåndter(message, grunnbeløpsregulering, context) { person ->
            person.håndter(grunnbeløpsregulering)
        }
    }

    override fun behandle(
        message: InfotrygdendringMessage,
        infotrygdEndring: Infotrygdendring,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, infotrygdEndring, context) { person ->
            HendelseProbe.onInfotrygdendring()
            person.håndter(infotrygdEndring)
        }
    }

    override fun behandle(
        message: UtbetalingshistorikkEtterInfotrygdendringMessage,
        utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, utbetalingshistorikkEtterInfotrygdendring, context) { person ->
            HendelseProbe.onUtbetalingshistorikkEtterInfotrygdendring()
            person.håndter(utbetalingshistorikkEtterInfotrygdendring)
        }
    }

    private fun <Hendelse : PersonHendelse> opprettPersonOgHåndter(
        personopplysninger: Personopplysninger,
        message: HendelseMessage,
        hendelse: Hendelse,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        handler: (Person) -> Unit
    ) {
        hentPersonOgHåndter(personopplysninger, message, hendelse, context, historiskeFolkeregisteridenter, handler)
    }

    private fun <Hendelse : PersonHendelse> hentPersonOgHåndter(
        message: HendelseMessage,
        hendelse: Hendelse,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        handler: (Person) -> Unit
    ) {
        hentPersonOgHåndter(null, message, hendelse, context, historiskeFolkeregisteridenter, handler)
    }
    private fun <Hendelse : PersonHendelse> hentPersonOgHåndter(
        personopplysninger: Personopplysninger?,
        message: HendelseMessage,
        hendelse: Hendelse,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String> = emptyList(),
        handler: (Person) -> Unit
    ) {
        val jurist = MaskinellJurist()
        val personMediator = PersonMediator(message, hendelse, hendelseRepository)
        val datadelingMediator = DatadelingMediator(hendelse)
        val subsumsjonMediator = SubsumsjonMediator(jurist, hendelse.fødselsnummer(), message, versjonAvKode)
        person(message, hendelse, historiskeFolkeregisteridenter, jurist, personopplysninger) { person  ->
            person.addObserver(personMediator)
            person.addObserver(VedtaksperiodeProbe)
            handler(person)
        }
        finalize(context, personMediator, subsumsjonMediator, datadelingMediator, hendelse)
    }

    private fun person(message: HendelseMessage, hendelse: PersonHendelse, historiskeFolkeregisteridenter: List<String>, jurist: MaskinellJurist, personopplysninger: Personopplysninger?, block: (Person) -> Unit) {
        val personidentifikator = hendelse.fødselsnummer().somPersonidentifikator()
        val tidligereBehandledeIdenter = personDao.hentTidligereBehandledeIdenter(historiskeFolkeregisteridenter)
        val tidligereBehandlinger = personDao.lesOppPersoner(tidligereBehandledeIdenter.map { tidligereBehandletIdent -> tidligereBehandletIdent.somPersonidentifikator() })
            .map { serialisertPerson -> serialisertPerson.second.deserialize(jurist) { hendelseRepository.hentAlleHendelser(serialisertPerson.first) } }
        personDao.hentEllerOpprettPerson(personidentifikator, hendelse.aktørId(), message, {
            personopplysninger?.person(jurist)?.serialize()
        }) { serialisertPerson ->
            serialisertPerson.deserialize(jurist, tidligereBehandlinger) { hendelseRepository.hentAlleHendelser(personidentifikator) }.also(block).serialize()
        }
    }

    private fun finalize(
        context: MessageContext,
        personMediator: PersonMediator,
        subsumsjonMediator: SubsumsjonMediator,
        datadelingMediator: DatadelingMediator,
        hendelse: PersonHendelse
    ) {
        personMediator.finalize(rapidsConnection, context)
        subsumsjonMediator.finalize(context)
        datadelingMediator.finalize(context)
        if (!hendelse.harAktiviteter()) return
        if (hendelse.harFunksjonelleFeilEllerVerre()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(context, hendelse)
    }
}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage, context: MessageContext)
    fun behandle(
        personopplysninger: Personopplysninger,
        message: NySøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    )
    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsgiverMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    )
    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadNavMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: List<String>
    )
    fun behandle(personopplysninger: Personopplysninger, message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext)
    fun behandle(message: InntektsmeldingReplayMessage, inntektsmelding: InntektsmeldingReplay, context: MessageContext)
    fun behandle(message: InntektsmeldingReplayUtførtMessage, replayUtført: InntektsmeldingReplayUtført, context: MessageContext)
    fun behandle(message: UtbetalingpåminnelseMessage, påminnelse: Utbetalingpåminnelse, context: MessageContext)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext)
    fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext)
    fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext)
    fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført, context: MessageContext)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext)
    fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext)
    fun behandle(message: AvstemmingMessage, avstemming: Avstemming, context: MessageContext)
    fun behandle(message: MigrateMessage, migrate: Migrate, context: MessageContext)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext)
    fun behandle(message: OverstyrInntektMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: OverstyrArbeidsgiveropplysningerMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext)
    fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext)
    fun behandle(message: InfotrygdendringMessage, infotrygdEndring: Infotrygdendring, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkEtterInfotrygdendringMessage, utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring, context: MessageContext)
}
