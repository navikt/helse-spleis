package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Hendelseskontekst
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
    val utbetaltEndretEventer = mutableListOf<ObservedEvent<PersonObserver.UtbetalingEndretEvent>>()
    val vedtakFattetEvent = mutableMapOf<UUID, PersonObserver.VedtakFattetEvent>()

    val revurderingIgangsattEvent = mutableListOf<PersonObserver.RevurderingIgangsattEvent>()

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

    val avvisteRevurderinger = mutableListOf<PersonObserver.RevurderingAvvistEvent>()

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

    override fun avstemt(hendelseskontekst: Hendelseskontekst, result: Map<String, Any>) {
        avstemming = result
    }

    override fun utbetalingUtenUtbetaling(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingUtenUtbetalingEventer.add(event)
    }

    override fun utbetalingUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingMedUtbetalingEventer.add(event)
    }

    override fun feriepengerUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.FeriepengerUtbetaltEvent) {
        feriepengerUtbetaltEventer.add(event)
    }

    override fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingEndretEvent) {
        utbetaltEndretEventer.add(ObservedEvent(event, hendelseskontekst))
    }

    override fun vedtakFattet(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtakFattetEvent) {
        vedtakFattetEvent[hendelseskontekst.vedtaksperiodeId()] = event
    }

    override fun vedtaksperiodeEndret(hendelseskontekst: Hendelseskontekst, event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = hendelseskontekst.vedtaksperiodeId()
        vedtaksperiodeendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf() }.add(event)
        vedtaksperioder.getOrPut(hendelseskontekst.orgnummer()) { mutableSetOf() }.add(sisteVedtaksperiode)
        tilstandsendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf(event.forrigeTilstand) }.add(event.gjeldendeTilstand)
        inntektsmeldingReplayEventer.remove(sisteVedtaksperiode)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    internal fun nullstillTilstandsendringer() {
        tilstandsendringer.replaceAll { _, value -> mutableListOf(value.last()) }
    }

    override fun manglerInntektsmelding(hendelseskontekst: Hendelseskontekst, orgnr: String, event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event)
    }

    override fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(event)
    }

    override fun trengerArbeidsgiveropplysninger(hendelseskontekst: Hendelseskontekst, event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        trengerArbeidsgiveropplysningerVedtaksperioder.add(event)
    }

    override fun inntektsmeldingReplay(personidentifikator: Personidentifikator, vedtaksperiodeId: UUID) {
        inntektsmeldingReplayEventer.add(vedtaksperiodeId)
    }

    override fun annullering(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeForkastet(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtaksperiodeForkastetEvent) {
        forkastedeEventer[hendelseskontekst.vedtaksperiodeId()] = event
    }

    override fun opprettOppgaveForSpeilsaksbehandlere(hendelseskontekst: Hendelseskontekst, event: PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent) {
        opprettOppgaverTilSpeilsaksbehandlerEventer.add(event)
    }

    override fun opprettOppgave(hendelseskontekst: Hendelseskontekst, event: PersonObserver.OpprettOppgaveEvent) {
        opprettOppgaverEventer.add(event)
    }

    override fun utsettOppgave(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtsettOppgaveEvent) {
        utsettOppgaveEventer.add(event)
    }

    override fun revurderingAvvist(hendelseskontekst: Hendelseskontekst, event: PersonObserver.RevurderingAvvistEvent) {
        avvisteRevurderinger.add(event)
    }

    override fun revurderingIgangsatt(
        event: PersonObserver.RevurderingIgangsattEvent,
        personidentifikator: Personidentifikator,
        aktørId: String
    ) {
        revurderingIgangsattEvent.add(event)
    }

    companion object {
        private fun Hendelseskontekst.toMap() = mutableMapOf<String, String>().also { appendTo(it::set) }

        private fun Hendelseskontekst.vedtaksperiodeId(): UUID {
            return UUID.fromString(toMap()["vedtaksperiodeId"])
        }

        private fun Hendelseskontekst.orgnummer() = toMap()["organisasjonsnummer"]!!

        data class ObservedEvent<EventType>(val event: EventType, private val kontekst: Hendelseskontekst) {
            fun vedtaksperiodeId() = kontekst.vedtaksperiodeId()
            fun orgnummer() = kontekst.orgnummer()
        }
    }
}
