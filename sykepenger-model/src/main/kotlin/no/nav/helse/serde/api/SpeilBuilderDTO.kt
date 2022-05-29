package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.v2.Generasjon
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import no.nav.helse.utbetalingstidslinje.Begrunnelse

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
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
    val ghostPerioder: List<GhostPeriodeDTO>,
    val generasjoner: List<Generasjon>
)

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

enum class TilstandstypeDTO {
    TilUtbetaling,
    TilAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    Oppgaver,
    Venter,
    VenterPåKiling,
    IngenUtbetaling,
    KunFerie,
    Feilet,
    RevurderingFeilet,
    TilInfotrygd;
}

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
