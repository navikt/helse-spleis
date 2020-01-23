package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Pakke
import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.ModelSykepengehistorikk
import no.nav.helse.hendelser.ModelYtelser
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal object TestConstants {

    private val fakeFNR = "01019510000"

    fun sykepengehistorikk(
        perioder: List<SpolePeriode> = emptyList(),
        sisteHistoriskeSykedag: LocalDate? = null
    ) = sisteHistoriskeSykedag?.let {
        listOf(
            SpolePeriode(
                fom = it.minusMonths(1),
                tom = it,
                grad = "100"
            )
        )
    } ?: perioder

    data class Ytelse(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class ForeldrepengeLøsning(
        @JvmField val Foreldrepengeytelse: Ytelse?,
        @JvmField val Svangerskapsytelse: Ytelse?
    )

    fun foreldrepenger(
        foreldrepengeytelse: Ytelse?,
        svangerskapsytelse: Ytelse?
    ) = ForeldrepengeLøsning(
        Foreldrepengeytelse = foreldrepengeytelse,
        Svangerskapsytelse = svangerskapsytelse
    )

    fun foreldrepengeytelse(fom: LocalDate, tom: LocalDate) = Ytelse(fom = fom, tom = tom)

    fun ytelser(
        aktørId: String = "1",
        fødselsnummer: String = fakeFNR,
        organisasjonsnummer: String = "123546564",
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        sykepengehistorikk: List<SpolePeriode> = listOf(),
        foreldrepenger: ForeldrepengeLøsning? = null

    ) = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        sykepengehistorikk = ModelSykepengehistorikk(
            perioder = sykepengehistorikk.map { it.fom to it.tom },
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = foreldrepenger.let {
            ModelForeldrepenger(
                foreldrepengeytelse = it?.Foreldrepengeytelse?.let { it.fom to it.tom },
                svangerskapsytelse = it?.Svangerskapsytelse?.let { it.fom to it.tom },
                aktivitetslogger = Aktivitetslogger()
            )
        },
        rapportertdato = LocalDateTime.now(),
        originalJson = "{}"
    )

    fun manuellSaksbehandlingLøsning(
        organisasjonsnummer: String = "123546564",
        aktørId: String = "1",
        fødselsnummer: String = fakeFNR,
        vedtaksperiodeId: String = UUID.randomUUID().toString(),
        utbetalingGodkjent: Boolean,
        saksbehandler: String
    ) = Behov.nyttBehov(
        ArbeidstakerHendelse.Hendelsestype.ManuellSaksbehandling,
        listOf(Behovstype.GodkjenningFraSaksbehandler),
        aktørId,
        fødselsnummer,
        organisasjonsnummer,
        UUID.fromString(vedtaksperiodeId),
        mapOf(
            "saksbehandlerIdent" to saksbehandler
        )
    ).løsBehov(
        mapOf(
            Behovstype.GodkjenningFraSaksbehandler.toString() to mapOf("godkjent" to utbetalingGodkjent)
        )
    )

    fun påminnelseHendelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        aktørId: String = "1",
        organisasjonsnummer: String = "123456789"
    ) = Påminnelse.Builder().build(
        objectMapper.writeValueAsString(
            mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fakeFNR,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to tilstand.toString(),
                "antallGangerPåminnet" to 0,
                "tilstandsendringstidspunkt" to LocalDateTime.now().toString(),
                "påminnelsestidspunkt" to LocalDateTime.now().toString(),
                "nestePåminnelsestidspunkt" to LocalDateTime.now().toString()
            )
        )
    ) ?: fail { "påminnelse er null" }
}

internal fun Behov.løsBehov(løsning: Any): Behov {
    val pakke = Pakke.fromJson(this.toJson())
    pakke["@besvart"] = LocalDateTime.now().toString()
    pakke["@løsning"] = løsning
    pakke["@final"] = true
    return Behov.fromJson(pakke.toJson())
}

internal data class SpolePeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: String
)

internal class Uke(ukenr: Long) {
    val mandag = LocalDate.of(2018, 1, 1)
        .plusWeeks(ukenr - 1L)
    val tirsdag get() = mandag.plusDays(1)
    val onsdag get() = mandag.plusDays(2)
    val torsdag get() = mandag.plusDays(3)
    val fredag get() = mandag.plusDays(4)
    val lørdag get() = mandag.plusDays(5)
    val søndag get() = mandag.plusDays(6)
}

internal operator fun ConcreteSykdomstidslinje.get(index: LocalDate) = flatten().firstOrNull { it.førsteDag() == index }

internal fun SykepengesoknadDTO.toJsonNode(): JsonNode = objectMapper.valueToTree(this)
internal fun SykepengesoknadDTO.toJson(): String = objectMapper.writeValueAsString(this)
internal fun Inntektsmeldingkontrakt.toJsonNode(): JsonNode = objectMapper.valueToTree(this)
internal fun Inntektsmeldingkontrakt.toJson(): String = objectMapper.writeValueAsString(this)

internal val Int.juni
    get() = LocalDate.of(2019, Month.JUNE, this)

internal val Int.juli
    get() = LocalDate.of(2019, Month.JULY, this)

internal val Int.august
    get() = LocalDate.of(2019, Month.AUGUST, this)

internal val Int.september
    get() = LocalDate.of(2019, Month.SEPTEMBER, this)

internal val Int.oktober
    get() = LocalDate.of(2019, Month.OCTOBER, this)
