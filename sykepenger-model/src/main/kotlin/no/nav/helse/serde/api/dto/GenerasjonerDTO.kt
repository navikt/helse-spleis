package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.dto.Behandlingstype.VENTER
import no.nav.helse.serde.api.dto.Behandlingstype.VENTER_PÅ_INFORMASJON
import no.nav.helse.serde.api.speil.builders.BeregningId

data class Generasjon(
    val id: UUID, // Runtime
    val perioder: List<Tidslinjeperiode>
)

enum class Behandlingstype {
    // Perioder som aldri har blitt beregnet hos oss
    UBEREGNET,

    // Perioder som har blitt beregnet - dvs har fått en utbetaling av noe slag
    BEHANDLET,

    // Perioder som venter på beregning
    VENTER,

    // Perioder som venter på dokumenter eller annen informasjon og dermed ikke er klare for beregning
    VENTER_PÅ_INFORMASJON
}

enum class Periodetilstand {
    TilUtbetaling,
    TilAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    UtbetalingFeilet,
    RevurderingFeilet,
    ForberederGodkjenning,
    ManglerInformasjon,
    VenterPåAnnenPeriode,
    TilGodkjenning,
    IngenUtbetaling,
    //Gamle tilstander under
    Feilet,
    Oppgaver,
    Venter,
    VenterPåKiling,
    KunFerie,
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

interface Tidslinjeperiode {
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
    val periodetilstand: Periodetilstand

    fun erSammeVedtaksperiode(other: Tidslinjeperiode) = vedtaksperiodeId == other.vedtaksperiodeId
    fun venter() = behandlingstype in setOf(VENTER, VENTER_PÅ_INFORMASJON)
}

data class UberegnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val periodetilstand: Periodetilstand
) : Tidslinjeperiode {
    override val tidslinjeperiodeId: UUID = UUID.randomUUID()
}

// Dekker datagrunnlaget vi trenger for å populere både pølsen og _hele_ saksbildet
data class BeregnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val behandlingstype: Behandlingstype,
    override val erForkastet: Boolean,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val opprettet: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    val beregningId: BeregningId,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val skjæringstidspunkt: LocalDate,
    val maksdato: LocalDate,
    val utbetaling: Utbetaling,
    val hendelser: List<HendelseDTO>,
    val vilkårsgrunnlagshistorikkId: UUID,
    val periodevilkår: Vilkår,
    val aktivitetslogg: List<AktivitetDTO>,
    val refusjon: Refusjon?,
    val tilstand: Periodetilstand
) : Tidslinjeperiode {
    override val tidslinjeperiodeId: UUID = UUID.randomUUID()

    internal fun erAnnullering() = utbetaling.type == Utbetalingtype.ANNULLERING
    internal fun erRevurdering() = utbetaling.type == Utbetalingtype.REVURDERING
    internal fun harSammeFagsystemId(other: BeregnetPeriode) = fagsystemId() == other.fagsystemId()

    private fun fagsystemId() = utbetaling.arbeidsgiverFagsystemId

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
    Forkastet,
    Godkjent,
    GodkjentUtenUtbetaling,
    IkkeGodkjent,
    Overført,
    Sendt,
    Ubetalt,
    UtbetalingFeilet,
    Utbetalt
}

enum class Utbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER
}

data class Utbetaling(
    val type: Utbetalingtype,
    val status: Utbetalingstatus,
    val arbeidsgiverNettoBeløp: Int,
    val personNettoBeløp: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val oppdrag: Map<String, SpeilOppdrag>,
    val vurdering: Vurdering?,
    val id: UUID,
    val tilGodkjenning: Boolean
) {
    fun erAnnullering() = type == Utbetalingtype.ANNULLERING
    private fun erForkastetRevurdering() = status == Utbetalingstatus.Forkastet && type == Utbetalingtype.REVURDERING
    private fun erUtbetalingFeilet() = status == Utbetalingstatus.UtbetalingFeilet && type == Utbetalingtype.UTBETALING
    fun utbetales() = status in listOf(Utbetalingstatus.Godkjent, Utbetalingstatus.Sendt, Utbetalingstatus.Overført)
    fun utbetalt() = status in listOf(Utbetalingstatus.Utbetalt, Utbetalingstatus.GodkjentUtenUtbetaling)
    fun kanUtbetales() = !erUtbetalingFeilet() && !erForkastetRevurdering()
    fun tilGodkjenning() = tilGodkjenning

    internal fun revurderingFeilet(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) = (erForkastetRevurdering() || status == Utbetalingstatus.Ubetalt) && tilstand == Vedtaksperiode.RevurderingFeilet
    internal fun utbetalingFeilet(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) = erUtbetalingFeilet() && tilstand == Vedtaksperiode.UtbetalingFeilet

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
