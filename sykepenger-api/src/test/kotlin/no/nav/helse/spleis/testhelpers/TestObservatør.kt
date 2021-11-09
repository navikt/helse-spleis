package no.nav.helse.spleis.testhelpers

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.person.*
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import org.junit.jupiter.api.fail
import java.util.*

internal class TestObservatør : PersonObserver {
    internal val tilstandsendringer = mutableMapOf<UUID, MutableList<TilstandType>>()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val reberegnedeVedtaksperioder = mutableListOf<UUID>()
    val manglendeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val trengerIkkeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val utbetalingUtenUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val utbetalingMedUtbetalingEventer = mutableListOf<PersonObserver.UtbetalingUtbetaltEvent>()
    val feriepengerUtbetaltEventer = mutableListOf<PersonObserver.FeriepengerUtbetaltEvent>()
    val utbetaltEndretEventer = mutableListOf<ObservedEvent<PersonObserver.UtbetalingEndretEvent>>()
    val vedtakFattetEvent = mutableMapOf<UUID, PersonObserver.VedtakFattetEvent>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    val utbetaltEventer = mutableListOf<PersonObserver.UtbetaltEvent>()
    private val avbruttEventer = mutableMapOf<UUID, TilstandType>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    lateinit var avstemming: Map<String, Any>
    val inntektsmeldingReplayEventer = mutableListOf<UUID>()

    val avvisteRevurderinger = mutableListOf<PersonObserver.RevurderingAvvistEvent>()

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = sisteVedtaksperiode
    fun sisteVedtaksperiode(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun vedtaksperiodeIndeks(orgnummer: String, id: UUID) = vedtaksperioder.getValue(orgnummer).indexOf(id)
    fun bedtOmInntektsmeldingReplay(vedtaksperiodeId: UUID) = vedtaksperiodeId in inntektsmeldingReplayEventer

    fun avbruttePerioder() = avbruttEventer.size
    fun avbrutt(vedtaksperiodeId: UUID) = avbruttEventer.getValue(vedtaksperiodeId)

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
        vedtaksperiodeendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(hendelseskontekst.orgnummer()) { mutableSetOf() }.add(sisteVedtaksperiode)
        if (event.gjeldendeTilstand != event.forrigeTilstand) {
            tilstandsendringer.getOrPut(hendelseskontekst.vedtaksperiodeId()) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        }
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    override fun vedtaksperiodeReberegnet(hendelseskontekst: Hendelseskontekst) {
        reberegnedeVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    override fun manglerInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    override fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(hendelseskontekst.vedtaksperiodeId())
    }

    override fun inntektsmeldingReplay(fødselsnummer: Fødselsnummer, vedtaksperiodeId: UUID) {
        inntektsmeldingReplayEventer.add(vedtaksperiodeId)
    }

    override fun vedtaksperiodeUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetaltEvent) {
        utbetaltEventer.add(event)
    }

    override fun annullering(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeAvbrutt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        avbruttEventer[hendelseskontekst.vedtaksperiodeId()] = event.gjeldendeTilstand
    }

    override fun revurderingAvvist(hendelseskontekst: Hendelseskontekst, event: PersonObserver.RevurderingAvvistEvent) {
        avvisteRevurderinger.add(event)
    }

    companion object {
        private fun Hendelseskontekst.toMap() = mutableMapOf<String, String>().also { appendTo(it::set) }

        private fun Hendelseskontekst.vedtaksperiodeId(): UUID {
            return UUID.fromString(toMap()["vedtaksperiodeId"])
        }

        private fun Hendelseskontekst.orgnummer(): String {
            return toMap()["organisasjonsnummer"]!!
        }

        data class ObservedEvent<EventType>(val event: EventType, private val kontekst: Hendelseskontekst) {
            fun vedtaksperiodeId() = kontekst.vedtaksperiodeId()
            fun orgnummer() = kontekst.orgnummer()
        }
    }
}
