package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.sykdomstidslinje.harTilstøtende
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

class Utbetalingshistorikk(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(aktivitetslogg) {
    private val utbetalinger = Periode.sorter(utbetalinger)

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun valider(periode: no.nav.helse.hendelser.Periode): Aktivitetslogg {
        Periode.Utbetalingsperiode.valider(utbetalinger, aktivitetslogg, periode, organisasjonsnummer)
        Inntektsopplysning.valider(inntektshistorikk, aktivitetslogg, periode)
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, organisasjonsnummer, inntekthistorikk) }
    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String,
        private val refusjonTilArbeidsgiver: Boolean,
        private val refusjonTom: LocalDate? = null
    ) {

        internal companion object {
            fun valider(
                liste: List<Inntektsopplysning>,
                aktivitetslogg: Aktivitetslogg,
                periode: no.nav.helse.hendelser.Periode
            ) {
                liste
                    .filter { it.sykepengerFom >= periode.start.minusMonths(12) }
                    .distinctBy { it.orgnummer }
                    .onEach { it.valider(aktivitetslogg, periode) }
                    .also {
                        if (it.size > 1) aktivitetslogg.error("Har inntekt fra flere arbeidsgivere i Infotrygd innen 12 måneder fra perioden")
                    }
            }
        }

        internal fun valider(aktivitetslogg: Aktivitetslogg, periode: no.nav.helse.hendelser.Periode) {
            if (orgnummer.isBlank()) aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            if (refusjonTom != null && periode.etter(refusjonTom)) aktivitetslogg.error("Refusjon fra Infotrygd opphører i eller før perioden")
            if (!refusjonTilArbeidsgiver) aktivitetslogg.error("Utbetaling skal gå rett til bruker")
        }

        internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntekthistorikk: Inntekthistorikk) {
            if (organisasjonsnummer != orgnummer) return
            inntekthistorikk.add(
                sykepengerFom.minusDays(1), // Assuming salary is the day before the first sykedag
                hendelseId,
                inntektPerMåned.toBigDecimal()
            )
        }
    }

    internal fun append(oldtid: Oldtidsutbetalinger) {
        utbetalinger.forEach {
            it.append(oldtid)
        }
    }

    sealed class Periode(fom: LocalDate, tom: LocalDate) {
        internal companion object {
            fun sorter(liste: List<Periode>) = liste.sortedBy { it.periode.start }
        }

        protected val periode = no.nav.helse.hendelser.Periode(fom, tom)

        internal open fun tidslinje() = Utbetalingstidslinje()

        internal open fun append(oldtid: Oldtidsutbetalinger) {}

        internal open fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
            if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
        }

        abstract class Utbetalingsperiode(
            fom: LocalDate,
            tom: LocalDate,
            private val beløp: Int,
            private val grad: Int,
            private val orgnr: String
        ) : Periode(fom, tom) {
            private val ugradertBeløp = ((beløp * 100) / grad.toDouble()).roundToInt()
            private val maksDagsats = Grunnbeløp.`6G`.dagsats(fom) == ugradertBeløp

            override fun tidslinje() = Utbetalingstidslinje().apply {
                periode.forEach { dag(this, it, grad.toDouble()) }
            }

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Double) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(0))
                else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(ugradertBeløp))
            }

            internal companion object {
                fun valider(
                    liste: List<Periode>,
                    aktivitetslogg: Aktivitetslogg,
                    periode: no.nav.helse.hendelser.Periode,
                    organisasjonsnummer: String
                ): Aktivitetslogg {
                    if (liste.harTilstøtendePeriodeFraAnnenArbeidsgiver(periode, organisasjonsnummer)) {
                        aktivitetslogg.error("Det finnes en tilstøtende utbetalt periode i Infotrygd med et annet organisasjonsnummer enn denne vedtaksperioden.")
                        return aktivitetslogg
                    }

                    liste.onEach { it.valider(aktivitetslogg, periode) }
                    if (liste.harHistoriskeSammenhengendePerioderMedEndring())
                        aktivitetslogg.warn(
                            "Dagsatsen har endret seg minst én gang i en historisk, sammenhengende periode i Infotrygd.%s",
                            if (liste.harTilstøtende(periode)) " Direkte overgang fra Infotrygd; kontroller at sykepengegrunnlaget er riktig." else ""
                        )
                    return aktivitetslogg
                }

                private fun List<Periode>.harTilstøtende(periode: no.nav.helse.hendelser.Periode) =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .any { it.periode.endInclusive.harTilstøtende(periode.start) }

                private fun List<Periode>.harTilstøtendePeriodeFraAnnenArbeidsgiver(periode: no.nav.helse.hendelser.Periode, organisasjonsnummer: String) =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .filter { it.periode.endInclusive.harTilstøtende(periode.start) }
                        .any { it.orgnr != organisasjonsnummer }

                private fun List<Periode>.harHistoriskeSammenhengendePerioderMedEndring() =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .zipWithNext { left, right -> erTilstøtendeMedEndring(left, right) }
                        .any { it }

                private fun erTilstøtendeMedEndring(left: Utbetalingsperiode, right: Utbetalingsperiode) =
                    left.periode.endInclusive.harTilstøtende(right.periode.start) && left.ugradertBeløp != right.ugradertBeløp && !left.maksDagsats && !right.maksDagsats
            }
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int,
            private val orgnummer: String
        ) : Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(orgnummer, tidslinje())
            }
        }

        class ReduksjonArbeidsgiverRefusjon(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int,
            private val orgnummer: String
        ) : Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(orgnummer, tidslinje())
            }
        }

        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int, orgnummer: String) :
            Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int, orgnummer: String) :
            Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun tidslinje() = Utbetalingstidslinje()
                .apply { periode.forEach { addFridag(it, Økonomi.ikkeBetalt().inntekt(0)) } }

            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {}
        }

        class KontertRegnskap(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Etterbetaling(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Tilbakeført(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Konvertert(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Sanksjon(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Opphold(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Ukjent(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
                if (periode.endInclusive < other.start.minusDays(18)) return
                aktivitetslogg.warn(
                    "Perioden er lagt inn i Infotrygd - men mangler inntektsopplysninger. Fjern perioden fra SP UB hvis du utbetaler via speil.",
                    this::class.simpleName
                )
            }
        }

        class Ugyldig(private val fom: LocalDate?, private val tom: LocalDate?) :
            IgnorertPeriode(LocalDate.MIN, LocalDate.MAX) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
                val tekst = when {
                    fom == null || tom == null -> "mangler fom- eller tomdato"
                    fom > tom -> "fom er nyere enn tom"
                    else -> null
                }
                aktivitetslogg.error(
                    "Det er en ugyldig utbetalingsperiode i Infotrygd%s",
                    tekst?.let { " ($it)" } ?: "")
            }
        }
    }
}
