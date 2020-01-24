package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import java.time.LocalDate

class ModelSykepengehistorikk(
    private val utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val aktivitetslogger: Aktivitetslogger
) {

    private val sisteFraværsdag: LocalDate? = utbetalinger.maxBy { it.tom }?.tom

    internal fun utbetalingslinjer(): List<Utbetalingslinje> = utbetalinger.map { it.utbetalingslinjer(aktivitetslogger) }
    internal fun sisteFraværsdag() = sisteFraværsdag
    internal fun inntektsopplysninger() = inntektshistorikk

    internal fun valider(): Aktivitetslogger {
        utbetalinger.forEach { it.valider(this, aktivitetslogger) }
        inntektshistorikk.forEach {it.valider(this, aktivitetslogger) }
        return aktivitetslogger
    }

    class Inntektsopplysning(
        val sykepengerFom: LocalDate,
        val inntektPerMåned: Int,
        val orgnummer: String
    ) {
        fun valider(modelSykepengehistorikk: ModelSykepengehistorikk, aktivitetslogger: Aktivitetslogger) {
            if (orgnummer.isBlank()) { aktivitetslogger.error("Orgnummer må være satt: $orgnummer")}
        }
    }

    sealed class Periode(val fom: LocalDate, val tom: LocalDate, val dagsats: Int) {
        open fun utbetalingslinjer(aktivitetslogger: Aktivitetslogger): Utbetalingslinje {
            aktivitetslogger.severe("Kan ikke hente ut utbetaligslinjer for denne periodetypen")
        }
        open fun valider(historikk: ModelSykepengehistorikk, aktivitetslogger: Aktivitetslogger) {
            aktivitetslogger.error("Perioden er ikke støttet")
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int
        ) : Periode(fom, tom, dagsats) {
            override fun utbetalingslinjer(aktivitetslogger: Aktivitetslogger): Utbetalingslinje {
                return Utbetalingslinje(fom, tom, dagsats)
            }

            override fun valider(historikk: ModelSykepengehistorikk, aktivitetslogger: Aktivitetslogger) {
                if (fom > tom) aktivitetslogger.error("Utbetalingsperioder kan ikke ha en FOM etter TOM")
            }
        }

        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Etterbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class KontertRegnskap(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class ReduksjonArbRef(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Tilbakeført(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Konvertert(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ferie(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Opphold(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Sanksjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ukjent(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
    }
}
