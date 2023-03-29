package no.nav.helse.serde.api.speil

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Vedtaksperiode.AvsluttetUtenUtbetaling
import no.nav.helse.person.Vedtaksperiode.AvventerBlokkerendePeriode
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenning
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenningRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikk
import no.nav.helse.person.Vedtaksperiode.AvventerRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerSimulering
import no.nav.helse.person.Vedtaksperiode.AvventerSimuleringRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerVilkårsprøving
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GenerasjonDTO
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.HendelseDTO.Companion.finn
import no.nav.helse.serde.api.dto.Periodetilstand.Annullert
import no.nav.helse.serde.api.dto.Periodetilstand.ForberederGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.IngenUtbetaling
import no.nav.helse.serde.api.dto.Periodetilstand.ManglerInformasjon
import no.nav.helse.serde.api.dto.Periodetilstand.RevurderingFeilet
import no.nav.helse.serde.api.dto.Periodetilstand.TilAnnullering
import no.nav.helse.serde.api.dto.Periodetilstand.TilGodkjenning
import no.nav.helse.serde.api.dto.Periodetilstand.TilUtbetaling
import no.nav.helse.serde.api.dto.Periodetilstand.Utbetalt
import no.nav.helse.serde.api.dto.Periodetilstand.UtbetaltVenterPåAnnenPeriode
import no.nav.helse.serde.api.dto.Periodetilstand.VenterPåAnnenPeriode
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SøknadNavDTO
import no.nav.helse.serde.api.dto.Tidslinjeperiode
import no.nav.helse.serde.api.dto.Tidslinjeperiode.Companion.sorterEtterHendelse
import no.nav.helse.serde.api.dto.Tidslinjeperiode.Companion.sorterEtterPeriode
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.Utbetaling
import no.nav.helse.serde.api.dto.Utbetalingstatus
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.serde.api.speil.IVedtaksperiode.Companion.tilGodkjenning
import no.nav.helse.serde.api.speil.Tidslinjeberegninger.ITidslinjeberegning
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.speil.builders.PeriodeVarslerBuilder

class Generasjoner {
    private val nåværendeGenerasjon = mutableListOf<Tidslinjeperiode>()
    private val generasjoner = mutableListOf<GenerasjonDTO>()
    private var tilstand: Byggetilstand = Byggetilstand.Initiell

    internal fun build(): List<GenerasjonDTO> {
        byggGenerasjon(nåværendeGenerasjon)
        return this.generasjoner.toList()
    }

    internal fun revurdertPeriode(periode: BeregnetPeriode) {
        tilstand.revurdertPeriode(this, periode)
    }

    internal fun annullertPeriode(periode: BeregnetPeriode) {
        tilstand.annullertPeriode(this, periode)
    }

    internal fun uberegnetPeriode(uberegnetPeriode: UberegnetPeriode) {
        tilstand.uberegnetPeriode(this, uberegnetPeriode)
    }

    internal fun utbetaltPeriode(beregnetPeriode: BeregnetPeriode) {
        tilstand.utbetaltPeriode(this, beregnetPeriode)
    }

    private fun byggGenerasjon(periodene: List<Tidslinjeperiode>) {
        if (periodene.isEmpty()) return
        generasjoner.add(0, GenerasjonDTO(UUID.randomUUID(), periodene.sorterEtterPeriode()))
    }

    private fun leggTilNyRad() {
        byggGenerasjon(nåværendeGenerasjon.filterNot { it.venter() })
    }

    private fun leggTilNyRadOgPeriode(periode: Tidslinjeperiode, nesteTilstand: Byggetilstand) {
        leggTilNyRad()
        leggTilNyPeriode(periode, nesteTilstand)
    }

    private fun leggTilNyPeriode(periode: Tidslinjeperiode, nesteTilstand: Byggetilstand? = null) {
        val index = nåværendeGenerasjon.indexOfFirst { other -> periode.erSammeVedtaksperiode(other) }
        if (index >= 0) nåværendeGenerasjon[index] = periode
        else nåværendeGenerasjon.add(periode)
        nesteTilstand?.also { this.tilstand = nesteTilstand }
    }

    private interface Byggetilstand {

        fun uberegnetPeriode(generasjoner: Generasjoner, periode: UberegnetPeriode) {
            generasjoner.leggTilNyPeriode(periode)
        }
        fun utbetaltPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
        }
        fun annullertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode, AnnullertGenerasjon)
        }
        fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
        }

        object Initiell : Byggetilstand {
            override fun annullertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) =
                error("forventet ikke en annullert periode i tilstand ${this::class.simpleName}!")
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) =
                error("forventet ikke en revurdert periode i tilstand ${this::class.simpleName}!")
        }

        class AktivGenerasjon(private val forrigeBeregnet: BeregnetPeriode) : Byggetilstand {
            override fun utbetaltPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                // en tidligere utbetalt periode vil bety at en tidligere uberegnet periode er omgjort/eller er out of order
                if (periode < forrigeBeregnet) return generasjoner.leggTilNyPeriode(periode, EndringITidligerePeriodeGenerasjon(periode))
                generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
            }
        }

        class EndringITidligerePeriodeGenerasjon(private val outOfOrderPeriode: BeregnetPeriode) : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                val perioder = generasjoner.nåværendeGenerasjon.filter { it >= outOfOrderPeriode && it < periode }
                generasjoner.nåværendeGenerasjon.removeAll(perioder)
                generasjoner.leggTilNyRad()
                perioder.forEach { generasjoner.leggTilNyPeriode(it) }
                generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
            }
        }

        object AnnullertGenerasjon : Byggetilstand {
            override fun annullertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                generasjoner.leggTilNyPeriode(periode)
            }
        }

        class RevurdertGenerasjon(private val revurderingen: BeregnetPeriode) : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                if (periode.sammeUtbetaling(revurderingen)) return generasjoner.leggTilNyPeriode(periode)
                generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
            }
        }
    }
}

internal class Tidslinjeperioder(
    private val alder: Alder,
    private val forkastetVedtaksperiodeIder: List<UUID>,
    vedtaksperioder: List<IVedtaksperiode>,
    tidslinjeberegninger: Tidslinjeberegninger,
    vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
) {
    private var perioder: List<Tidslinjeperiode>

    private fun erForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId in forkastetVedtaksperiodeIder

    init {
        perioder = vedtaksperioder.flatMap { periode ->
            when {
                periode.utbetalinger.isEmpty() -> listOf(uberegnetPeriode(periode, erForkastet(periode.vedtaksperiodeId)))
                else -> periode.utbetalinger.map { (vilkårsgrunnlag, utbetaling) ->
                    val tidslinjeberegning = tidslinjeberegninger.finn(utbetaling.beregningId)
                    utbetaling.settTilGodkjenning(vedtaksperioder)
                    beregnetPeriode(
                        periode = periode,
                        vilkårsgrunnlagTilutbetaling = vilkårsgrunnlag to utbetaling,
                        tidslinjeberegning = tidslinjeberegning,
                        erForkastet = erForkastet(periode.vedtaksperiodeId),
                        vilkårsgrunnlaghistorikk = vilkårsgrunnlaghistorikk
                    )
                }
            }
        }.sorterEtterHendelse()
    }

    internal fun tilGenerasjoner() = Generasjoner().apply {
        perioder.forEach { periode -> periode.tilGenerasjon(this) }
    }.build()

    private fun uberegnetPeriode(periode: IVedtaksperiode, erForkastet: Boolean): UberegnetPeriode {
        val tidslinje = periode.sykdomstidslinje.merge(emptyList())
        return UberegnetPeriode(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            fom = periode.fom,
            tom = periode.tom,
            sammenslåttTidslinje = tidslinje,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = erForkastet,
            opprettet = periode.opprettet,
            oppdatert = periode.oppdatert,
            skjæringstidspunkt = periode.skjæringstidspunkt,
            hendelser = periode.hendelser,
            periodetilstand = when (periode.tilstand) {
                is AvsluttetUtenUtbetaling -> IngenUtbetaling
                is AvventerRevurdering,
                is AvventerBlokkerendePeriode -> VenterPåAnnenPeriode
                is AvventerHistorikk,
                is AvventerVilkårsprøving -> ForberederGodkjenning
                else -> ManglerInformasjon
            }
        )
    }

    private fun beregnetPeriode(
        periode: IVedtaksperiode,
        vilkårsgrunnlagTilutbetaling: Pair<IVilkårsgrunnlag, IUtbetaling>,
        tidslinjeberegning: ITidslinjeberegning,
        erForkastet: Boolean,
        vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
    ): BeregnetPeriode {
        val utbetaling = vilkårsgrunnlagTilutbetaling.second
        val avgrensetUtbetalingstidslinje = utbetaling.utbetalingstidslinje.filter { periode.fom <= it.dato && it.dato <= periode.tom }
        val sammenslåttTidslinje =
            tidslinjeberegning.sammenslåttTidslinje(utbetaling.utbetalingstidslinje, periode.fom, periode.tom)
        val varsler = PeriodeVarslerBuilder(
            periode.aktivitetsloggForPeriode
        ).build()
        val utbetalingDTO = utbetaling.toDTO()
        val erAnnullering = utbetalingDTO.type == Utbetalingtype.ANNULLERING

        val vilkårsgrunnlag = vilkårsgrunnlagTilutbetaling.first
        if (!erAnnullering) vilkårsgrunnlaghistorikk.leggIBøtta(vilkårsgrunnlag)
        return BeregnetPeriode(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            beregningId = utbetaling.beregningId,
            fom = periode.fom,
            tom = periode.tom,
            erForkastet = erForkastet,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            skjæringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt,
            hendelser = periode.hendelser,
            maksdato = utbetaling.maksdato,
            beregnet = utbetaling.opprettet,
            opprettet = periode.opprettet,
            oppdatert = periode.oppdatert,
            periodevilkår = periodevilkår(vilkårsgrunnlag.skjæringstidspunkt, periode, utbetaling, avgrensetUtbetalingstidslinje, periode.hendelser),
            sammenslåttTidslinje = sammenslåttTidslinje,
            gjenståendeSykedager = utbetaling.gjenståendeSykedager,
            forbrukteSykedager = utbetaling.forbrukteSykedager,
            utbetaling = utbetalingDTO,
            // todo: vilkårsgrunnlagId er ikke relevant å sende for annulleringer
            vilkårsgrunnlagId = vilkårsgrunnlag.id.takeUnless { erAnnullering },
            aktivitetslogg = varsler,
            periodetilstand = utledePeriodetilstand(utbetalingDTO, periode.tilstand, avgrensetUtbetalingstidslinje),
        )
    }

    private fun utledePeriodetilstand(utbetalingDTO: Utbetaling, periodetilstand: Vedtaksperiodetilstand, avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>) =
        when (utbetalingDTO.status) {
            Utbetalingstatus.Annullert -> Annullert
            Utbetalingstatus.IkkeGodkjent -> RevurderingFeilet
            Utbetalingstatus.Utbetalt -> when {
                periodetilstand == AvventerRevurdering -> UtbetaltVenterPåAnnenPeriode
                avgrensetUtbetalingstidslinje.none { it.type == UtbetalingstidslinjedagType.NavDag } -> IngenUtbetaling
                else -> Utbetalt
            }
            Utbetalingstatus.Ubetalt -> when {
                utbetalingDTO.type == Utbetalingtype.ANNULLERING -> TilAnnullering
                utbetalingDTO.tilGodkjenning() -> TilGodkjenning
                periodetilstand in setOf(AvventerSimulering, AvventerSimuleringRevurdering) -> ForberederGodkjenning
                else -> VenterPåAnnenPeriode
            }
            Utbetalingstatus.GodkjentUtenUtbetaling -> when (utbetalingDTO.type) {
                Utbetalingtype.REVURDERING -> Utbetalt
                else -> IngenUtbetaling
            }
            Utbetalingstatus.Godkjent,
            Utbetalingstatus.Overført -> when {
                utbetalingDTO.type == Utbetalingtype.ANNULLERING -> TilAnnullering
                else -> TilUtbetaling
            }
        }

    private fun List<Utbetalingstidslinjedag>.sisteNavDag() =
        lastOrNull { it.type == UtbetalingstidslinjedagType.NavDag }

    private fun periodevilkår(
        skjæringstidspunkt: LocalDate,
        periode: IVedtaksperiode,
        utbetaling: IUtbetaling,
        avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>,
        hendelser: List<HendelseDTO>
    ): BeregnetPeriode.Vilkår {
        val sisteSykepengedag = avgrensetUtbetalingstidslinje.sisteNavDag()?.dato ?: periode.tom
        val sykepengedager = BeregnetPeriode.Sykepengedager(
            skjæringstidspunkt,
            utbetaling.maksdato,
            utbetaling.forbrukteSykedager,
            utbetaling.gjenståendeSykedager,
            utbetaling.maksdato > sisteSykepengedag
        )
        val alderSisteSykepengedag = alder.let {
            BeregnetPeriode.Alder(it.alderPåDato(sisteSykepengedag), it.innenfor70årsgrense(sisteSykepengedag))
        }
        val søknadsfrist = hendelser.finn<SøknadNavDTO>()?.let {
            BeregnetPeriode.Søknadsfrist(
                sendtNav = it.sendtNav,
                søknadFom = it.fom,
                søknadTom = it.tom,
                oppfylt = it.søknadsfristOppfylt()
            )
        }

        return BeregnetPeriode.Vilkår(sykepengedager, alderSisteSykepengedag, søknadsfrist)
    }
}

internal class IVedtaksperiode(
    val vedtaksperiodeId: UUID,
    private val forkastet: Boolean,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektskilde: Inntektskilde,
    val hendelser: List<HendelseDTO>,
    utbetalinger: List<Pair<IVilkårsgrunnlag, IUtbetaling>>,
    val periodetype: Periodetype,
    val sykdomstidslinje: List<Sykdomstidslinjedag>,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
    val tilstand: Vedtaksperiodetilstand,
    val skjæringstidspunkt: LocalDate,
    val aktivitetsloggForPeriode: Aktivitetslogg
) {
    val utbetalinger = utbetalinger.toMutableList()

    fun beholdAktivOgAnnullert(annulleringer: AnnulleringerAkkumulator): Boolean {
        val annulleringerForVedtaksperioden = utbetalinger
            .groupBy { it.second.korrelasjonsId }
            .mapNotNull { (_, utbetalinger) ->
                // mapper annullering til å peke på samme vilkårsgrunnlag som siste utbetaling
                val sisteVilkårsgrunnlagId = utbetalinger.last().first
                annulleringer.finnAnnullering(utbetalinger.first().second)?.let {
                    sisteVilkårsgrunnlagId to it
                }
            }
        this.utbetalinger.addAll(annulleringerForVedtaksperioden)

        return !forkastet || annulleringerForVedtaksperioden.isNotEmpty()
    }

    fun tilGodkjenning() = tilstand in listOf(AvventerGodkjenning, AvventerGodkjenningRevurdering)

    internal companion object {
        fun List<IVedtaksperiode>.tilGodkjenning(utbetaling: IUtbetaling) =
            any { periode -> periode.utbetalinger.any { it.second.erSammeSom(utbetaling) } && periode.tilGodkjenning() }
    }
}

internal class IUtbetaling(
    val id: UUID,
    val korrelasjonsId: UUID,
    val beregningId: BeregningId,
    val opprettet: LocalDateTime,
    val utbetalingstidslinje: List<Utbetalingstidslinjedag>,
    val maksdato: LocalDate,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    type: String,
    tilstand: String,
    private val arbeidsgiverNettoBeløp: Int,
    private val personNettoBeløp: Int,
    private val arbeidsgiverFagsystemId: String,
    private val personFagsystemId: String,
    private val vurdering: Utbetaling.Vurdering?,
    private val oppdrag: Map<String, SpeilOppdrag>
) {
    private var erTilGodkjenning = false
    private val type: Utbetalingtype = utledType(type)
    private val status: Utbetalingstatus = utledStatus(this.type, tilstand)

    fun erSammeSom(other: IUtbetaling) = id == other.id
    fun hørerSammen(other: IUtbetaling) = korrelasjonsId == other.korrelasjonsId

    fun settTilGodkjenning(vedtaksperioder: List<IVedtaksperiode>) {
        erTilGodkjenning = vedtaksperioder.tilGodkjenning(this)
    }

    fun toDTO(): Utbetaling {
        return Utbetaling(
            type = type,
            status = status,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
            vurdering = vurdering,
            id = id,
            oppdrag = oppdrag,
            tilGodkjenning = erTilGodkjenning,
            korrelasjonsId = korrelasjonsId
        )
    }

    internal companion object {
        internal fun Map<UUID, IUtbetaling>.leggTil(utbetaling: IUtbetaling): Map<UUID, IUtbetaling> {
            return this.toMutableMap().also {
                it.putIfAbsent(utbetaling.korrelasjonsId, utbetaling)
            }
        }

        private fun utledType(type: String) = when (type) {
            "UTBETALING" -> Utbetalingtype.UTBETALING
            "ETTERUTBETALING" -> Utbetalingtype.ETTERUTBETALING
            "ANNULLERING" -> Utbetalingtype.ANNULLERING
            "REVURDERING" -> Utbetalingtype.REVURDERING
            else -> error("har ingen mappingregel for $type")
        }

        private fun utledStatus(type: Utbetalingtype, tilstand: String): Utbetalingstatus {
            return when (tilstand) {
                "Annullert" -> Utbetalingstatus.Annullert
                "Godkjent" -> Utbetalingstatus.Godkjent
                "GodkjentUtenUtbetaling" -> Utbetalingstatus.GodkjentUtenUtbetaling
                "IkkeGodkjent" -> when (type) {
                    Utbetalingtype.REVURDERING -> Utbetalingstatus.IkkeGodkjent
                    else -> error("forsøker å mappe en IKKE_GODKJENT-utbetaling til Speil, som ikke er revurdering")
                }
                "Overført" -> Utbetalingstatus.Overført
                "Ubetalt" -> Utbetalingstatus.Ubetalt
                "Utbetalt" -> Utbetalingstatus.Utbetalt
                else -> error("har ingen mappingregel for $tilstand")
            }
        }
    }
}
