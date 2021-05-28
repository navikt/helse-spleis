package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.reflection.OppdragReflect
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import kotlin.math.roundToInt

internal class Feriepengeutbetaling private constructor(
    private val feriepengeberegner: Feriepengeberegner,
    private val infotrygdFeriepengebeløpPerson: Double,
    private val infotrygdFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpArbeidsgiver: Double,
    private val oppdrag: Oppdrag,
    val utbetalingId: UUID
) {
    var overføringstidspunkt: LocalDateTime? = null
    var avstemmingsnøkkel: Long? = null

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        fun List<Feriepengeutbetaling>.gjelderFeriepengeutbetaling(hendelse: UtbetalingHendelse) = any { hendelse.erRelevant(it.oppdrag.fagsystemId()) }
    }

    private val observers = mutableListOf<UtbetalingObserver>()

    internal fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel,
            utbetalingId
        )
        feriepengeberegner.accept(visitor)
        oppdrag.accept(visitor)
        visitor.postVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel,
        )
    }

    fun håndter(utbetalingHendelse: UtbetalingHendelse, person: Person) {
        if (!utbetalingHendelse.erRelevant(oppdrag.fagsystemId())) return

        utbetalingHendelse.info("Behandler svar fra Oppdrag/UR/spenn for feriepenger ")
        utbetalingHendelse.valider()
        val utbetaltOk = !utbetalingHendelse.hasErrorsOrWorse()
        lagreInformasjon(utbetalingHendelse, utbetaltOk)

        if (!utbetaltOk) {
            sikkerLogg.info("Utbetaling av feriepenger med fagsystemId fagsystemId() feilet.")
            return
        }

        person.feriepengerUtbetalt(
            PersonObserver.FeriepengerUtbetaltEvent(
                arbeidsgiverOppdrag = OppdragReflect(oppdrag).toMap(),
            )
        )

        person.utbetalingEndret(
            PersonObserver.UtbetalingEndretEvent(
                utbetalingId = utbetalingId,
                type = Utbetaling.Utbetalingtype.FERIEPENGER.name,
                arbeidsgiverOppdrag = OppdragReflect(oppdrag).toBehovMap(),
                personOppdrag = OppdragReflect(Oppdrag(utbetalingHendelse.fødselsnummer(), Fagområde.SykepengerRefusjon)).toBehovMap(),
                forrigeStatus = Utbetalingstatus.fraTilstand(Utbetaling.Ubetalt).name,
                gjeldendeStatus = Utbetalingstatus.fraTilstand(Utbetaling.Utbetalt).name
            )
        )
    }

    private fun lagreInformasjon(hendelse: UtbetalingHendelse, gikkBra: Boolean) {
        overføringstidspunkt = hendelse.overføringstidspunkt
        avstemmingsnøkkel = hendelse.avstemmingsnøkkel
        hendelse.info("Data for feriepenger fra Oppdrag/UR: tidspunkt: $overføringstidspunkt, avstemmingsnøkkel $avstemmingsnøkkel og utbetalt ok: ${if (gikkBra) "ja" else "nei"}")
    }

    internal fun overfør(aktivitetslogg: Aktivitetslogg) {
        oppdrag.overfør(aktivitetslogg, null, "SPLEIS")
    }

    internal class Builder(
        private val aktørId: String,
        private val orgnummer: String,
        private val feriepengeberegner: Feriepengeberegner,
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
    ) {
        internal fun build(): Feriepengeutbetaling {
            val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)

            if (hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() != 0 &&
                hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() !in infotrygdHarUtbetaltTilArbeidsgiver
            ) {
                sikkerLogg.info(
                    """
                    Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp
                    AktørId: $aktørId
                    Arbeidsgiver: $orgnummer
                    Infotrygd har utbetalt $infotrygdHarUtbetaltTilArbeidsgiver
                    Vi har beregnet at infotrygd har utbetalt ${hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt()}
                    """.trimIndent()
                )
            }

            val infotrygdFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForInfotrygdPerson(orgnummer)
            val infotrygdFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer)
            val spleisFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForSpleis(orgnummer)

            val infotrygdFeriepengebeløpPersonUtenPersonhack = feriepengeberegner.beregnFeriepengerForInfotrygdPersonUtenPersonhack(orgnummer)

            if (infotrygdFeriepengebeløpPerson != infotrygdFeriepengebeløpPersonUtenPersonhack) {
                sikkerLogg.info(
                    """
                    Feriepengebeløp utbetalt til person er forskjellig fra beløpet som skulle vært utbetalt til person etter ordinære regler
                    AktørId: $aktørId
                    Arbeidsgiver: $orgnummer
                    Faktisk utbetalt til person: $infotrygdFeriepengebeløpPerson
                    Burde egentlig vært utbetalt: $infotrygdFeriepengebeløpPersonUtenPersonhack
                    """.trimIndent()
                )
            }

            val totaltFeriepengebeløpArbeidsgiver: Double = feriepengeberegner.beregnFeriepengerForArbeidsgiver(orgnummer)
            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Double = feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(orgnummer)

            val oppdrag = Oppdrag(
                mottaker = orgnummer,
                fagområde = Fagområde.SykepengerRefusjon,
                linjer = listOf(
                    Utbetalingslinje(
                        fom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atDay(1),
                        tom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atEndOfMonth(),
                        satstype = Satstype.ENG,
                        beløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd.roundToInt(),
                        aktuellDagsinntekt = null,
                        grad = null,
                        klassekode = Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig
                    )
                ),
                sisteArbeidsgiverdag = null
            )

            sikkerLogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                AktørId: $aktørId
                Arbeidsgiver: $orgnummer
                IT har utbetalt til arbeidsgiver: $infotrygdHarUtbetaltTilArbeidsgiver
                Hva vi har beregnet at IT har utbetalt til arbeidsgiver: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                IT sin arbeidsgiverandel: $infotrygdFeriepengebeløpArbeidsgiver
                Spleis sin arbeidsgiverandel: $spleisFeriepengebeløpArbeidsgiver
                Totalt feriepengebeløp: $totaltFeriepengebeløpArbeidsgiver
                Differanse: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd
                Oppdrag: ${OppdragReflect(oppdrag).toMap()}
                Datoer: ${feriepengeberegner.feriepengedatoer()}
                """.trimIndent()
            )

            return Feriepengeutbetaling(
                feriepengeberegner = feriepengeberegner,
                infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                oppdrag = oppdrag,
                utbetalingId = UUID.randomUUID()
            )
        }
    }
}
