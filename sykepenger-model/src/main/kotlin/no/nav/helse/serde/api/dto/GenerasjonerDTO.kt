package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.dto.Periodetilstand.ForberederGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.ManglerInformasjon
import no.nav.helse.serde.api.dto.Periodetilstand.Utbetalt
import no.nav.helse.serde.api.dto.Periodetilstand.VenterPåAnnenPeriode
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.KorrelasjonsId

data class GenerasjonDTO(
    val id: UUID, // Runtime
    val perioder: List<Tidslinjeperiode>
)

enum class Periodetilstand {
    TilUtbetaling,
    TilAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    RevurderingFeilet,
    ForberederGodkjenning,
    ManglerInformasjon,
    UtbetaltVenterPåAnnenPeriode,
    VenterPåAnnenPeriode,
    TilGodkjenning,
    IngenUtbetaling,
    TilInfotrygd;
}

data class Utbetalingsinfo(
    val inntekt: Int? = null,
    val utbetaling: Int? = null,
    val personbeløp: Int? = null,
    val arbeidsgiverbeløp: Int? = null,
    val refusjonsbeløp: Int? = null,
    val totalGrad: Double? = null
)

abstract class Tidslinjeperiode : Comparable<Tidslinjeperiode> {
    abstract val vedtaksperiodeId: UUID
    abstract val fom: LocalDate
    abstract val tom: LocalDate
    abstract val sammenslåttTidslinje: List<SammenslåttDag>
    abstract val periodetype: Periodetype
    abstract val inntektskilde: Inntektskilde
    abstract val erForkastet: Boolean
    abstract val opprettet: LocalDateTime
    abstract val periodetilstand: Periodetilstand
    abstract val skjæringstidspunkt: LocalDate

    internal fun erSammeVedtaksperiode(other: Tidslinjeperiode) = vedtaksperiodeId == other.vedtaksperiodeId
    internal open fun venter() = periodetilstand in setOf(VenterPåAnnenPeriode, ForberederGodkjenning, ManglerInformasjon)
    override fun compareTo(other: Tidslinjeperiode) = tom.compareTo(other.tom)
}

private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private fun LocalDate.format() = format(formatter)

data class UberegnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val skjæringstidspunkt: LocalDate
) : Tidslinjeperiode() {
    override fun toString(): String {
        return "${fom.format()}-${tom.format()} - $periodetilstand"
    }
}

// Dekker datagrunnlaget vi trenger for å populere både pølsen og _hele_ saksbildet
data class BeregnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val erForkastet: Boolean,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val opprettet: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val skjæringstidspunkt: LocalDate,
    val beregningId: BeregningId,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val maksdato: LocalDate,
    val utbetaling: Utbetaling,
    val hendelser: List<HendelseDTO>,
    val periodevilkår: Vilkår,
    val aktivitetslogg: List<AktivitetDTO>,
    val vilkårsgrunnlagId: UUID?
) : Tidslinjeperiode() {

    override fun venter(): Boolean = super.venter() && periodetilstand != Utbetalt

    internal fun erAnnullering() = utbetaling.type == Utbetalingtype.ANNULLERING
    internal fun erRevurdering() = utbetaling.type == Utbetalingtype.REVURDERING
    internal fun hørerSammen(other: Tidslinjeperiode) = other is BeregnetPeriode && utbetaling.hørerSammen(other.utbetaling)
    override fun toString(): String {
        return "${fom.format()}-${tom.format()} - $periodetilstand - ${utbetaling.type}"
    }

    data class Vilkår(
        val sykepengedager: Sykepengedager,
        val alder: Alder,
        val søknadsfrist: Søknadsfrist?
    )

    data class Sykepengedager(
        val skjæringstidspunkt: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenståendeDager: Int?,
        val oppfylt: Boolean
    )

    data class Alder(
        val alderSisteSykedag: Int,
        val oppfylt: Boolean
    )

    data class Søknadsfrist(
        val sendtNav: LocalDateTime,
        val søknadFom: LocalDate,
        val søknadTom: LocalDate,
        val oppfylt: Boolean
    )
}

data class SpeilOppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val simulering: Simulering?,
    val utbetalingslinjer: List<Utbetalingslinje>
) {
    data class Simulering(
        val totalbeløp: Int,
        val perioder: List<Simuleringsperiode>
    )

    data class Simuleringsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalinger: List<Simuleringsutbetaling>
    )

    data class Simuleringsutbetaling(
        val mottakerId: String,
        val mottakerNavn: String,
        val forfall: LocalDate,
        val feilkonto: Boolean,
        val detaljer: List<Simuleringsdetaljer>
    )

    data class Simuleringsdetaljer(
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

    data class Utbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: Int,
        val grad: Int,
        val endringskode: EndringskodeDTO
    )
}

enum class Utbetalingstatus {
    Annullert,
    Godkjent,
    GodkjentUtenUtbetaling,
    IkkeGodkjent,
    Overført,
    Ubetalt,
    Utbetalt
}

enum class Utbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER
}

class Utbetaling(
    val type: Utbetalingtype,
    val status: Utbetalingstatus,
    val arbeidsgiverNettoBeløp: Int,
    val personNettoBeløp: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val oppdrag: Map<String, SpeilOppdrag>,
    val vurdering: Vurdering?,
    val id: UUID,
    val tilGodkjenning: Boolean,
    private val korrelasjonsId: KorrelasjonsId
) {
    fun tilGodkjenning() = tilGodkjenning

    fun hørerSammen(other: Utbetaling) = korrelasjonsId == other.korrelasjonsId

    data class Vurdering(
        val godkjent: Boolean,
        val tidsstempel: LocalDateTime,
        val automatisk: Boolean,
        val ident: String
    )
}

data class Refusjon(
    val arbeidsgiverperioder: List<Periode>,
    val endringer: List<Endring>,
    val førsteFraværsdag: LocalDate?,
    val sisteRefusjonsdag: LocalDate?,
    val beløp: Double?,
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class Endring(
        val beløp: Double,
        val dato: LocalDate
    )
}
