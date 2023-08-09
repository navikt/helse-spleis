package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.dto.HendelseDTO.Companion.finn
import no.nav.helse.serde.api.dto.Periodetilstand.ForberederGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.ManglerInformasjon
import no.nav.helse.serde.api.dto.Periodetilstand.TilSkjønnsfastsettelse
import no.nav.helse.serde.api.dto.Periodetilstand.Utbetalt
import no.nav.helse.serde.api.dto.Periodetilstand.VenterPåAnnenPeriode
import no.nav.helse.serde.api.speil.Generasjoner
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.speil.merge
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde

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
    TilSkjønnsfastsettelse,
    TilGodkjenning,
    IngenUtbetaling,
    TilInfotrygd;
}

data class Utbetalingsinfo(
    val personbeløp: Int? = null,
    val arbeidsgiverbeløp: Int? = null,
    val totalGrad: Int // Speil vises grad i heltall
) {
    fun harUtbetaling() = personbeløp != null || arbeidsgiverbeløp != null
}

enum class Tidslinjeperiodetype {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    OVERGANG_FRA_IT,
    INFOTRYGDFORLENGELSE;
}

abstract class Tidslinjeperiode : Comparable<Tidslinjeperiode> {
    abstract val vedtaksperiodeId: UUID
    abstract val fom: LocalDate
    abstract val tom: LocalDate
    abstract val sammenslåttTidslinje: List<SammenslåttDag>
    abstract val periodetype: Tidslinjeperiodetype
    abstract val inntektskilde: UtbetalingInntektskilde
    abstract val erForkastet: Boolean
    abstract val opprettet: LocalDateTime
    abstract val oppdatert: LocalDateTime
    abstract val periodetilstand: Periodetilstand
    abstract val skjæringstidspunkt: LocalDate
    abstract val hendelser: List<HendelseDTO>
    protected abstract val sorteringstidspunkt: LocalDateTime

    internal open fun registrerBruk(vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk, organisasjonsnummer: String): Tidslinjeperiode {
        return this
    }
    internal fun erSammeVedtaksperiode(other: Tidslinjeperiode) = vedtaksperiodeId == other.vedtaksperiodeId
    internal open fun venter() = periodetilstand in setOf(VenterPåAnnenPeriode, ForberederGodkjenning, ManglerInformasjon)

    internal abstract fun tilGenerasjon(generasjoner: Generasjoner)
    override fun compareTo(other: Tidslinjeperiode) = tom.compareTo(other.tom)

    internal companion object {
        fun List<Tidslinjeperiode>.sorterEtterHendelse() = this
            .sortedBy { it.sorteringstidspunkt }
        fun List<Tidslinjeperiode>.sorterEtterPeriode() = this
            .sortedByDescending { it.fom }
    }
}

private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private fun LocalDate.format() = format(formatter)

data class UberegnetVilkårsprøvdPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val periodetype: Tidslinjeperiodetype, // feltet gir ikke mening for uberegnede perioder
    override val inntektskilde: UtbetalingInntektskilde, // feltet gir ikke mening for uberegnede perioder
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val oppdatert: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val skjæringstidspunkt: LocalDate,
    override val hendelser: List<HendelseDTO>,
    val vilkårsgrunnlagId: UUID
) : Tidslinjeperiode() {
    override val sorteringstidspunkt = opprettet

    internal constructor(uberegnetPeriode: UberegnetPeriode, vilkårsgrunnlagId: UUID, tidslinjeperiodetype: Tidslinjeperiodetype) :
            this(
                vedtaksperiodeId = uberegnetPeriode.vedtaksperiodeId,
                fom = uberegnetPeriode.fom,
                tom = uberegnetPeriode.tom,
                sammenslåttTidslinje = uberegnetPeriode.sammenslåttTidslinje,
                periodetype = tidslinjeperiodetype,
                inntektskilde = uberegnetPeriode.inntektskilde,
                erForkastet = uberegnetPeriode.erForkastet,
                opprettet = uberegnetPeriode.opprettet,
                oppdatert = uberegnetPeriode.oppdatert,
                periodetilstand = uberegnetPeriode.periodetilstand,
                skjæringstidspunkt = uberegnetPeriode.skjæringstidspunkt,
                hendelser = uberegnetPeriode.hendelser,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
            )

    override fun tilGenerasjon(generasjoner: Generasjoner) {
        generasjoner.uberegnetVilkårsprøvdPeriode(this)
    }

}

data class UberegnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val periodetype: Tidslinjeperiodetype, // feltet gir ikke mening for uberegnede perioder
    override val inntektskilde: UtbetalingInntektskilde, // feltet gir ikke mening for uberegnede perioder
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val oppdatert: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val skjæringstidspunkt: LocalDate,
    override val hendelser: List<HendelseDTO>
) : Tidslinjeperiode() {
    override val sorteringstidspunkt = opprettet
    override fun toString(): String {
        return "${fom.format()}-${tom.format()} - $periodetilstand"
    }

    override fun tilGenerasjon(generasjoner: Generasjoner) {
        generasjoner.uberegnetPeriode(this)
    }

    override fun registrerBruk(
        vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk,
        organisasjonsnummer: String
    ): Tidslinjeperiode {
        return vilkårsgrunnlaghistorikk.potensiellUBeregnetVilkårsprøvdPeriode(this, skjæringstidspunkt, organisasjonsnummer, vedtaksperiodeId) ?: this
    }

    internal class Builder(
        private val vedtaksperiodeId: UUID,
        private val skjæringstidspunkt: LocalDate,
        private val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        private val opprettet: LocalDateTime,
        private val oppdatert: LocalDateTime,
        private val periode: Periode,
        private val hendelser: List<HendelseDTO>
    ) {
        private lateinit var sykdomstidslinje: List<Sykdomstidslinjedag>

        internal fun build() = UberegnetPeriode(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periode.start,
            tom = periode.endInclusive,
            sammenslåttTidslinje = sykdomstidslinje.merge(emptyList()),
            periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // feltet gir ikke mening for uberegnede perioder
            inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // feltet gir ikke mening for uberegnede perioder
            erForkastet = false,
            opprettet = opprettet,
            oppdatert = oppdatert,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            periodetilstand = when (tilstand) {
                is Vedtaksperiode.AvsluttetUtenUtbetaling -> Periodetilstand.IngenUtbetaling
                is Vedtaksperiode.AvventerRevurdering,
                is Vedtaksperiode.AvventerBlokkerendePeriode -> VenterPåAnnenPeriode
                is Vedtaksperiode.AvventerHistorikk,
                is Vedtaksperiode.AvventerVilkårsprøving -> ForberederGodkjenning
                is Vedtaksperiode.AvventerSkjønnsmessigFastsettelse -> TilSkjønnsfastsettelse
                else -> ManglerInformasjon
            }

        )

        internal fun medSykdomstidslinje(sykdomstidslinje: List<Sykdomstidslinjedag>) = apply {
            if (this::sykdomstidslinje.isInitialized && sykdomstidslinje != this.sykdomstidslinje) {
                // Hvis sykdomstidslinjen allerede er initialisert for en uberegnet periode,
                // skyldes det at vi besøker sykdomstidslinjen fra vedtaksperiodeutbetalinger.
                // Vi lar ikke disse sykdomstidslinjene overskrive sykdomstidslinjen allerede satt fra vedtaksperioden
                return this
            }
            this.sykdomstidslinje = sykdomstidslinje
        }
    }
}

// Dekker datagrunnlaget vi trenger for å populere både pølsen og _hele_ saksbildet
data class BeregnetPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val sammenslåttTidslinje: List<SammenslåttDag>,
    override val erForkastet: Boolean, // feltet trengs ikke i speil
    override val periodetype: Tidslinjeperiodetype,
    override val inntektskilde: UtbetalingInntektskilde, // verdien av dette feltet brukes bare for å sjekke !=null i speil
    override val opprettet: LocalDateTime,
    val beregnet: LocalDateTime,
    override val oppdatert: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val skjæringstidspunkt: LocalDate,
    override val hendelser: List<HendelseDTO>,
    // todo: feltet brukes så og si ikke i speil, kan fjernes fra graphql
    // verdien av ID-en brukes ifm. å lage en unik ID for notatet om utbetalingene.
    val beregningId: UUID,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val maksdato: LocalDate,
    val utbetaling: Utbetaling,
    val periodevilkår: Vilkår,
    val vilkårsgrunnlagId: UUID? // dette feltet er i != for beregnede perioder, men må være nullable så lenge annullerte perioder mappes til beregnet periode
) : Tidslinjeperiode() {
    override val sorteringstidspunkt = beregnet

    override fun venter(): Boolean = super.venter() && periodetilstand != Utbetalt

    override fun registrerBruk(vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk, organisasjonsnummer: String): BeregnetPeriode {
        val periodetype = vilkårsgrunnlagId?.let {
            vilkårsgrunnlaghistorikk
                .leggIBøtta(it)
                .utledPeriodetype(organisasjonsnummer, vedtaksperiodeId)
        } ?: return this
        return this.copy(periodetype = periodetype)
    }

    internal fun sammeUtbetaling(other: BeregnetPeriode) = this.utbetaling.id == other.utbetaling.id
            || (Toggle.ForenkleRevurdering.enabled && this.utbetaling.korrelasjonsId == other.utbetaling.korrelasjonsId)

    internal fun somAnnullering(annulleringer: List<AnnullertUtbetaling>): AnnullertPeriode? {
        val annulleringen = annulleringer.firstOrNull { it.annullerer(this.utbetaling.korrelasjonsId) } ?: return null
        return AnnullertPeriode(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            opprettet = opprettet,
            // feltet gir ikke mening for annullert periode:
            vilkår = Vilkår(
                sykepengedager = Sykepengedager(fom, LocalDate.MAX, null, null, false),
                alder = this.periodevilkår.alder,
                søknadsfrist = null
            ),
            beregnet = annulleringen.annulleringstidspunkt,
            oppdatert = oppdatert,
            periodetilstand = annulleringen.periodetilstand,
            hendelser = hendelser,
            beregningId = beregningId,
            utbetaling = Utbetaling(
                Utbetalingtype.ANNULLERING,
                this.utbetaling.korrelasjonsId,
                annulleringen.utbetalingstatus,
                0,
                0,
                this.utbetaling.arbeidsgiverFagsystemId,
                this.utbetaling.personFagsystemId,
                emptyMap(),
                null,
                annulleringen.id
            )
        )
    }

    override fun toString(): String {
        return "${fom.format()}-${tom.format()} - $periodetilstand - ${utbetaling.type}"
    }

    override fun tilGenerasjon(generasjoner: Generasjoner) {
        check(utbetaling.type in setOf(Utbetalingtype.REVURDERING, Utbetalingtype.UTBETALING)) {
            "beregnet periode skal bare anvendes på utbetalte perioder"
        }
        when (utbetaling.type) {
            Utbetalingtype.REVURDERING -> generasjoner.revurdertPeriode(this)
            else -> generasjoner.utbetaltPeriode(this)
        }
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

    /**
     * Bygger en periode som har minst én utbetaling
     */
    internal class Builder(
        private val vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeId: UUID,
        private val periodetilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        private val opprettet: LocalDateTime,
        private val oppdatert: LocalDateTime,
        private val periode: Periode,
        private val hendelser: List<HendelseDTO>
    ) {
        private lateinit var beregnet: LocalDateTime
        private lateinit var skjæringstidspunkt: LocalDate
        private lateinit var utbetalingId: UUID
        private lateinit var korrelasjonsId: UUID
        private var gjenståendeSykedager: Int = 0
        private var forbrukteSykedager: Int = 0
        private lateinit var maksdato: LocalDate
        private lateinit var vilkårsgrunnlagId: UUID

        private lateinit var utbetalingstidslinje: List<Utbetalingstidslinjedag>
        private lateinit var sykdomstidslinje: List<Sykdomstidslinjedag>

        private var utbetalingtype: Utbetalingtype? = null
        private lateinit var utbetalingstatus: Utbetalingstatus
        private var utbetalingvurdering: Utbetaling.Vurdering? = null

        private var arbeidsgiverNettoBeløp: Int = 0
        private var personNettoBeløp: Int = 0
        private lateinit var arbeidsgiverFagsystemId: String
        private lateinit var personFagsystemId: String
        private val oppdrag = mutableMapOf<String, SpeilOppdrag>()

        fun build(alder: no.nav.helse.Alder): BeregnetPeriode? {
            val utbetalingtype = this.utbetalingtype ?: return null

            val avgrensetUtbetalingstidslinje = utbetalingstidslinje.filter { it.dato in periode }
            val utbetalingDTO = Utbetaling(
                type = utbetalingtype,
                korrelasjonsId = korrelasjonsId,
                status = utbetalingstatus,
                arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                personNettoBeløp = personNettoBeløp,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                oppdrag = oppdrag,
                vurdering = utbetalingvurdering,
                id = utbetalingId
            )
            return BeregnetPeriode(
                vedtaksperiodeId = vedtaksperiodeId,
                beregningId = utbetalingId,
                fom = periode.start,
                tom = periode.endInclusive,
                erForkastet = false,
                periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // TODO: fikse,
                inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // verdien av feltet brukes ikke i speil
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                maksdato = maksdato,
                beregnet = beregnet,
                opprettet = opprettet,
                oppdatert = oppdatert,
                periodevilkår = periodevilkår(alder, skjæringstidspunkt, avgrensetUtbetalingstidslinje, hendelser),
                sammenslåttTidslinje = sykdomstidslinje.merge(utbetalingstidslinje),
                gjenståendeSykedager = gjenståendeSykedager,
                forbrukteSykedager = forbrukteSykedager,
                utbetaling = utbetalingDTO,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                periodetilstand = utledePeriodetilstand(utbetalingDTO, avgrensetUtbetalingstidslinje),
            )
        }

        private fun utledePeriodetilstand(utbetalingDTO: Utbetaling, avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>) =
            when (utbetalingDTO.status) {
                Utbetalingstatus.IkkeGodkjent -> Periodetilstand.RevurderingFeilet
                Utbetalingstatus.Utbetalt -> when {
                    periodetilstand == Vedtaksperiode.AvventerRevurdering -> Periodetilstand.UtbetaltVenterPåAnnenPeriode
                    avgrensetUtbetalingstidslinje.none { it.utbetalingsinfo()?.harUtbetaling() == true } -> Periodetilstand.IngenUtbetaling
                    else -> Utbetalt
                }
                Utbetalingstatus.Ubetalt -> when {
                    periodetilstand in setOf(Vedtaksperiode.AvventerGodkjenningRevurdering, Vedtaksperiode.AvventerGodkjenning) -> Periodetilstand.TilGodkjenning
                    periodetilstand == Vedtaksperiode.AvventerGjennomførtRevurdering && klarTilGodkjenning(utbetalingDTO) -> Periodetilstand.TilGodkjenning
                    periodetilstand in setOf(Vedtaksperiode.AvventerSimulering, Vedtaksperiode.AvventerSimuleringRevurdering) -> ForberederGodkjenning
                    else -> VenterPåAnnenPeriode
                }
                Utbetalingstatus.GodkjentUtenUtbetaling -> when {
                    periodetilstand == Vedtaksperiode.AvventerRevurdering -> Periodetilstand.UtbetaltVenterPåAnnenPeriode
                    utbetalingDTO.type == Utbetalingtype.REVURDERING -> Utbetalt
                    else -> Periodetilstand.IngenUtbetaling
                }
                Utbetalingstatus.Godkjent,
                Utbetalingstatus.Overført -> Periodetilstand.TilUtbetaling
                else -> error("har ikke mappingregel for ${utbetalingDTO.status}")
            }
        private fun klarTilGodkjenning(utbetalingDTO: Utbetaling): Boolean {
            return utbetalingDTO.oppdrag.any { it.value.simulering != null }
                    || utbetalingDTO.oppdrag.all { it.value.utbetalingslinjer.all { it.endringskode == EndringskodeDTO.UEND } }
        }

        private fun List<Utbetalingstidslinjedag>.sisteNavDag() =
            lastOrNull { it.type == UtbetalingstidslinjedagType.NavDag }

        private fun periodevilkår(
            alder: no.nav.helse.Alder,
            skjæringstidspunkt: LocalDate,
            avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>,
            hendelser: List<HendelseDTO>
        ): Vilkår {
            val sisteSykepengedag = avgrensetUtbetalingstidslinje.sisteNavDag()?.dato ?: periode.endInclusive
            val sykepengedager = Sykepengedager(skjæringstidspunkt, maksdato, forbrukteSykedager, gjenståendeSykedager, maksdato > sisteSykepengedag)
            val alderSisteSykepengedag = alder.let {
                Alder(it.alderPåDato(sisteSykepengedag), it.innenfor70årsgrense(sisteSykepengedag))
            }
            val søknadsfrist = hendelser.finn<SøknadNavDTO>()?.let {
                Søknadsfrist(
                    sendtNav = it.sendtNav,
                    søknadFom = it.fom,
                    søknadTom = it.tom,
                    oppfylt = it.søknadsfristOppfylt()
                )
            }

            return Vilkår(sykepengedager, alderSisteSykepengedag, søknadsfrist)
        }

        internal fun medUtbetaling(
            id: UUID,
            korrelasjonsId: UUID,
            status: String,
            opprettet: LocalDateTime,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeDager: Int
        ) = apply {
            medUtbetaling(Utbetalingtype.UTBETALING, id, korrelasjonsId, status, opprettet, maksdato, forbrukteSykedager, gjenståendeDager)
        }
        internal fun medRevurdering(
            id: UUID,
            korrelasjonsId: UUID,
            status: String,
            opprettet: LocalDateTime,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeDager: Int
        ) = apply {
            medUtbetaling(Utbetalingtype.REVURDERING, id, korrelasjonsId, status, opprettet, maksdato, forbrukteSykedager, gjenståendeDager)
        }

        private fun medUtbetaling(
            type: Utbetalingtype,
            id: UUID,
            korrelasjonsId: UUID,
            status: String,
            opprettet: LocalDateTime,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeDager: Int
        ) {
            this.utbetalingtype = type
            this.korrelasjonsId = korrelasjonsId
            this.utbetalingstatus = utledStatus(type, status)
            this.utbetalingId = id
            this.beregnet = opprettet
            this.maksdato = maksdato
            this.forbrukteSykedager = forbrukteSykedager
            this.gjenståendeSykedager = gjenståendeDager
        }

        internal fun medVurdering(godkjent: Boolean, tidsstempel: LocalDateTime, automatisk: Boolean, ident: String) = apply {
            this.utbetalingvurdering = Utbetaling.Vurdering(godkjent, tidsstempel, automatisk, ident)
        }

        fun medArbeidsgiveroppdrag(oppdrag: SpeilOppdrag) = apply {
            this.arbeidsgiverFagsystemId = oppdrag.fagsystemId
            this.arbeidsgiverNettoBeløp = oppdrag.nettobeløp
            this.oppdrag[oppdrag.fagsystemId] = oppdrag
        }

        fun medPersonoppdrag(oppdrag: SpeilOppdrag) = apply {
            this.personFagsystemId = oppdrag.fagsystemId
            this.personNettoBeløp = oppdrag.nettobeløp
            this.oppdrag[oppdrag.fagsystemId] = oppdrag
        }

        fun medUtbetalingstidslinje(utbetalingstidslinje: List<Utbetalingstidslinjedag>) {
            this.utbetalingstidslinje = utbetalingstidslinje
        }

        fun medVilkårsgrunnlag(vilkårsgrunnlagId: UUID, skjæringstidspunkt: LocalDate) {
            this.vilkårsgrunnlagId = vilkårsgrunnlagId
            this.skjæringstidspunkt = skjæringstidspunkt
        }

        fun medSykdomstidslinje(sykdomstidslinje: List<Sykdomstidslinjedag>) = apply {
            this.sykdomstidslinje = sykdomstidslinje
        }

        private fun utledStatus(type: Utbetalingtype, status: String): Utbetalingstatus {
            return when (status) {
                "ANNULLERT" -> Utbetalingstatus.Annullert
                "GODKJENT" -> Utbetalingstatus.Godkjent
                "GODKJENT_UTEN_UTBETALING" -> Utbetalingstatus.GodkjentUtenUtbetaling
                "IKKE_GODKJENT" -> when (type) {
                    Utbetalingtype.REVURDERING -> Utbetalingstatus.IkkeGodkjent
                    else -> error("forsøker å mappe en IKKE_GODKJENT-utbetaling til Speil, som ikke er revurdering")
                }
                "OVERFØRT" -> Utbetalingstatus.Overført
                "IKKE_UTBETALT" -> Utbetalingstatus.Ubetalt
                "UTBETALT" -> Utbetalingstatus.Utbetalt
                else -> error("har ingen mappingregel for $status")
            }
        }
    }
}

data class AnnullertPeriode(
    override val vedtaksperiodeId: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val opprettet: LocalDateTime,
    val vilkår: BeregnetPeriode.Vilkår, // feltet gir ikke mening for annullert periode
    val beregnet: LocalDateTime,
    override val oppdatert: LocalDateTime,
    override val periodetilstand: Periodetilstand,
    override val hendelser: List<HendelseDTO>,

    // todo: feltet brukes så og si ikke i speil, kan fjernes fra graphql
    // verdien av ID-en brukes ifm. å lage en unik ID for notatet om utbetalingene.
    val beregningId: UUID,
    val utbetaling: Utbetaling
) : Tidslinjeperiode() {
    override val sammenslåttTidslinje: List<SammenslåttDag> = emptyList() // feltet gir ikke mening for annullert periode
    override val erForkastet = true
    override val skjæringstidspunkt = fom // feltet gir ikke mening for annullert periode
    override val periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING // feltet gir ikke mening for annullert periode
    override val inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER // feltet gir ikke mening for annullert periode
    override val sorteringstidspunkt = beregnet

    override fun tilGenerasjon(generasjoner: Generasjoner) {
        generasjoner.annullertPeriode(this)
    }

    // returnerer en beregnet perioder for
    // at mapping til graphql skal være lik.
    // TODO: Speil bør ha et konsept om 'AnnullertPeriode' som egen type,
    // slik at vi kan slippe å sende så mange unødvendige felter for annulleringene
    fun somBeregnetPeriode(): BeregnetPeriode {
        return BeregnetPeriode(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            sammenslåttTidslinje = sammenslåttTidslinje,
            erForkastet = erForkastet,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
            opprettet = opprettet,
            beregnet = beregnet,
            oppdatert = oppdatert,
            periodetilstand = periodetilstand,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            beregningId = beregningId,
            gjenståendeSykedager = null,
            forbrukteSykedager = null,
            maksdato = LocalDate.MAX,
            utbetaling = utbetaling,
            periodevilkår = vilkår,
            vilkårsgrunnlagId = null
        )
    }
}

internal class AnnullertUtbetaling(
    internal val id: UUID,
    private val korrelasjonsId: UUID,
    internal val annulleringstidspunkt: LocalDateTime,
    internal val utbetalingstatus: Utbetalingstatus
) {
    val periodetilstand = when (utbetalingstatus) {
        Utbetalingstatus.Annullert -> Periodetilstand.Annullert
        else -> Periodetilstand.TilAnnullering
    }

    fun annullerer(korrelasjonsId: UUID) = this.korrelasjonsId == korrelasjonsId

}

data class SpeilOppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val nettobeløp: Int,
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

    class Builder(
        private val fagsystemId: String,
        private val tidsstempel: LocalDateTime,
        private val nettobeløp: Int,
        simuleringresultat: SimuleringResultat?
    ) {
        private var simulering: Simulering? = null
        private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()

        init {
            medSimulering(simuleringresultat)
        }

        fun medOppdragslinje(fom: LocalDate, tom: LocalDate, beløp: Int, grad: Int, endringskode: EndringskodeDTO) = apply {
            utbetalingslinjer.add(Utbetalingslinje(fom, tom, beløp, grad, endringskode))
        }

        fun build() = SpeilOppdrag(fagsystemId, tidsstempel, nettobeløp, simulering, utbetalingslinjer)

        private fun medSimulering(simuleringsresultat: SimuleringResultat?) {
            if (simuleringsresultat == null) return
            this.simulering = Simulering(
                totalbeløp = simuleringsresultat.totalbeløp,
                perioder = simuleringsresultat.perioder.map { periode ->
                    Simuleringsperiode(
                        fom = periode.periode.start,
                        tom = periode.periode.endInclusive,
                        utbetalinger = periode.utbetalinger.map { utbetaling ->
                            Simuleringsutbetaling(
                                mottakerId = utbetaling.utbetalesTil.id,
                                mottakerNavn = utbetaling.utbetalesTil.navn,
                                forfall = utbetaling.forfallsdato,
                                feilkonto = utbetaling.feilkonto,
                                detaljer = utbetaling.detaljer.map {
                                    Simuleringsdetaljer(
                                        faktiskFom = it.periode.start,
                                        faktiskTom = it.periode.endInclusive,
                                        konto = it.konto,
                                        beløp = it.beløp,
                                        tilbakeføring = it.tilbakeføring,
                                        sats = it.sats.sats,
                                        typeSats = it.sats.type,
                                        antallSats = it.sats.antall,
                                        uføregrad = it.uføregrad,
                                        klassekode = it.klassekode.kode,
                                        klassekodeBeskrivelse = it.klassekode.beskrivelse,
                                        utbetalingstype = it.utbetalingstype,
                                        refunderesOrgNr = it.refunderesOrgnummer
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    }
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
    val korrelasjonsId: UUID,
    val status: Utbetalingstatus,
    val arbeidsgiverNettoBeløp: Int,
    val personNettoBeløp: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val oppdrag: Map<String, SpeilOppdrag>,
    val vurdering: Vurdering?,
    val id: UUID
) {
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
