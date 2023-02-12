package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.math.RoundingMode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeUtbetalinger
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Prosentdel
import org.junit.jupiter.api.Assertions

@JsonIgnoreProperties("jurist")
private class PersonMixin

@JsonIgnoreProperties("person", "jurist")
private class ArbeidsgiverMixin

@JsonIgnoreProperties("person", "arbeidsgiver", "jurist")
private class VedtaksperiodeMixin

@JsonIgnoreProperties("arbeidsgiver")
private class VedtaksperiodeUtbetalingerMixin

@JsonIgnoreProperties("observers", "forrigeHendelse")
private class UtbetalingMixin

@JsonIgnoreProperties("dagtyperSomAvvises")
private class BegrunnelseMixin

@JsonIgnoreProperties("oppslag", "utbetalingstidslinjeoppslag", "arbeidsgiverperiodecache")
private class InfotrygdhistorikkElementMixin

@JsonSerialize(using = AktivitetsloggSerializer::class)
private class AktivitetsloggMixin

@JsonIgnoreProperties("ansattPerioder")
private class SkattSykepengegrunnlagMixin

internal class AktivitetsloggSerializer : JsonSerializer<Aktivitetslogg>() {
    override fun serialize(value: Aktivitetslogg?, gen: JsonGenerator, serializers: SerializerProvider?) {
        if (value == null) return
        gen.writeObject(AktivitetsloggMap().map(value).toString())
    }
}
internal class BigDecimalSerializer : JsonSerializer<BigDecimal>() {
    private companion object {
        private const val PRECISION = 15
    }
    override fun serialize(value: BigDecimal?, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(value?.setScale(PRECISION, RoundingMode.HALF_EVEN).toString())
    }
}

private class ProsentdelMixin {
    @JsonSerialize(using = BigDecimalSerializer::class)
    private lateinit var brøkdel: BigDecimal
}

private val objectMapper = jacksonObjectMapper()
    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .setMixIns(
        mutableMapOf(
            Person::class.java to PersonMixin::class.java,
            Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
            Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java,
            VedtaksperiodeUtbetalinger::class.java to VedtaksperiodeUtbetalingerMixin::class.java,
            Utbetaling::class.java to UtbetalingMixin::class.java,
            Begrunnelse::class.java to BegrunnelseMixin::class.java,
            InfotrygdhistorikkElement::class.java to InfotrygdhistorikkElementMixin::class.java,
            Prosentdel::class.java to ProsentdelMixin::class.java,
            Aktivitetslogg::class.java to AktivitetsloggMixin::class.java,
            SkattSykepengegrunnlag::class.java to SkattSykepengegrunnlagMixin::class.java
        )
    )
    .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
    .registerModule(JavaTimeModule())

internal fun assertJsonEquals(expected: Any, actual: Any) {
    val expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected)
    val actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual)
    Assertions.assertEquals(expectedJson, actualJson)
}
