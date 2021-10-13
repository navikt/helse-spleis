package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.utbetalingslinjer.Utbetaling
import org.junit.jupiter.api.Assertions

@JsonIgnoreProperties("person")
private class ArbeidsgiverMixin

@JsonIgnoreProperties("person", "arbeidsgiver")
private class VedtaksperiodeMixin

@JsonIgnoreProperties("observers", "forrigeHendelse")
private class UtbetalingMixin

private val objectMapper = jacksonObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setMixIns(
        mutableMapOf(
            Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
            Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java,
            Utbetaling::class.java to UtbetalingMixin::class.java,
        )
    )
    .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
    .registerModule(JavaTimeModule())

internal fun assertJsonEquals(expected: Any, actual: Any) {
    val expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected)
    val actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual)
    Assertions.assertEquals(expectedJson, actualJson)
}
