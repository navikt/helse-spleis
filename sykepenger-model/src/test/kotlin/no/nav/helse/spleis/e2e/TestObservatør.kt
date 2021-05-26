package no.nav.helse.spleis.e2e

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import no.nav.helse.person.TilstandType
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
    val vedtakFattetEvent = mutableMapOf<UUID, PersonObserver.VedtakFattetEvent>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()

    val utbetaltEventer = mutableListOf<PersonObserver.UtbetaltEvent>()
    private val avbruttEventer = mutableMapOf<UUID, TilstandType>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    lateinit var avstemming: Map<String, Any>
    val inntektsmeldingReplayEventer = mutableListOf<UUID>()

    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = sisteVedtaksperiode
    fun sisteVedtaksperiode(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun vedtaksperiodeIndeks(orgnummer: String, id: UUID) = vedtaksperioder.getValue(orgnummer).indexOf(id)
    fun bedtOmInntektsmeldingReplay(vedtaksperiodeId: UUID) = vedtaksperiodeId in inntektsmeldingReplayEventer

    fun avbruttePerioder() = avbruttEventer.size
    fun avbrutt(vedtaksperiodeId: UUID) = avbruttEventer.getValue(vedtaksperiodeId)

    override fun avstemt(result: Map<String, Any>) {
        avstemming = result
    }

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingUtenUtbetalingEventer.add(event)
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        utbetalingMedUtbetalingEventer.add(event)
    }

    override fun vedtakFattet(event: PersonObserver.VedtakFattetEvent) {
        vedtakFattetEvent[event.vedtaksperiodeId] = event
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(event.organisasjonsnummer) { mutableSetOf() }.add(sisteVedtaksperiode)
        if (event.gjeldendeTilstand != event.forrigeTilstand) {
            tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        }
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }


    override fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {
        reberegnedeVedtaksperioder.add(vedtaksperiodeId)
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun inntektsmeldingReplay(event: PersonObserver.InntektsmeldingReplayEvent) {
        inntektsmeldingReplayEventer.add(event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        utbetaltEventer.add(event)
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        annulleringer.add(event)
    }

    override fun vedtaksperiodeAvbrutt(event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        avbruttEventer[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }
}
