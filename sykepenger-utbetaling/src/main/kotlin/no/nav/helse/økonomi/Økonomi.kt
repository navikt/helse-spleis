package no.nav.helse.økonomi

import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.slf4j.LoggerFactory

class Økonomi private constructor(
    val grad: Prosentdel,
    val totalGrad: Prosentdel = grad,
    val utbetalingsgrad: Prosentdel = grad,
    val arbeidsgiverRefusjonsbeløp: Inntekt = INGEN,
    val aktuellDagsinntekt: Inntekt = INGEN,
    val dekningsgrunnlag: Inntekt = INGEN,
    val arbeidsgiverbeløp: Inntekt? = null,
    val personbeløp: Inntekt? = null,
    val tilstand: Tilstand = Tilstand.KunGrad,
) {
    companion object {
        private val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        private val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun sykdomsgrad(grad: Prosentdel) =
            Økonomi(grad)

        fun ikkeBetalt() = Økonomi(
            grad = 0.prosent,
            utbetalingsgrad = 0.prosent,
            tilstand = Tilstand.KunGrad
        )

        fun List<Økonomi>.totalSykdomsgrad(): Prosentdel {
            val økonomiList = totalSykdomsgrad(this)
            return økonomiList.first().totalGrad
        }

        private fun List<Økonomi>.aktuellDagsinntekt() = map { it.aktuellDagsinntekt }.summer()

        private fun totalSykdomsgrad(økonomiList: List<Økonomi>, gradStrategi: (Økonomi) -> Prosentdel): Prosentdel {
            val aktuellDagsinntekt = økonomiList.aktuellDagsinntekt()
            if (aktuellDagsinntekt == INGEN) {
                return (økonomiList.sumOf { gradStrategi(it).times(100.0) } / økonomiList.size).prosent
            }
            val totalgrad = Inntekt.vektlagtGjennomsnitt(økonomiList.map { gradStrategi(it) to it.aktuellDagsinntekt }, aktuellDagsinntekt)
            return totalgrad
        }

        fun totalSykdomsgrad(økonomiList: List<Økonomi>): List<Økonomi> {
            val totalgrad = totalSykdomsgrad(økonomiList, Økonomi::sykdomsgrad)
            return økonomiList.map { økonomi: Økonomi ->
                økonomi.kopierMed(totalgrad = totalgrad)
            }
        }

        fun List<Økonomi>.erUnderGrensen() = none { !it.totalGrad.erUnderGrensen() }

        private fun totalUtbetalingsgrad(økonomiList: List<Økonomi>) = totalSykdomsgrad(økonomiList, Økonomi::utbetalingsgrad)

        fun betal(sykepengegrunnlagBegrenset6G: Inntekt, økonomiList: List<Økonomi>): List<Økonomi> {
            val utbetalingsgrad = totalUtbetalingsgrad(økonomiList)
            val foreløpig = delteUtbetalinger(økonomiList)
            return fordelBeløp(foreløpig, sykepengegrunnlagBegrenset6G, utbetalingsgrad)
        }

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.map { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, sykepengegrunnlagBegrenset6G: Inntekt, utbetalingsgrad: Prosentdel): List<Økonomi> {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList.map { økonomi -> økonomi.kopierMed() }

            val inntektstapSomSkalDekkesAvNAV = maxOf(INGEN, (sykepengegrunnlagBegrenset6G * utbetalingsgrad).rundTilDaglig())
            val fordelingRefusjon = fordel(økonomiList, totalArbeidsgiver, inntektstapSomSkalDekkesAvNAV, { økonomi, inntekt -> økonomi.kopierMed(arbeidsgiverbeløp = inntekt) }, arbeidsgiverBeløp)
            val totalArbeidsgiverrefusjon = totalArbeidsgiver(fordelingRefusjon)
            val fordelingPerson = fordel(fordelingRefusjon, total - totalArbeidsgiverrefusjon, inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverrefusjon, { økonomi, inntekt -> økonomi.kopierMed(personbeløp = inntekt) }, personBeløp)
            val totalPersonbeløp = totalPerson(fordelingPerson)
            val restbeløp = inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverrefusjon - totalPersonbeløp
            val restfordeling = restfordeling(fordelingPerson, restbeløp)
            return restfordeling.map { økonomi -> økonomi.kopierMed() }
        }

        private fun restfordeling(økonomiList: List<Økonomi>, grense: Inntekt): List<Økonomi> {
            // På grunn av ulike avrundinger mellom arbeidsgiverrefusjon og sykepengegrunnlag kan det oppstå
            // differanse på 1 krone som da ville vært dumt å fordele på personbeløp
            // TODO: Finn en måte å fordele denne ene kronen på arbeidsgivere
            if (grense == 1.daglig) return økonomiList.also {
                sikkerlogg.info("Restbeløp på 1 krone")
            }
            var budsjett = grense
            var list = økonomiList
            // Fordeler 1 krone per arbeidsforhold som skal ha en utbetaling uansett frem til hele potten er fordelt
            while (budsjett > INGEN) {
                list = list.map {
                    val personbeløp = personBeløp(it)
                    if (budsjett > INGEN && (arbeidsgiverBeløp(it) > INGEN || personbeløp > INGEN)) {
                        budsjett -= 1.daglig
                        it.kopierMed(personbeløp = personbeløp + 1.daglig)
                    } else {
                        it
                    }
                }
            }
            return list

        }

        private fun fordel(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt, setter: (Økonomi, Inntekt) -> Økonomi, getter: (Økonomi) -> Inntekt): List<Økonomi> {
            return økonomiList
                .reduserOver6G(grense, total, getter)
                .fordel1Kr(grense, total, setter)
        }

        private fun List<Økonomi>.reduserOver6G(grense: Inntekt, total: Inntekt, getter: (Økonomi) -> Inntekt): List<Triple<Økonomi, Inntekt, Double>> {
            val ratio = reduksjon(grense, total)
            return map {
                val redusertBeløp = getter(it).times(ratio)
                val rundetNed = redusertBeløp.rundNedTilDaglig()
                val differanse = (redusertBeløp - rundetNed).daglig
                Triple(it, rundetNed, differanse)
            }
        }

        // fordeler 1 kr til hver av arbeidsgiverne som har mest i differanse i beløp
        private fun List<Triple<Økonomi, Inntekt, Double>>.fordel1Kr(grense: Inntekt, total: Inntekt, setter: (Økonomi, Inntekt) -> Økonomi): List<Økonomi> {
            val maksimalt = total.coerceAtMost(grense)
            val rest = (maksimalt - map { it.second }.summer()).dagligInt
            val sortertEtterTap = sortedByDescending { (_, _, differanse) -> differanse }.take(rest)
            return map { (økonomi, beløp) ->
                val ekstra = if (sortertEtterTap.any { (other) -> other === økonomi }) 1.daglig else INGEN
                setter(økonomi, beløp + ekstra)
            }
        }

        private fun reduksjon(grense: Inntekt, total: Inntekt): Prosentdel {
            if (total == INGEN) return 0.prosent
            return grense ratio total
        }

        private fun total(økonomiList: List<Økonomi>, strategi: (Økonomi) -> Inntekt): Inntekt =
            økonomiList.map { strategi(it) }.summer()

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>) = total(økonomiList, arbeidsgiverBeløp)

        private fun totalPerson(økonomiList: List<Økonomi>) = total(økonomiList, personBeløp)

        fun gjenopprett(dto: ØkonomiInnDto, erAvvistDag: Boolean): Økonomi {
            val utbetalingsgrad = dto.utbetalingsgrad?.let { Prosentdel.gjenopprett(it) } ?: when {
                // alle avviste dager har 0 % utbetalingsgrad
                erAvvistDag -> 0.prosent
                // dager som har inntekt, som var i tilstand IkkeBetalt, vil kunne ha en sykdomsgradprosent,
                // men de er blitt utbetalt med 0 % utbetalingsgrad (hardkodet INGEN som beløp)
                dto.grad.prosent > 0.0 && dto.arbeidsgiverbeløp != null && (dto.arbeidsgiverbeløp!!.beløp == 0.0 && dto.personbeløp!!.beløp == 0.0) -> 0.prosent
                dto.grad.prosent > 0.0 && dto.arbeidsgiverbeløp == null -> 0.prosent
                else -> Prosentdel.gjenopprett(dto.grad)
            }
            if (dto.arbeidsgiverbeløp != null && (dto.arbeidsgiverbeløp!!.beløp != 0.0 || dto.personbeløp!!.beløp != 0.0)) {
                check(utbetalingsgrad > 0.prosent) {
                    "utbetalingsprosenten er 0, men det er beregnet utbetalingsbeløp"
                }
            }
            return Økonomi(
                grad = Prosentdel.gjenopprett(dto.grad),
                totalGrad = Prosentdel.gjenopprett(dto.totalGrad),
                utbetalingsgrad = utbetalingsgrad,
                arbeidsgiverRefusjonsbeløp = Inntekt.gjenopprett(dto.arbeidsgiverRefusjonsbeløp),
                aktuellDagsinntekt = Inntekt.gjenopprett(dto.aktuellDagsinntekt),
                dekningsgrunnlag = Inntekt.gjenopprett(dto.dekningsgrunnlag),
                arbeidsgiverbeløp = dto.arbeidsgiverbeløp?.let { Inntekt.gjenopprett(it) },
                personbeløp = dto.personbeløp?.let { Inntekt.gjenopprett(it) },
                tilstand = when {
                    dto.arbeidsgiverbeløp == null -> when {
                        dto.arbeidsgiverRefusjonsbeløp.beløp > 0.0 || dto.aktuellDagsinntekt.beløp > 0.0 -> Tilstand.HarInntekt
                        else -> Tilstand.KunGrad
                    }
                    else -> Tilstand.HarBeløp
                }
            )
        }
    }

    init {
        require(dekningsgrunnlag >= INGEN) { "dekningsgrunnlag kan ikke være negativ." }
    }

    fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, refusjonsbeløp: Inntekt): Økonomi =
        tilstand.inntekt(
            økonomi = this,
            aktuellDagsinntekt = aktuellDagsinntekt,
            refusjonsbeløp = refusjonsbeløp,
            dekningsgrunnlag = dekningsgrunnlag
        )

    // sykdomsgrader opprettes som int, og det gir ikke mening å runde opp og på den måten "gjøre personen mer syk"
    fun <R> brukAvrundetGrad(block: (grad: Int) -> R) = block(grad.toDouble().toInt())

    // speil viser grad som nedrundet int (det rundes -ikke- oppover siden det ville gjort 19.5 % (for liten sykdomsgrad) til 20 % (ok sykdomsgrad)
    fun <R> brukTotalGrad(block: (totalGrad: Int) -> R) = block(totalGrad.toDouble().toInt())

    private fun sykdomsgrad() = tilstand.sykdomsgrad(this)

    private fun betal() = tilstand.betal(this)

    private fun _betal(): Økonomi {
        val total = (dekningsgrunnlag * utbetalingsgrad).rundTilDaglig()
        val gradertArbeidsgiverRefusjonsbeløp = (arbeidsgiverRefusjonsbeløp * utbetalingsgrad).rundTilDaglig()
        val arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        return kopierMed(
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = (total - arbeidsgiverbeløp).coerceAtLeast(INGEN),
            tilstand = Tilstand.HarBeløp
        )
    }

    fun ikkeBetalt() = kopierMed(utbetalingsgrad = 0.prosent)

    internal fun dagligBeløpForFagområde(område: Fagområde): Int? = when (område) {
        Fagområde.SykepengerRefusjon -> arbeidsgiverbeløp?.daglig?.toInt()
        Fagområde.Sykepenger -> personbeløp?.daglig?.toInt()
    }

    private fun kopierMed(
        grad: Prosentdel = this.grad,
        totalgrad: Prosentdel = this.totalGrad,
        utbetalingsgrad: Prosentdel = this.utbetalingsgrad,
        arbeidsgiverRefusjonsbeløp: Inntekt = this.arbeidsgiverRefusjonsbeløp,
        aktuellDagsinntekt: Inntekt = this.aktuellDagsinntekt,
        dekningsgrunnlag: Inntekt = this.dekningsgrunnlag,
        arbeidsgiverbeløp: Inntekt? = this.arbeidsgiverbeløp,
        personbeløp: Inntekt? = this.personbeløp,
        tilstand: Tilstand = this.tilstand,
    ) = Økonomi(
        grad = grad,
        totalGrad = totalgrad,
        utbetalingsgrad = utbetalingsgrad,
        arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp,
        aktuellDagsinntekt = aktuellDagsinntekt,
        dekningsgrunnlag = dekningsgrunnlag,
        arbeidsgiverbeløp = arbeidsgiverbeløp,
        personbeløp = personbeløp,
        tilstand = tilstand
    )

    sealed class Tilstand {

        internal open fun sykdomsgrad(økonomi: Økonomi) = økonomi.grad

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            refusjonsbeløp: Inntekt,
            dekningsgrunnlag: Inntekt
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt i tilstand ${this::class.simpleName}")
        }

        internal open fun betal(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke beregne utbetaling i tilstand ${this::class.simpleName}")
        }

        internal data object KunGrad : Tilstand() {

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                refusjonsbeløp: Inntekt,
                dekningsgrunnlag: Inntekt
            ) = økonomi.kopierMed(
                arbeidsgiverRefusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag,
                tilstand = HarInntekt
            )

            override fun betal(økonomi: Økonomi) = økonomi.kopierMed(
                arbeidsgiverbeløp = INGEN,
                personbeløp = INGEN,
                tilstand = HarBeløp
            )
        }

        internal data object HarInntekt : Tilstand() {
            override fun betal(økonomi: Økonomi) = økonomi._betal()
        }

        internal data object HarBeløp : Tilstand()
    }

    fun subsumsjonsdata() = Dekningsgrunnlagsubsumsjon(
        årligInntekt = aktuellDagsinntekt.årlig,
        årligDekningsgrunnlag = dekningsgrunnlag.årlig
    )

    fun dto() = ØkonomiUtDto(
        grad = grad.dto(),
        totalGrad = totalGrad.dto(),
        utbetalingsgrad = utbetalingsgrad.dto(),
        arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp.dto(),
        aktuellDagsinntekt = aktuellDagsinntekt.dto(),
        dekningsgrunnlag = dekningsgrunnlag.dto(),
        arbeidsgiverbeløp = arbeidsgiverbeløp?.dto(),
        personbeløp = personbeløp?.dto()
    )
}

data class Dekningsgrunnlagsubsumsjon(
    val årligInntekt: Double,
    val årligDekningsgrunnlag: Double
)

fun List<Økonomi>.betal(sykepengegrunnlagBegrenset6G: Inntekt) = Økonomi.betal(sykepengegrunnlagBegrenset6G, this)
