package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.serde.SerialisertPerson
import org.apache.kafka.clients.producer.ProducerRecord

private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun startFeriepenger(factory: ConsumerProducerFactory, arbeidId: String, opptjeningsår: Year, antallPersonerOmGangen: Int = 10, dryrun: Boolean = true) {
    factory.createProducer().use { producer ->
        opprettOgUtførArbeid(arbeidId, size = antallPersonerOmGangen) { session, fnr ->
            hentPerson(session, fnr).let { data ->
                try {
                    val dto = SerialisertPerson(data).tilPersonDto()
                    if (dto.potensiellFeriepengekjøring(opptjeningsår)) {
                        sikkerlogg.info("sender behov om SykepengehistorikkForFeriepenger for fødselsnummer=${dto.fødselsnummer}")
                        val event = SykepengehistorikkForFeriepenger(dto.fødselsnummer, opptjeningsår)
                        if (!dryrun) {
                            producer.send(ProducerRecord("tbd.teknisk.v1", dto.fødselsnummer, event.tilJson()))
                        }
                    }
                } catch (err: Exception) {
                    log.info("person lar seg ikke serialisere: ${err.message}")
                }
            }
        }
        producer.flush()
    }
}

private fun PersonInnDto.potensiellFeriepengekjøring(opptjeningsår: Year): Boolean {
    return arbeidsgivere.any { arbeidsgiver ->
        arbeidsgiver.utbetalinger.aktive().any { utbetaling ->
            utbetaling.arbeidsgiverOppdrag.harUtbetalt(opptjeningsår) || utbetaling.personOppdrag.harUtbetalt(opptjeningsår)
        }
    }
}

private fun List<UtbetalingInnDto>.aktive(): List<UtbetalingInnDto> {
    return this
        .filterNot { it.tilstand in setOf(UtbetalingTilstandDto.FORKASTET, UtbetalingTilstandDto.NY, UtbetalingTilstandDto.IKKE_GODKJENT, UtbetalingTilstandDto.IKKE_UTBETALT) }
        .groupBy { it.korrelasjonsId }
        .map { (_, utbetalinger) -> utbetalinger.maxBy { it.tidsstempel } }
        .filterNot { utbetaling -> utbetaling.tilstand == UtbetalingTilstandDto.ANNULLERT }
}

private fun OppdragInnDto.harUtbetalt(opptjeningsår: Year): Boolean {
    return linjer.any { linje ->
        linje.datoStatusFom == null && opptjeningsår.value in linje.fom.year..linje.tom.year
    }
}

@JsonIgnoreProperties(value = ["opptjeningsår"])
data class SykepengehistorikkForFeriepenger(
    val fødselsnummer: String,
    val opptjeningsår: Year
) {
    @JsonProperty("@event_name")
    val eventName: String = "behov"

    @JsonProperty("@id")
    val id: UUID = UUID.randomUUID()

    @JsonProperty("@opprettet")
    val opprettet: LocalDateTime = LocalDateTime.now()

    @JsonProperty("@behov")
    val behov=  listOf("SykepengehistorikkForFeriepenger")

    @JsonProperty("SykepengehistorikkForFeriepenger")
    val behovdetaljer = mapOf(
        "historikkFom" to opptjeningsår.atMonth(Month.JANUARY).atDay(1),
        "historikkTom" to opptjeningsår.atMonth(Month.DECEMBER).atDay(31),
    )
}

fun SykepengehistorikkForFeriepenger.tilJson() =
    objectMapper.writeValueAsString(this)
