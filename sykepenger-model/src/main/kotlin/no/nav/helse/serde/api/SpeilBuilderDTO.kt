package no.nav.helse.serde.api

import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.api.v2.Generasjon
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val inntektsgrunnlag: List<InntektsgrunnlagDTO>,
    val vilkårsgrunnlagHistorikk: Map<UUID, Map<LocalDate, Vilkårsgrunnlag>>,
    val dødsdato: LocalDate?,
    val versjon: Int
)

data class AktivitetDTO(
    val vedtaksperiodeId: UUID,
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String
)

data class ArbeidsgiverDTO(
    val organisasjonsnummer: String,
    val id: UUID,
    val vedtaksperioder: List<VedtaksperiodeDTOBase>,
    val utbetalingshistorikk: List<UtbetalingshistorikkElementDTO>,
    val generasjoner: List<Generasjon>?
)


data class VedtaksperiodeDTO(
    override val id: UUID,
    override val gruppeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tilstand: TilstandstypeDTO,
    override val fullstendig: Boolean = true,
    override val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean = false,
    val utbetalingsreferanse: String?,
    val utbetalinger: UtbetalingerDTO,
    val utbetalteUtbetalinger: UtbetalingerDTO,
    val sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    val godkjentAv: String?,
    val godkjenttidspunkt: LocalDateTime?,
    val automatiskBehandlet: Boolean,
    val vilkår: VilkårDTO,
    val inntektsmeldingId: UUID?,
    val inntektFraInntektsmelding: Double?,
    val totalbeløpArbeidstaker: Int,
    val hendelser: List<HendelseDTO>,
    val dataForVilkårsvurdering: GrunnlagsdataDTO?,
    val simuleringsdata: SimuleringsdataDTO?,
    val aktivitetslogg: List<AktivitetDTO>,
    val forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
    val periodetype: Periodetype,
    val beregningIder: List<UUID> = emptyList()
) : VedtaksperiodeDTOBase

data class UtbetalingerDTO(
    val arbeidsgiverUtbetaling: UtbetalingDTO?,
    val personUtbetaling: UtbetalingDTO?
) {
    data class UtbetalingDTO(
        val linjer: List<UtbetalingslinjeDTO>,
        val fagsystemId: String
    )

    data class UtbetalingslinjeDTO(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: Int,
        val grad: Double
    )
}

data class UfullstendigVedtaksperiodeDTO(
    override val id: UUID,
    override val gruppeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tilstand: TilstandstypeDTO,
    override val fullstendig: Boolean = false,
    override val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean = false
) : VedtaksperiodeDTOBase

interface VedtaksperiodeDTOBase {
    val id: UUID
    val gruppeId: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tilstand: TilstandstypeDTO
    val fullstendig: Boolean
    val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>
    val inntektskilde: Inntektskilde
    val erForkastet: Boolean
}

data class SykdomstidslinjedagDTO(
    val dagen: LocalDate,
    val type: SpeilDagtype,
    val kilde: KildeDTO,
    val grad: Double? = null
) {
    data class KildeDTO(
        val type: SpeilKildetype,
        val kildeId: UUID
    )
}

interface UtbetalingstidslinjedagDTO {
    val type: TypeDataDTO
    val inntekt: Int
    val dato: LocalDate
}

data class NavDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.NavDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val utbetaling: Int,
    val grad: Double,
    val totalGrad: Double?
) : UtbetalingstidslinjedagDTO

data class AvvistDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.AvvistDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val begrunnelser: List<BegrunnelseDTO>,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class UtbetalingsdagDTO(
    override val type: TypeDataDTO,
    override val inntekt: Int,
    override val dato: LocalDate
) : UtbetalingstidslinjedagDTO

data class UtbetalingsdagMedGradDTO(
    override val type: TypeDataDTO,
    override val inntekt: Int,
    override val dato: LocalDate,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class UfullstendigVedtaksperiodedagDTO(
    override val type: TypeDataDTO,
    override val dato: LocalDate,
    override val inntekt: Int = 0
) : UtbetalingstidslinjedagDTO

enum class TypeDataDTO {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Helgedag,   // SpeilBuilder only code breakout of Fridag
    Arbeidsdag,
    Feriedag,   // SpeilBuilder only code breakout of Fridag
    AvvistDag,
    UkjentDag,
    ForeldetDag
}

enum class MedlemskapstatusDTO {
    JA, NEI, VET_IKKE
}

data class GrunnlagsdataDTO(
    val beregnetÅrsinntektFraInntektskomponenten: Double,
    val avviksprosent: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val harOpptjening: Boolean,
    val medlemskapstatus: MedlemskapstatusDTO
)

data class SimuleringsdataDTO(
    val totalbeløp: Int,
    val perioder: List<PeriodeDTO>
) {
    data class PeriodeDTO(
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalinger: List<UtbetalingDTO>
    )

    data class UtbetalingDTO(
        val utbetalesTilId: String,
        val utbetalesTilNavn: String,
        val forfall: LocalDate,
        val feilkonto: Boolean,
        val detaljer: List<DetaljerDTO>
    )

    data class DetaljerDTO(
        val faktiskFom: LocalDate,
        val faktiskTom: LocalDate,
        val konto: String,
        val beløp: Int,
        val tilbakeføring: Boolean,
        val sats: Int,
        val typeSats: String,
        val antallSats: Int,
        val uføregrad: Int,
        val klassekode: String,
        val klassekodeBeskrivelse: String,
        val utbetalingstype: String,
        val refunderesOrgNr: String
    )
}

enum class BegrunnelseDTO {
    SykepengedagerOppbrukt,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    EtterDødsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70,
}

enum class TilstandstypeDTO(private val visForkastet: Boolean = false) {
    TilUtbetaling,
    TilAnnullering(true),
    Utbetalt(true),
    Annullert(true),
    AnnulleringFeilet(true),
    Oppgaver,
    Venter,
    VenterPåKiling,
    IngenUtbetaling,
    KunFerie,
    Feilet(true),
    TilInfotrygd;

    fun visesNårForkastet() = this.visForkastet
}

data class VilkårDTO(
    val sykepengedager: SykepengedagerDTO,
    val alder: AlderDTO,
    val opptjening: OpptjeningDTO?,
    val søknadsfrist: SøknadsfristDTO?,
    val medlemskapstatus: MedlemskapstatusDTO?
)

data class SykepengedagerDTO(
    val forbrukteSykedager: Int?,
    val skjæringstidspunkt: LocalDate,
    val beregningsdato: LocalDate = skjæringstidspunkt, // backward-compatible with Speil (temporary)
    val førsteFraværsdag: LocalDate = skjæringstidspunkt, // backward-compatible with Speil (temporary)
    val førsteSykepengedag: LocalDate?,
    val maksdato: LocalDate,
    val gjenståendeDager: Int?,
    val oppfylt: Boolean
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

data class InntektsgrunnlagDTO(
    val skjæringstidspunkt: LocalDate,
    val sykepengegrunnlag: Double?,
    val omregnetÅrsinntekt: Double?,
    val sammenligningsgrunnlag: Double?,
    val avviksprosent: Double?,
    val maksUtbetalingPerDag: Double?,
    val inntekter: List<ArbeidsgiverinntektDTO>,
    val oppfyllerKravOmMinstelønn: Boolean?,
    val grunnbeløp: Int
) {
    data class ArbeidsgiverinntektDTO(
        val arbeidsgiver: String,
        val omregnetÅrsinntekt: OmregnetÅrsinntektDTO?,
        val sammenligningsgrunnlag: SammenligningsgrunnlagDTO? = null // Per arbeidsgivere
    ) {
        data class OmregnetÅrsinntektDTO(
            val kilde: InntektkildeDTO,
            val beløp: Double,
            val månedsbeløp: Double,
            val inntekterFraAOrdningen: List<InntekterFraAOrdningenDTO>? = null //kun gyldig for A-ordningen
        ) {
            enum class InntektkildeDTO {
                Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen
            }

            data class InntekterFraAOrdningenDTO(
                val måned: YearMonth,
                val sum: Double
            )
        }

        data class SammenligningsgrunnlagDTO(
            val beløp: Double,
            val inntekterFraAOrdningen: List<InntekterFraAOrdningenDTO>
        ) {
            data class InntekterFraAOrdningenDTO(
                val måned: YearMonth,
                val sum: Double
            )
        }
    }
}
