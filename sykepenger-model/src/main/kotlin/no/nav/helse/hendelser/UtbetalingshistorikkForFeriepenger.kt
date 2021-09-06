package no.nav.helse.hendelser

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode.Companion.kodeForDato
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilArbeidsgiver
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilPerson
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.FeriepengeutbetalingsperiodeVisitor
import no.nav.helse.person.PersonHendelse
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Year
import java.util.*

class UtbetalingshistorikkForFeriepenger(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val utbetalinger: List<Utbetalingsperiode>,
    private val feriepengehistorikk: List<Feriepenger>,
    private val arbeidskategorikoder: Arbeidskategorikoder,
    internal val opptjeningsår: Year,
    internal val skalBeregnesManuelt: Boolean,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    internal fun accept(visitor: FeriepengeutbetalingsperiodeVisitor) {
        utbetalinger.forEach { it.accept(visitor) }
    }

    internal fun utbetalteFeriepengerTilPerson() =
        feriepengehistorikk.utbetalteFeriepengerTilPerson(opptjeningsår)

    internal fun utbetalteFeriepengerTilArbeidsgiver(orgnummer: String) =
        feriepengehistorikk.utbetalteFeriepengerTilArbeidsgiver(orgnummer, opptjeningsår)

    internal fun harRettPåFeriepenger(dato: LocalDate, orgnummer: String) = arbeidskategorikoder.harRettPåFeriepenger(dato, orgnummer)

    internal fun sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver: (String) -> Unit) {
        utbetalinger.forEach { it.sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver) }
    }

    class Feriepenger(
        val orgnummer: String,
        val beløp: Int,
        val fom: LocalDate,
        val tom: LocalDate
    ) {
        internal companion object {
            internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilPerson(opptjeningsår: Year) =
                filter { it.orgnummer.all('0'::equals) }.filter { Year.from(it.fom) == opptjeningsår.plusYears(1) }.map { it.beløp }

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

        internal fun sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver: (String) -> Unit) {
            opprettManglendeArbeidsgiver(orgnr)
        }

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

    class Arbeidskategorikoder(
        private val arbeidskategorikoder: List<KodePeriode>
    ) {
        internal fun harRettPåFeriepenger(dato: LocalDate, orgnummer: String) = arbeidskategorikoder.kodeForDato(dato).girRettTilFeriepenger(orgnummer)

        class KodePeriode(
            private val periode: Periode,
            private val arbeidskategorikode: Arbeidskategorikode
        ) {
            companion object {
                internal fun List<KodePeriode>.kodeForDato(dato: LocalDate) =
                    first { dato in it.periode }.arbeidskategorikode
            }
        }

        enum class Arbeidskategorikode(private val kode: String, internal val girRettTilFeriepenger: (String) -> Boolean) {
            Arbeidstaker("01", { true }),
            ArbeidstakerSelvstendig("03", { it != "0" }),
            Sjømenn("04", { true }),
            Befal("08", { true }),
            MenigKorporal("09", { true }),
            ArbeidstakerSjømenn("10", { true }),
            SvalbardArbeidere("12", { true }),
            ArbeidstakerJordbruker("13", { it != "0" }),
            Yrkesskade("14", { true }),
            AmbassadePersonell("15", { true }),
            ArbeidstakerFisker("17", { it != "0" }),
            ArbeidstakerOppdragstaker("20", { it != "0" }),
            FFU22("22", { true }),
            ArbeidstakerALøyse("23", { it != "0" }),
            ArbOppdragstakerUtenForsikring("25", { it != "0" }),
            FiskerHyre("27", { true }),

            Fisker("00", { false }),
            Selvstendig("02", { false }),
            Jordbruker("05", { false }),
            Arbeidsledig("06", { false }),
            Inaktiv("07", { false }),
            Turnuskandidater("11", { false }),
            Reindrift("16", { false }),
            IkkeIBruk("18", { false }),
            Oppdragstaker("19", { false }),
            FFU21("21", { false }),
            OppdragstakerUtenForsikring("24", { false }),
            SelvstendigDagmammaDagpappa("26", { false }),

            InntektsopplysningerMangler("99", { false }),
            Tom("", { false });

            companion object {
                fun finn(kode: String) = values().firstOrNull { it.kode.trim() == kode.trim() } ?: run {
                    sikkerLogg.info("Ukjent arbeidskategorikode $kode")
                    Tom
                }
            }
        }
    }
}
