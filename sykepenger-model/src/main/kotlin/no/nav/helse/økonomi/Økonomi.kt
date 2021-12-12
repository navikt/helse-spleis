package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import kotlin.math.roundToInt

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private var totalGrad: Prosentdel = grad,
    private var arbeidsgiverRefusjonsbeløp: Inntekt = INGEN,
    private var arbeidsgiverperiode: Arbeidsgiverperiode? = null,
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
        internal val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        internal val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }

        internal fun sykdomsgrad(grad: Prosentdel) =
            Økonomi(grad)

        internal fun ikkeBetalt() = sykdomsgrad(0.prosent)

        internal fun ikkeBetalt(arbeidsgiverperiode: Arbeidsgiverperiode?) = ikkeBetalt().also {
            it.arbeidsgiverperiode = arbeidsgiverperiode
        }

        internal fun totalSykdomsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dekningsgrunnlag })

        internal fun List<Økonomi>.avgrensTilArbeidsgiverperiode(periode: Periode): Periode? {
            return map { it.arbeidsgiverperiode }.firstOrNull()?.firstOrNull()?.let { førsteArbeidsgiverperiodedag ->
                Periode(førsteArbeidsgiverperiodedag, periode.endInclusive)
            }?.takeUnless { it == periode }
        }

        internal fun betal(økonomiList: List<Økonomi>, virkningsdato: LocalDate): List<Økonomi> = økonomiList.also {
            delteUtbetalinger(it)
            økonomiList.forEach { it.totalGrad = totalSykdomsgrad(økonomiList) }
            fordelBeløp(it, virkningsdato)
        }

        private fun maksbeløp(økonomi: Økonomi) =
            (økonomi.grunnbeløpgrense?.rundTilDaglig()!! * økonomi.totalGrad).rundTilDaglig()

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, virkningsdato: LocalDate) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = false }

            check(økonomiList.any { it.skjæringstidspunkt != null }) { "ingen økonomiobjekt har skjæringstidspunkt" }
            check(økonomiList.filter { it.skjæringstidspunkt != null }.distinctBy { it.skjæringstidspunkt }.count() == 1) { "det finnes flere unike skjæringstidspunkt for økonomiobjekt på samme dag" }

            val skjæringstidspunkt = økonomiList.firstNotNullOf { it.skjæringstidspunkt }
            økonomiList.forEach { it.grunnbeløpgrense = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningsdato) }

            val grense = maksbeløp(økonomiList.first())
            fordelArbeidsgiverbeløp(økonomiList, totalArbeidsgiver, grense)
            fordelPersonbeløp(økonomiList, totalPerson + totalArbeidsgiver - totalArbeidsgiver(økonomiList), grense - totalArbeidsgiver(økonomiList))
            økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = total > grense }
        }

        private fun fordelPersonbeløp(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt) {
            val beregningsresultat = beregnUtbetalingFørAvrunding(
                økonomiList,
                total,
                budsjett,
            ) { it.personbeløp!! }

            beregningsresultat.forEach { it.økonomi.personbeløp = it.utbetalingEtterAvrunding() }

            val totaltRestbeløp =
                (total.coerceAtMost(budsjett) - totalPerson(økonomiList))
                    .reflection { _, _, _, dagligInt -> dagligInt }

            beregningsresultat
                .sortedByDescending { it.differanse() }
                .take(totaltRestbeløp)
                .forEach { it.økonomi.personbeløp = it.økonomi.personbeløp!! + 1.daglig }
        }

        private fun fordelArbeidsgiverbeløp(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt) {
            val beregningsresultat = beregnUtbetalingFørAvrunding(
                økonomiList,
                total,
                grense
            ) { it.arbeidsgiverbeløp!! }

            beregningsresultat.forEach { it.økonomi.arbeidsgiverbeløp = it.utbetalingEtterAvrunding() }

            val totaltRestbeløp =
                (total.coerceAtMost(grense) - totalArbeidsgiver(økonomiList))
                    .reflection { _, _, _, dagligInt -> dagligInt }

            beregningsresultat
                .sortedByDescending { it.differanse() }
                .take(totaltRestbeløp)
                .forEach { it.økonomi.arbeidsgiverbeløp = it.økonomi.arbeidsgiverbeløp!! + 1.daglig }
        }

        private fun beregnUtbetalingFørAvrunding(
            økonomiList: List<Økonomi>,
            total: Inntekt,
            grense: Inntekt,
            get: (Økonomi) -> Inntekt,
        ): List<Beregningsresultat> {
            if (total <= 0.daglig) {
                return økonomiList.map { Beregningsresultat(økonomi = it, utbetalingFørAvrunding = 0.daglig) }
            }

            val ratio = grense ratio total

            return økonomiList
                .map {
                    val utbetalingFørAvrunding = get(it) * ratio.coerceAtMost(1.0)
                    Beregningsresultat(
                        økonomi = it,
                        utbetalingFørAvrunding = utbetalingFørAvrunding
                    )
                }
        }

        data class Beregningsresultat(
            internal val økonomi: Økonomi,
            internal val utbetalingFørAvrunding: Inntekt,
        ) {
            internal fun differanse() = (utbetalingFørAvrunding - utbetalingEtterAvrunding()).reflection { _, _, daglig, _ -> daglig }
            internal fun utbetalingEtterAvrunding() = utbetalingFørAvrunding.rundNedTilDaglig()
        }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.arbeidsgiverbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        private fun totalPerson(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.personbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.any { it.er6GBegrenset() }
    }

    init {
        require(dekningsgrunnlag >= INGEN) { "dekningsgrunnlag kan ikke være negativ." }
    }

    internal fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, skjæringstidspunkt: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode? = null): Økonomi =
        tilstand.inntekt(this, aktuellDagsinntekt, dekningsgrunnlag, skjæringstidspunkt, arbeidsgiverperiode)

    internal fun arbeidsgiverRefusjon(refusjonsbeløp: Inntekt?) =
        tilstand.arbeidsgiverRefusjon(this, refusjonsbeløp)

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun toMap() = tilstand.toMap(this)

    private fun _toMapKunGrad() = mutableMapOf<String, Any>().also { map ->
        medData { grad, _, _, _, _, _, _, _, _ ->
            /* ikke legg på flere felter - alle er enten null eller har defaultverdi */
            map["grad"] = grad
        }
    }

    private fun _toMap() = mutableMapOf<String, Any>().also { map ->
        medData { grad,
                  arbeidsgiverRefusjonsbeløp,
                  dekningsgrunnlag,
                  skjæringstidspunkt,
                  totalGrad,
                  aktuellDagsinntekt,
                  arbeidsgiverbeløp,
                  personbeløp,
                  er6GBegrenset ->
            map["grad"] = grad
            map["totalGrad"] = totalGrad
            map.compute("arbeidsgiverperiode") { _, _ ->
                arbeidsgiverperiode?.toList()?.grupperSammenhengendePerioder()?.map {
                    mapOf("fom" to it.start, "tom" to it.endInclusive)
                }
            }
            map["arbeidsgiverRefusjonsbeløp"] = arbeidsgiverRefusjonsbeløp
            map.compute("skjæringstidspunkt") { _, _ -> skjæringstidspunkt }
            map.compute("grunnbeløpgrense") { _, _ -> grunnbeløpgrense?.reflection { årlig, _, _, _ -> årlig } }
            map.compute("dekningsgrunnlag") { _, _ -> dekningsgrunnlag }
            map.compute("aktuellDagsinntekt") { _, _ -> aktuellDagsinntekt }
            map.compute("arbeidsgiverbeløp") { _, _ -> arbeidsgiverbeløp }
            map.compute("personbeløp") { _, _ -> personbeløp }
            map.compute("er6GBegrenset") { _, _ -> er6GBegrenset }
        }
    }
    internal fun <R> medData(lambda: MedØkonomiData<R>) = tilstand.medData(this, lambda)

    internal fun <R> medAvrundetData(
        block: (
            grad: Int,
            arbeidsgiverRefusjonsbeløp: Int,
            dekningsgrunnlag: Int,
            aktuellDagsinntekt: Int,
            arbeidsgiverbeløp: Int?,
            personbeløp: Int?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        medData { grad: Double,
                  arbeidsgiverRefusjonsbeløp: Double,
                  dekningsgrunnlag: Double,
                  _: LocalDate?,
                  _: Double,
                  aktuellDagsinntekt: Double,
                  arbeidsgiverbeløp: Double?,
                  personbeløp: Double?,
                  er6GBegrenset: Boolean? ->
            block(
                grad.roundToInt(),
                arbeidsgiverRefusjonsbeløp.roundToInt(),
                dekningsgrunnlag.roundToInt(),
                aktuellDagsinntekt.roundToInt(),
                arbeidsgiverbeløp?.roundToInt(),
                personbeløp?.roundToInt(),
                er6GBegrenset
            )
        }

    internal fun <R> medData(block: (grad: Double, aktuellDagsinntekt: Double) -> R) =
        medData { grad: Double,
                  _: Double,
                  _: Double?,
                  _: LocalDate?,
                  _: Double,
                  aktuellDagsinntekt: Double,
                  _: Double?,
                  _: Double?,
                  _: Boolean? ->
            block(grad, aktuellDagsinntekt)
        }

    internal fun medAvrundetData(block: (grad: Int, aktuellDagsinntekt: Int) -> Unit) {
        medAvrundetData { grad: Int,
                          _: Int,
                          _: Int?,
                          aktuellDagsinntekt: Int,
                          _: Int?,
                          _: Int?,
                          _: Boolean? ->
            block(grad, aktuellDagsinntekt)
        }
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

    private fun grad() = tilstand.grad(this)

    private fun betal() = this.also { tilstand.betal(this) }

    internal fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    internal fun harPersonbeløp() = personbeløp!! > INGEN

    private fun _betal() {
        val total = dekningsgrunnlag * grad().ratio()
        val gradertArbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp * grad().ratio()
        arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        personbeløp = (total - arbeidsgiverbeløp!!).coerceAtLeast(INGEN)
    }

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: AvvistDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavHelgDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: ArbeidsgiverperiodeDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: Arbeidsdag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Sykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal abstract fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>): R

        internal open fun toMap(økonomi: Økonomi): Map<String, Any> {
            return økonomi._toMap()
        }

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            dekningsgrunnlag: Inntekt,
            skjæringstidspunkt: LocalDate,
            arbeidsgiverperiode: Arbeidsgiverperiode?
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

        internal open fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?): Økonomi {
            throw IllegalStateException("Kan ikke sette arbeidsgiverrefusjonsbeløp i tilstand ${this::class.simpleName}")
        }

        internal object KunGrad : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi

            override fun toMap(økonomi: Økonomi) = økonomi._toMapKunGrad()

            override fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?) = økonomi

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                dekningsgrunnlag: Inntekt,
                skjæringstidspunkt: LocalDate,
                arbeidsgiverperiode: Arbeidsgiverperiode?
            ) = Økonomi(
                grad = økonomi.grad,
                totalGrad = økonomi.totalGrad,
                arbeidsgiverperiode = arbeidsgiverperiode ?: økonomi.arbeidsgiverperiode,
                arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                skjæringstidspunkt = skjæringstidspunkt,
                grunnbeløpgrense = Grunnbeløp.`6G`.beløp(skjæringstidspunkt),
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

        internal object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraInntekt(lambda)

            override fun arbeidsgiverRefusjon(økonomi: Økonomi, refusjonsbeløp: Inntekt?) = økonomi.apply {
                økonomi.arbeidsgiverRefusjonsbeløp = refusjonsbeløp ?: økonomi.aktuellDagsinntekt
            }

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        internal object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
            }

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }

        internal object Låst : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

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

        internal object LåstMedBeløp : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun betal(økonomi: Økonomi) {}

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun <R> medData(økonomi: Økonomi, lambda: MedØkonomiData<R>) = økonomi.medDataFraBeløp(lambda)
        }
    }
}

internal fun List<Økonomi>.totalSykdomsgrad(): Prosentdel = Økonomi.totalSykdomsgrad(this)

internal fun List<Økonomi>.betal(virkningsdato: LocalDate) = Økonomi.betal(this, virkningsdato)

internal fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)

internal fun interface MedØkonomiData<R> {
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
