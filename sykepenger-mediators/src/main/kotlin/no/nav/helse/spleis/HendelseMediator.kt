package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GjenopptaBehandling
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsendringer
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.Migrate
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.PersonDao
import no.nav.helse.spleis.meldinger.model.AnmodningOmForkastingMessage
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.DødsmeldingMessage
import no.nav.helse.spleis.meldinger.model.FeriepengeutbetalingMessage
import no.nav.helse.spleis.meldinger.model.ForkastSykmeldingsperioderMessage
import no.nav.helse.spleis.meldinger.model.GjenopptaBehandlingMessage
import no.nav.helse.spleis.meldinger.model.GrunnbeløpsreguleringMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.IdentOpphørtMessage
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage
import no.nav.helse.spleis.meldinger.model.InntektsendringerMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingerReplayMessage
import no.nav.helse.spleis.meldinger.model.MigrateMessage
import no.nav.helse.spleis.meldinger.model.MinimumSykdomsgradVurdertMessage
import no.nav.helse.spleis.meldinger.model.NavNoInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NavNoKorrigertInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NavNoSelvbestemtInntektsmeldingMessage
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyArbeidsledigTidligereArbeidstakerSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyFrilansSøknadMessage
import no.nav.helse.spleis.meldinger.model.NyJordbrukerSøknadMessage
import no.nav.helse.spleis.meldinger.model.NySelvstendigSøknadMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage
import no.nav.helse.spleis.meldinger.model.PåminnelseMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadAnnetMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsledigMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsledigTidligereArbeidstakerMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadFiskerMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadFrilansMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadJordbrukerMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage
import no.nav.helse.spleis.meldinger.model.SendtSøknadSelvstendigMessage
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.spleis.meldinger.model.SkjønnsmessigFastsettelseMessage
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage
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
    private val hendelseRepository: HendelseRepository,
    private val personDao: PersonDao,
    private val versjonAvKode: String,
    private val støtterIdentbytte: Boolean = false,
    private val subsumsjonsproducer: Subsumsjonproducer
) : IHendelseMediator {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val behovMediator = BehovMediator(sikkerLogg)

    override fun behandle(message: HendelseMessage, context: MessageContext) {
        message.behandle(this, context)
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NySøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyFrilansSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NySelvstendigSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(personopplysninger: Personopplysninger, message: NyJordbrukerSøknadMessage, sykmelding: Sykmelding, context: MessageContext, historiskeFolkeregisteridenter: Set<Personidentifikator>) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigTidligereArbeidstakerSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSykmelding()
            person.håndterSykmelding(eventBus, sykmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsgiverMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadNavMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadNav()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFrilansMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadSelvstendigMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(personopplysninger: Personopplysninger, message: SendtSøknadJordbrukerMessage, søknad: Søknad, context: MessageContext, historiskeFolkeregisteridenter: Set<Personidentifikator>) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadJordbruker()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFiskerMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadAnnetMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigTidligereArbeidstakerMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    ) {
        opprettPersonOgHåndter(personopplysninger, message, context, historiskeFolkeregisteridenter) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSøknadFrilans()
            person.håndterSøknad(eventBus, søknad, aktivitetslogg)
        }
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onInntektsmelding()
            person.håndterInntektsmelding(eventBus, inntektsmelding, aktivitetslogg)
        }
    }

    override fun behandle(message: NavNoInntektsmeldingMessage, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onNavNoInntektsmelding()
            person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, aktivitetslogg)
        }
    }

    override fun behandle(message: NavNoKorrigertInntektsmeldingMessage, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onNavNoInntektsmelding()
            person.håndterKorrigerteArbeidsgiveropplysninger(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetslogg)
        }
    }

    override fun behandle(message: NavNoSelvbestemtInntektsmeldingMessage, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onNavNoSelvbestemtInntektsmelding()
            person.håndterKorrigerteArbeidsgiveropplysninger(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetslogg)
        }
    }


    override fun behandle(message: InntektsmeldingerReplayMessage, replays: InntektsmeldingerReplay, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onInntektsmeldingReplay()
            person.håndterInntektsmeldingerReplay(eventBus, replays, aktivitetslogg)
        }
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndterUtbetalingshistorikk(eventBus, utbetalingshistorikk, aktivitetslogg)
        }
    }

    override fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetalingshistorikkForFeriepenger()
            person.håndterUtbetalingshistorikkForFeriepenger(eventBus, utbetalingshistorikkForFeriepenger, aktivitetslogg)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onYtelser()
            person.håndterYtelser(eventBus, ytelser, aktivitetslogg)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetslogg)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onSimulering()
            person.håndterSimulering(eventBus, simulering, aktivitetslogg)
        }
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndterUtbetalingsgodkjenning(eventBus, utbetalingsgodkjenning, aktivitetslogg)
        }
    }

    override fun behandle(message: FeriepengeutbetalingMessage, utbetaling: FeriepengeutbetalingHendelse, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetaling()
            person.håndterFeriepengeutbetalingHendelse(eventBus, utbetaling, aktivitetslogg)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetaling()
            person.håndterUtbetalingHendelse(eventBus, utbetaling, aktivitetslogg)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndterPåminnelse(eventBus, påminnelse, aktivitetslogg)
        }
    }

    override fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterPersonPåminnelse(eventBus, påminnelse, aktivitetslogg)
        }
    }

    override fun behandle(message: GjenopptaBehandlingMessage, gjenopptaBehandling: GjenopptaBehandling, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterGjenopptaBehandling(eventBus, gjenopptaBehandling, aktivitetslogg)
        }
    }

    override fun behandle(
        message: AnmodningOmForkastingMessage,
        anmodning: AnmodningOmForkasting,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterAnmodningOmForkasting(eventBus, anmodning, aktivitetslogg)
        }
    }

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndterAnnulerUtbetaling(eventBus, annullerUtbetaling, aktivitetslogg)
        }
    }

    override fun behandle(message: AvstemmingMessage, personidentifikator: Personidentifikator, context: MessageContext) {
        person(personidentifikator, message, emptySet(), Regelverkslogg.EmptyLog, null) { person ->
            val dto = person.dto()
            val avstemmer = Avstemmer(dto)
            context.publish(avstemmer.tilJsonMessage().toJson().also {
                sikkerLogg.info("sender person_avstemt:\n$it")
            })
        }
    }

    override fun behandle(message: MigrateMessage, migrate: Migrate, context: MessageContext) {
        hentPersonOgHåndter(message, context) { _, _, _ -> /* intentionally left blank */ }
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndterOverstyrTidslinje(eventBus, overstyrTidslinje, aktivitetslogg)
        }
    }

    override fun behandle(message: OverstyrArbeidsgiveropplysningerMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onOverstyrArbeidsgiveropplysninger()
            person.håndterOverstyrArbeidsgiveropplysninger(eventBus, overstyrArbeidsgiveropplysninger, aktivitetslogg)
        }
    }

    override fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onOverstyrArbeidsforhold()
            person.håndterOverstyrArbeidsforhold(eventBus, overstyrArbeidsforhold, aktivitetslogg)
        }
    }

    override fun behandle(message: GrunnbeløpsreguleringMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterGrunnbeløpsregulering(eventBus, grunnbeløpsregulering, aktivitetslogg)
        }
    }

    override fun behandle(message: DødsmeldingMessage, dødsmelding: Dødsmelding, context: MessageContext) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterDødsmelding(eventBus, dødsmelding, aktivitetslogg)
        }
    }

    override fun behandle(
        nyPersonidentifikator: Personidentifikator,
        message: IdentOpphørtMessage,
        identOpphørt: IdentOpphørt,
        gamleIdenter: Set<Personidentifikator>,
        context: MessageContext
    ) {
        hentPersonOgHåndter(nyPersonidentifikator, null, message, context, gamleIdenter) { eventBus, person, aktivitetslogg ->
            if (støtterIdentbytte) {
                person.håndterIdentOpphørt(eventBus, identOpphørt, aktivitetslogg, nyPersonidentifikator)
            }
            context.publish(
                JsonMessage.newMessage(
                    "slackmelding", mapOf(
                    "melding" to "Det er en person som har byttet ident."
                )
                ).toJson()
            )
        }
    }

    override fun behandle(
        message: InfotrygdendringMessage,
        infotrygdEndring: Infotrygdendring,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onInfotrygdendring()
            person.håndterInfotrygdendringer(eventBus, infotrygdEndring, aktivitetslogg)
        }
    }

    override fun behandle(
        message: InntektsendringerMessage,
        inntektsendringer: Inntektsendringer,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onInntektsendringer()
            person.håndterInntektsendringer(eventBus, inntektsendringer, aktivitetslogg)
        }
    }


    override fun behandle(
        message: UtbetalingshistorikkEtterInfotrygdendringMessage,
        utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onUtbetalingshistorikkEtterInfotrygdendring()
            person.håndterUtbetalingshistorikkEtterInfotrygdendring(eventBus, utbetalingshistorikkEtterInfotrygdendring, aktivitetslogg)
        }
    }

    override fun behandle(
        message: ForkastSykmeldingsperioderMessage,
        forkastSykmeldingsperioder: ForkastSykmeldingsperioder,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onForkastSykmeldingsperioder()
            person.håndterForkastSykmeldingsperioder(eventBus, forkastSykmeldingsperioder, aktivitetslogg)
        }
    }

    override fun behandle(
        message: AvbruttSøknadMessage,
        avbruttSøknad: AvbruttSøknad,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            HendelseProbe.onAvbruttSøknad()
            person.håndterAvbruttSøknad(eventBus, avbruttSøknad, aktivitetslogg)
        }
    }

    override fun behandle(
        message: SkjønnsmessigFastsettelseMessage,
        skjønnsmessigFastsettelse: SkjønnsmessigFastsettelse,
        context: MessageContext
    ) {
        hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
            person.håndterSkjønnsmessigFastsettelse(eventBus, skjønnsmessigFastsettelse, aktivitetslogg)
        }
    }

    override fun behandle(
        message: MinimumSykdomsgradVurdertMessage,
        minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurderingMelding,
        context: MessageContext
    ) {
        if (minimumSykdomsgradsvurdering.valider()) {
            hentPersonOgHåndter(message, context) { eventBus, person, aktivitetslogg ->
                person.håndterMinimumSykdomsgradsvurderingMelding(eventBus, minimumSykdomsgradsvurdering, aktivitetslogg)
            }
        }
    }

    private fun opprettPersonOgHåndter(
        personopplysninger: Personopplysninger,
        message: HendelseMessage,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
        handler: (EventBus, Person, IAktivitetslogg) -> Unit
    ) {
        val personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer)
        hentPersonOgHåndter(personidentifikator, personopplysninger, message, context, historiskeFolkeregisteridenter, handler)
    }

    private fun hentPersonOgHåndter(
        message: HendelseMessage,
        context: MessageContext,
        handler: (EventBus, Person, IAktivitetslogg) -> Unit
    ) {
        val personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer)
        hentPersonOgHåndter(personidentifikator, null, message, context, handler = handler)
    }

    private fun hentPersonOgHåndter(
        personidentifikator: Personidentifikator,
        personopplysninger: Personopplysninger?,
        message: HendelseMessage,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator> = emptySet(),
        handler: (EventBus, Person, IAktivitetslogg) -> Unit
    ) {
        val aktivitetslogg = Aktivitetslogg().kontekst(message)

        val subsumsjonMediator = SubsumsjonMediator(message, versjonAvKode)
        val personMediator = PersonMediator(message)
        val datadelingMediator = DatadelingMediator(aktivitetslogg, message)

        val eventBus = EventBus()
        eventBus.register(VedtaksperiodeProbe)

        person(personidentifikator, message, historiskeFolkeregisteridenter, subsumsjonMediator, personopplysninger) { person ->
            handler(eventBus, person, aktivitetslogg)
        }
        ferdigstill(context, eventBus, personMediator, subsumsjonMediator, datadelingMediator, message, aktivitetslogg)
    }

    private fun person(personidentifikator: Personidentifikator, message: HendelseMessage, historiskeFolkeregisteridenter: Set<Personidentifikator>, regelverkslogg: Regelverkslogg, personopplysninger: Personopplysninger?, block: (Person) -> Unit) {
        personDao.hentEllerOpprettPerson(
            regelverkslogg = regelverkslogg,
            personidentifikator = personidentifikator,
            historiskeFolkeregisteridenter = historiskeFolkeregisteridenter,
            message = message,
            hendelseRepository = hendelseRepository,
            lagNyPerson = { personopplysninger?.person(regelverkslogg) },
            håndterPerson = { person -> person.also(block) }
        )
    }

    private fun ferdigstill(
        context: MessageContext,
        eventBus: EventBus,
        personMediator: PersonMediator,
        subsumsjonMediator: SubsumsjonMediator,
        datadelingMediator: DatadelingMediator,
        message: HendelseMessage,
        aktivitetslogg: Aktivitetslogg
    ) {
        personMediator.ferdigstill(context, eventBus)
        subsumsjonMediator.ferdigstill(subsumsjonsproducer)
        datadelingMediator.ferdigstill(context)
        if (aktivitetslogg.aktiviteter.isEmpty()) return
        if (aktivitetslogg.harFunksjonelleFeil()) sikkerLogg.info("aktivitetslogg inneholder errors:\n$aktivitetslogg")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n$aktivitetslogg")
        behovMediator.håndter(context, message, aktivitetslogg)
    }
}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage, context: MessageContext)
    fun behandle(
        personopplysninger: Personopplysninger,
        message: NySøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: NyFrilansSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: NySelvstendigSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: NyJordbrukerSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: NyArbeidsledigTidligereArbeidstakerSøknadMessage,
        sykmelding: Sykmelding,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsgiverMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFrilansMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadSelvstendigMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadJordbrukerMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )
    
    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadFiskerMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadAnnetMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadArbeidsledigTidligereArbeidstakerMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(
        personopplysninger: Personopplysninger,
        message: SendtSøknadNavMessage,
        søknad: Søknad,
        context: MessageContext,
        historiskeFolkeregisteridenter: Set<Personidentifikator>
    )

    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding, context: MessageContext)
    fun behandle(message: NavNoSelvbestemtInntektsmeldingMessage, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: NavNoInntektsmeldingMessage, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: NavNoKorrigertInntektsmeldingMessage, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: InntektsmeldingerReplayMessage, replays: InntektsmeldingerReplay, context: MessageContext)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse, context: MessageContext)
    fun behandle(message: PersonPåminnelseMessage, påminnelse: PersonPåminnelse, context: MessageContext)
    fun behandle(message: GjenopptaBehandlingMessage, gjenopptaBehandling: GjenopptaBehandling, context: MessageContext)
    fun behandle(message: AnmodningOmForkastingMessage, anmodning: AnmodningOmForkasting, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk, context: MessageContext)
    fun behandle(message: UtbetalingshistorikkForFeriepengerMessage, utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger, context: MessageContext)
    fun behandle(message: YtelserMessage, ytelser: Ytelser, context: MessageContext)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag, context: MessageContext)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning, context: MessageContext)
    fun behandle(message: FeriepengeutbetalingMessage, utbetaling: FeriepengeutbetalingHendelse, context: MessageContext)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse, context: MessageContext)
    fun behandle(message: SimuleringMessage, simulering: Simulering, context: MessageContext)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling, context: MessageContext)
    fun behandle(message: AvstemmingMessage, personidentifikator: Personidentifikator, context: MessageContext)
    fun behandle(message: MigrateMessage, migrate: Migrate, context: MessageContext)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje, context: MessageContext)
    fun behandle(message: OverstyrArbeidsgiveropplysningerMessage, overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, context: MessageContext)
    fun behandle(message: OverstyrArbeidsforholdMessage, overstyrArbeidsforhold: OverstyrArbeidsforhold, context: MessageContext)
    fun behandle(message: GrunnbeløpsreguleringMessage, grunnbeløpsregulering: Grunnbeløpsregulering, context: MessageContext)
    fun behandle(message: InfotrygdendringMessage, infotrygdEndring: Infotrygdendring, context: MessageContext)
    fun behandle(message: InntektsendringerMessage, inntektsendringer: Inntektsendringer, context: MessageContext)
    fun behandle(message: DødsmeldingMessage, dødsmelding: Dødsmelding, context: MessageContext)
    fun behandle(
        nyPersonidentifikator: Personidentifikator,
        message: IdentOpphørtMessage,
        identOpphørt: IdentOpphørt,
        gamleIdenter: Set<Personidentifikator>,
        context: MessageContext
    )

    fun behandle(message: UtbetalingshistorikkEtterInfotrygdendringMessage, utbetalingshistorikkEtterInfotrygdendring: UtbetalingshistorikkEtterInfotrygdendring, context: MessageContext)
    fun behandle(message: ForkastSykmeldingsperioderMessage, forkastSykmeldingsperioder: ForkastSykmeldingsperioder, context: MessageContext)
    fun behandle(message: AvbruttSøknadMessage, avbruttSøknad: AvbruttSøknad, context: MessageContext)
    fun behandle(message: SkjønnsmessigFastsettelseMessage, skjønnsmessigFastsettelse: SkjønnsmessigFastsettelse, context: MessageContext)
    fun behandle(message: MinimumSykdomsgradVurdertMessage, minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurderingMelding, context: MessageContext)
}
