package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.serde.SerialisertPerson
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.UUID

// Midlertidig: engangsmigrering som gir varsler i spesialist spleis-behandlingId. Slettes når migreringen er kjørt.

private val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

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
            dto.behandlingskobling().chunked(50).forEach { vedtaksperioder ->
                val melding = Behandlingskobling(dto.fødselsnummer, dryrun = dryrun, vedtaksperioder = vedtaksperioder)
                producer.send(ProducerRecord("tbd.rapid.v1", dto.fødselsnummer, melding.tilJson()))
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
                    endringer = behandling.endringer.map { it.tidsstempel to it.utbetalingId },
                )
            },
    )

// opprettet er tidsstempelet til første endring. utbetalinger er alle utbetalingene behandlingen har hatt, uten duplikater.
internal fun kobletBehandling(
    behandlingId: UUID,
    endringer: List<Pair<LocalDateTime, UUID?>>,
): KobletBehandling =
    KobletBehandling(
        behandlingId = behandlingId,
        opprettet = endringer.first().first,
        utbetalinger = endringer.mapNotNull { it.second }.distinct(),
    )

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
)
