package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.dto.BehandlingtilstandDto.ANNULLERT_PERIODE
import no.nav.helse.dto.BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.dto.BehandlingtilstandDto.BEREGNET
import no.nav.helse.dto.BehandlingtilstandDto.BEREGNET_OMGJØRING
import no.nav.helse.dto.BehandlingtilstandDto.BEREGNET_REVURDERING
import no.nav.helse.dto.BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST
import no.nav.helse.dto.BehandlingtilstandDto.TIL_INFOTRYGD
import no.nav.helse.dto.BehandlingtilstandDto.UBEREGNET
import no.nav.helse.dto.BehandlingtilstandDto.UBEREGNET_OMGJØRING
import no.nav.helse.dto.BehandlingtilstandDto.UBEREGNET_REVURDERING
import no.nav.helse.dto.BehandlingtilstandDto.VEDTAK_FATTET
import no.nav.helse.dto.BehandlingtilstandDto.VEDTAK_IVERKSATT
import no.nav.helse.dto.deserialisering.BehandlingerInnDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.serde.SerialisertPerson
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC

private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun migrereBehandlinger(factory: ConsumerProducerFactory, arbeidId: String) {
    factory.createProducer().use { producer ->
        opprettOgUtførArbeid(arbeidId, size = 500) { session, fnr ->
            hentPerson(session, fnr)?.let { (aktørId, data) ->
                val mdcContextMap = MDC.getCopyOfContextMap() ?: emptyMap()
                try {
                    MDC.put("aktørId", aktørId)
                    val node = SerialisertPerson(data).tilPersonDto()
                    val behandlinger = finnBehandlinger(node)
                    if (behandlinger.isEmpty()) return@let
                    val fødselsnummer = fødselsnummerSomString(fnr)
                    val event = BehandlingerEvent(fødselsnummer, aktørId, behandlinger)
                    val melding = objectMapper.writeValueAsString(event)
                    producer.send(ProducerRecord("tbd.teknisk.v1", null, fødselsnummer, melding))
                    sikkerlogg.info("Skrev behandlinger til tbd.teknisk.v1 for:\n{}", melding)

                } catch (err: Exception) {
                    log.info("$aktørId lar seg ikke serialisere: ${err.message}")
                    sikkerlogg.error("$aktørId lar seg ikke serialisere: ${err.message}", err)
                } finally {
                    MDC.setContextMap(mdcContextMap)
                }
            }
        }
        producer.flush()
    }
    runBlocking {
        log.info("Venter med å skru av i ett minutt for at securelogs-sidecar forhåpentligvis skal synce loggene")
        delay(60000L)
    }
}

private fun finnBehandlinger(person: PersonInnDto): List<BehandlingDto> {
    return person.arbeidsgivere.flatMap { arbeidsgiver ->
        arbeidsgiver.vedtaksperioder.flatMap { vedtaksperiode ->
            mapBehandling(vedtaksperiode.id, vedtaksperiode.behandlinger)
        } + arbeidsgiver.forkastede.flatMap { vedtaksperiode ->
            mapBehandling(vedtaksperiode.vedtaksperiode.id, vedtaksperiode.vedtaksperiode.behandlinger)
        }
    }
}

private fun mapBehandling(vedtaksperiodeId: UUID, behandlinger: BehandlingerInnDto): List<BehandlingDto> {
    return behandlinger.behandlinger.map { behandling ->
        BehandlingDto(
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandling.id,
            fom = behandling.endringer.last().periode.fom,
            tom = behandling.endringer.last().periode.tom,
            skjæringstidspunkt = behandling.endringer.last().skjæringstidspunkt,
            utbetalingId = behandling.endringer.lastOrNull()?.utbetalingId,
            status = when (behandling.tilstand) {
                ANNULLERT_PERIODE, TIL_INFOTRYGD -> BehandlingstatusDto.TATT_I_INFOTRYGD
                AVSLUTTET_UTEN_VEDTAK -> BehandlingstatusDto.AVSLUTTET_UTEN_VEDTAK
                BEREGNET,
                BEREGNET_OMGJØRING,
                BEREGNET_REVURDERING,
                REVURDERT_VEDTAK_AVVIST,
                UBEREGNET,
                UBEREGNET_OMGJØRING,
                UBEREGNET_REVURDERING,
                VEDTAK_FATTET-> BehandlingstatusDto.ÅPEN_OG_VENTENDE
                VEDTAK_IVERKSATT -> BehandlingstatusDto.AVSLUTTET_MED_VEDTAK
            }
        )
    }
}


private fun fødselsnummerSomString(fnr: Long) = fnr.toString().let { if (it.length == 11) it else "0$it" }

private data class BehandlingerEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val behandlinger: List<BehandlingDto>
) {
    @JsonProperty("@event_name")
    val eventName: String = "behandlinger"
    @JsonProperty("@id")
    val id: UUID = UUID.randomUUID()
    @JsonProperty("@opprettet")
    val opprettet: LocalDateTime = LocalDateTime.now()
}
private data class BehandlingDto(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val status: BehandlingstatusDto,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val utbetalingId: UUID?
)

private enum class BehandlingstatusDto { ÅPEN_OG_VENTENDE, AVSLUTTET_MED_VEDTAK, AVSLUTTET_UTEN_VEDTAK, TATT_I_INFOTRYGD }
