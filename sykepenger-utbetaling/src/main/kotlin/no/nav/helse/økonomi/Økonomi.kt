package no.nav.helse.økonomi

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
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList

            val inntektstapSomSkalDekkesAvNAV = maxOf(INGEN, (sykepengegrunnlagBegrenset6G * utbetalingsgrad).rundTilDaglig())
            val fordelingRefusjon = fordel(
                økonomiList = økonomiList,
                total = totalArbeidsgiver,
                grense = inntektstapSomSkalDekkesAvNAV,
                setter = { økonomi, inntekt -> økonomi.copy(reservertArbeidsgiverbeløp = inntekt) },
                getter = { it.reservertArbeidsgiverbeløp!! }
            )
            val totalArbeidsgiverrefusjon = totalArbeidsgiver(fordelingRefusjon)
            val fordelingPerson = fordel(
                økonomiList = fordelingRefusjon,
                total = total - totalArbeidsgiverrefusjon,
                grense = inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverrefusjon,
                setter = { økonomi, inntekt -> økonomi.copy(reservertPersonbeløp = inntekt) },
                getter = { it.reservertPersonbeløp!! }
            )
            val totalPersonbeløp = totalPerson(fordelingPerson)
            val restbeløp = inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverrefusjon - totalPersonbeløp
            val restfordeling = restfordeling(fordelingPerson, restbeløp)
            return restfordeling
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
                    val reservertPersonbeløp = it.reservertPersonbeløp!!
                    if (budsjett > INGEN && (it.reservertArbeidsgiverbeløp!! > INGEN || reservertPersonbeløp > INGEN)) {
                        budsjett -= 1.daglig
                        it.copy(reservertPersonbeløp = reservertPersonbeløp + 1.daglig)
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
        val total = (aktuellDagsinntekt * utbetalingsgrad).rundTilDaglig()
        val gradertArbeidsgiverRefusjonsbeløp = (refusjonsbeløp * utbetalingsgrad).rundTilDaglig()
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
