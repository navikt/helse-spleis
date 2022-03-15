package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.util.Objects
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi

abstract class Utbetalingsperiode(
    protected val orgnr: String,
    fom: LocalDate,
    tom: LocalDate,
    protected val grad: Prosentdel,
    protected val inntekt: Inntekt
) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: SykdomstidslinjeHendelse.Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.sykedager(start, endInclusive, grad, kilde)
    }

    override fun utbetalingstidslinje() =
        Utbetalingstidslinje.Builder().apply {
            this@Utbetalingsperiode.forEach { dag -> nyDag(this, dag) }
        }.build()

    private fun nyDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        val økonomi = Økonomi.sykdomsgrad(grad)
        if (dato.erHelg()) return builder.addHelg(dato, økonomi.inntekt(Inntekt.INGEN, skjæringstidspunkt = dato))
        val refusjon = if (!harBrukerutbetaling()) inntekt else Inntekt.INGEN
        builder.addNAVdag(dato, økonomi.inntekt(inntekt, skjæringstidspunkt = dato).arbeidsgiverRefusjon(refusjon))
    }

    override fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode, skjæringstidspunkt: LocalDate, nødnummer: Nødnummer) {
        validerOverlapp(aktivitetslogg, periode)
        if (aktivitetslogg.hasErrorsOrWorse() || this.endInclusive < skjæringstidspunkt) return
        validerRelevant(aktivitetslogg, nødnummer)
    }

    private fun validerRelevant(aktivitetslogg: IAktivitetslogg, nødnummer: Nødnummer) {
        if (orgnr in nødnummer) aktivitetslogg.error("Det er registrert utbetaling på nødnummer")
    }

    override fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        if (!overlapperMed(periode)) return
        aktivitetslogg.info("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", start, endInclusive)
        aktivitetslogg.error("Utbetaling i Infotrygd overlapper med vedtaksperioden")
    }

    override fun gjelder(nødnummer: Nødnummer) = this.orgnr in nødnummer
    override fun gjelder(orgnummer: String) = orgnummer == this.orgnr
    override fun utbetalingEtter(orgnumre: List<String>, dato: LocalDate) =
        start >= dato && this.orgnr !in orgnumre

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        other as Utbetalingsperiode
        return this.orgnr == other.orgnr && this.start == other.start && this.grad == other.grad && this.inntekt == other.inntekt
    }

    override fun hashCode() = Objects.hash(orgnr, start, endInclusive, grad, inntekt, this::class)
}

class ArbeidsgiverUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate, grad: Prosentdel, inntekt: Inntekt) :
    Utbetalingsperiode(orgnr, fom, tom, grad, inntekt.rundTilDaglig()) {

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(orgnr, this, grad, inntekt)
    }
}

class PersonUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate, grad: Prosentdel, inntekt: Inntekt) :
    Utbetalingsperiode(orgnr, fom, tom, grad, inntekt.rundTilDaglig()) {

    override fun accept(visitor: InfotrygdhistorikkVisitor) {
        visitor.visitInfotrygdhistorikkPersonUtbetalingsperiode(orgnr, this, grad, inntekt)
    }

    override fun harBrukerutbetaling() = true
}

data class UgyldigPeriode(
    private val fom: LocalDate?,
    private val tom: LocalDate?,
    private val utbetalingsgrad: Int?
) {
    internal fun valider(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.error("Det er en ugyldig utbetalingsperiode i Infotrygd%s", feiltekst()?.let { " ($it)" } ?: "")
    }

    private fun feiltekst() = when {
        fom == null || tom == null -> "mangler fom- eller tomdato"
        fom > tom -> "fom er nyere enn tom"
        utbetalingsgrad == null -> "utbetalingsgrad mangler"
        utbetalingsgrad <= 0 -> "utbetalingsgrad er mindre eller lik 0"
        else -> null
    }

    internal fun toMap() = mapOf<String, Any?>(
        "fom" to fom,
        "tom" to tom,
        "utbetalingsgrad" to utbetalingsgrad
    )
}
