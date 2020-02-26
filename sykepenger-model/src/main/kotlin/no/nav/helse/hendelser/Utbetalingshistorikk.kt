package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.util.*

class Utbetalingshistorikk(
    private val utbetalinger: List<Periode>,
    private val ukjentePerioder: List<JsonNode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val aktivitetslogg: Aktivitetslogg
) {

    private val sisteFraværsdag: LocalDate? = utbetalinger.maxBy { it.tom }?.tom

    internal fun utbetalingslinjer(): List<Utbetalingslinje> =
        utbetalinger.map { it.utbetalingslinjer(aktivitetslogg) }

    internal fun sisteFraværsdag() = sisteFraværsdag

    internal fun valider(): Aktivitetslogg {
        utbetalinger.forEach { it.valider(this, aktivitetslogg) }
        inntektshistorikk.forEach { it.valider(aktivitetslogg) }
        if (ukjentePerioder.isNotEmpty()) { aktivitetslogg.error("Utbetalingshistorikk fra Infotrygd inneholder perioder vi ikke klarer å tolke") }
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }
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

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate, internal val dagsats: Int) {
        open fun utbetalingslinjer(aktivitetslogg: Aktivitetslogg): Utbetalingslinje {
            aktivitetslogg.severe("Kan ikke hente ut utbetalingslinjer for perioden %s", this::class.simpleName)
        }

        open fun valider(historikk: Utbetalingshistorikk, aktivitetslogg: Aktivitetslogg) {
            aktivitetslogg.error("Utbetalingsperioden %s (fra Infotrygd) er ikke støttet", this::class.simpleName)
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int
        ) : Periode(fom, tom, dagsats) {
            override fun utbetalingslinjer(aktivitetslogg: Aktivitetslogg): Utbetalingslinje {
                return Utbetalingslinje(fom, tom, dagsats)
            }

            override fun valider(historikk: Utbetalingshistorikk, aktivitetslogg: Aktivitetslogg) {
                if (fom > tom) aktivitetslogg.error("Utbetalingsperiode fra Infotrygd har en FOM etter TOM")
            }
        }

        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
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
