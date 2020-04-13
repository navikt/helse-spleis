package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class Utbetalingshistorikk(
    private val utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val graderingsliste: List<Graderingsperiode>,
    private val aktivitetslogg: Aktivitetslogg
) {

    internal fun utbetalingstidslinje(førsteFraværsdag: LocalDate?) = this.utbetalinger
        .filtrerUtbetalinger(førsteFraværsdag)
        .map { it.toTidslinje(graderingsliste, aktivitetslogg) }
        .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun valider(arbeidsgivertidslinje: Sykdomstidslinje, førsteFraværsdag: LocalDate?): Aktivitetslogg {
        utbetalinger.filtrerUtbetalinger(førsteFraværsdag)
            .onEach { it.valider(aktivitetslogg) }
            .maxBy { it.tom }
            ?.tom
            ?.also { sisteUtbetalteDag ->
                val førsteDag = arbeidsgivertidslinje.førsteDag()
                if (sisteUtbetalteDag >= førsteDag)
                    aktivitetslogg.error("Hele eller deler av perioden til arbeidsgiver er utbetalt i Infotrygd")
                else if (sisteUtbetalteDag >= førsteDag.minusDays(18))
                    aktivitetslogg.error("Har utbetalt periode i Infotrygd nærmere enn 18 dager fra første dag")
            }
        inntektshistorikk.forEach { it.valider(aktivitetslogg) }
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }
    }

    private fun List<Periode>.filtrerUtbetalinger(førsteFraværsdag: LocalDate?): List<Periode> {
        if (førsteFraværsdag == null) return this
        var forrigePeriodeFom: LocalDate = førsteFraværsdag
        return sortedByDescending { it.tom }.filter { periode ->
            (ChronoUnit.DAYS.between(periode.tom, forrigePeriodeFom) <= (26 * 7)).also {
                if (it && periode.fom < forrigePeriodeFom) forrigePeriodeFom = periode.fom
            }
        }
    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String
    ) {

        internal fun valider(aktivitetslogg: Aktivitetslogg) {
            if (orgnummer.isBlank()) {
                aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            }
        }

        internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
            inntekthistorikk.add(sykepengerFom, hendelseId, inntektPerMåned.toBigDecimal())
        }
    }

    class Graderingsperiode(private val fom: LocalDate, private val tom: LocalDate, internal val grad: Double) {
        internal fun datoIPeriode(dato: LocalDate) =
            dato.isAfter(fom.minusDays(1)) && dato.isBefore(tom.plusDays(1))
    }

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate, internal val dagsats: Int) {
        internal open fun toTidslinje(
            graderingsliste: List<Graderingsperiode>,
            aktivitetslogg: Aktivitetslogg
        ) = Utbetalingstidslinje()

        open fun valider(aktivitetslogg: Aktivitetslogg) {
            if (fom > tom) aktivitetslogg.error(
                "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                this::class.simpleName
            )
        }

        protected fun List<Graderingsperiode>.finnGradForUtbetalingsdag(dag: LocalDate) =
            this.find { it.datoIPeriode(dag) }?.grad ?: Double.NaN

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int
        ) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply {
                    fom.datesUntil(tom.plusDays(1)).forEach {
                        if (it.erHelg()) this.addHelg(
                            0.0,
                            it,
                            graderingsliste.finnGradForUtbetalingsdag(it)
                        ) else this.addNAVdag(dagsats.toDouble(), it, graderingsliste.finnGradForUtbetalingsdag(it))
                    }
                }
        }

        class ReduksjonArbeidsgiverRefusjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply {
                    fom.datesUntil(tom.plusDays(1)).forEach {
                        if (it.erHelg()) this.addHelg(
                            0.0,
                            it,
                            graderingsliste.finnGradForUtbetalingsdag(it)
                        ) else this.addNAVdag(dagsats.toDouble(), it, graderingsliste.finnGradForUtbetalingsdag(it))
                    }
                }
        }

        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply {
                    fom.datesUntil(tom.plusDays(1)).forEach {
                        if (it.erHelg()) this.addHelg(
                            0.0,
                            it,
                            graderingsliste.finnGradForUtbetalingsdag(it)
                        ) else this.addNAVdag(dagsats.toDouble(), it, graderingsliste.finnGradForUtbetalingsdag(it))
                    }
                }
        }

        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply {
                    fom.datesUntil(tom.plusDays(1)).forEach {
                        if (it.erHelg()) this.addHelg(
                            0.0,
                            it,
                            graderingsliste.finnGradForUtbetalingsdag(it)
                        ) else this.addNAVdag(dagsats.toDouble(), it, graderingsliste.finnGradForUtbetalingsdag(it))
                    }
                }
        }

        class Ferie(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply {
                    fom.datesUntil(tom.plusDays(1)).forEach {
                        this.addFridag(dagsats.toDouble(), it)
                    }
                }
        }

        class Etterbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class KontertRegnskap(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class Tilbakeført(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class Konvertert(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class Opphold(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class Sanksjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun valider(aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.info(
                    "Utbetalingsperioden %s fra Infotrygd har en FOM etter TOM",
                    this::class.simpleName
                )
            }
        }

        class Ukjent(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun toTidslinje(
                graderingsliste: List<Graderingsperiode>,
                aktivitetslogg: Aktivitetslogg
            ): Utbetalingstidslinje {
                aktivitetslogg.severe("Kan ikke hente ut utbetalingslinjer for perioden %s", this::class.simpleName)
            }

            override fun valider(aktivitetslogg: Aktivitetslogg) {
                aktivitetslogg.warn(
                    "Det er en utbetalingsperiode som er lagt inn i Infotrygd uten at inntektsopplysninger er registrert.",
                    this::class.simpleName
                )
            }
        }

        class Ugyldig(fom: LocalDate?, tom: LocalDate?, dagsats: Int) :
            Periode(fom ?: LocalDate.MIN, tom ?: LocalDate.MAX, dagsats) {
            override fun toTidslinje(
                graderingsliste: List<Graderingsperiode>,
                aktivitetslogg: Aktivitetslogg
            ): Utbetalingstidslinje {
                aktivitetslogg.severe("Kan ikke hente ut utbetalingslinjer for perioden %s", this::class.simpleName)
            }

            override fun valider(aktivitetslogg: Aktivitetslogg) {
                aktivitetslogg.warn(
                    "Det er en utbetalingsperiode i Infotrygd som mangler fom- eller tomdato",
                    this::class.simpleName
                )
            }
        }
    }
}
