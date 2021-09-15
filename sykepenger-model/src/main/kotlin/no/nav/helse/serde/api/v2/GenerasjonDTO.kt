package no.nav.helse.serde.api.v2

import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.*
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.serde.mapping.SpeilKildetype
import java.time.LocalDate
import java.util.*

internal data class Generasjon(
    val id: UUID, // Runtime
    val perioder: List<Periode>
)

internal enum class Behandlingstype {
    UBEHANDLET, BEHANDLET, KORT_PERIODE
}

internal interface Periode {
    val fom: LocalDate
    val tom: LocalDate
    val sykdomstidslinje: List<SammenslåttDag>
    val behandlingstype: Behandlingstype
    val periodetype: Periodetype
    val inntektskilde: Inntektskilde
    val erForkastet: Boolean
}

internal data class Utbetalingsinfo(
    val inntekt: Int? = null,
    val utbetaling: Int? = null,
    val totalGrad: Double? = null
)

internal data class SammenslåttDag(
    val dagen: LocalDate,
    val sykdomstidslinjedagtype: SpeilDagtype,
    val utbetalingsdagtype: TypeDataDTO,
    val kilde: KildeDTO,
    val grad: Double? = null,
    val utbetalingsinfo: Utbetalingsinfo? = null,
    val begrunnelser: List<BegrunnelseDTO>? = null,
) {
    data class KildeDTO(
        val type: SpeilKildetype,
        val kildeId: UUID
    )
}

// Dekker datagrunnlaget vi trenger for å populere både pølsen og _hele_ saksbildet
internal data class Tidslinjeperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sykdomstidslinje: List<SammenslåttDag>,
    override val behandlingstype: Behandlingstype,
    override val erForkastet: Boolean,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    val vedtaksperiodeDTO: VedtaksperiodeDTO,
    val skjæringstidspunkt: LocalDate,
    val maksdato: LocalDate,
    val utbetalingDTO: UtbetalingshistorikkElementDTO.UtbetalingDTO,
    val hendelser: List<HendelseDTO>,
    val simulering: SimuleringsdataDTO,
    //Lookup for vilkår(beregningId, skjæringstidspunkt), inntektsgrunnlag(beregningId, skjæringstidspunkt)

) : Periode {
    val utbetalingstilstand = utbetalingDTO.status
    val utbetalingstype = utbetalingDTO.type
}



