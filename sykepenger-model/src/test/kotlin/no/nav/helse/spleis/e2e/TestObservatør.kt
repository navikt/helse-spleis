package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.fail

internal class TestObservatør(person: Person? = null) : PersonObserver {
    init {
        person?.addObserver(this)
    }
    internal val tilstandsendringer = person?.inspektør?.sisteVedtaksperiodeTilstander()?.mapValues { mutableListOf(it.value) }?.toMutableMap() ?: mutableMapOf()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val manglendeInntektsmeldingVedtaksperioder = mutableListOf<PersonObserver.ManglendeInntektsmeldingEvent>()
    val trengerIkkeInntektsmeldingVedtaksperioder = mutableListOf<PersonObserver.TrengerIkkeInntektsmeldingEvent>()
    val trengerArbeidsgiveropplysningerVedtaksperioder = mutableListOf<PersonObserver.TrengerArbeidsgiveropplysningerEvent>()
    val utbetalingUtenUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val utbetalingMedUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val feriepengerUtbetaltEventer = mutableListOf<PersonObserver.FeriepengerUtbetaltEvent>()
    val utbetaltEndretEventer = mutableListOf<PersonObserver.UtbetalingEndretEvent>()
    val vedtakFattetEvent = mutableMapOf<UUID, PersonObserver.VedtakFattetEvent>()

    val overstyringIgangsatt = mutableListOf<PersonObserver.OverstyringIgangsatt>()

    val opprettOppgaverTilSpeilsaksbehandlerEventer = mutableListOf<PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent>()
    val opprettOppgaverEventer = mutableListOf<PersonObserver.OpprettOppgaveEvent>()
    private val utsettOppgaveEventer = mutableListOf<PersonObserver.UtsettOppgaveEvent>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = person?.inspektør?.vedtaksperioder()?.mapValues { (_, perioder) ->
        perioder.map { it.inspektør.id }.toMutableSet()
    }?.toMutableMap() ?: mutableMapOf()

    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    private val forkastedeEventer = mutableMapOf<UUID, PersonObserver.VedtaksperiodeForkastetEvent>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    lateinit var avstemming: Map<String, Any>
    val inntektsmeldingReplayEventer = mutableListOf<UUID>()

    internal fun replayInntektsmeldinger(block: () -> Unit): Set<UUID> {
        val replaysFør = inntektsmeldingReplayEventer.toSet()
        block()
        return inntektsmeldingReplayEventer.toSet() - replaysFør
    }

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = IdInnhenter { orgnummer -> vedtaksperioder.getValue(orgnummer).last() }

    fun sisteVedtaksperiodeId(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun sisteVedtaksperiodeIdOrNull(orgnummer: String) = vedtaksperioder[orgnummer]?.last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun bedtOmInntektsmeldingReplay(vedtaksperiodeId: UUID) = vedtaksperiodeId in inntektsmeldingReplayEventer
    fun kvitterInntektsmeldingReplay(vedtaksperiodeId: UUID) {
        inntektsmeldingReplayEventer.remove(vedtaksperiodeId)
    }
    fun forkastedePerioder() = forkastedeEventer.size
    fun forkastet(vedtaksperiodeId: UUID) = forkastedeEventer.getValue(vedtaksperiodeId)
    fun opprettOppgaveForSpeilsaksbehandlereEvent() = opprettOppgaverTilSpeilsaksbehandlerEventer.toList()
    fun opprettOppgaveEvent() = opprettOppgaverEventer.toList()
    fun utsettOppgaveEventer() = utsettOppgaveEventer.toList()

    override fun avstemt(result: Map<String, Any>) {
        avstemming = result
    }

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

    override fun vedtakFattet(event: PersonObserver.VedtakFattetEvent) {
        vedtakFattetEvent[event.vedtaksperiodeId] = event
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf() }.add(event)
        vedtaksperioder.getOrPut(event.organisasjonsnummer) { mutableSetOf() }.add(sisteVedtaksperiode)
        tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event.forrigeTilstand) }.add(event.gjeldendeTilstand)
        inntektsmeldingReplayEventer.remove(sisteVedtaksperiode)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }

    internal fun nullstillTilstandsendringer() {
        tilstandsendringer.replaceAll { _, value -> mutableListOf(value.last()) }
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event)
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(event)
    }

    override fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        trengerArbeidsgiveropplysningerVedtaksperioder.add(event)
    }

    override fun inntektsmeldingReplay(personidentifikator: Personidentifikator, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, førsteDagIArbeidsgiverperioden: LocalDate?) {
        inntektsmeldingReplayEventer.add(vedtaksperiodeId)
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        forkastedeEventer[event.vedtaksperiodeId] = event
    }

    override fun opprettOppgaveForSpeilsaksbehandlere(event: PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent) {
        opprettOppgaverTilSpeilsaksbehandlerEventer.add(event)
    }

    override fun opprettOppgave(event: PersonObserver.OpprettOppgaveEvent) {
        opprettOppgaverEventer.add(event)
    }

    override fun utsettOppgave(event: PersonObserver.UtsettOppgaveEvent) {
        utsettOppgaveEventer.add(event)
    }

    override fun overstyringIgangsatt(
        event: PersonObserver.OverstyringIgangsatt
    ) {
        overstyringIgangsatt.add(event)
    }
}
