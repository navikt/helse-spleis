package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.BehandlingtilstandDto.*
import no.nav.helse.dto.VedtaksperiodetilstandDto
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
            vedtaksperiode.behandlinger.behandlinger
                .filter { behandling ->
                    behandling.tilstand in setOf(
                        UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING, BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING
                    )
                }
                .also {
                    if (vedtaksperiode.tilstand == VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING) {
                        sikkerlogg.info("vedtaksperiodeId {} er i AUU mens siste behandling er ikke Avsluttet uten vedtak", kv("vedtaksperiodeId", vedtaksperiode.id))
                    }
                }
                .map { behandling ->
                    BehandlingDto(
                        vedtaksperiodeId = vedtaksperiode.id,
                        behandlingId = behandling.id,
                        type = when (behandling.tilstand) {
                            BEREGNET -> BehandlingtypeDto.Søknad
                            BEREGNET_OMGJØRING -> BehandlingtypeDto.Omgjøring
                            BEREGNET_REVURDERING -> BehandlingtypeDto.Revurdering
                            UBEREGNET -> BehandlingtypeDto.Søknad
                            UBEREGNET_OMGJØRING -> BehandlingtypeDto.Omgjøring
                            UBEREGNET_REVURDERING -> BehandlingtypeDto.Revurdering
                            else -> error("Forventet ikke ${behandling.tilstand}")
                        },
                        kilde = BehandlingkildeDto(
                            registrert = behandling.kilde.registert,
                            innsendt = behandling.kilde.innsendt,
                            avsender = when (behandling.kilde.avsender) {
                                AvsenderDto.ARBEIDSGIVER -> BehandlingavsenderDto.ARBEIDSGIVER
                                AvsenderDto.SAKSBEHANDLER -> BehandlingavsenderDto.SAKSBEHANDLER
                                AvsenderDto.SYKMELDT -> BehandlingavsenderDto.SYKMELDT
                                AvsenderDto.SYSTEM -> BehandlingavsenderDto.SYSTEM
                            }
                        )
                    )
                }
        }
    }
}

private fun fødselsnummerSomString(fnr: Long) = fnr.toString().let { if (it.length == 11) it else "0$it" }

private data class BehandlingerEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val behandlinger: List<BehandlingDto>
) {
    @JsonProperty("@event_name")
    val eventName: String = "pågående_behandlinger"
    @JsonProperty("@id")
    val id: UUID = UUID.randomUUID()
    @JsonProperty("@opprettet")
    val opprettet: LocalDateTime = LocalDateTime.now()
}
private data class BehandlingDto(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val kilde: BehandlingkildeDto,
    val type: BehandlingtypeDto
)

private data class BehandlingkildeDto(
    val registrert: LocalDateTime,
    val innsendt: LocalDateTime,
    val avsender: BehandlingavsenderDto
)

private enum class BehandlingavsenderDto { SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM }
private enum class BehandlingtypeDto { Søknad, Omgjøring, Revurdering }
