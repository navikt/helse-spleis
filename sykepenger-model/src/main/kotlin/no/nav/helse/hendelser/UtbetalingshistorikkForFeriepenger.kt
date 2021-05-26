package no.nav.helse.hendelser

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilArbeidsgiver
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilPerson
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.FeriepengeutbetalingsperiodeVisitor
import no.nav.helse.person.PersonHendelse
import java.time.LocalDate
import java.time.Year
import java.util.*

class UtbetalingshistorikkForFeriepenger(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val utbetalinger: List<Utbetalingsperiode>,
    private val feriepengehistorikk: List<Feriepenger>,
    //FIXME: Internal?
    internal val opptjeningsår: Year,
    internal val skalBeregnesManuelt: Boolean,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {
    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun accept(visitor: FeriepengeutbetalingsperiodeVisitor) {
        utbetalinger.forEach { it.accept(visitor) }
    }

    internal fun utbetalteFeriepengerTilPerson() =
        feriepengehistorikk.utbetalteFeriepengerTilPerson(opptjeningsår)

    internal fun utbetalteFeriepengerTilArbeidsgiver(orgnummer: String) =
        feriepengehistorikk.utbetalteFeriepengerTilArbeidsgiver(orgnummer, opptjeningsår)

    class Feriepenger(
        val orgnummer: String,
        val beløp: Int,
        val fom: LocalDate,
        val tom: LocalDate
    ) {
        internal companion object {
            internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilPerson(opptjeningsår: Year) =
                filter { it.orgnummer.all('0'::equals) }.filter { Year.from(it.fom) == opptjeningsår.plusYears(1) }.sumBy { it.beløp }

            internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilArbeidsgiver(orgnummer: String, opptjeningsår: Year) =
                filter { it.orgnummer == orgnummer }.filter { Year.from(it.fom) == opptjeningsår.plusYears(1) }.map { it.beløp }
        }
    }

    sealed class Utbetalingsperiode(
        protected val orgnr: String,
        fom: LocalDate,
        tom: LocalDate,
        protected val beløp: Int,
        protected val utbetalt: LocalDate
    ) {
        protected val periode: Periode = fom til tom

        internal abstract fun accept(visitor: FeriepengeutbetalingsperiodeVisitor)

        class Personutbetalingsperiode(
            orgnr: String,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            utbetalt: LocalDate
        ) : Utbetalingsperiode(orgnr, fom, tom, beløp, utbetalt) {
            override fun accept(visitor: FeriepengeutbetalingsperiodeVisitor) {
                visitor.visitPersonutbetalingsperiode(orgnr, periode, beløp, utbetalt)
            }
        }

        class Arbeidsgiverutbetalingsperiode(
            orgnr: String,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            utbetalt: LocalDate
        ) : Utbetalingsperiode(orgnr, fom, tom, beløp, utbetalt) {
            override fun accept(visitor: FeriepengeutbetalingsperiodeVisitor) {
                visitor.visitArbeidsgiverutbetalingsperiode(orgnr, periode, beløp, utbetalt)
            }
        }
    }
}
