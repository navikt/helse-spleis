package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-33 ledd 3`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.plus
import no.nav.helse.ukedager
import java.time.DayOfWeek
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit.YEARS

internal class Alder(private val fødselsdato: LocalDate) {
    private val syttiårsdagen: LocalDate = fødselsdato.plusYears(70)
    internal val sisteVirkedagFørFylte70år: LocalDate = syttiårsdagen.sisteVirkedagFør()
    private val redusertYtelseAlder: LocalDate = fødselsdato.plusYears(67)
    private val forhøyetInntektskravAlder: LocalDate = fødselsdato.plusYears(67)

    private companion object {
        private const val MAKS_SYKEPENGEDAGER_OVER_67 = 60
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59
        private const val MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE = 18
        private fun LocalDate.sisteVirkedagFør(): LocalDate = this.minusDays(
            when (this.dayOfWeek) {
                DayOfWeek.SUNDAY -> 2
                DayOfWeek.MONDAY -> 3
                else -> 1
            }
        )
    }

    internal fun innenfor67årsgrense(dato: LocalDate) = dato <= redusertYtelseAlder
    internal fun innenfor70årsgrense(dato: LocalDate) = dato <= sisteVirkedagFørFylte70år
    internal fun harNådd70årsgrense(dato: LocalDate) = dato >= sisteVirkedagFørFylte70år
    internal fun mistetSykepengerett(dato: LocalDate) = dato >= syttiårsdagen

    internal fun alderPåDato(dato: LocalDate) = YEARS.between(fødselsdato, dato).toInt()

    private fun alderVedSluttenAvÅret(year: Year) = YEARS.between(Year.from(fødselsdato), year).toInt()

    internal fun minimumInntekt(dato: LocalDate) = (if (forhøyetInntektskrav(dato)) Grunnbeløp.`2G` else Grunnbeløp.halvG).beløp(dato)

    // Forhøyet inntektskrav gjelder fra dagen _etter_ 67-årsdagen - se § 8-51 andre ledd der det spesifiseres _mellom_.
    internal fun forhøyetInntektskrav(dato: LocalDate) = dato > forhøyetInntektskravAlder

    internal fun begrunnelseForMinimumInntekt(skjæringstidspunkt: LocalDate) =
        if (forhøyetInntektskrav(skjæringstidspunkt)) Begrunnelse.MinimumInntektOver67 else Begrunnelse.MinimumInntekt

    internal fun begrunnelseForAlder(dagen: LocalDate): Begrunnelse {
        if (innenfor67årsgrense(dagen)) return Begrunnelse.SykepengedagerOppbrukt
        if (innenfor70årsgrense(dagen)) return Begrunnelse.SykepengedagerOppbruktOver67
        return Begrunnelse.Over70
    }

    internal fun forUngForÅSøke(søknadstidspunkt: LocalDate) = alderPåDato(søknadstidspunkt) < MINSTEALDER_UTEN_FULLMAKT_FRA_VERGE

    internal fun beregnFeriepenger(opptjeningsår: Year, beløp: Int): Double {
        val alderVedSluttenAvÅret = alderVedSluttenAvÅret(opptjeningsår)
        val prosentsats = if (alderVedSluttenAvÅret < ALDER_FOR_FORHØYET_FERIEPENGESATS) 0.102 else 0.125
        val feriepenger = beløp * prosentsats
        Aktivitetslogg().`§8-33 ledd 3`(beløp, opptjeningsår, prosentsats, alderVedSluttenAvÅret, feriepenger)
        return feriepenger
    }

    internal fun etterlevelse70år(aktivitetslogg: IAktivitetslogg, periode: Periode, avvisteDager: Set<LocalDate>, jurist: SubsumsjonObserver) {
        førFylte70(periode, jurist)
        fraOgMedFylte70(aktivitetslogg, periode, avvisteDager, jurist)
    }

    private fun førFylte70(periode: Periode, jurist: SubsumsjonObserver) {
        if (periode.start >= syttiårsdagen) return
        jurist.`§ 8-3 ledd 1 punktum 2`(
            oppfylt = true,
            syttiårsdagen = syttiårsdagen,
            utfallFom = periode.start,
            utfallTom = minOf(syttiårsdagen.minusDays(1), periode.endInclusive),
            tidslinjeFom = periode.start,
            tidslinjeTom = periode.endInclusive,
            avvisteDager = emptyList()
        )
    }

    private fun fraOgMedFylte70(aktivitetslogg: IAktivitetslogg, periode: Periode, avvisteDager: Set<LocalDate>, jurist: SubsumsjonObserver) {
        val avvisteDagerFraOgMedSøtti = avvisteDager.filter { it >= syttiårsdagen }
        if (avvisteDagerFraOgMedSøtti.isEmpty()) return
        aktivitetslogg.info("Utbetaling stoppet etter $syttiårsdagen, søker fylte 70 år.")
        jurist.`§ 8-3 ledd 1 punktum 2`(
            oppfylt = false,
            syttiårsdagen = syttiårsdagen,
            utfallFom = maxOf(syttiårsdagen, periode.start),
            utfallTom = periode.endInclusive,
            tidslinjeFom = periode.start,
            tidslinjeTom = periode.endInclusive,
            avvisteDager = avvisteDagerFraOgMedSøtti
        )
    }

    internal fun maksimumSykepenger(dato: LocalDate, forbrukteDager: Int, gjenståendeSykepengedager: Int, gjenståendeSykepengedagerOver67: Int): MaksimumSykepenger {
        val maksdatoOrdinærRett = MaksimumSykepenger.ordinærRett(dato, forbrukteDager, gjenståendeSykepengedager)
        val maksdatoBegrensetRett = MaksimumSykepenger.begrensetRett(dato, forbrukteDager, this, gjenståendeSykepengedagerOver67)
        val absoluttSisteDagMedSykepenger = MaksimumSykepenger.absoluttSisteDagMedSykepenger(dato, forbrukteDager, sisteVirkedagFørFylte70år)
        return MaksimumSykepenger.minsteAv(maksdatoOrdinærRett, maksdatoBegrensetRett, absoluttSisteDagMedSykepenger)
    }

    internal class MaksimumSykepenger private constructor(
        beregningFom: LocalDate,
        private val forbrukteDager: Int,
        private val sisteDag: LocalDate,
        private val tilstand: Tilstand
    ) {
        private val gjenståendeDager = if (sisteDag <= beregningFom) 0 else beregningFom.datesUntil(sisteDag)
            .filter { it.dayOfWeek !in setOf(SATURDAY, SUNDAY) }
            .count()
            .toInt()

        internal fun sisteDag(begrunnelse: Begrunnelse? = null): LocalDate {
            sporBegrunnelse(begrunnelse)
            return sisteDag
        }

        internal fun forbrukteDager() = forbrukteDager
        internal fun begrunnelse() = tilstand.begrunnAvvisning()

        internal fun gjenståendeDager(begrunnelse: Begrunnelse? = null): Int {
            sporBegrunnelse(begrunnelse)
            return gjenståendeDager
        }

        private fun sporBegrunnelse(begrunnelse: Begrunnelse?) {
            if (begrunnelse == null) return
            tilstand.sporBegrunnelse()(begrunnelse, sisteDag, forbrukteDager, gjenståendeDager)
        }

        private interface Tilstand {
            fun sporBegrunnelse(): Begrunnelse.(LocalDate, Int, Int) -> Unit
            fun begrunnAvvisning(): no.nav.helse.utbetalingstidslinje.Begrunnelse

            object Under67 : Tilstand {
                override fun sporBegrunnelse() = Begrunnelse::`§ 8-12 ledd 1 punktum 1`
                override fun begrunnAvvisning() = no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
            }
            object Over67 : Tilstand {
                override fun sporBegrunnelse() = Begrunnelse::`§ 8-51 ledd 3`
                override fun begrunnAvvisning() = no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbruktOver67
            }
            object Over70 : Tilstand {
                override fun sporBegrunnelse() = Begrunnelse::`§ 8-3 ledd 1 punktum 2`
                override fun begrunnAvvisning() = no.nav.helse.utbetalingstidslinje.Begrunnelse.Over70
            }
        }

        interface Begrunnelse {
            fun `§ 8-12 ledd 1 punktum 1`(dato: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {}
            fun `§ 8-51 ledd 3`(dato: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {}
            fun `§ 8-3 ledd 1 punktum 2`(dato: LocalDate, forbrukteDager: Int, gjenståendeDager: Int) {}
        }

        internal companion object {
            internal fun ordinærRett(dato: LocalDate, forbrukteDager: Int, gjenståendeSykepengedager: Int) =
                MaksimumSykepenger(dato,forbrukteDager, dato + gjenståendeSykepengedager.ukedager, Tilstand.Under67)

            internal fun begrensetRett(dato: LocalDate, forbrukteDager: Int, alder: Alder, gjenståendeSykepengedagerOver67: Int) =
                MaksimumSykepenger(dato, forbrukteDager, if (alder.innenfor67årsgrense(dato)) alder.redusertYtelseAlder + MAKS_SYKEPENGEDAGER_OVER_67.ukedager else dato + gjenståendeSykepengedagerOver67.ukedager, Tilstand.Over67)

            internal fun absoluttSisteDagMedSykepenger(dato: LocalDate, forbrukteDager: Int, sisteDag: LocalDate) =
                MaksimumSykepenger(dato, forbrukteDager, sisteDag, Tilstand.Over70)

            internal fun minsteAv(vararg dato: MaksimumSykepenger) =
                dato.minByOrNull { it.sisteDag }!!
        }
    }
}
