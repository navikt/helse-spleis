package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Person
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.VedtaksperiodeEndretEvent
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spill_av_im.Forespørsel
import org.junit.jupiter.api.fail

internal typealias InntektsmeldingId = UUID
internal typealias VedtaksperiodeId = UUID

internal class TestObservatør(person: Person? = null, other: TestObservatør? = null) : EventSubscription {

    internal val tilstandsendringer = person?.inspektør?.sisteVedtaksperiodeTilstander()?.mapValues { mutableListOf(it.value) }?.toMutableMap() ?: mutableMapOf()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val trengerArbeidsgiveropplysningerVedtaksperioder = mutableListOf<EventSubscription.TrengerArbeidsgiveropplysningerEvent>()
    val trengerIkkeArbeidsgiveropplysningerVedtaksperioder = mutableListOf<EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent>()
    val utbetalingUtenUtbetalingEventer = mutableListOf<EventSubscription.UtbetalingUtenUtbetalingEvent>()
    val utbetalingMedUtbetalingEventer = mutableListOf<EventSubscription.UtbetalingUtbetaltEvent>()
    val feriepengerUtbetaltEventer = mutableListOf<EventSubscription.FeriepengerUtbetaltEvent>()
    val utbetaltEndretEventer = mutableListOf<EventSubscription.UtbetalingEndretEvent>()
    val avsluttetMedVedtakEvent = mutableMapOf<UUID, EventSubscription.AvsluttetMedVedtakEvent>()
    val avsluttetMedVedtakEventer = mutableMapOf<UUID, MutableList<EventSubscription.AvsluttetMedVedtakEvent>>()
    val behandlingOpprettetEventer = mutableListOf<EventSubscription.BehandlingOpprettetEvent>()
    val behandlingLukketEventer = mutableListOf<EventSubscription.BehandlingLukketEvent>()
    val behandlingForkastetEventer = mutableListOf<EventSubscription.BehandlingForkastetEvent>()
    val avsluttetUtenVedtakEventer = mutableMapOf<UUID, MutableList<EventSubscription.AvsluttetUtenVedtakEvent>>()
    val overstyringIgangsatt = mutableListOf<EventSubscription.OverstyringIgangsatt>()
    val vedtaksperiodeVenter = mutableListOf<EventSubscription.VedtaksperiodeVenterEvent>()
    val inntektsmeldingFørSøknad = mutableListOf<EventSubscription.InntektsmeldingFørSøknadEvent>()
    val inntektsmeldingIkkeHåndtert = mutableListOf<InntektsmeldingId>()
    val inntektsmeldingHåndtert: MutableList<Pair<InntektsmeldingId, VedtaksperiodeId>> = other?.inntektsmeldingHåndtert ?: mutableListOf()
    val skatteinntekterLagtTilGrunnEventer = mutableListOf<EventSubscription.SkatteinntekterLagtTilGrunnEvent>()
    val søknadHåndtert = mutableListOf<Pair<UUID, UUID>>()
    val vedtaksperiodeAnnullertEventer = mutableListOf<EventSubscription.VedtaksperiodeAnnullertEvent>()
    val vedtaksperiodeOpprettetEventer = mutableListOf<EventSubscription.VedtaksperiodeOpprettet>()
    val overlappendeInfotrygdperioder = mutableListOf<EventSubscription.OverlappendeInfotrygdperioder>()
    val utkastTilVedtakEventer = mutableListOf<EventSubscription.UtkastTilVedtakEvent>()
    val sykefraværstilfelleIkkeFunnet = mutableListOf<EventSubscription.SykefraværstilfelleIkkeFunnet>()
    val analytiskDatapakkeEventer = mutableListOf<EventSubscription.AnalytiskDatapakkeEvent>()

    val vedtaksperiodeUtbetalinger = mutableMapOf<String, MutableMap<UUID, List<UUID>>>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = person?.inspektør?.vedtaksperioder()?.mapValues { (_, perioder) ->
        perioder.map { it.inspektør.id }.toMutableSet()
    }?.toMutableMap() ?: mutableMapOf()

    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    private val forkastedeEventer = mutableMapOf<UUID, EventSubscription.VedtaksperiodeForkastetEvent>()
    val annulleringer = mutableListOf<EventSubscription.UtbetalingAnnullertEvent>()
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

    override fun analytiskDatapakke(event: EventSubscription.AnalytiskDatapakkeEvent) {
        this.analytiskDatapakkeEventer.add(event)
    }

    override fun vedtaksperioderVenter(event: EventSubscription.VedtaksperioderVenterEvent) {
        vedtaksperiodeVenter.addAll(event.vedtaksperioder)
    }

    fun forkastedePerioder() = forkastedeEventer.size
    fun forkastet(vedtaksperiodeId: UUID) = forkastedeEventer.getValue(vedtaksperiodeId)

    override fun utbetalingUtenUtbetaling(event: EventSubscription.UtbetalingUtenUtbetalingEvent) {
        utbetalingUtenUtbetalingEventer.add(event)
    }

    override fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        utbetalingMedUtbetalingEventer.add(event)
    }

    override fun feriepengerUtbetalt(event: EventSubscription.FeriepengerUtbetaltEvent) {
        feriepengerUtbetaltEventer.add(event)
    }

    override fun utbetalingEndret(event: EventSubscription.UtbetalingEndretEvent) {
        utbetaltEndretEventer.add(event)
    }

    override fun avsluttetMedVedtak(event: EventSubscription.AvsluttetMedVedtakEvent) {
        avsluttetMedVedtakEvent[event.vedtaksperiodeId] = event
        avsluttetMedVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        behandlingOpprettetEventer.add(event)
    }

    override fun behandlingLukket(event: EventSubscription.BehandlingLukketEvent) {
        behandlingLukketEventer.add(event)
    }

    override fun behandlingForkastet(event: EventSubscription.BehandlingForkastetEvent) {
        behandlingForkastetEventer.add(event)
    }

    override fun avsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent) {
        avsluttetUtenVedtakEventer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
    }

    override fun vedtaksperiodeOpprettet(event: EventSubscription.VedtaksperiodeOpprettet) {
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

    private val trengerArbeidsgiveroppysninger = mutableMapOf<UUID, Set<EventSubscription.ForespurtOpplysning>>()
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

    private val EventSubscription.ForespurtOpplysning.somArbeidsgiveropplysning
        get() = when (this) {
            EventSubscription.Arbeidsgiverperiode -> Arbeidsgiveropplysning.OppgittArbeidgiverperiode::class
            is EventSubscription.Inntekt -> Arbeidsgiveropplysning.OppgittInntekt::class
            EventSubscription.Refusjon -> Arbeidsgiveropplysning.OppgittRefusjon::class
        }

    override fun trengerArbeidsgiveropplysninger(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent) {
        trengerArbeidsgiveropplysningerVedtaksperioder.add(event)
        trengerArbeidsgiveroppysninger[event.opplysninger.vedtaksperiodeId] = event.opplysninger.forespurteOpplysninger
    }

    override fun trengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent) {
        trengerIkkeArbeidsgiveropplysningerVedtaksperioder.add(event)
        trengerArbeidsgiveroppysninger.remove(event.vedtaksperiodeId)
    }

    override fun inntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent) {
        inntektsmeldingReplayEventer.add(
            Forespørsel(
                fnr = event.opplysninger.personidentifikator.toString(),
                orgnr = event.opplysninger.arbeidstaker.organisasjonsnummer,
                vedtaksperiodeId = event.opplysninger.vedtaksperiodeId,
                skjæringstidspunkt = event.opplysninger.skjæringstidspunkt,
                førsteFraværsdager = event.opplysninger.førsteFraværsdager.map { no.nav.helse.spill_av_im.FørsteFraværsdag(it.arbeidstaker.organisasjonsnummer, it.førsteFraværsdag) },
                sykmeldingsperioder = event.opplysninger.sykmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                egenmeldinger = event.opplysninger.egenmeldingsperioder.map { no.nav.helse.spill_av_im.Periode(it.start, it.endInclusive) },
                harForespurtArbeidsgiverperiode = EventSubscription.Arbeidsgiverperiode in event.opplysninger.forespurteOpplysninger
            )
        )
    }

    override fun annullering(event: EventSubscription.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeForkastet(event: EventSubscription.VedtaksperiodeForkastetEvent) {
        forkastedeEventer[event.vedtaksperiodeId] = event
    }

    override fun overstyringIgangsatt(
        event: EventSubscription.OverstyringIgangsatt
    ) {
        overstyringIgangsatt.add(event)
    }

    override fun overlappendeInfotrygdperioder(event: EventSubscription.OverlappendeInfotrygdperioder) {
        overlappendeInfotrygdperioder.add(event)
    }

    override fun inntektsmeldingFørSøknad(event: EventSubscription.InntektsmeldingFørSøknadEvent) {
        inntektsmeldingFørSøknad.add(event)
    }

    override fun inntektsmeldingIkkeHåndtert(event: EventSubscription.InntektsmeldingIkkeHåndtertEvent) {
        inntektsmeldingIkkeHåndtert.add(event.meldingsreferanseId)
    }

    override fun inntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent) {
        inntektsmeldingHåndtert.add(event.meldingsreferanseId to event.vedtaksperiodeId)
        trengerArbeidsgiveroppysninger.remove(event.vedtaksperiodeId)
    }

    override fun skatteinntekterLagtTilGrunn(event: EventSubscription.SkatteinntekterLagtTilGrunnEvent) {
        skatteinntekterLagtTilGrunnEventer.add(event)
    }

    override fun søknadHåndtert(event: EventSubscription.SøknadHåndtertEvent) {
        søknadHåndtert.add(event.meldingsreferanseId to event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent) {
        vedtaksperiodeAnnullertEventer.add(vedtaksperiodeAnnullertEvent)
    }

    override fun utkastTilVedtak(event: EventSubscription.UtkastTilVedtakEvent) {
        utkastTilVedtakEventer.add(event)
    }

    override fun sykefraværstilfelleIkkeFunnet(event: EventSubscription.SykefraværstilfelleIkkeFunnet) {
        sykefraværstilfelleIkkeFunnet.add(event)
    }

    override fun nyVedtaksperiodeUtbetaling(event: EventSubscription.VedtaksperiodeNyUtbetalingEvent) {
        vedtaksperiodeUtbetalinger
            .getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { mutableMapOf() }
            .compute(event.vedtaksperiodeId) { _, eksisterende ->
                (eksisterende ?: emptyList()).plusElement(event.utbetalingId)
            }
    }

    fun vedtaksperiodeUtbetalinger(vedtaksperiode: IdInnhenter, orgnummer: String): List<UUID> {
        return vedtaksperiodeUtbetalinger[orgnummer]?.get(vedtaksperiode.id(orgnummer)) ?: emptyList()
    }
}
