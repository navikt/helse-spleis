package no.nav.helse.økonomi

import kotlin.math.abs
import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.NullProsent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.slf4j.LoggerFactory

data class Økonomi(
    val sykdomsgrad: Prosentdel,
    val utbetalingsgrad: Prosentdel,
    val refusjonsbeløp: Inntekt,
    val aktuellDagsinntekt: Inntekt,
    val inntektjustering: Inntekt,
    val dekningsgrad: Prosentdel,
    val totalSykdomsgrad: Prosentdel = sykdomsgrad,
    val arbeidsgiverbeløp: Inntekt? = null,
    val personbeløp: Inntekt? = null,
    private val reservertArbeidsgiverbeløp: Inntekt? = null,
    private val reservertPersonbeløp: Inntekt? = null
) {
    companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun inntekt(sykdomsgrad: Prosentdel, aktuellDagsinntekt: Inntekt, dekningsgrad: Prosentdel, refusjonsbeløp: Inntekt, inntektjustering: Inntekt) =
            Økonomi(
                sykdomsgrad = sykdomsgrad,
                utbetalingsgrad = sykdomsgrad,
                refusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                inntektjustering = inntektjustering,
                dekningsgrad = dekningsgrad
            )

        fun ikkeBetalt(aktuellDagsinntekt: Inntekt = INGEN, inntektjustering: Inntekt = INGEN) = inntekt(
            sykdomsgrad = NullProsent,
            aktuellDagsinntekt = aktuellDagsinntekt,
            refusjonsbeløp = INGEN,
            dekningsgrad = DekningsgradArbeidstaker,
            inntektjustering = inntektjustering
        ).ikkeBetalt()

        private fun List<Økonomi>.aktuellDagsinntekt() = map { it.aktuellDagsinntekt }.summer()
        private fun List<Økonomi>.inntektjustering() = map { it.inntektjustering }.summer()

        private fun totalSykdomsgrad(økonomiList: List<Økonomi>, gradStrategi: (Økonomi) -> Prosentdel): Prosentdel {
            val aktuellDagsinntekt = økonomiList.aktuellDagsinntekt()
            if (aktuellDagsinntekt == INGEN) return NullProsent
            val totalgrad = Inntekt.vektlagtGjennomsnitt(økonomiList.map { gradStrategi(it) to it.aktuellDagsinntekt }, økonomiList.inntektjustering())
            return totalgrad
        }

        fun totalSykdomsgrad(økonomiList: List<Økonomi>): List<Økonomi> {
            val totalgrad = totalSykdomsgrad(økonomiList, Økonomi::sykdomsgrad)
            return økonomiList.map { økonomi: Økonomi ->
                økonomi.copy(totalSykdomsgrad = totalgrad)
            }
        }

        fun List<Økonomi>.erUnderGrensen() = none { !it.totalSykdomsgrad.erUnderGrensen() }

        private fun totalUtbetalingsgrad(økonomiList: List<Økonomi>) = totalSykdomsgrad(økonomiList, Økonomi::utbetalingsgrad)

        fun betal(sykepengegrunnlagBegrenset6G: Inntekt, økonomiList: List<Økonomi>): List<Økonomi> {
            val utbetalingsgrad = totalUtbetalingsgrad(økonomiList)
            val foreløpig = delteUtbetalinger(økonomiList)
            val fordelt = fordelBeløp(foreløpig, sykepengegrunnlagBegrenset6G, utbetalingsgrad)
            return fordelt.map { it.betal() }
        }

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.map { it.reserver() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, sykepengegrunnlagBegrenset6G: Inntekt, utbetalingsgrad: Prosentdel): List<Økonomi> {
            val totalArbeidsgiverFør6GBegrensning = totalArbeidsgiver(økonomiList)
            val totalPersonFør6GBegrensning = totalPerson(økonomiList)
            val total = totalArbeidsgiverFør6GBegrensning + totalPersonFør6GBegrensning
            if (total == INGEN) return økonomiList

            val inntektstapSomSkalDekkesAvNAV = maxOf(INGEN, sykepengegrunnlagBegrenset6G * utbetalingsgrad)
            val ratio = reduksjon(inntektstapSomSkalDekkesAvNAV, totalArbeidsgiverFør6GBegrensning)
            val totalArbeidsgiverEtter6GBegrensning1 = totalArbeidsgiverFør6GBegrensning * ratio

            val fordelingRefusjon = reduserBeløpTilTotal(
                økonomiList = økonomiList,
                total = totalArbeidsgiverFør6GBegrensning,
                grense = inntektstapSomSkalDekkesAvNAV,
                setter = { økonomi, inntekt -> økonomi.copy(reservertArbeidsgiverbeløp = inntekt) },
                getter = { it.reservertArbeidsgiverbeløp!! }
            )
            val totalArbeidsgiverEtter6GBegrensning = totalArbeidsgiver(fordelingRefusjon)
            check(totalArbeidsgiverEtter6GBegrensning1.rundTilDaglig() == totalArbeidsgiverEtter6GBegrensning.rundTilDaglig()) {
                "WHAAAAT! De skal jo være helt like"
            }

            val grenseForPersonUtbetaling = inntektstapSomSkalDekkesAvNAV.minus(totalArbeidsgiverEtter6GBegrensning)
            val sikkerGrenseForPersonUtbetaling = if (erDoublePresisjonsfeil(grenseForPersonUtbetaling)) INGEN else grenseForPersonUtbetaling

            val fordelingPersonOgRefusjon = reduserBeløpTilTotal(
                økonomiList = fordelingRefusjon,
                total = total - totalArbeidsgiverEtter6GBegrensning,
                grense = sikkerGrenseForPersonUtbetaling,
                setter = { økonomi, inntekt -> økonomi.copy(reservertPersonbeløp = inntekt) },
                getter = { it.reservertPersonbeløp!! }
            )
            val totalPersonbeløpEtter6GBegrensning = totalPerson(fordelingPersonOgRefusjon)
            val restbeløp = inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverEtter6GBegrensning - totalPersonbeløpEtter6GBegrensning
            check(restbeløp.rundTilDaglig() == INGEN) {
                "Det er et restbeløp på kr $restbeløp etter all fordeling"
            }
            return fordelingPersonOgRefusjon
        }

        internal fun erDoublePresisjonsfeil(inntekt: Inntekt): Boolean {
            return inntekt < INGEN && abs(inntekt.daglig) < 1e-6
        }

        private fun reduserBeløpTilTotal(økonomiList: List<Økonomi>, total: Inntekt, grense: Inntekt, setter: (Økonomi, Inntekt) -> Økonomi, getter: (Økonomi) -> Inntekt): List<Økonomi> {
            val ratio = reduksjon(grense, total)
            return økonomiList.map {
                val redusertBeløp = getter(it).times(ratio)
                setter(it, redusertBeløp)
            }
        }

        private fun reduksjon(grense: Inntekt, total: Inntekt): Prosentdel {
            if (total == INGEN) return NullProsent
            return grense ratio total
        }

        private fun total(økonomiList: List<Økonomi>, strategi: (Økonomi) -> Inntekt): Inntekt =
            økonomiList.map { strategi(it) }.summer()

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>) = total(økonomiList) { it.reservertArbeidsgiverbeløp!! }

        private fun totalPerson(økonomiList: List<Økonomi>) = total(økonomiList) { it.reservertPersonbeløp!! }

        fun gjenopprett(dto: ØkonomiInnDto): Økonomi {
            return Økonomi(
                sykdomsgrad = Prosentdel.gjenopprett(dto.grad),
                totalSykdomsgrad = Prosentdel.gjenopprett(dto.totalGrad),
                utbetalingsgrad = Prosentdel.gjenopprett(dto.utbetalingsgrad),
                refusjonsbeløp = Inntekt.gjenopprett(dto.arbeidsgiverRefusjonsbeløp),
                aktuellDagsinntekt = Inntekt.gjenopprett(dto.aktuellDagsinntekt),
                inntektjustering = Inntekt.gjenopprett(dto.inntektjustering),
                dekningsgrad = Prosentdel.gjenopprett(dto.dekningsgrad),
                arbeidsgiverbeløp = dto.arbeidsgiverbeløp?.let { Inntekt.gjenopprett(it) },
                personbeløp = dto.personbeløp?.let { Inntekt.gjenopprett(it) },
                reservertArbeidsgiverbeløp = dto.reservertArbeidsgiverbeløp?.let { Inntekt.gjenopprett(it) },
                reservertPersonbeløp = dto.reservertPersonbeløp?.let { Inntekt.gjenopprett(it) }
            )
        }
        private val DekningsgradArbeidstaker = 100.prosent
        private val DekningsgradSelvstendig = 80.prosent
    }

    init {
        require(dekningsgrad == DekningsgradSelvstendig || dekningsgrad == DekningsgradArbeidstaker) { "dekningsgrad må være 100 % eller 80 % var $dekningsgrad." }
    }

    // sykdomsgrader opprettes som int, og det gir ikke mening å runde opp og på den måten "gjøre personen mer syk"
    fun <R> brukAvrundetGrad(block: (grad: Int) -> R) = block(sykdomsgrad.toDouble().toInt())

    // speil viser grad som nedrundet int (det rundes -ikke- oppover siden det ville gjort 19.5 % (for liten sykdomsgrad) til 20 % (ok sykdomsgrad)
    fun <R> brukTotalGrad(block: (totalGrad: Int) -> R) = block(totalSykdomsgrad.toDouble().toInt())

    private fun reserver(): Økonomi {
        val total = aktuellDagsinntekt * utbetalingsgrad
        val gradertArbeidsgiverRefusjonsbeløp = refusjonsbeløp * utbetalingsgrad
        val arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        return copy(
            reservertArbeidsgiverbeløp = arbeidsgiverbeløp,
            reservertPersonbeløp = (total - arbeidsgiverbeløp).coerceAtLeast(INGEN)
        )
    }

    private fun betal(): Økonomi {
        check(arbeidsgiverbeløp == null) { "Arbeidsgiverbeløp skal kun settes én gang!" }
        check(personbeløp == null) { "Personbeløp skal kun settes én gang!" }
        return copy(
            arbeidsgiverbeløp = (reservertArbeidsgiverbeløp!! * dekningsgrad).rundTilDaglig(),
            personbeløp = (reservertPersonbeløp!! * dekningsgrad).rundTilDaglig(),
        )
    }

    fun ikkeBetalt() = copy(utbetalingsgrad = NullProsent)

    fun dto() = ØkonomiUtDto(
        grad = sykdomsgrad.dto(),
        totalGrad = totalSykdomsgrad.dto(),
        utbetalingsgrad = utbetalingsgrad.dto(),
        arbeidsgiverRefusjonsbeløp = refusjonsbeløp.dto(),
        aktuellDagsinntekt = aktuellDagsinntekt.dto(),
        inntektjustering = inntektjustering.dto(),
        dekningsgrad = dekningsgrad.dto(),
        arbeidsgiverbeløp = arbeidsgiverbeløp?.dto(),
        personbeløp = personbeløp?.dto(),
        reservertArbeidsgiverbeløp = reservertArbeidsgiverbeløp?.dto(),
        reservertPersonbeløp = reservertPersonbeløp?.dto()
    )
}

fun List<Økonomi>.betal(sykepengegrunnlagBegrenset6G: Inntekt) = Økonomi.betal(sykepengegrunnlagBegrenset6G, this)
