package no.nav.helse.serde.api.v2

import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.HendelseDTO
import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.fjernErstattede
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.sammenstillMedNeste
import no.nav.helse.serde.api.v2.Generasjoner.Generasjon.Companion.toDTO
import no.nav.helse.serde.api.v2.Tidslinjebereginger.ITidslinjeberegning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Generasjoner(perioder: Perioder) {
    private val generasjoner: List<Generasjon> = perioder.toGenerasjoner()

    internal fun build(): List<no.nav.helse.serde.api.v2.Generasjon> {
        return generasjoner
            .sammenstillMedNeste()
            .fjernErstattede()
            .toDTO()
            .reversed()
    }

    internal class Generasjon(perioder: List<Periode>) {
        private val perioder = perioder.toMutableList()
        private var erstattet: Boolean = false

        private val tidslinjeperioder = perioder.filterIsInstance<Tidslinjeperiode>()

        private fun finnPeriode(periode: Periode) = perioder.find { it.erSammeVedtaksperiode(periode) }
        private fun inneholderPeriode(periode: Periode) = finnPeriode(periode) != null
        private fun erFagsystemIdAnnullert(periode: Tidslinjeperiode): Boolean = tidslinjeperioder.any { it.harSammeFagsystemId(periode) && it.erAnnullering() }
        private fun harMinstÉnRevurdertPeriodeTidligereEnn(periode: Periode): Boolean = tidslinjeperioder.any { it.erRevurdering() && it.fom < periode.fom }

        private fun finnKandidaterForSammenstilling(nesteGenerasjon: Generasjon): Pair<List<Periode>, List<Periode>> {
            return perioder.partition {
//                if (it is KortPeriode) return@partition SKAL_SAMMENSTILLES
                if (it is Tidslinjeperiode && !it.erAnnullering() && nesteGenerasjon.erFagsystemIdAnnullert(it)) return@partition SKAL_IKKE_SAMMENSTILLES
                if (nesteGenerasjon.harMinstÉnRevurdertPeriodeTidligereEnn(it)) return@partition SKAL_IKKE_SAMMENSTILLES
                !nesteGenerasjon.inneholderPeriode(it)
            }
        }

        private fun utvidMed(perioder: List<Periode>) {
            this.perioder.addAll(perioder)
        }

        companion object {
            private const val SKAL_IKKE_SAMMENSTILLES: Boolean = false
            private const val SKAL_SAMMENSTILLES: Boolean = true

            fun List<Generasjon>.sammenstillMedNeste(): List<Generasjon> {
                forEachIndexed { index, generasjon ->
                    if (index == size - 1) return@forEachIndexed
                    val nesteGenerasjon = this[index + 1]
                    val (perioderSomSkalSammenstilles, ikkeSammenstiltePerioder) = generasjon.finnKandidaterForSammenstilling(nesteGenerasjon)
                    nesteGenerasjon.utvidMed(perioderSomSkalSammenstilles)
                    generasjon.erstattet = ikkeSammenstiltePerioder.isEmpty()
                }
                return this
            }

            fun List<Generasjon>.fjernErstattede() = filterNot { it.erstattet }

            fun List<Generasjon>.toDTO(): List<no.nav.helse.serde.api.v2.Generasjon> {
                return map {
                    Generasjon(UUID.randomUUID(), it.perioder)
                }
            }
        }
    }
}

internal class Perioder(
    private val forkastetVedtaksperiodeIder: List<UUID>,
    vedtaksperioder: List<IVedtaksperiode>,
    tidslinjeberegninger: Tidslinjebereginger
) {
    private var perioder: List<Periode>

    private fun erForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId in forkastetVedtaksperiodeIder

    init {
        perioder = vedtaksperioder.toMutableList().flatMap { periode ->
            when {
                periode.utbetalinger.isEmpty() -> listOf(nyKortPeriode(periode, erForkastet(periode.vedtaksperiodeId)))
                else -> periode.utbetalinger.map { utbetaling ->
                    val tidslinjeberegning = tidslinjeberegninger.finn(utbetaling.beregningId)
                    nyPeriode(periode, utbetaling, tidslinjeberegning, erForkastet(periode.vedtaksperiodeId))
                }
            }
        }.sortedBy { it.opprettet }
    }

    internal fun toGenerasjoner() = perioder.map {
        Generasjoner.Generasjon(listOf(it))
    }

    private fun nyKortPeriode(periode: IVedtaksperiode, erForkastet: Boolean): KortPeriode {
        return KortPeriode(
            vedtaksperiodeId = periode.vedtaksperiodeId,
            fom = periode.fom,
            tom = periode.tom,
            sammenslåttTidslinje = periode.sykdomstidslinje.merge(emptyList()),
            behandlingstype = Behandlingstype.KORT_PERIODE,
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = erForkastet,
            opprettet = periode.opprettet
        )
    }

    private fun nyPeriode(
        periode: IVedtaksperiode,
        utbetaling: IUtbetaling,
        tidslinjeberegning: ITidslinjeberegning,
        erForkastet: Boolean
    ): Tidslinjeperiode {
        return Tidslinjeperiode(
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
            simulering = periode.simuleringsdataDTO,
            maksdato = utbetaling.maksdato,
            opprettet = utbetaling.opprettet,
            sammenslåttTidslinje = tidslinjeberegning.sykdomstidslinje(utbetaling.utbetalingstidslinje, periode.fom, periode.tom),
            gjenståendeSykedager = utbetaling.gjenståendeSykedager,
            forbrukteSykedager = utbetaling.forbrukteSykedager,
            utbetalingDTO = utbetaling.toDTO(),
            inntektshistorikkId = tidslinjeberegning.inntektshistorikkId,
            vilkårsgrunnlagshistorikkId = tidslinjeberegning.vilkårsgrunnlagshistorikkId
        )
    }
}

internal class IVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val behandlingstype: Behandlingstype,
    val inntektskilde: Inntektskilde,
    val hendelser: List<HendelseDTO>,
    val simuleringsdataDTO: SimuleringsdataDTO?,
    utbetalinger: List<IUtbetaling>,
    val periodetype: Periodetype,
    val sykdomstidslinje: List<SykdomstidslinjedagDTO>,
    val opprettet: LocalDateTime,
    val skjæringstidspunkt: LocalDate
) {
    val utbetalinger = utbetalinger.toMutableList()

    fun håndterAnnullering(annulleringer: AnnulleringerAkkumulator) {
        fagsystemId()?.let {
            annulleringer.finnAnnullering(it)?.let { annullering ->
                utbetalinger.add(annullering)
            }
        }
    }

    private fun fagsystemId() = utbetalinger.firstOrNull()?.fagsystemId()
}

internal class IUtbetaling(
    val beregningId: BeregningId,
    val opprettet: LocalDateTime,
    val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
    val maksdato: LocalDate,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    private val type: String,
    private val tilstand: String,
    private val arbeidsgiverNettoBeløp: Int,
    private val personNettoBeløp: Int,
    private val arbeidsgiverFagsystemId: String,
    private val personFagsystemId: String,
    private val vurderingDTO: UtbetalingDTO.VurderingDTO?
) {
    fun fagsystemId() = arbeidsgiverFagsystemId
    fun toDTO(): UtbetalingDTO {
        return UtbetalingDTO(
            type,
            tilstand,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            arbeidsgiverFagsystemId,
            personFagsystemId,
            vurderingDTO
        )
    }
}
