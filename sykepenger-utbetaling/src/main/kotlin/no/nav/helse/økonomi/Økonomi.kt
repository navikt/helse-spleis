package no.nav.helse.økonomi

import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.slf4j.LoggerFactory

data class Økonomi(
    val sykdomsgrad: Prosentdel,
    val utbetalingsgrad: Prosentdel,
    val refusjonsbeløp: Inntekt,
    val aktuellDagsinntekt: Inntekt,
    val dekningsgrunnlag: Inntekt,
    val totalSykdomsgrad: Prosentdel = sykdomsgrad,
    val arbeidsgiverbeløp: Inntekt? = null,
    val personbeløp: Inntekt? = null
) {
    companion object {
        private val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        private val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun inntekt(sykdomsgrad: Prosentdel, aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt, refusjonsbeløp: Inntekt) =
            Økonomi(
                sykdomsgrad = sykdomsgrad,
                utbetalingsgrad = sykdomsgrad,
                refusjonsbeløp = refusjonsbeløp,
                aktuellDagsinntekt = aktuellDagsinntekt,
                dekningsgrunnlag = dekningsgrunnlag
            )

        fun ikkeBetalt(aktuellDagsinntekt: Inntekt = INGEN) = inntekt(
            sykdomsgrad = 0.prosent,
            aktuellDagsinntekt = aktuellDagsinntekt,
            dekningsgrunnlag = INGEN,
            refusjonsbeløp = INGEN
        ).ikkeBetalt()

        private fun List<Økonomi>.aktuellDagsinntekt() = map { it.aktuellDagsinntekt }.summer()

        private fun totalSykdomsgrad(økonomiList: List<Økonomi>, gradStrategi: (Økonomi) -> Prosentdel): Prosentdel {
            val aktuellDagsinntekt = økonomiList.aktuellDagsinntekt()
            if (aktuellDagsinntekt == INGEN) return 0.prosent
            val totalgrad = Inntekt.vektlagtGjennomsnitt(økonomiList.map { gradStrategi(it) to it.aktuellDagsinntekt }, aktuellDagsinntekt)
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
            return fordelBeløp(foreløpig, sykepengegrunnlagBegrenset6G, utbetalingsgrad)
        }

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.map { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, sykepengegrunnlagBegrenset6G: Inntekt, utbetalingsgrad: Prosentdel): List<Økonomi> {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            val total = totalArbeidsgiver + totalPerson
            if (total == INGEN) return økonomiList

            val inntektstapSomSkalDekkesAvNAV = maxOf(INGEN, (sykepengegrunnlagBegrenset6G * utbetalingsgrad).rundTilDaglig())
            val fordelingRefusjon = fordel(økonomiList, totalArbeidsgiver, inntektstapSomSkalDekkesAvNAV, { økonomi, inntekt -> økonomi.copy(arbeidsgiverbeløp = inntekt) }, arbeidsgiverBeløp)
            val totalArbeidsgiverrefusjon = totalArbeidsgiver(fordelingRefusjon)
            val fordelingPerson = fordel(fordelingRefusjon, total - totalArbeidsgiverrefusjon, inntektstapSomSkalDekkesAvNAV - totalArbeidsgiverrefusjon, { økonomi, inntekt ->
                økonomi.copy(personbeløp = inntekt)
            }, personBeløp)
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
                    val personbeløp = personBeløp(it)
                    if (budsjett > INGEN && (arbeidsgiverBeløp(it) > INGEN || personbeløp > INGEN)) {
                        budsjett -= 1.daglig
                        it.copy(personbeløp = personbeløp + 1.daglig)
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

        fun gjenopprett(dto: ØkonomiInnDto): Økonomi {
            return Økonomi(
                sykdomsgrad = Prosentdel.gjenopprett(dto.grad),
                totalSykdomsgrad = Prosentdel.gjenopprett(dto.totalGrad),
                utbetalingsgrad = Prosentdel.gjenopprett(dto.utbetalingsgrad),
                refusjonsbeløp = Inntekt.gjenopprett(dto.arbeidsgiverRefusjonsbeløp),
                aktuellDagsinntekt = Inntekt.gjenopprett(dto.aktuellDagsinntekt),
                dekningsgrunnlag = Inntekt.gjenopprett(dto.dekningsgrunnlag),
                arbeidsgiverbeløp = dto.arbeidsgiverbeløp?.let { Inntekt.gjenopprett(it) },
                personbeløp = dto.personbeløp?.let { Inntekt.gjenopprett(it) }
            )
        }
    }

    init {
        require(dekningsgrunnlag >= INGEN) { "dekningsgrunnlag kan ikke være negativ." }
    }

    // sykdomsgrader opprettes som int, og det gir ikke mening å runde opp og på den måten "gjøre personen mer syk"
    fun <R> brukAvrundetGrad(block: (grad: Int) -> R) = block(sykdomsgrad.toDouble().toInt())

    // speil viser grad som nedrundet int (det rundes -ikke- oppover siden det ville gjort 19.5 % (for liten sykdomsgrad) til 20 % (ok sykdomsgrad)
    fun <R> brukTotalGrad(block: (totalGrad: Int) -> R) = block(totalSykdomsgrad.toDouble().toInt())

    private fun betal(): Økonomi {
        val total = (dekningsgrunnlag * utbetalingsgrad).rundTilDaglig()
        val gradertArbeidsgiverRefusjonsbeløp = (refusjonsbeløp * utbetalingsgrad).rundTilDaglig()
        val arbeidsgiverbeløp = gradertArbeidsgiverRefusjonsbeløp.coerceAtMost(total)
        return copy(
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = (total - arbeidsgiverbeløp).coerceAtLeast(INGEN)
        )
    }

    fun ikkeBetalt() = copy(utbetalingsgrad = 0.prosent)

    internal fun dagligBeløpForFagområde(område: Fagområde): Int? = when (område) {
        Fagområde.SykepengerRefusjon -> arbeidsgiverbeløp?.daglig?.toInt()
        Fagområde.Sykepenger -> personbeløp?.daglig?.toInt()
    }

    fun dto() = ØkonomiUtDto(
        grad = sykdomsgrad.dto(),
        totalGrad = totalSykdomsgrad.dto(),
        utbetalingsgrad = utbetalingsgrad.dto(),
        arbeidsgiverRefusjonsbeløp = refusjonsbeløp.dto(),
        aktuellDagsinntekt = aktuellDagsinntekt.dto(),
        dekningsgrunnlag = dekningsgrunnlag.dto(),
        arbeidsgiverbeløp = arbeidsgiverbeløp?.dto(),
        personbeløp = personbeløp?.dto()
    )
}

fun List<Økonomi>.betal(sykepengegrunnlagBegrenset6G: Inntekt) = Økonomi.betal(sykepengegrunnlagBegrenset6G, this)
