package no.nav.helse.serde.api

import no.nav.helse.serde.mapping.JsonDagType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal data class SykdomstidslinjedagDTO(val dagen: LocalDate, val type: JsonDagType, val grad: Double? = null)
internal data class VedtaksperiodeDTO(
    val sykdomstidslinje: MutableList<SykdomstidslinjedagDTO>,
    val id: UUID,
    val maksdato: LocalDate?,
    val forbrukteSykedager: Int?,
    val godkjentAv: String?,
    val godkjenttidspunkt: LocalDateTime?,
    val utbetalingsreferanse: String?,
    val førsteFraværsdag: LocalDate?,
    val inntektFraInntektsmelding: Double?,
    val totalbeløpArbeidstaker: Double?,
    val tilstand: TilstandstypeDTO,
    val hendelser: MutableSet<UUID>,
    val dataForVilkårsvurdering: GrunnlagsdataDTO?,
    val utbetalingstidslinje: MutableList<UtbetalingstidslinjedagDTO>,
    val utbetalingslinjer: MutableList<UtbetalingslinjeDTO>
)

internal interface UtbetalingstidslinjedagDTO {
    val type: TypeDataDTO
    val inntekt: Double
    val dato: LocalDate
}

internal data class NavDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.NavDag,
    override val inntekt: Double,
    override val dato: LocalDate,
    val utbetaling: Int,
    val grad: Double
) : UtbetalingstidslinjedagDTO

internal data class AvvistDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.AvvistDag,
    override val inntekt: Double,
    override val dato: LocalDate,
    val begrunnelse: BegrunnelseDTO,
    val grad: Double
) : UtbetalingstidslinjedagDTO

internal data class UtbetalingsdagDTO(
    override val type: TypeDataDTO,
    override val inntekt: Double,
    override val dato: LocalDate
) : UtbetalingstidslinjedagDTO

internal data class UtbetalingsdagMedGradDTO(
    override val type: TypeDataDTO,
    override val inntekt: Double,
    override val dato: LocalDate,
    val grad: Double
) : UtbetalingstidslinjedagDTO

internal enum class TypeDataDTO {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Arbeidsdag,
    Fridag,
    AvvistDag,
    UkjentDag,
    ForeldetDag
}

internal data class GrunnlagsdataDTO(
    val erEgenAnsatt: Boolean,
    val beregnetÅrsinntektFraInntektskomponenten: Double,
    val avviksprosent: Double,
    val antallOpptjeningsdagerErMinst: Int,
    val harOpptjening: Boolean
)

internal enum class BegrunnelseDTO {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad
}

internal data class UtbetalingslinjeDTO(val fom: LocalDate, val tom: LocalDate, val dagsats: Int, val grad: Double)

internal interface HendelseDTO {
    val hendelseId: UUID
    val fom: LocalDate
    val tom: LocalDate
    val type: String
}

internal data class InntektsmeldingDTO(
    override val hendelseId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val type: String,
    val rapportertdato: LocalDateTime,
    val beregnetInntekt: Double
) : HendelseDTO

internal data class SøknadDTO(
    override val hendelseId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val type: String,
    val rapportertdato: LocalDateTime,
    val sendtNav: LocalDateTime
) : HendelseDTO

internal data class SykmeldingDTO(
    override val hendelseId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val type: String,
    val rapportertdato: LocalDateTime
) : HendelseDTO

internal enum class TilstandstypeDTO {
    TilUtbetaling,
    Utbetalt,
    Oppgaver,
    Venter,
    IngenUtbetaling,
    Feilet
}
