package no.nav.helse.serde.api

import no.nav.helse.serde.mapping.JsonDagType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>
)

data class AktivitetDTO (
    val vedtaksperiodeId: UUID,
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String
)

data class ArbeidsgiverDTO(
    val organisasjonsnummer: String,
    val id: UUID,
    val vedtaksperioder: List<VedtaksperiodeDTOBase>
)

data class VedtaksperiodeDTO(
    override val id: UUID,
    override val gruppeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tilstand: TilstandstypeDTO,
    override val fullstendig: Boolean = true,
    val utbetalingsreferanse: String?,
    val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
    val sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    val godkjentAv: String?,
    val godkjenttidspunkt: LocalDateTime?,
    val vilkår: VilkårDTO,
    val førsteFraværsdag: LocalDate?,
    val inntektFraInntektsmelding: Double?,
    val totalbeløpArbeidstaker: Int,
    val hendelser: List<HendelseDTO>,
    val dataForVilkårsvurdering: GrunnlagsdataDTO?,
    val aktivitetslogg: List<AktivitetDTO>
) : VedtaksperiodeDTOBase

data class UfullstendigVedtaksperiodeDTO(
    override val id: UUID,
    override val gruppeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tilstand: TilstandstypeDTO,
    override val fullstendig: Boolean = false
) : VedtaksperiodeDTOBase

interface VedtaksperiodeDTOBase {
    val id: UUID
    val gruppeId: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tilstand: TilstandstypeDTO
    val fullstendig: Boolean
}

data class SykdomstidslinjedagDTO(
    val dagen: LocalDate,
    val type: JsonDagType,
    val grad: Double? = null
)

interface UtbetalingstidslinjedagDTO {
    val type: TypeDataDTO
    val inntekt: Double
    val dato: LocalDate
}

data class NavDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.NavDag,
    override val inntekt: Double,
    override val dato: LocalDate,
    val utbetaling: Int,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class AvvistDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.AvvistDag,
    override val inntekt: Double,
    override val dato: LocalDate,
    val begrunnelse: BegrunnelseDTO,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class UtbetalingsdagDTO(
    override val type: TypeDataDTO,
    override val inntekt: Double,
    override val dato: LocalDate
) : UtbetalingstidslinjedagDTO

data class UtbetalingsdagMedGradDTO(
    override val type: TypeDataDTO,
    override val inntekt: Double,
    override val dato: LocalDate,
    val grad: Double
) : UtbetalingstidslinjedagDTO

enum class TypeDataDTO {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Helgedag,
    Arbeidsdag,
    Feriedag,
    AvvistDag,
    UkjentDag,
    ForeldetDag
}

data class GrunnlagsdataDTO(
    val erEgenAnsatt: Boolean,
    val beregnetÅrsinntektFraInntektskomponenten: Double,
    val avviksprosent: Double,
    val antallOpptjeningsdagerErMinst: Int,
    val harOpptjening: Boolean
)

enum class BegrunnelseDTO {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad
}

data class UtbetalingslinjeDTO(val fom: LocalDate, val tom: LocalDate, val dagsats: Int, val grad: Double)

interface HendelseDTO {
    val id: String
    val type: String
}

data class InntektsmeldingDTO(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : HendelseDTO{
    override val type = "INNTEKTSMELDING"
}

data class SøknadNavDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtNav: LocalDateTime
) : HendelseDTO {
    override val type = "SENDT_SØKNAD_NAV"
}

data class SøknadArbeidsgiverDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : HendelseDTO {
    override val type = "SENDT_SØKNAD_ARBEIDSGIVER"
}

data class SykmeldingDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime
) : HendelseDTO {
    override val type = "NY_SØKNAD"
}

enum class TilstandstypeDTO {
    TilUtbetaling,
    Utbetalt,
    Oppgaver,
    Venter,
    IngenUtbetaling,
    Feilet,
    TilInfotrygd
}

data class VilkårDTO(
    val sykepengedager: SykepengedagerDTO,
    val alder: AlderDTO,
    val opptjening: OpptjeningDTO?,
    val søknadsfrist: SøknadsfristDTO?,
    val sykepengegrunnlag: SykepengegrunnlagDTO?
)

data class SykepengedagerDTO(
    val forbrukteSykedager: Int?,
    val førsteFraværsdag: LocalDate?,
    val førsteSykepengedag: LocalDate?,
    val maksdato: LocalDate?,
    val gjenståendeDager: Int?,
    val oppfylt: Boolean?
)

data class AlderDTO(
    val alderSisteSykedag: Int,
    val oppfylt: Boolean?
)

data class OpptjeningDTO(
    val antallKjenteOpptjeningsdager: Int,
    val fom: LocalDate,
    val oppfylt: Boolean
)

data class SøknadsfristDTO(
    val sendtNav: LocalDateTime,
    val søknadFom: LocalDate,
    val søknadTom: LocalDate,
    val oppfylt: Boolean
)

data class SykepengegrunnlagDTO(
    val sykepengegrunnlag: Double?,
    val grunnbeløp: Int,
    val oppfylt: Boolean?
)
