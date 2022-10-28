package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.util.Objects
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_IT_1
import no.nav.helse.person.Varselkode.RV_IT_10
import no.nav.helse.person.Varselkode.RV_IT_3
import no.nav.helse.person.Varselkode.RV_IT_4
import no.nav.helse.person.Varselkode.RV_IT_6
import no.nav.helse.person.Varselkode.RV_IT_7
import no.nav.helse.person.Varselkode.RV_IT_8
import no.nav.helse.person.Varselkode.RV_IT_9
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
        if (dato.erHelg()) return builder.addHelg(dato, økonomi.inntekt(INGEN, skjæringstidspunkt = dato, `6G` = INGEN))
        builder.addNAVdag(dato, økonomi.inntekt(inntekt, skjæringstidspunkt = dato, `6G` = INGEN).arbeidsgiverRefusjon(INGEN))
    }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        organisasjonsnummer: String,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        nødnummer: Nødnummer
    ) {
        validerOverlapp(aktivitetslogg, periode)
        validerNødnummerbruk(aktivitetslogg, skjæringstidspunkt, nødnummer)
        validerNyereOpplysninger(aktivitetslogg, organisasjonsnummer, periode)
    }

    private fun validerNødnummerbruk(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, nødnummer: Nødnummer) {
        if (this.endInclusive < skjæringstidspunkt) return
        if (orgnr !in nødnummer) return
        aktivitetslogg.funksjonellFeil(RV_IT_4)
    }
    private fun validerOverlapp(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        if (!overlapperMed(periode)) return
        aktivitetslogg.info("Utbetaling i Infotrygd %s til %s overlapper med vedtaksperioden", start, endInclusive)
        aktivitetslogg.funksjonellFeil(RV_IT_3)
    }

    private fun validerNyereOpplysninger(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String, periode: Periode) {
        if (!gjelder(organisasjonsnummer)) return
        if (this.start <= periode.endInclusive) return
        aktivitetslogg.funksjonellFeil(RV_IT_1)
    }

    override fun gjelder(nødnummer: Nødnummer) = this.orgnr in nødnummer
    override fun gjelder(orgnummer: String) = orgnummer == this.orgnr

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
}

data class UgyldigPeriode(
    private val fom: LocalDate?,
    private val tom: LocalDate?,
    private val utbetalingsgrad: Int?
) {
    internal fun valider(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.funksjonellFeil(feiltekst())
    }

    private fun feiltekst(): Varselkode = when {
        fom == null || tom == null -> RV_IT_6
        fom > tom -> RV_IT_7
        utbetalingsgrad == null -> RV_IT_8
        utbetalingsgrad <= 0 -> RV_IT_9
        else -> RV_IT_10
    }

    internal fun toMap() = mapOf<String, Any?>(
        "fom" to fom,
        "tom" to tom,
        "utbetalingsgrad" to utbetalingsgrad
    )
}
