package no.nav.helse.serde.api.v2

import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.v2.Behandlingstype.VENTER
import no.nav.helse.serde.api.v2.buildere.BeregningId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal data class Generasjon(
    val id: UUID, // Runtime
    val perioder: List<Tidslinjeperiode>
)

internal enum class Behandlingstype {
    // Perioder som aldri har blitt beregnet hos oss
    UBEREGNET,
    // Perioder som har blitt beregnet - dvs har fått en utbetaling av noe slag
    BEHANDLET,
    // Perioder som venter på beregning
    VENTER
}

internal interface Tidslinjeperiode {
    // Brukes i Speil for å kunne korrelere tidslinje-komponenten og saksbildet. Trenger ikke være persistent på tvers av snapshots.
    val tidslinjeperiodeId: UUID
    val vedtaksperiodeId: UUID
    val fom: LocalDate
    val tom: LocalDate
    val sammenslåttTidslinje: List<SammenslåttDag>
    val behandlingstype: Behandlingstype
    val periodetype: Periodetype
    val inntektskilde: Inntektskilde
    val erForkastet: Boolean
    val opprettet: LocalDateTime

    fun erSammeVedtaksperiode(other: Tidslinjeperiode) = vedtaksperiodeId == other.vedtaksperiodeId
    fun venter() = behandlingstype == VENTER
}

internal data class Utbetalingsinfo(
    val inntekt: Int? = null,
    val utbetaling: Int? = null,
    val totalGrad: Double? = null
)

internal data class UberegnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime
) : Tidslinjeperiode {
    override val tidslinjeperiodeId: UUID = UUID.randomUUID()
}

// Dekker datagrunnlaget vi trenger for å populere både pølsen og _hele_ saksbildet
internal data class BeregnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val behandlingstype: Behandlingstype,
    override val erForkastet: Boolean,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val opprettet: LocalDateTime,
    val beregningId: BeregningId,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val skjæringstidspunkt: LocalDate,
    val maksdato: LocalDate,
    val utbetaling: Utbetaling,
    val hendelser: List<Hendelse>,
    val simulering: SimuleringsdataDTO?,
    val vilkårsgrunnlagshistorikkId: UUID,
    val periodevilkår: Vilkår,
    val aktivitetslogg: List<AktivitetDTO>
) : Tidslinjeperiode {
    override val tidslinjeperiodeId: UUID = UUID.randomUUID()

    internal fun erAnnullering() = utbetalingstype == "ANNULLERING"
    internal fun erRevurdering() = utbetalingstype == "REVURDERING"
    internal fun harSammeFagsystemId(other: BeregnetPeriode) = fagsystemId() == other.fagsystemId()

    private fun fagsystemId() = utbetaling.arbeidsgiverFagsystemId
    val utbetalingstilstand = utbetaling.status
    val utbetalingstype = utbetaling.type

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

data class Utbetaling(
    val type: String,
    val status: String,
    val arbeidsgiverNettoBeløp: Int,
    val personNettoBeløp: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val vurdering: Vurdering?
) {
    fun erAnnullering() = type == "ANNULLERING"
    data class Vurdering(
        val godkjent: Boolean,
        val tidsstempel: LocalDateTime,
        val automatisk: Boolean,
        val ident: String
    )
}



