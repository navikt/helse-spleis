package no.nav.helse.økonomi

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
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
    private var totalGrad: Prosentdel = grad,
    private val arbeidsgiverRefusjonsbeløp: Inntekt = INGEN,
    private var arbeidsgiverperiode: ArbeidsgiverperiodePort? = null,
    private val aktuellDagsinntekt: Inntekt = INGEN,
    private val dekningsgrunnlag: Inntekt = INGEN,
    private val skjæringstidspunkt: LocalDate? = null,
    private var grunnbeløpgrense: Inntekt? = null,
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

        fun ikkeBetalt(grad: Prosentdel = 0.prosent) = sykdomsgrad(grad)

        fun ikkeBetalt(arbeidsgiverperiode: ArbeidsgiverperiodePort?, grad: Prosentdel = 0.prosent) = ikkeBetalt(grad).also {
            it.arbeidsgiverperiode = arbeidsgiverperiode
        }

        fun totalSykdomsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.sykdomsgrad() to it.aktuellDagsinntekt })

        fun totalUtbetalingsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.utbetalingsgrad() to it.aktuellDagsinntekt })

        fun List<Økonomi>.avgrensTilArbeidsgiverperiode(periode: Periode): Periode? {
            return map { it.arbeidsgiverperiode }
                .firstOrNull()?.firstOrNull()?.let { førsteArbeidsgiverperiodedag ->
                Periode(førsteArbeidsgiverperiodedag, periode.endInclusive)
            }?.takeUnless { it == periode }
        }

        fun betal(økonomiList: List<Økonomi>): List<Økonomi> = økonomiList.also {
            val utbetalingsgrad = totalUtbetalingsgrad(økonomiList)
            delteUtbetalinger(it)
            totalSykdomsgrad(økonomiList).also { totalSykdomsgrad ->
                økonomiList.forEach { økonomi -> økonomi.totalGrad = totalSykdomsgrad }
            }
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

    fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, skjæringstidspunkt: LocalDate, `6G`: Inntekt, arbeidsgiverperiode: ArbeidsgiverperiodePort? = null, refusjonsbeløp: Inntekt): Økonomi =
        tilstand.inntekt(this, aktuellDagsinntekt, refusjonsbeløp, dekningsgrunnlag, skjæringstidspunkt, arbeidsgiverperiode, `6G`)

    fun lås() = tilstand.lås(this)

    fun låsOpp() = tilstand.låsOpp(this)

    fun builder(builder: ØkonomiBuilder) {
        tilstand.builder(this, builder)
    }

    private fun _buildKunGrad(builder: ØkonomiBuilder) {
        medData { grad, _, _, _, _, _, _, _, _ ->
            /* ikke legg på flere felter - alle er enten null eller har defaultverdi */
            builder.grad(grad)
        }
    }

    private fun _build(builder: ØkonomiBuilder) {
        medData { grad,
                  arbeidsgiverRefusjonsbeløp,
                  dekningsgrunnlag,
                  skjæringstidspunkt,
                  totalGrad,
                  aktuellDagsinntekt,
                  arbeidsgiverbeløp,
                  personbeløp,
                  er6GBegrenset ->
            builder.grad(grad)
                .arbeidsgiverRefusjonsbeløp(arbeidsgiverRefusjonsbeløp)
                .dekningsgrunnlag(dekningsgrunnlag)
                .skjæringstidspunkt(skjæringstidspunkt)
                .totalGrad(totalGrad)
                .aktuellDagsinntekt(aktuellDagsinntekt)
                .arbeidsgiverbeløp(arbeidsgiverbeløp)
                .personbeløp(personbeløp)
                .er6GBegrenset(er6GBegrenset)
                .arbeidsgiverperiode(arbeidsgiverperiode)
                .grunnbeløpsgrense(grunnbeløpgrense?.reflection { årlig, _, _, _ -> årlig })
                .tilstand(tilstand)
        }
    }

    fun <R> medData(lambda: MedØkonomiData<R>) = tilstand.medData(this, lambda)

    fun <R> medAvrundetData(
        block: (
            grad: Int,
            arbeidsgiverRefusjonsbeløp: Int,
            dekningsgrunnlag: Int,
            skjæringstidspunkt: LocalDate?,
            totalGrad: Int,
            aktuellDagsinntekt: Int,
            arbeidsgiverbeløp: Int?,
            personbeløp: Int?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        medData { grad: Double,
                  arbeidsgiverRefusjonsbeløp: Double,
                  dekningsgrunnlag: Double,
                  skjæringstidspunkt: LocalDate?,
                  totalGrad: Double,
                  aktuellDagsinntekt: Double,
                  arbeidsgiverbeløp: Double?,
                  personbeløp: Double?,
                  er6GBegrenset: Boolean? ->
            block(
                grad.roundToInt(),
                arbeidsgiverRefusjonsbeløp.roundToInt(),
                dekningsgrunnlag.roundToInt(),
                skjæringstidspunkt,
                totalGrad.roundToInt(),
                aktuellDagsinntekt.roundToInt(),
                arbeidsgiverbeløp?.roundToInt(),
                personbeløp?.roundToInt(),
                er6GBegrenset
            )
        }

    fun <R> medData(block: (grad: Double, totalGrad: Double, aktuellDagsinntekt: Double) -> R) =
        medData { grad: Double,
                  _: Double,
                  _: Double?,
                  _: LocalDate?,
                  totalGrad: Double,
                  aktuellDagsinntekt: Double,
                  _: Double?,
                  _: Double?,
                  _: Boolean? ->
            block(grad, totalGrad, aktuellDagsinntekt)
        }

    fun medAvrundetData(block: (grad: Int, aktuellDagsinntekt: Int) -> Unit) {
        medAvrundetData { grad, _, _, _, _, aktuellDagsinntekt, _, _, _ -> block(grad, aktuellDagsinntekt) }
    }

    private fun <R> medDataFraBeløp(lambda: MedØkonomiData<R>) = lambda(
        grad.toDouble(),
        arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
        dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
        skjæringstidspunkt,
        totalGrad.toDouble(),
        aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
        arbeidsgiverbeløp!!.reflection { _, _, daglig, _ -> daglig },
        personbeløp!!.reflection { _, _, daglig, _ -> daglig },
        er6GBegrenset
    )

    private fun <R> medDataFraInntekt(lambda: MedØkonomiData<R>) = lambda(
        grad.toDouble(),
        arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
        dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
        skjæringstidspunkt,
        totalGrad.toDouble(),
        aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
        null, null, null
    )

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

    fun ikkeBetalt(): Økonomi {
        return Økonomi(
            grad = grad,
            totalGrad = totalGrad,
            arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp,
            arbeidsgiverperiode = arbeidsgiverperiode,
            aktuellDagsinntekt = aktuellDagsinntekt,
            dekningsgrunnlag = dekningsgrunnlag,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnbeløpgrense = grunnbeløpgrense,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            er6GBegrenset = er6GBegrenset,
            tilstand = Tilstand.IkkeBetalt
        )
    }

    sealed class Tilstand {

        internal open fun utbetalingsgrad(økonomi: Økonomi) = sykdomsgrad(økonomi)
        internal open fun sykdomsgrad(økonomi: Økonomi) = økonomi.grad

        internal abstract fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>): R

        internal open fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
            økonomi._build(builder)
        }

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            refusjonsbeløp: Inntekt,
            dekningsgrunnlag: Inntekt,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverperiode: ArbeidsgiverperiodePort?,
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

        internal open fun låsOpp(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse opp Økonomi i tilstand ${this::class.simpleName}")
        }

        internal object KunGrad : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi

            override fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
                økonomi._buildKunGrad(builder)
            }

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                refusjonsbeløp: Inntekt,
                dekningsgrunnlag: Inntekt,
                skjæringstidspunkt: LocalDate,
                arbeidsgiverperiode: ArbeidsgiverperiodePort?,
                `6G`: Inntekt
            ) = Økonomi(
                grad = økonomi.grad,
                totalGrad = økonomi.totalGrad,
                arbeidsgiverperiode = arbeidsgiverperiode ?: økonomi.arbeidsgiverperiode,
                arbeidsgiverRefusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnbeløpgrense = `6G`,
                tilstand = HarInntekt
            )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = INGEN
                økonomi.personbeløp = INGEN
                økonomi.tilstand = HarBeløp
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) =
                lambda(
                    grad = økonomi.grad.toDouble(),
                    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
                    dekningsgrunnlag = økonomi.dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
                    skjæringstidspunkt = null,
                    totalGrad = økonomi.totalGrad.toDouble(),
                    aktuellDagsinntekt = økonomi.aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
                    arbeidsgiverbeløp = null,
                    personbeløp = null,
                    er6GBegrenset = null
                )
        }

        // Tenkt tilstand for arbeidsgiverperiodedager: vi trenger sykdomsgraden, men ikke utbetale noe
        internal object IkkeBetalt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi

            override fun builder(økonomi: Økonomi, builder: ØkonomiBuilder) {
                økonomi._buildKunGrad(builder)
            }

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                refusjonsbeløp: Inntekt,
                dekningsgrunnlag: Inntekt,
                skjæringstidspunkt: LocalDate,
                arbeidsgiverperiode: ArbeidsgiverperiodePort?,
                `6G`: Inntekt
            ) = Økonomi(
                grad = økonomi.grad,
                totalGrad = økonomi.totalGrad,
                arbeidsgiverperiode = arbeidsgiverperiode ?: økonomi.arbeidsgiverperiode,
                arbeidsgiverRefusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnbeløpgrense = `6G`,
                tilstand = IkkeBetalt
            )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = INGEN
                økonomi.personbeløp = INGEN
                økonomi.tilstand = HarBeløp
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) =
                lambda(
                    grad = økonomi.grad.toDouble(),
                    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.reflection { _, _, daglig, _ -> daglig },
                    dekningsgrunnlag = økonomi.dekningsgrunnlag.reflection { _, _, daglig, _ -> daglig },
                    skjæringstidspunkt = null,
                    totalGrad = økonomi.totalGrad.toDouble(),
                    aktuellDagsinntekt = økonomi.aktuellDagsinntekt.reflection { _, _, daglig, _ -> daglig },
                    arbeidsgiverbeløp = null,
                    personbeløp = null,
                    er6GBegrenset = null
                )
        }

        object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraInntekt(lambda)

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }

        object Låst : Tilstand() {

            override fun utbetalingsgrad(økonomi: Økonomi) = 0.prosent
            override fun sykdomsgrad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraInntekt(lambda)

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

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun betal(økonomi: Økonomi) {}

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }
    }

    class Builder : ØkonomiBuilder() {
        fun build() = when (tilstand) {
            is Tilstand.KunGrad -> Økonomi(grad.prosent)
            else -> Økonomi(
                grad.prosent,
                totalGrad?.prosent!!,
                arbeidsgiverRefusjonsbeløp?.daglig!!,
                arbeidsgiverperiode,
                aktuellDagsinntekt?.daglig!!,
                dekningsgrunnlag?.daglig!!,
                skjæringstidspunkt,
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
    protected var skjæringstidspunkt: LocalDate? = null
    protected var totalGrad: Double? = null
    protected var aktuellDagsinntekt: Double? = null
    protected var arbeidsgiverbeløp: Double? = null
    protected var personbeløp: Double? = null
    protected var er6GBegrenset: Boolean? = null
    protected var arbeidsgiverperiode: ArbeidsgiverperiodePort? = null
    protected var grunnbeløpgrense: Double? = null
    protected var tilstand: Økonomi.Tilstand? = null


    fun grad(grad: Double): ØkonomiBuilder = apply {
        this.grad = grad
    }

    fun tilstand(tilstand: Økonomi.Tilstand): ØkonomiBuilder = apply {
        this.tilstand = tilstand
    }

    fun arbeidsgiverperiode(arbeidsgiverperiode: ArbeidsgiverperiodePort?) = apply {
        this.arbeidsgiverperiode = arbeidsgiverperiode
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

    fun skjæringstidspunkt(skjæringstidspunkt: LocalDate?) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
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

fun List<Økonomi>.totalSykdomsgrad(): Prosentdel = Økonomi.totalSykdomsgrad(this)

fun List<Økonomi>.betal() = Økonomi.betal(this)

fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)

fun interface MedØkonomiData<R> {
    operator fun invoke(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        skjæringstidspunkt: LocalDate?,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ): R
}

interface ArbeidsgiverperiodePort {
    fun firstOrNull(): LocalDate?
    fun toList(): List<LocalDate>
}