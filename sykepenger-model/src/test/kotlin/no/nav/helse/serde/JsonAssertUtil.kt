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
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Prosentdel
import org.junit.jupiter.api.Assertions

@JsonIgnoreProperties("jurist", "aktivitetslogg")
private class PersonMixin

@JsonIgnoreProperties("person", "jurist")
private class ArbeidsgiverMixin

@JsonIgnoreProperties("person", "arbeidsgiver", "arbeidsgiverjurist")
private class VedtaksperiodeMixin

@JsonIgnoreProperties("observatører")
private class GenerasjonerMixin

@JsonIgnoreProperties("observatører", "kilde")
private class GenerasjonMixin

@JsonIgnoreProperties("observers", "forrigeHendelse")
private class UtbetalingMixin

@JsonIgnoreProperties("dagtyperSomAvvises")
private class BegrunnelseMixin

@JsonIgnoreProperties("oppslag", "utbetalingstidslinjeoppslag", "arbeidsgiverperiodecache")
private class InfotrygdhistorikkElementMixin

@JsonIgnoreProperties("ansattPerioder")
private class SkattSykepengegrunnlagMixin

@JsonIgnoreProperties("opptjeningsdager\$delegate")
private class OpptjeningMixin

@JsonIgnoreProperties("ønsketVilkårsgrunnlagId")
private class SykepengegrunnlagMixin

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
            Generasjoner::class.java to GenerasjonerMixin::class.java,
            Generasjoner.Generasjon::class.java to GenerasjonMixin::class.java,
            Utbetaling::class.java to UtbetalingMixin::class.java,
            Begrunnelse::class.java to BegrunnelseMixin::class.java,
            InfotrygdhistorikkElement::class.java to InfotrygdhistorikkElementMixin::class.java,
            Prosentdel::class.java to ProsentdelMixin::class.java,
            SkattSykepengegrunnlag::class.java to SkattSykepengegrunnlagMixin::class.java,
            Opptjening::class.java to OpptjeningMixin::class.java,
            Sykepengegrunnlag::class.java to SykepengegrunnlagMixin::class.java
        )
    )
    .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
    .registerModule(JavaTimeModule())

internal fun assertJsonEquals(expected: Any, actual: Any) {
    val expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected)
    val actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual)
    Assertions.assertEquals(expectedJson, actualJson)
}
