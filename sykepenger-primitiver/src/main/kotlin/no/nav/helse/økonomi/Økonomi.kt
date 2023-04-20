package no.nav.helse.økonomi

import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.fraRatio
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class Økonomi private constructor(
    private val grad: Prosentdel,
    private val totalGrad: Prosentdel = grad,
    private val arbeidsgiverRefusjonsbeløp: Inntekt = INGEN,
    private val aktuellDagsinntekt: Inntekt = INGEN,
    private val dekningsgrunnlag: Inntekt = INGEN,
    private val grunnbeløpgrense: Inntekt? = null,
    private var arbeidsgiverbeløp: Inntekt? = null,
    private var personbeløp: Inntekt? = null,
    private var er6GBegrenset: Boolean? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad,
) {
    companion object {
        private val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        private val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }

        fun sykdomsgrad(grad: Prosentdel) =
            Økonomi(grad)

        fun ikkeBetalt() = Økonomi(
            grad = 0.prosent,
            tilstand = Tilstand.IkkeBetalt
        )

        fun List<Økonomi>.totalSykdomsgrad() = totalSykdomsgrad(this).first().totalGrad
        fun totalSykdomsgrad(økonomiList: List<Økonomi>): List<Økonomi> {
            val totalgrad = Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.sykdomsgrad() to it.aktuellDagsinntekt })
            return økonomiList.map { økonomi: Økonomi ->
                økonomi.kopierMed(totalgrad = totalgrad)
            }
        }

        fun List<Økonomi>.erUnderGrensen() = any { it.totalGrad.erUnderGrensen() }

        private fun totalUtbetalingsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.utbetalingsgrad() to it.aktuellDagsinntekt })

        fun betal(økonomiList: List<Økonomi>): List<Økonomi> = økonomiList.also {
            val utbetalingsgrad = totalUtbetalingsgrad(økonomiList)
            delteUtbetalinger(it)
            fordelBeløp(it, utbetalingsgrad)
        }

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, utbetalingsgrad: Prosentdel) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = false }

            // todo: trenger ikke vite 6G hvis inntektene er redusert ihht. 6G når de settes på Økonomi
            val grunnbeløp = økonomiList.firstNotNullOf { it.grunnbeløpgrense }
            val grunnlagForSykepengegrunnlag = økonomiList.map { it.aktuellDagsinntekt }.summer()
            val sykepengegrunnlagBegrenset6G = minOf(grunnlagForSykepengegrunnlag, grunnbeløp)
            val er6GBegrenset = grunnlagForSykepengegrunnlag > grunnbeløp

            val sykepengegrunnlag = (sykepengegrunnlagBegrenset6G * utbetalingsgrad).rundTilDaglig()
            fordel(økonomiList, totalArbeidsgiver, sykepengegrunnlag, { økonomi, inntekt -> økonomi.arbeidsgiverbeløp = inntekt }, arbeidsgiverBeløp)
            val totalArbeidsgiverrefusjon = totalArbeidsgiver(økonomiList)
            fordel(økonomiList, total - totalArbeidsgiverrefusjon, sykepengegrunnlag - totalArbeidsgiverrefusjon, { økonomi, inntekt -> økonomi.personbeløp = inntekt }, personBeløp)
            økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = er6GBegrenset }
        }

        private fun fordel(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt, setter: (Økonomi, Inntekt?) -> Unit, getter: (Økonomi) -> Inntekt?) {
            val ratio = reduksjon(grense, total)
            val beregningsresultat = økonomiList.map { Beregningsresultat(it, getter(it)?.times(ratio)) }

            beregningsresultat.forEach { it.oppdater(setter) }

            val totaltRestbeløp = (total.coerceAtMost(grense) - total(økonomiList, getter))
                .reflection { _, _, _, dagligInt -> dagligInt }

            Beregningsresultat.fordel(beregningsresultat, totaltRestbeløp, setter, getter)
        }

        private fun reduksjon(grense: Inntekt, total: Inntekt): Prosentdel {
            if (total == INGEN) return 0.prosent
            return fraRatio((grense ratio total).coerceAtMost(1.0))
        }

        private class Beregningsresultat(private val økonomi: Økonomi, beløp: Inntekt?) {
            private val utbetalingFørAvrunding = beløp ?: INGEN
            private val utbetalingEtterAvrunding = utbetalingFørAvrunding.rundNedTilDaglig()
            private val differanse = (utbetalingFørAvrunding - utbetalingEtterAvrunding).reflection { _, _, daglig, _ -> daglig }

            fun oppdater(setter: (Økonomi, Inntekt?) -> Unit) {
                setter(this.økonomi, this.utbetalingEtterAvrunding)
            }

            companion object {
                fun fordel(liste: List<Beregningsresultat>, rest: Int, setter: (Økonomi, Inntekt?) -> Unit, getter: (Økonomi) -> Inntekt?) {
                    liste
                        .sortedByDescending { it.differanse }
                        .take(rest)
                        .forEach { setter(it.økonomi, getter(it.økonomi)?.plus(1.daglig)) }
                }
            }
        }

        private fun total(økonomiList: List<Økonomi>, strategi: (Økonomi) -> Inntekt?): Inntekt =
            økonomiList.mapNotNull { strategi(it) }.summer()

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>) = total(økonomiList, arbeidsgiverBeløp)

        private fun totalPerson(økonomiList: List<Økonomi>) = total(økonomiList, personBeløp)

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.any { it.er6GBegrenset() }
    }

    init {
        require(dekningsgrunnlag >= INGEN) { "dekningsgrunnlag kan ikke være negativ." }
    }

    fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, `6G`: Inntekt, refusjonsbeløp: Inntekt): Økonomi =
        tilstand.inntekt(this, aktuellDagsinntekt, refusjonsbeløp, dekningsgrunnlag, `6G`)

    fun lås() = tilstand.lås(this)

    fun builder(builder: ØkonomiBuilder) {
        tilstand.builder(this, builder)
    }

    private fun _buildKunGrad(builder: ØkonomiBuilder) {
        /* ikke legg på flere felter - alle er enten null eller har defaultverdi */
        builder.grad(grad.toDouble())
    }

    private fun _build(builder: ØkonomiBuilder) {
        builder.grad(grad.toDouble())
            .arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig })
            .dekningsgrunnlag(dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig })
            .totalGrad(totalGrad.toDouble())
            .aktuellDagsinntekt(aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig })
            .arbeidsgiverbeløp(arbeidsgiverbeløp?.reflection { _, _, daglig, _ -> daglig })
            .personbeløp(personbeløp?.reflection { _, _, daglig, _ -> daglig })
            .er6GBegrenset(er6GBegrenset)
            .grunnbeløpsgrense(grunnbeløpgrense?.reflection { årlig, _, _, _ -> årlig })
            .tilstand(tilstand)
    }

    fun <R> brukGrad(block: (grad: Double) -> R) = block(grad.toDouble())
    fun <R> brukAvrundetGrad(block: (grad: Int) -> R) = block(grad.roundToInt())
    fun <R> brukTotalGrad(block: (totalGrad: Double) -> R) = block(totalGrad.toDouble())
    fun brukAvrundetDagsinntekt(block: (aktuellDagsinntekt: Int) -> Unit) = block(aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig }.roundToInt())
    fun medAvrundetData(block: (grad: Int, aktuellDagsinntekt: Int) -> Unit) =
        block(grad.roundToInt(), aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig }.roundToInt())

    fun accept(visitor: ØkonomiVisitor) {
        visitor.visitØkonomi(
            grad.toDouble(),
            arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
            dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
            totalGrad.toDouble(),
            aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
            arbeidsgiverbeløp?.reflection { _, _, daglig, _ -> daglig },
            personbeløp?.reflection { _, _, daglig, _ -> daglig },
            er6GBegrenset
        )
        visitor.visitAvrundetØkonomi(
            grad.roundToInt(),
            arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig.roundToInt() },
            dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig.roundToInt() },
            totalGrad.roundToInt(),
            aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig.roundToInt() },
            arbeidsgiverbeløp?.reflection { _, _, daglig, _ -> daglig.roundToInt() },
            personbeløp?.reflection { _, _, daglig, _ -> daglig.roundToInt() },
            er6GBegrenset
        )
    }

    private fun utbetalingsgrad() = tilstand.utbetalingsgrad(this)
    private fun sykdomsgrad() = tilstand.sykdomsgrad(this)

    private fun betal() = this.also { tilstand.betal(this) }

    fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    private fun _betal() {
        val total = (dekningsgrunnlag * utbetalingsgrad()).rundTilDaglig()
        val gradertArbeidsgiverRefusjonsbeløp = (arbeidsgiverRefusjonsbeløp * utbetalingsgrad()).rundTilDaglig()
        arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        personbeløp = (total - arbeidsgiverbeløp!!).coerceAtLeast(INGEN)
    }

    fun ikkeBetalt() = kopierMed(tilstand = Tilstand.IkkeBetalt)

    private fun kopierMed(
        grad: Prosentdel = this.grad,
        totalgrad: Prosentdel = this.totalGrad,
        arbeidsgiverRefusjonsbeløp: Inntekt = this.arbeidsgiverRefusjonsbeløp,
        aktuellDagsinntekt: Inntekt = this.aktuellDagsinntekt,
        dekningsgrunnlag: Inntekt = this.dekningsgrunnlag,
        grunnbeløpgrense: Inntekt? = this.grunnbeløpgrense,
        arbeidsgiverbeløp: Inntekt? = this.arbeidsgiverbeløp,
        personbeløp: Inntekt? = this.personbeløp,
        er6GBegrenset: Boolean? = this.er6GBegrenset,
        tilstand: Tilstand = this.tilstand,
    ) = Økonomi(
        grad = grad,
        totalGrad = totalgrad,
        arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp,
        aktuellDagsinntekt = aktuellDagsinntekt,
        dekningsgrunnlag = dekningsgrunnlag,
        grunnbeløpgrense = grunnbeløpgrense,
        arbeidsgiverbeløp = arbeidsgiverbeløp,
        personbeløp = personbeløp,
        er6GBegrenset = er6GBegrenset,
        tilstand = tilstand
    )

    sealed class Tilstand {

        internal open fun utbetalingsgrad(økonomi: Økonomi) = sykdomsgrad(økonomi)
        internal open fun sykdomsgrad(økonomi: Økonomi) = økonomi.grad

        internal open fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
            økonomi._build(builder)
        }

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            refusjonsbeløp: Inntekt,
            dekningsgrunnlag: Inntekt,
            `6G`: Inntekt
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt i tilstand ${this::class.simpleName}")
        }

        internal open fun betal(økonomi: Økonomi) {
            throw IllegalStateException("Kan ikke beregne utbetaling i tilstand ${this::class.simpleName}")
        }

        internal open fun er6GBegrenset(økonomi: Økonomi): Boolean {
            throw IllegalStateException("Beløp er ikke beregnet ennå")
        }

        internal open fun lås(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse Økonomi i tilstand ${this::class.simpleName}")
        }

        internal object KunGrad : Tilstand() {

            override fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
                økonomi._buildKunGrad(builder)
            }

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                refusjonsbeløp: Inntekt,
                dekningsgrunnlag: Inntekt,
                `6G`: Inntekt
            ) = økonomi.kopierMed(
                grad = økonomi.grad,
                totalgrad = økonomi.totalGrad,
                arbeidsgiverRefusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                grunnbeløpgrense = `6G`,
                tilstand = HarInntekt
            )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = INGEN
                økonomi.personbeløp = INGEN
                økonomi.tilstand = HarBeløp
            }

        }

        internal object IkkeBetalt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                refusjonsbeløp: Inntekt,
                dekningsgrunnlag: Inntekt,
                `6G`: Inntekt
            ) = økonomi.kopierMed(
                grad = økonomi.grad,
                totalgrad = økonomi.totalGrad,
                arbeidsgiverRefusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                grunnbeløpgrense = `6G`,
                tilstand = IkkeBetalt
            )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = INGEN
                økonomi.personbeløp = INGEN
                økonomi.tilstand = HarBeløp
            }

        }

        object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.kopierMed(tilstand = Låst)


            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

        }

        object Låst : Tilstand() {

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent
            override fun sykdomsgrad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi


            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = 0.daglig
                økonomi.personbeløp = 0.daglig
                økonomi.tilstand = LåstMedBeløp
            }
        }

        object LåstMedBeløp : Tilstand() {

            override fun sykdomsgrad(økonomi: Økonomi) = 0.prosent
            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun er6GBegrenset(økonomi: Økonomi) = false

        }
    }

    class Builder : ØkonomiBuilder() {
        fun build() = when (tilstand) {
            is Tilstand.KunGrad -> Økonomi(grad.prosent)
            else -> Økonomi(
                grad.prosent,
                totalGrad?.prosent!!,
                arbeidsgiverRefusjonsbeløp?.daglig!!,
                aktuellDagsinntekt?.daglig!!,
                dekningsgrunnlag?.daglig!!,
                grunnbeløpgrense?.årlig,
                arbeidsgiverbeløp?.daglig,
                personbeløp?.daglig,
                er6GBegrenset,
                tilstand!!
            )
        }
    }
}

abstract class ØkonomiBuilder {
    protected var grad by Delegates.notNull<Double>()
    protected var arbeidsgiverRefusjonsbeløp: Double? = null
    protected var dekningsgrunnlag: Double? = null
    protected var totalGrad: Double? = null
    protected var aktuellDagsinntekt: Double? = null
    protected var arbeidsgiverbeløp: Double? = null
    protected var personbeløp: Double? = null
    protected var er6GBegrenset: Boolean? = null
    protected var grunnbeløpgrense: Double? = null
    protected var tilstand: Økonomi.Tilstand? = null


    fun grad(grad: Double): ØkonomiBuilder = apply {
        this.grad = grad
    }

    fun tilstand(tilstand: Økonomi.Tilstand): ØkonomiBuilder = apply {
        this.tilstand = tilstand
    }

    fun grunnbeløpsgrense(grunnbeløpgrense: Double?) = apply {
        this.grunnbeløpgrense = grunnbeløpgrense
    }

    fun arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp: Double?) = apply {
        this.arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp
    }

    fun dekningsgrunnlag(dekningsgrunnlag: Double?) = apply {
        this.dekningsgrunnlag = dekningsgrunnlag
    }

    fun totalGrad(totalGrad: Double?) = apply {
        this.totalGrad = totalGrad
    }

    fun aktuellDagsinntekt(aktuellDagsinntekt: Double?) = apply {
        this.aktuellDagsinntekt = aktuellDagsinntekt
    }

    fun arbeidsgiverbeløp(arbeidsgiverbeløp: Double?) = apply {
        this.arbeidsgiverbeløp = arbeidsgiverbeløp
    }

    fun personbeløp(personbeløp: Double?) = apply {
        this.personbeløp = personbeløp
    }

    fun er6GBegrenset(er6GBegrenset: Boolean?) = apply {
        this.er6GBegrenset = er6GBegrenset
    }
}

fun List<Økonomi>.betal() = Økonomi.betal(this)

fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)

/*
Fordi vi må vekk fra `medData` og gjøre like ting sånn nogenlunde likt.
 */
interface ØkonomiVisitor {
    fun visitØkonomi(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ) {}
    fun visitAvrundetØkonomi(
        grad: Int,
        arbeidsgiverRefusjonsbeløp: Int,
        dekningsgrunnlag: Int,
        totalGrad: Int,
        aktuellDagsinntekt: Int,
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?,
        er6GBegrenset: Boolean?
    ) {}
}