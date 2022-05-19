package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

internal class Feriepengeutbetaling private constructor(
    private val feriepengeberegner: Feriepengeberegner,
    private val infotrygdFeriepengebeløpPerson: Double,
    private val infotrygdFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpArbeidsgiver: Double,
    private val oppdrag: Oppdrag,
    private val personoppdrag: Oppdrag,
    private val utbetalingId: UUID,
    private val sendTilOppdrag: Boolean,
    private val sendPersonoppdragTilOS: Boolean,
) : Aktivitetskontekst {
    var overføringstidspunkt: LocalDateTime? = null
    var avstemmingsnøkkel: Long? = null

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        fun List<Feriepengeutbetaling>.gjelderFeriepengeutbetaling(hendelse: UtbetalingHendelse) = any { hendelse.erRelevant(it.oppdrag.fagsystemId()) || hendelse.erRelevant(it.personoppdrag.fagsystemId()) }
        internal fun ferdigFeriepengeutbetaling(
            feriepengeberegner: Feriepengeberegner,
            infotrygdFeriepengebeløpPerson: Double,
            infotrygdFeriepengebeløpArbeidsgiver: Double,
            spleisFeriepengebeløpArbeidsgiver: Double,
            oppdrag: Oppdrag,
            personoppdrag: Oppdrag,
            utbetalingId: UUID,
            sendTilOppdrag: Boolean,
            sendPersonoppdragTilOS: Boolean,
        ): Feriepengeutbetaling =
            Feriepengeutbetaling(
                feriepengeberegner = feriepengeberegner,
                infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                oppdrag = oppdrag,
                personoppdrag = personoppdrag,
                utbetalingId = utbetalingId,
                sendTilOppdrag = sendTilOppdrag,
                sendPersonoppdragTilOS = sendPersonoppdragTilOS,
            )
    }

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel,
            utbetalingId,
            sendTilOppdrag,
            sendPersonoppdragTilOS,
        )
        feriepengeberegner.accept(visitor)
        visitor.preVisitFeriepengerArbeidsgiveroppdrag()
        oppdrag.accept(visitor)
        visitor.preVisitFeriepengerPersonoppdrag()
        personoppdrag.accept(visitor)
        visitor.postVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel,
            utbetalingId,
            sendTilOppdrag,
            sendPersonoppdragTilOS,
        )
    }

    fun håndter(utbetalingHendelse: UtbetalingHendelse, person: Person) {
        if (!utbetalingHendelse.erRelevant(oppdrag.fagsystemId(), personoppdrag.fagsystemId(), utbetalingId)) return

        utbetalingHendelse.info("Behandler svar fra Oppdrag/UR/spenn for feriepenger")
        utbetalingHendelse.valider()
        val utbetaltOk = !utbetalingHendelse.hasErrorsOrWorse()
        lagreInformasjon(utbetalingHendelse, utbetaltOk)

        if (!utbetaltOk) {
            sikkerLogg.info("Utbetaling av feriepenger med utbetalingId $utbetalingId og fagsystemIder ${oppdrag.fagsystemId()} og ${personoppdrag.fagsystemId()} feilet.")
            return
        }

        person.feriepengerUtbetalt(
            utbetalingHendelse.hendelseskontekst(),
            PersonObserver.FeriepengerUtbetaltEvent(
                arbeidsgiverOppdrag = oppdrag.toHendelseMap(),
                personOppdrag = personoppdrag.toHendelseMap()
            )
        )

        person.utbetalingEndret(
            utbetalingHendelse.hendelseskontekst(),
            PersonObserver.UtbetalingEndretEvent(
                utbetalingId = utbetalingId,
                type = Utbetalingtype.FERIEPENGER.name,
                arbeidsgiverOppdrag = oppdrag.toHendelseMap(),
                personOppdrag = personoppdrag.toHendelseMap(),
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

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Feriepengeutbetaling", mapOf("utbetalingId" to "$utbetalingId"))

    internal fun overfør(hendelse: PersonHendelse) {
        hendelse.kontekst(this)
        if (sendTilOppdrag) oppdrag.overfør(hendelse, null, "SPLEIS")
        if (sendPersonoppdragTilOS) personoppdrag.overfør(hendelse, null, "SPLEIS")
    }

    internal fun gjelderForÅr(år: Year) = feriepengeberegner.gjelderForÅr(år)

    internal class Builder(
        private val aktørId: String,
        private val fødselsnummer: Fødselsnummer,
        private val orgnummer: String,
        private val feriepengeberegner: Feriepengeberegner,
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        private val tidligereFeriepengeutbetalinger: List<Feriepengeutbetaling>
    ) {
        internal fun build(): Feriepengeutbetaling {
            val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdPersonForEnArbeidsgiver(orgnummer)

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

            val totaltFeriepengebeløpArbeidsgiver: Double = feriepengeberegner.beregnFeriepengerForArbeidsgiver(orgnummer)
            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Double = feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(orgnummer)
            val beløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd.roundToInt()

            val forrigeSendteOppdrag =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendTilOppdrag }
                    ?.oppdrag
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val fagsystemId =
                forrigeSendteOppdrag
                    ?.fagsystemId()
                    ?: genererUtbetalingsreferanse(UUID.randomUUID())

            var oppdrag = Oppdrag(
                mottaker = orgnummer,
                fagområde = Fagområde.SykepengerRefusjon,
                linjer = listOf(
                    Utbetalingslinje(
                        fom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atDay(1),
                        tom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atEndOfMonth(),
                        satstype = Satstype.Engang,
                        beløp = beløp,
                        aktuellDagsinntekt = null,
                        grad = null,
                        klassekode = Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig
                    )
                ),
                fagsystemId = fagsystemId,
                sisteArbeidsgiverdag = null
            )

            val sendTilOppdrag = if (forrigeSendteOppdrag == null) { beløp != 0 } else { beløp != forrigeSendteOppdrag.totalbeløp() || beløp == 0 }

            if (forrigeSendteOppdrag != null) {
                oppdrag = if (beløp == 0) {
                    forrigeSendteOppdrag.annuller(utbetalingshistorikkForFeriepenger)
                } else {
                    oppdrag.minus(forrigeSendteOppdrag, utbetalingshistorikkForFeriepenger)
                }
            }

            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson =  infotrygdFeriepengebeløpPerson - hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
            val personbeløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson.roundToInt()

            val forrigeSendteOppdragForPerson =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendPersonoppdragTilOS }
                    ?.personoppdrag
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val fagsystemIdPersonoppdrag =
                forrigeSendteOppdragForPerson
                    ?.fagsystemId()
                    ?: genererUtbetalingsreferanse(UUID.randomUUID())

            var personoppdrag = Oppdrag(
                mottaker = fødselsnummer.toString(),
                fagområde = Fagområde.Sykepenger,
                linjer = listOf(
                    Utbetalingslinje(
                        fom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atDay(1),
                        tom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atEndOfMonth(),
                        satstype = Satstype.Engang,
                        beløp = personbeløp,
                        aktuellDagsinntekt = null,
                        grad = null,
                        klassekode = Klassekode.SykepengerArbeidstakerFeriepenger,
                    )
                ),
                fagsystemId = fagsystemIdPersonoppdrag,
                sisteArbeidsgiverdag = null,
            )

            val sendPersonoppdragTilOS = if (forrigeSendteOppdragForPerson == null) { personbeløp != 0 } else { personbeløp != forrigeSendteOppdragForPerson.totalbeløp() || personbeløp == 0 }

            if (forrigeSendteOppdragForPerson != null) {
                personoppdrag = if (personbeløp == 0) {
                    forrigeSendteOppdragForPerson.annuller(utbetalingshistorikkForFeriepenger)
                } else {
                    personoppdrag.minus(forrigeSendteOppdragForPerson, utbetalingshistorikkForFeriepenger)
                }
            }

            if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < -499 || differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson > 100) sikkerLogg.info(
                """
                ${if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < 0) "Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale" else "Utbetalt for lite i Infotrygd"} for person & orgnr-kombo:
                AktørId: $aktørId
                Arbeidsgiver: $orgnummer
                Diff: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson
                Hva vi har beregnet at IT har utbetalt til person for denne AG: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                """.trimIndent()
            )
            sikkerLogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                AktørId: $aktørId
                Arbeidsgiver: $orgnummer
                IT har utbetalt til arbeidsgiver: $infotrygdHarUtbetaltTilArbeidsgiver
                Hva vi har beregnet at IT har utbetalt til arbeidsgiver: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver
                Hva vi har beregnet at IT har utbetalt til person for denne AG: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
                Diff mellom hva vi har beregnet at IT har utbetalt til person for denne AG og hva vi syns de burde ha betalt: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                IT sin arbeidsgiverandel: $infotrygdFeriepengebeløpArbeidsgiver
                Spleis sin arbeidsgiverandel: $spleisFeriepengebeløpArbeidsgiver
                Totalt feriepengebeløp: $totaltFeriepengebeløpArbeidsgiver
                Differanse: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd
                Oppdrag: ${oppdrag.toHendelseMap()}
                Datoer: ${feriepengeberegner.feriepengedatoer()}
                Differanse fra forrige sendte oppdrag: ${forrigeSendteOppdrag?.totalbeløp()?.minus(oppdrag.totalbeløp())}
                Skal sendes til oppdrag: $sendTilOppdrag
                Skal personoppdrag sendes: $sendPersonoppdragTilOS
                """.trimIndent()
            )

            return Feriepengeutbetaling(
                feriepengeberegner = feriepengeberegner,
                infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                oppdrag = oppdrag,
                personoppdrag = personoppdrag,
                utbetalingId = UUID.randomUUID(),
                sendTilOppdrag = sendTilOppdrag,
                sendPersonoppdragTilOS = sendPersonoppdragTilOS,
            )
        }
    }
}
