package no.nav.helse.serde.api

import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>
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
    val periodetype: Periodetype
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
    val grad: Double
) : UtbetalingstidslinjedagDTO

data class AvvistDagDTO(
    override val type: TypeDataDTO = TypeDataDTO.AvvistDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val begrunnelse: BegrunnelseDTO,
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
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad
}

interface HendelseDTO {
    val id: String
    val type: String
}

data class InntektsmeldingDTO(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : HendelseDTO {
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

enum class TilstandstypeDTO(private val visForkastet: Boolean = false) {
    TilUtbetaling,
    TilAnnullering(true),
    Utbetalt(true),
    Annullert(true),
    AnnulleringFeilet(true),
    Oppgaver,
    Venter,
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
    val sykepengegrunnlag: SykepengegrunnlagDTO?,
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
