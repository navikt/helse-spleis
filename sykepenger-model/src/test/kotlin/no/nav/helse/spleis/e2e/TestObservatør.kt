package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spill_av_im.Forespørsel
import org.junit.jupiter.api.fail

internal typealias InntektsmeldingId = UUID
internal typealias VedtaksperiodeId = UUID

internal class TestObservatør(person: Person? = null) : PersonObserver {
    init {
        person?.addObserver(this)
    }

    internal val tilstandsendringer = person?.inspektør?.sisteVedtaksperiodeTilstander()?.mapValues { mutableListOf(it.value) }?.toMutableMap() ?: mutableMapOf()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val trengerArbeidsgiveropplysningerVedtaksperioder = mutableListOf<PersonObserver.TrengerArbeidsgiveropplysningerEvent>()
    val trengerIkkeArbeidsgiveropplysningerVedtaksperioder = mutableListOf<PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent>()
    val utbetalingUtenUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val utbetalingMedUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val feriepengerUtbetaltEventer = mutableListOf<PersonObserver.FeriepengerUtbetaltEvent>()
    val utbetaltEndretEventer = mutableListOf<PersonObserver.UtbetalingEndretEvent>()
    val avsluttetMedVedtakEvent = mutableMapOf<UUID, PersonObserver.AvsluttetMedVedtakEvent>()
    val avsluttetMedVedtakEventer = mutableMapOf<UUID, MutableList<PersonObserver.AvsluttetMedVedtakEvent>>()
    val behandlingOpprettetEventer = mutableListOf<PersonObserver.BehandlingOpprettetEvent>()
    val behandlingLukketEventer = mutableListOf<PersonObserver.BehandlingLukketEvent>()
    val behandlingForkastetEventer = mutableListOf<PersonObserver.BehandlingForkastetEvent>()
    val avsluttetUtenVedtakEventer = mutableMapOf<UUID, MutableList<PersonObserver.AvsluttetUtenVedtakEvent>>()
    val overstyringIgangsatt = mutableListOf<PersonObserver.OverstyringIgangsatt>()
    val vedtaksperiodeVenter = mutableListOf<PersonObserver.VedtaksperiodeVenterEvent>()
    val inntektsmeldingFørSøknad = mutableListOf<PersonObserver.InntektsmeldingFørSøknadEvent>()
    val inntektsmeldingIkkeHåndtert = mutableListOf<InntektsmeldingId>()
    val inntektsmeldingHåndtert = mutableListOf<Pair<InntektsmeldingId, VedtaksperiodeId>>()
    val skatteinntekterLagtTilGrunnEventer = mutableListOf<PersonObserver.SkatteinntekterLagtTilGrunnEvent>()
    val søknadHåndtert = mutableListOf<Pair<UUID, UUID>>()
    val vedtaksperiodeAnnullertEventer = mutableListOf<PersonObserver.VedtaksperiodeAnnullertEvent>()
    val vedtaksperiodeOpprettetEventer = mutableListOf<PersonObserver.VedtaksperiodeOpprettet>()
    val overlappendeInfotrygdperioder = mutableListOf<PersonObserver.OverlappendeInfotrygdperioder>()
    val utkastTilVedtakEventer = mutableListOf<PersonObserver.UtkastTilVedtakEvent>()
    val sykefraværstilfelleIkkeFunnet = mutableListOf<PersonObserver.SykefraværstilfelleIkkeFunnet>()
    val analytiskDatapakkeEventer = mutableListOf<PersonObserver.AnalytiskDatapakkeEvent>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = person?.inspektør?.vedtaksperioder()?.mapValues { (_, perioder) ->
        perioder.map { it.inspektør.id }.toMutableSet()
    }?.toMutableMap() ?: mutableMapOf()

    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    private val forkastedeEventer = mutableMapOf<UUID, PersonObserver.VedtaksperiodeForkastetEvent>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    val inntektsmeldingReplayEventer = mutableListOf<Forespørsel>()

    internal fun replayInntektsmeldinger(block: () -> Unit): Set<Forespørsel> {
        val replaysFør = inntektsmeldingReplayEventer.toSet()
        block()
        return inntektsmeldingReplayEventer.toSet() - replaysFør
    }

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = IdInnhenter { orgnummer -> vedtaksperioder.getValue(orgnummer).last() }

    fun sisteVedtaksperiodeId(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun kvitterInntektsmeldingReplay(vedtaksperiodeId: UUID) {
        inntektsmeldingReplayEventer.removeAll { it.vedtaksperiodeId == vedtaksperiodeId }
    }

    override fun analytiskDatapakke(event: PersonObserver.AnalytiskDatapakkeEvent) {
        this.analytiskDatapakkeEventer.add(event)
    }

    override fun vedtaksperioderVenter(eventer: List<PersonObserver.VedtaksperiodeVenterEvent>) {
        vedtaksperiodeVenter.addAll(eventer)
    }

    fun forkastedePerioder() = forkastedeEventer.size
    fun forkastet(vedtaksperiodeId: UUID) = forkastedeEventer.getValue(vedtaksperiodeId)

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingUtenUtbetalingEventer.add(event)
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingMedUtbetalingEventer.add(event)
    }

    override fun feriepengerUtbetalt(event: PersonObserver.FeriepengerUtbetaltEvent) {
        feriepengerUtbetaltEventer.add(event)
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        utbetaltEndretEventer.add(event)
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        avsluttetMedVedtakEvent[event.vedtaksperiodeId] = event
        avsluttetMedVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        behandlingOpprettetEventer.add(event)
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        behandlingLukketEventer.add(event)
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        behandlingForkastetEventer.add(event)
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        avsluttetUtenVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun vedtaksperiodeOpprettet(event: PersonObserver.VedtaksperiodeOpprettet) {
        vedtaksperiodeOpprettetEventer.add(event)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
        vedtaksperioder.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { mutableSetOf() }.add(sisteVedtaksperiode)
        tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event.forrigeTilstand) }.add(event.gjeldendeTilstand)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)

        if (event.forrigeTilstand == TilstandType.AVVENTER_INNTEKTSMELDING) {
            trengerArbeidsgiveroppysninger.remove(event.vedtaksperiodeId)
        }
    }

    internal fun nullstillTilstandsendringer() {
        tilstandsendringer.replaceAll { _, value -> mutableListOf(value.last()) }
    }

    private val trengerArbeidsgiveroppysninger = mutableMapOf<UUID, Set<PersonObserver.ForespurtOpplysning>>()
    internal fun forsikreForespurteArbeidsgiveropplysninger(vedtaksperiodeId: UUID, vararg oppgitt: Arbeidsgiveropplysning) {
        val forespurt = trengerArbeidsgiveroppysninger[vedtaksperiodeId] ?: error("Det er ikke forespurt arbeidsgiveropplysninger for $vedtaksperiodeId")
        if (oppgitt.isEmpty()) return
        val relevante = oppgitt.filter { it is Arbeidsgiveropplysning.OppgittInntekt || it is Arbeidsgiveropplysning.OppgittArbeidgiverperiode || it is Arbeidsgiveropplysning.OppgittRefusjon }

        val forespurteOpplysninger = forespurt.map { it.somArbeidsgiveropplysning }.toSet()
        val oppgittOpplysninger = relevante.map { it::class }.toSet()

        val ikkeForespurt = (oppgittOpplysninger - forespurteOpplysninger).takeUnless { it.isEmpty() } ?: return

        // Denne kan du endre fra println til error om du vil kose deg.
        // Som oftest er det feil i test-setup om man sender inn noe vi ikke har spurt om, men ettersom det er enkelte tester som eksplisitt tester at man sender inn mer enn man spør om kan det ikke være error til vanlig
        println("Spurte ikke om ${ikkeForespurt.joinToString { it.simpleName!! }}, men fikk det læll")
    }

    private val PersonObserver.ForespurtOpplysning.somArbeidsgiveropplysning
        get() = when (this) {
            PersonObserver.Arbeidsgiverperiode -> Arbeidsgiveropplysning.OppgittArbeidgiverperiode::class
            is PersonObserver.Inntekt -> Arbeidsgiveropplysning.OppgittInntekt::class
            PersonObserver.Refusjon -> Arbeidsgiveropplysning.OppgittRefusjon::class
        }

    override fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        trengerArbeidsgiveropplysningerVedtaksperioder.add(event)
        trengerArbeidsgiveroppysninger[event.vedtaksperiodeId] = event.forespurteOpplysninger
    }

    override fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        trengerIkkeArbeidsgiveropplysningerVedtaksperioder.add(event)
        trengerArbeidsgiveroppysninger.remove(event.vedtaksperiodeId)
    }

    override fun inntektsmeldingReplay(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        inntektsmeldingReplayEventer.add(
            Forespørsel(
                fnr = event.personidentifikator.toString(),
                orgnr = event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                vedtaksperiodeId = event.vedtaksperiodeId,
                skjæringstidspunkt = event.skjæringstidspunkt,
                førsteFraværsdager = event.førsteFraværsdager.map { no.nav.helse.spill_av_im.FørsteFraværsdag(it.yrkesaktivitetssporing.somOrganisasjonsnummer, it.førsteFraværsdag) },
                sykmeldingsperioder = event.sykmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                egenmeldinger = event.egenmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                harForespurtArbeidsgiverperiode = PersonObserver.Arbeidsgiverperiode in event.forespurteOpplysninger
            )
        )
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        forkastedeEventer[event.vedtaksperiodeId] = event
    }

    override fun overstyringIgangsatt(
        event: PersonObserver.OverstyringIgangsatt
    ) {
        overstyringIgangsatt.add(event)
    }

    override fun overlappendeInfotrygdperioder(event: PersonObserver.OverlappendeInfotrygdperioder) {
        overlappendeInfotrygdperioder.add(event)
    }

    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) {
        inntektsmeldingFørSøknad.add(event)
    }

    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, speilrelatert: Boolean) {
        inntektsmeldingIkkeHåndtert.add(inntektsmeldingId)
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        inntektsmeldingHåndtert.add(inntektsmeldingId to vedtaksperiodeId)
        trengerArbeidsgiveroppysninger.remove(vedtaksperiodeId)
    }

    override fun skatteinntekterLagtTilGrunn(event: PersonObserver.SkatteinntekterLagtTilGrunnEvent) {
        skatteinntekterLagtTilGrunnEventer.add(event)
    }

    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        søknadHåndtert.add(søknadId to vedtaksperiodeId)
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        vedtaksperiodeAnnullertEventer.add(vedtaksperiodeAnnullertEvent)
    }

    override fun utkastTilVedtak(event: PersonObserver.UtkastTilVedtakEvent) {
        utkastTilVedtakEventer.add(event)
    }

    override fun sykefraværstilfelleIkkeFunnet(event: PersonObserver.SykefraværstilfelleIkkeFunnet) {
        sykefraværstilfelleIkkeFunnet.add(event)
    }
}
