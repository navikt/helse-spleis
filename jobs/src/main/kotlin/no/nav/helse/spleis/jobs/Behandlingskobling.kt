package no.nav.helse.spleis.jobs

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.spleisJsonMapperBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

// Midlertidig: engangsmigrering som gir varsler i spesialist spleis-behandlingId. Slettes når migreringen er kjørt.

private val objectMapper: ObjectMapper = spleisJsonMapperBuilder().build()

fun startBehandlingskobling(
    factory: ConsumerProducerFactory,
    arbeidId: String,
    utvalg: Int,
    dryrun: Boolean,
) {
    require(utvalg in 1..100) { "utvalg må være mellom 1 og 100" }
    log.info("Behandlingskobling starter med arbeidId=$arbeidId, utvalg=$utvalg og dryrun=$dryrun")
    var antallSendt = 0
    factory.createProducer().use { producer ->
        opprettOgUtførArbeid(arbeidId, size = 100, utvalg = utvalg) { connection, fnr ->
            val (skjemaVersjon, data) = hentPerson(connection, fnr)
            val dto = SerialisertPerson(data, skjemaVersjon).tilPersonDto()
            // Deler opp, så meldingene holder seg godt under maksstørrelsen på Kafka for personer med mange vedtaksperioder
            dto
                .behandlingskobling()
                .chunked(50)
                .map { vedtaksperioder ->
                    val melding = Behandlingskobling(dto.fødselsnummer, dryrun = dryrun, vedtaksperioder = vedtaksperioder)
                    producer.send(ProducerRecord("tbd.rapid.v1", dto.fødselsnummer, melding.tilJson()))
                }
                // Venter på kvittering, så feil (f.eks. manglende tilgang til topicet) kastes og personen ikke markeres som ferdig
                .forEach {
                    it.get()
                    antallSendt += 1
                }
        }
        producer.flush()
    }
    log.info("Behandlingskobling ferdig med arbeidId=$arbeidId. Sendte $antallSendt meldinger")
}

internal fun PersonInnDto.behandlingskobling(): List<KobletVedtaksperiode> =
    arbeidsgivere.flatMap { arbeidsgiver ->
        (arbeidsgiver.vedtaksperioder + arbeidsgiver.forkastede.map { it.vedtaksperiode })
            .map { it.behandlingskobling() }
    }

private fun VedtaksperiodeInnDto.behandlingskobling(): KobletVedtaksperiode =
    KobletVedtaksperiode(
        vedtaksperiodeId = id,
        behandlinger =
            behandlinger.behandlinger.map { behandling ->
                kobletBehandling(
                    behandlingId = behandling.id,
                    tilstand = behandling.tilstand,
                    endringer = behandling.endringer.map { it.tidsstempel to it.utbetalingId },
                )
            },
    )

// opprettet er tidsstempelet til første endring. utbetalinger er alle utbetalingene behandlingen har hatt, uten duplikater.
internal fun kobletBehandling(
    behandlingId: UUID,
    tilstand: BehandlingtilstandDto,
    endringer: List<Pair<LocalDateTime, UUID?>>,
): KobletBehandling =
    KobletBehandling(
        behandlingId = behandlingId,
        opprettet = endringer.first().first,
        utbetalinger = endringer.mapNotNull { it.second }.distinct(),
        erÅpenForEndring = tilstand.erÅpenForEndring(),
    )

// Samme regel som Behandling.erÅpenForEndring i modellen
private fun BehandlingtilstandDto.erÅpenForEndring() =
    when (this) {
        BehandlingtilstandDto.UBEREGNET,
        BehandlingtilstandDto.UBEREGNET_OMGJØRING,
        BehandlingtilstandDto.UBEREGNET_REVURDERING,
        BehandlingtilstandDto.UBEREGNET_ANNULLERING,
        BehandlingtilstandDto.BEREGNET,
        BehandlingtilstandDto.BEREGNET_OMGJØRING,
        BehandlingtilstandDto.BEREGNET_REVURDERING,
        -> true

        BehandlingtilstandDto.VEDTAK_FATTET,
        BehandlingtilstandDto.VEDTAK_IVERKSATT,
        BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST,
        BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK,
        BehandlingtilstandDto.ANNULLERT_PERIODE,
        BehandlingtilstandDto.OVERFØRT_ANNULLERING,
        BehandlingtilstandDto.TIL_INFOTRYGD,
        -> false
    }

internal data class Behandlingskobling(
    val fødselsnummer: String,
    val dryrun: Boolean,
    val vedtaksperioder: List<KobletVedtaksperiode>,
) {
    fun tilJson(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "@id" to UUID.randomUUID(),
                "@event_name" to "spleis_behandlingskobling",
                "@opprettet" to LocalDateTime.now(),
                "fødselsnummer" to fødselsnummer,
                "dryrun" to dryrun,
                "vedtaksperioder" to vedtaksperioder,
            ),
        )
}

internal data class KobletVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val behandlinger: List<KobletBehandling>,
)

internal data class KobletBehandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val utbetalinger: List<UUID>,
    val erÅpenForEndring: Boolean,
)
