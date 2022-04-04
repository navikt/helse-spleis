package no.nav.helse.serde.api.v2

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.AvsluttetUtenUtbetaling
import no.nav.helse.serde.api.v2.Behandlingstype.UBEREGNET
import no.nav.helse.serde.api.v2.Behandlingstype.VENTER
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.fjernErstattede
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.sammenstillMedNeste
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.sorterGenerasjoner
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.toDTO
import no.nav.helse.serde.api.v2.HendelseDTO.Companion.finn
import no.nav.helse.serde.api.v2.Tidslinjeberegninger.ITidslinjeberegning
import no.nav.helse.serde.api.v2.buildere.BeregningId
import no.nav.helse.serde.api.v2.buildere.IVilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.v2.buildere.InntektsmeldingId
import no.nav.helse.serde.api.v2.buildere.PeriodeVarslerBuilder

internal class Generasjoner(perioder: Tidslinjeperioder) {
    private val generasjoner: List<Generasjon> = perioder.toGenerasjoner()

    internal fun build(): List<no.nav.helse.serde.api.v2.Generasjon> {
        return generasjoner
            .sammenstillMedNeste()
            .fjernErstattede()
            .sorterGenerasjoner()
            .toDTO()
            .reversed()
    }

    internal class Generasjon(perioder: List<Tidslinjeperiode>) {
        private val perioder = perioder.toMutableList()
        private var erstattet: Boolean = false

        private val tidslinjeperioder = perioder.filterIsInstance<BeregnetPeriode>()

        private fun finnPeriode(periode: Tidslinjeperiode) = perioder.find { it.erSammeVedtaksperiode(periode) }
        private fun inneholderPeriode(periode: Tidslinjeperiode) = finnPeriode(periode) != null
        private fun erFagsystemIdAnnullert(periode: BeregnetPeriode): Boolean =
            tidslinjeperioder.any { it.harSammeFagsystemId(periode) && it.erAnnullering() }

        private fun harMinstÉnRevurdertPeriodeTidligereEnn(tidslinjeperiode: Tidslinjeperiode): Boolean =
            tidslinjeperioder.any { it.erRevurdering() && it.fom < tidslinjeperiode.fom }

        private fun finnKandidaterForSammenstilling(nesteGenerasjon: Generasjon): Pair<List<Tidslinjeperiode>, List<Tidslinjeperiode>> {
            return perioder.partition {
                if (it is UberegnetPeriode) return@partition SKAL_SAMMENSTILLES
                if (it is BeregnetPeriode && !it.erAnnullering() && nesteGenerasjon.erFagsystemIdAnnullert(it)) return@partition SKAL_IKKE_SAMMENSTILLES
                if (nesteGenerasjon.harMinstÉnRevurdertPeriodeTidligereEnn(it)) return@partition SKAL_IKKE_SAMMENSTILLES
                !nesteGenerasjon.inneholderPeriode(it)
            }
        }

        private fun sorterFallende(): Generasjon {
            perioder.sortByDescending { it.fom }
            return this
        }

        private fun fjernPerioderSomVenter(perioder: List<Tidslinjeperiode>) {
            this.perioder.removeAll(perioder.filter { it.venter() })
        }

        private fun utvidMed(perioder: List<Tidslinjeperiode>) {
            this.perioder.addAll(perioder)
        }

        internal companion object {
            private const val SKAL_IKKE_SAMMENSTILLES: Boolean = false
            private const val SKAL_SAMMENSTILLES: Boolean = true

            internal fun List<Generasjon>.sammenstillMedNeste(): List<Generasjon> {
                forEachIndexed { index, generasjon ->
                    if (index == size - 1) return@forEachIndexed // siste generasjon skal ikke gjøres noe med
                    val nesteGenerasjon = this[index + 1]
                    val (perioderSomSkalSammenstilles, ikkeSammenstiltePerioder) = generasjon.finnKandidaterForSammenstilling(
                        nesteGenerasjon
                    )
                    nesteGenerasjon.utvidMed(perioderSomSkalSammenstilles)
                    generasjon.fjernPerioderSomVenter(perioderSomSkalSammenstilles)
                    generasjon.erstattet = ikkeSammenstiltePerioder.isEmpty()
                }
                return this
            }

            internal fun List<Generasjon>.fjernErstattede() = filterNot { it.erstattet }

            internal fun List<Generasjon>.sorterGenerasjoner() = map { it.sorterFallende() }

            internal fun List<Generasjon>.toDTO(): List<no.nav.helse.serde.api.v2.Generasjon> {
                return map {
                    Generasjon(UUID.randomUUID(), it.perioder)
                }
            }
        }
    }
}

internal class Tidslinjeperioder(
    private val fødselsnummer: Fødselsnummer,
    private val forkastetVedtaksperiodeIder: List<UUID>,
    private val vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk,
    private val refusjoner: Map<InntektsmeldingId, Refusjon>,
    vedtaksperioder: List<IVedtaksperiode>,
    tidslinjeberegninger: Tidslinjeberegninger
) {
    private var perioder: List<Tidslinjeperiode>

    private fun erForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId in forkastetVedtaksperiodeIder

    init {
        perioder = vedtaksperioder.toMutableList().flatMap { periode ->
            when {
                periode.utbetalinger.isEmpty() -> listOf(
                    uberegnetPeriode(
                        periode,
                        erForkastet(periode.vedtaksperiodeId)
                    )
                )
                else -> periode.utbetalinger.map { utbetaling ->
                    val tidslinjeberegning = tidslinjeberegninger.finn(utbetaling.beregningId)
                    val refusjon = refusjoner[periode.inntektsmeldingId()]
                    beregnetPeriode(
                        periode = periode,
                        utbetaling = utbetaling,
                        tidslinjeberegning = tidslinjeberegning,
                        erForkastet = erForkastet(periode.vedtaksperiodeId),
                        refusjon = refusjon
                    )
                }
            }
        }.sortedBy { it.opprettet }
    }

    internal fun toGenerasjoner() = perioder.map {
        Generasjoner.Generasjon(listOf(it))
    }

    private fun uberegnetPeriode(periode: IVedtaksperiode, erForkastet: Boolean): UberegnetPeriode {
        return UberegnetPeriode(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            fom = periode.fom,
            tom = periode.tom,
            sammenslåttTidslinje = periode.sykdomstidslinje.merge(emptyList()),
            behandlingstype = if (periode.tilstand == AvsluttetUtenUtbetaling) UBEREGNET else VENTER,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = erForkastet,
            opprettet = periode.oppdatert
        )
    }

    private fun beregnetPeriode(
        periode: IVedtaksperiode,
        utbetaling: IUtbetaling,
        tidslinjeberegning: ITidslinjeberegning,
        erForkastet: Boolean,
        refusjon: Refusjon?
    ): BeregnetPeriode {
        val sammenslåttTidslinje =
            tidslinjeberegning.sammenslåttTidslinje(utbetaling.utbetalingstidslinje, periode.fom, periode.tom)
        val varsler = PeriodeVarslerBuilder(
            periode.aktivitetsloggForPeriode
        ).build()
        val utbetalingDTO = utbetaling.toDTO()
        return BeregnetPeriode(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            beregningId = utbetaling.beregningId,
            fom = periode.fom,
            tom = periode.tom,
            erForkastet = erForkastet,
            behandlingstype = periode.behandlingstype,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            skjæringstidspunkt = periode.skjæringstidspunkt,
            hendelser = periode.hendelser,
            maksdato = utbetaling.maksdato,
            opprettet = utbetaling.opprettet,
            periodevilkår = periodevilkår(periode, utbetaling, sammenslåttTidslinje, periode.hendelser),
            sammenslåttTidslinje = sammenslåttTidslinje,
            gjenståendeSykedager = utbetaling.gjenståendeSykedager,
            forbrukteSykedager = utbetaling.forbrukteSykedager,
            utbetaling = utbetalingDTO,
            vilkårsgrunnlagshistorikkId = tidslinjeberegning.vilkårsgrunnlagshistorikkId,
            aktivitetslogg = varsler,
            refusjon = refusjon,
            tilstand = periode.tilstand.type.tilPeriodetilstand(utbetalingDTO, sammenslåttTidslinje)
        )
    }

    private fun List<SammenslåttDag>.sisteNavDag() =
        lastOrNull { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.NavDag }

    private fun periodevilkår(
        periode: IVedtaksperiode,
        utbetaling: IUtbetaling,
        sammenslåttTidslinje: List<SammenslåttDag>,
        hendelser: List<HendelseDTO>
    ): BeregnetPeriode.Vilkår {
        val sisteSykepengedag = sammenslåttTidslinje.sisteNavDag()?.dagen ?: periode.tom
        val sykepengedager = BeregnetPeriode.Sykepengedager(
            periode.skjæringstidspunkt,
            utbetaling.maksdato,
            utbetaling.forbrukteSykedager,
            utbetaling.gjenståendeSykedager,
            utbetaling.maksdato > sisteSykepengedag
        )
        val alder = fødselsnummer.alder().let {
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

        return BeregnetPeriode.Vilkår(sykepengedager, alder, søknadsfrist)
    }
}

internal class IVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val behandlingstype: Behandlingstype,
    val inntektskilde: Inntektskilde,
    val hendelser: List<HendelseDTO>,
    utbetalinger: List<IUtbetaling>,
    val periodetype: Periodetype,
    val sykdomstidslinje: List<Sykdomstidslinjedag>,
    val oppdatert: LocalDateTime,
    val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
    val skjæringstidspunkt: LocalDate,
    val aktivitetsloggForPeriode: Aktivitetslogg
) {
    val utbetalinger = utbetalinger.toMutableList()

    fun håndterAnnullering(annulleringer: AnnulleringerAkkumulator) {
        fagsystemId()?.let {
            annulleringer.finnAnnullering(it)?.let { annullering ->
                utbetalinger.add(annullering)
            }
        }
    }

    fun inntektsmeldingId(): InntektsmeldingId? =
        hendelser.find { it.type == "INNTEKTSMELDING" }?.let { UUID.fromString(it.id) }

    private fun fagsystemId() = utbetalinger.firstOrNull()?.fagsystemId()
}

internal class IUtbetaling(
    val id: UUID,
    val beregningId: BeregningId,
    val opprettet: LocalDateTime,
    val utbetalingstidslinje: List<Utbetalingstidslinjedag>,
    val maksdato: LocalDate,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    private val type: String,
    private val tilstand: String,
    private val arbeidsgiverNettoBeløp: Int,
    private val personNettoBeløp: Int,
    private val arbeidsgiverFagsystemId: String,
    private val personFagsystemId: String,
    private val vurdering: Utbetaling.Vurdering?,
    private val oppdrag: Map<String, SpeilOppdrag>
) {
    fun fagsystemId() = arbeidsgiverFagsystemId
    fun toDTO(): Utbetaling {
        return Utbetaling(
            type = Utbetalingtype.valueOf(type),
            status = Utbetalingstatus.valueOf(tilstand),
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
            vurdering = vurdering,
            id = id,
            oppdrag = oppdrag
        )
    }
}

private fun TilstandType.tilPeriodetilstand(utbetaling: Utbetaling, tidslinje: List<SammenslåttDag>): Periodetilstand =
    when (this) {
        TilstandType.START,
        TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
        TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
        TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
        TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
        TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP,
        TilstandType.AVVENTER_VILKÅRSPRØVING,
        TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
        TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
        TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
        TilstandType.AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
        TilstandType.AVVENTER_SIMULERING,
        TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
        TilstandType.AVVENTER_SIMULERING_REVURDERING,
        TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
        TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
        TilstandType.AVVENTER_HISTORIKK_REVURDERING,
        TilstandType.AVVENTER_REVURDERING,
        TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
        TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
        TilstandType.AVVENTER_HISTORIKK -> Periodetilstand.Venter
        TilstandType.AVVENTER_UFERDIG,
        TilstandType.AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER,
        TilstandType.AVVENTER_ARBEIDSGIVERE -> Periodetilstand.VenterPåKiling
        TilstandType.TIL_INFOTRYGD -> Periodetilstand.TilInfotrygd
        TilstandType.UTBETALING_FEILET -> Periodetilstand.Feilet
        TilstandType.REVURDERING_FEILET -> Periodetilstand.RevurderingFeilet
        TilstandType.TIL_UTBETALING -> Periodetilstand.TilUtbetaling
        TilstandType.AVVENTER_GODKJENNING_REVURDERING,
        TilstandType.AVVENTER_GODKJENNING -> Periodetilstand.Oppgaver
        TilstandType.AVSLUTTET,
        TilstandType.AVSLUTTET_UTEN_UTBETALING -> when (utbetaling.type) {
            Utbetalingtype.ANNULLERING -> when (utbetaling.status) {
                Utbetalingstatus.Annullert -> Periodetilstand.Annullert
                Utbetalingstatus.UtbetalingFeilet -> Periodetilstand.AnnulleringFeilet
                else -> Periodetilstand.TilAnnullering
            }
            Utbetalingtype.REVURDERING -> Periodetilstand.Utbetalt
            else -> when (utbetaling.status) {
                Utbetalingstatus.Utbetalt,
                Utbetalingstatus.GodkjentUtenUtbetaling -> {
                    when {
                        tidslinje.inneholderKunFeriedager() -> Periodetilstand.KunFerie
                        tidslinje.inneholderSykepengedager() -> Periodetilstand.Utbetalt
                        else -> Periodetilstand.IngenUtbetaling
                    }
                }
                Utbetalingstatus.UtbetalingFeilet -> Periodetilstand.Feilet
                else -> Periodetilstand.TilUtbetaling
            }
        }
    }

private fun List<SammenslåttDag>.inneholderKunFeriedager(): Boolean =
    all {
        it.utbetalingstidslinjedagtype in setOf(
            UtbetalingstidslinjedagType.NavHelgDag,
            UtbetalingstidslinjedagType.Helgedag,
            UtbetalingstidslinjedagType.Feriedag
        )
    }

private fun List<SammenslåttDag>.inneholderSykepengedager(): Boolean =
    any { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.NavDag }