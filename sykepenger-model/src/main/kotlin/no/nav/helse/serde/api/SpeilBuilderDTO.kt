package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.api.v2.Generasjon
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.utbetalingstidslinje.Begrunnelse

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val inntektsgrunnlag: List<InntektsgrunnlagDTO>,
    val vilkårsgrunnlagHistorikk: Map<UUID, Map<LocalDate, Vilkårsgrunnlag>>,
    val arbeidsforholdPerSkjæringstidspunkt: Map<LocalDate, List<ArbeidsforholdDTO>>,
    val dødsdato: LocalDate?,
    val versjon: Int
)

data class ArbeidsforholdDTO(
    val orgnummer: String,
    val ansattFom: LocalDate,
    val ansattTom: LocalDate?,
    val deaktivert: Boolean
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
    val ghostPerioder: List<GhostPeriodeDTO>,
    val utbetalingshistorikk: List<UtbetalingshistorikkElementDTO>,
    val generasjoner: List<Generasjon>
)

data class VedtaksperiodeDTO(
    override val id: UUID,
    override val gruppeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tilstand: TilstandstypeDTO,
    override val fullstendig: Boolean = true,
    override val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
    override val sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean = false,
    val utbetalingsreferanse: String?,
    val utbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?,
    val sisteUtbetaling: UtbetalingshistorikkElementDTO.UtbetalingDTO?,
    val vilkår: VilkårDTO,
    val inntektsmeldingId: UUID?,
    val inntektFraInntektsmelding: Double?,
    val hendelser: List<HendelseDTO>,
    val dataForVilkårsvurdering: GrunnlagsdataDTO?,
    val aktivitetslogg: List<AktivitetDTO>,
    val forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
    val periodetype: Periodetype,
    val beregningIder: List<UUID> = emptyList()
) : VedtaksperiodeDTOBase {
    @Deprecated("Speil må lese fra utbetaling.vurdering")
    val godkjentAv: String? = utbetaling?.vurdering?.ident

    @Deprecated("Speil må lese fra utbetaling.vurdering")
    val godkjenttidspunkt: LocalDateTime? = utbetaling?.vurdering?.tidsstempel

    @Deprecated("Speil må lese fra utbetaling.vurdering")
    val automatiskBehandlet: Boolean = utbetaling?.vurdering?.automatisk ?: false

    @Deprecated("Speil må lese fra utbetaling")
    val simuleringsdata = utbetaling?.arbeidsgiverOppdrag?.simuleringsResultat

    @Deprecated("Speil burde lest fra utbetaling.arbeidsgiverOppdrag og utbetaling.personOppdrag")
    val totalbeløpArbeidstaker: Int = (utbetaling?.arbeidsgiverNettoBeløp ?: 0) + (utbetaling?.personNettoBeløp ?: 0)

    @Deprecated("Speil må bytte til sisteUtbetaling")
    val utbetalinger: UtbetalingerDTO = UtbetalingerDTO(
        arbeidsgiverUtbetaling = sisteUtbetaling?.arbeidsgiverOppdrag?.let {
            UtbetalingerDTO.UtbetalingDTO(
                fagsystemId = it.fagsystemId,
                linjer = it.utbetalingslinjer
            )
        },
        personUtbetaling = sisteUtbetaling?.personOppdrag?.let {
            UtbetalingerDTO.UtbetalingDTO(
                fagsystemId = it.fagsystemId,
                linjer = it.utbetalingslinjer
            )
        }
    )

    @Deprecated("Speil må bytte til sisteUtbetaling")
    val utbetalteUtbetalinger: UtbetalingerDTO = utbetalinger
}

data class GhostPeriodeDTO(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vilkårsgrunnlagHistorikkInnslagId: UUID?,
    val deaktivert: Boolean
)

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
        val grad: Int,
        val endringskode: EndringskodeDTO
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
    override val sykdomstidslinje: List<SykdomstidslinjedagDTO>,
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
    val sykdomstidslinje: List<SykdomstidslinjedagDTO>
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
    val type: DagtypeDTO
    val inntekt: Int
    val dato: LocalDate
}

data class NavDagDTO(
    override val type: DagtypeDTO = DagtypeDTO.NavDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val utbetaling: Int,
    val personbeløp: Int,
    val arbeidsgiverbeløp: Int,
    val refusjonsbeløp: Int?,
    val grad: Double,
    val totalGrad: Double?
) : UtbetalingstidslinjedagDTO

data class AvvistDagDTO(
    override val type: DagtypeDTO = DagtypeDTO.AvvistDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val begrunnelser: List<BegrunnelseDTO>,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class IkkeUtbetaltDagDTO(
    override val type: DagtypeDTO,
    override val inntekt: Int,
    override val dato: LocalDate
) : UtbetalingstidslinjedagDTO

data class NavHelgedagDTO(
    override val type: DagtypeDTO,
    override val inntekt: Int,
    override val dato: LocalDate,
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class UfullstendigVedtaksperiodedagDTO(
    override val type: DagtypeDTO,
    override val dato: LocalDate,
    override val inntekt: Int = 0
) : UtbetalingstidslinjedagDTO

enum class DagtypeDTO {
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
        val sats: Double,
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
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    EtterDødsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70;

    internal companion object {
        fun fraBegrunnelse(begrunnelse: Begrunnelse) = when (begrunnelse) {
            is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
            is Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbrukt
            is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
            is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
            is Begrunnelse.MinimumInntekt -> MinimumInntekt
            is Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
            is Begrunnelse.EtterDødsdato -> EtterDødsdato
            is Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
            is Begrunnelse.ManglerOpptjening -> ManglerOpptjening
            is Begrunnelse.Over70 -> Over70
            is Begrunnelse.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
        }
    }
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
    RevurderingFeilet(true),
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
                Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen, IkkeRapportert
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
