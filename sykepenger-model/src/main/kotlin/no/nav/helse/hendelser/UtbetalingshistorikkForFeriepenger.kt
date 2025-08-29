package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import no.nav.helse.erHelg
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Kilde
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Mottaker
import no.nav.helse.feriepenger.Feriepengegrunnlagstidslinje
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode.Companion.kodeForDato
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilArbeidsgiver
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger.Companion.utbetalteFeriepengerTilPerson

class UtbetalingshistorikkForFeriepenger(
    meldingsreferanseId: MeldingsreferanseId,
    private val utbetalinger: List<Utbetalingsperiode>,
    private val feriepengehistorikk: List<Feriepenger>,
    private val arbeidskategorikoder: Arbeidskategorikoder,
    internal val opptjeningsår: Year,
    internal val skalBeregnesManuelt: Boolean,
    internal val datoForSisteFeriepengekjøringIInfotrygd: LocalDate
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun utbetalteFeriepengerTilPerson() =
        feriepengehistorikk.utbetalteFeriepengerTilPerson(opptjeningsår)

    internal fun utbetalteFeriepengerTilArbeidsgiver(orgnummer: String) =
        feriepengehistorikk.utbetalteFeriepengerTilArbeidsgiver(orgnummer, opptjeningsår)

    internal fun harRettPåFeriepenger(dato: LocalDate, orgnummer: String) = arbeidskategorikoder.harRettPåFeriepenger(dato, orgnummer)

    internal fun sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver: (String) -> Unit) {
        utbetalinger.forEach { it.sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver) }
    }

    private fun erUtbetaltEtterFeriepengekjøringIT(sisteKjøringIInfotrygd: LocalDate, utbetalt: LocalDate) = sisteKjøringIInfotrygd <= utbetalt

    internal fun grunnlagForFeriepenger(sisteKjøringIInfotrygd: LocalDate): Feriepengegrunnlagstidslinje {
        return Feriepengegrunnlagstidslinje.Builder().apply {
            utbetalinger
                .filterNot { dag -> erUtbetaltEtterFeriepengekjøringIT(sisteKjøringIInfotrygd, dag.utbetalt) }
                .forEach { dag ->
                    dag.periode
                        .filterNot { it.erHelg() }
                        .filter { harRettPåFeriepenger(it, dag.orgnr) }
                        .forEach { dato ->
                            when (dag) {
                                is Utbetalingsperiode.Arbeidsgiverutbetalingsperiode -> leggTilUtbetaling(dato, dag.orgnr, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, dag.beløp)
                                is Utbetalingsperiode.Personutbetalingsperiode -> leggTilUtbetaling(dato, dag.orgnr, Mottaker.PERSON, Kilde.INFOTRYGD, dag.beløp)
                            }
                        }
                }
        }.build()
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
        val orgnr: String,
        fom: LocalDate,
        tom: LocalDate,
        val beløp: Int,
        val utbetalt: LocalDate
    ) {
        val periode: Periode = fom til tom

        internal fun sikreAtArbeidsgivereEksisterer(opprettManglendeArbeidsgiver: (String) -> Unit) {
            opprettManglendeArbeidsgiver(orgnr)
        }

        class Personutbetalingsperiode(
            orgnr: String,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            utbetalt: LocalDate
        ) : Utbetalingsperiode(orgnr, fom, tom, beløp, utbetalt)

        class Arbeidsgiverutbetalingsperiode(
            orgnr: String,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            utbetalt: LocalDate
        ) : Utbetalingsperiode(orgnr, fom, tom, beløp, utbetalt)
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
                fun finn(kode: String) = entries.firstOrNull { it.kode.trim() == kode.trim() } ?: Tom
            }
        }
    }
}
