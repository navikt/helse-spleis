package no.nav.helse.serde.api.v2

import no.nav.helse.serde.api.v2.buildere.BeregningId
import no.nav.helse.serde.api.v2.buildere.GenerasjonIder
import no.nav.helse.serde.api.v2.buildere.VilkårsgrunnlagshistorikkId
import java.time.LocalDate

internal class Tidslinjebereginger(generasjonIder: List<GenerasjonIder>, sykdomshistorikkAkkumulator: SykdomshistorikkAkkumulator) {
    private val beregninger: List<ITidslinjeberegning> = lagTidslinjeberegninger(generasjonIder, sykdomshistorikkAkkumulator)

    private fun lagTidslinjeberegninger(
        generasjonIder: List<GenerasjonIder>,
        sykdomshistorikkAkkumulator: SykdomshistorikkAkkumulator
    ): List<ITidslinjeberegning> {
        return generasjonIder.map {
            val sykdomstidslinje = sykdomshistorikkAkkumulator.finnTidslinje(it.sykdomshistorikkId) ?: throw IllegalStateException("Finner ikke sykdomshistorikk for historikkId'en! Hvordan kan det skje?")
            ITidslinjeberegning(
                it.beregningId,
                sykdomstidslinje,
                it.vilkårsgrunnlagshistorikkId
            )
        }
    }

    internal fun finn(beregningId: BeregningId): ITidslinjeberegning {
        return beregninger.find { it.beregningId == beregningId } ?: throw IllegalStateException("Finner ikke tidslinjeberegning for beregningId'en! Hvordan kan det skje?")
    }

    internal class ITidslinjeberegning(
        internal val beregningId: BeregningId,
        private val sykdomstidslinje: List<Sykdomstidslinjedag>,
        internal val vilkårsgrunnlagshistorikkId: VilkårsgrunnlagshistorikkId
    ) {
        fun sammenslåttTidslinje(utbetalingstidslinje: List<Utbetalingstidslinjedag>, fom: LocalDate, tom: LocalDate): List<SammenslåttDag> {
            return sykdomstidslinje
                .subset(fom, tom)
                .merge(utbetalingstidslinje)
        }

        private fun List<Sykdomstidslinjedag>.subset(fom: LocalDate, tom: LocalDate) = this.filter { it.dagen in fom..tom }
    }
}

internal fun List<Sykdomstidslinjedag>.merge(utbetalingstidslinje: List<Utbetalingstidslinjedag>): List<SammenslåttDag> {

    fun utbetalingsinfo(utbetalingsdag: Utbetalingstidslinjedag) = if (utbetalingsdag is NavDag)
        Utbetalingsinfo(utbetalingsdag.inntekt, utbetalingsdag.utbetaling, utbetalingsdag.totalGrad)
    else null

    fun begrunnelser(utbetalingsdag: Utbetalingstidslinjedag) =
        if (utbetalingsdag is AvvistDag) utbetalingsdag.begrunnelser else null

    return map { sykdomsdag ->
        val utbetalingsdag = utbetalingstidslinje.find { it.dato.isEqual(sykdomsdag.dagen) }
        SammenslåttDag(
            sykdomsdag.dagen,
            sykdomsdag.type,
            utbetalingsdag?.type ?: UtbetalingstidslinjedagType.UkjentDag,
            kilde = sykdomsdag.kilde,
            grad = sykdomsdag.grad,
            utbetalingsinfo = utbetalingsdag?.utbetalingsinfo(),
            begrunnelser = utbetalingsdag?.let { begrunnelser(it) }
        )
    }
}
