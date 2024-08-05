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
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.serde.SerialisertPerson
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC

private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun migrereGrunnbeløp(factory: ConsumerProducerFactory, arbeidId: String) {
    factory.createProducer().use { producer ->
        opprettOgUtførArbeid(arbeidId, size = 500) { session, fnr ->
            hentPerson(session, fnr)?.let { (aktørId, data) ->
                val mdcContextMap = MDC.getCopyOfContextMap() ?: emptyMap()
                try {
                    MDC.put("aktørId", aktørId)
                    val node = SerialisertPerson(data).tilPersonDto()
                    val grunnbeløp = finnGrunnbeløp(node)
                    if (grunnbeløp.isEmpty()) return@let
                    val fødselsnummer = fødselsnummerSomString(fnr)
                    val event = GrunnbeløpEvent(fødselsnummer, aktørId, grunnbeløp)
                    val melding = objectMapper.writeValueAsString(event)
                    producer.send(ProducerRecord("tbd.teknisk.v1", null, fødselsnummer, melding))
                    sikkerlogg.info("Skrev grunnbeløp til tbd.teknisk.v1 for:\n{}", melding)

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

private fun finnGrunnbeløp(person: PersonInnDto): List<Grunnbeløp> {
    val historikk = person.vilkårsgrunnlagHistorikk.historikk
    if (historikk.isEmpty()) return emptyList()
    val siste = historikk.first()
    return siste.vilkårsgrunnlag.filterNot { it.sykepengegrunnlag.vurdertInfotrygd }.map {
        spleisVilkårsgrunnlag -> Grunnbeløp(
            skjæringstidspunkt = spleisVilkårsgrunnlag.skjæringstidspunkt,
            `6G` = spleisVilkårsgrunnlag.sykepengegrunnlag.`6G`.beløp
        )
    }
}


private fun fødselsnummerSomString(fnr: Long) = fnr.toString().let { if (it.length == 11) it else "0$it" }

private data class GrunnbeløpEvent(
    val fødselsnummer: String,
    val aktørId: String,
    val grunnbeløp: List<Grunnbeløp>
) {
    @JsonProperty("@event_name")
    val eventName: String = "grunnbeløp"
    @JsonProperty("@id")
    val id: UUID = UUID.randomUUID()
    @JsonProperty("@opprettet")
    val opprettet: LocalDateTime = LocalDateTime.now()
}
private data class Grunnbeløp(
    val skjæringstidspunkt: LocalDate,
    val `6G`: Double
)

