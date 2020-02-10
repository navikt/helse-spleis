package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.util.*

class ModelSykepengehistorikk(
    private val utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val aktivitetslogger: Aktivitetslogger
) {

    private val sisteFraværsdag: LocalDate? = utbetalinger.maxBy { it.tom }?.tom

    internal fun utbetalingslinjer(): List<Utbetalingslinje> =
        utbetalinger.map { it.utbetalingslinjer(aktivitetslogger) }

    internal fun sisteFraværsdag() = sisteFraværsdag

    internal fun valider(): Aktivitetslogger {
        utbetalinger.forEach { it.valider(this, aktivitetslogger) }
        inntektshistorikk.forEach { it.valider(aktivitetslogger) }
        return aktivitetslogger
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Sykepengehistorikk")
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }

    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String
    ) {

        internal fun valider(aktivitetslogger: Aktivitetslogger) {
            if (orgnummer.isBlank()) {
                aktivitetslogger.error("Orgnummer må være satt: $orgnummer")
            }
        }

        internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
            inntekthistorikk.add(sykepengerFom, hendelseId, inntektPerMåned.toBigDecimal())
        }
    }

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate, internal val dagsats: Int) {
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
        class ReduksjonArbeidsgiverRefusjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Tilbakeført(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Konvertert(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ferie(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Opphold(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Sanksjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ukjent(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
    }
}
