package no.nav.helse.spleis.e2e

import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretTilstandEvent
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.fail
import java.util.*

internal class TestObservatør : PersonObserver {
    internal val tilstander = mutableMapOf<UUID, MutableList<TilstandType>>()
    val utbetalteVedtaksperioder = mutableListOf<UUID>()
    val reberegnedeVedtaksperioder = mutableListOf<UUID>()
    val manglendeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val trengerIkkeInntektsmeldingVedtaksperioder = mutableListOf<UUID>()
    val inntektsmeldingerLagtPåKjøl = mutableListOf<UUID>()
    val hendelserTilReplay = mutableMapOf<UUID, List<UUID>>()

    private lateinit var sisteVedtaksperiode: UUID
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val tilstandsendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretTilstandEvent>>()

    val utbetaltEventer = mutableListOf<PersonObserver.UtbetaltEvent>()
    val avbruttEventer = mutableListOf<PersonObserver.VedtaksperiodeAvbruttEvent>()
    val annulleringer = mutableListOf<PersonObserver.UtbetalingAnnullertEvent>()
    lateinit var avstemming: Map<String, Any>
    val inntektsmeldingReplayEventer = mutableListOf<UUID>()

    fun hendelseider(vedtaksperiodeId: UUID) =
        tilstandsendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }
    fun sisteVedtaksperiode() = sisteVedtaksperiode
    fun sisteVedtaksperiode(orgnummer: String) = vedtaksperioder.getValue(orgnummer).last()
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]
    fun vedtaksperiodeIndeks(orgnummer: String, id: UUID) = vedtaksperioder.getValue(orgnummer).indexOf(id)

    override fun avstemt(result: Map<String, Any>) {
        avstemming = result
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretTilstandEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
        tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(event.organisasjonsnummer) { mutableSetOf() }.add(sisteVedtaksperiode)
        tilstander.getOrPut(event.vedtaksperiodeId) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {
        reberegnedeVedtaksperioder.add(vedtaksperiodeId)
    }

    override fun vedtaksperiodeReplay(event: PersonObserver.VedtaksperiodeReplayEvent) {
        hendelserTilReplay[event.vedtaksperiodeId] = event.hendelseIder
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        manglendeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        trengerIkkeInntektsmeldingVedtaksperioder.add(event.vedtaksperiodeId)
    }

    override fun inntektsmeldingLagtPåKjøl(event: PersonObserver.InntektsmeldingLagtPåKjølEvent) {
        inntektsmeldingerLagtPåKjøl.add(event.hendelseId)
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
        avbruttEventer.add(event)
    }
}
