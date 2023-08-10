package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

internal class Feriepengeutbetaling private constructor(
    private val feriepengeberegner: Feriepengeberegner,
    private val infotrygdFeriepengebeløpPerson: Double,
    private val infotrygdFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpPerson: Double,
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
            spleisFeriepengebeløpPerson: Double,
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
                spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
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
            spleisFeriepengebeløpPerson,
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
            spleisFeriepengebeløpPerson,
            overføringstidspunkt,
            avstemmingsnøkkel,
            utbetalingId,
            sendTilOppdrag,
            sendPersonoppdragTilOS,
        )
    }

    fun håndter(utbetalingHendelse: UtbetalingHendelse, organisasjonsnummer: String, person: Person) {
        if (!utbetalingHendelse.erRelevant(oppdrag.fagsystemId(), personoppdrag.fagsystemId(), utbetalingId)) return

        utbetalingHendelse.info("Behandler svar fra Oppdrag/UR/spenn for feriepenger")
        utbetalingHendelse.valider()
        val utbetaltOk = !utbetalingHendelse.harFunksjonelleFeilEllerVerre()
        lagreInformasjon(utbetalingHendelse, utbetaltOk)

        if (!utbetaltOk) {
            sikkerLogg.info("Utbetaling av feriepenger med utbetalingId $utbetalingId og fagsystemIder ${oppdrag.fagsystemId()} og ${personoppdrag.fagsystemId()} feilet.")
            return
        }

        person.feriepengerUtbetalt(
            PersonObserver.FeriepengerUtbetaltEvent(
                organisasjonsnummer = organisasjonsnummer,
                arbeidsgiverOppdrag = oppdrag.toHendelseMap(),
                personOppdrag = personoppdrag.toHendelseMap()
            )
        )

        person.utbetalingEndret(
            PersonObserver.UtbetalingEndretEvent(
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                type = Utbetalingtype.FERIEPENGER.name,
                forrigeStatus = Utbetalingstatus.IKKE_UTBETALT.name,
                gjeldendeStatus = Utbetalingstatus.UTBETALT.name,
                arbeidsgiverOppdrag = oppdrag.toHendelseMap(),
                personOppdrag = personoppdrag.toHendelseMap(),
                korrelasjonsId = UUID.randomUUID()
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
        private val personidentifikator: Personidentifikator,
        private val orgnummer: String,
        private val feriepengeberegner: Feriepengeberegner,
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        private val tidligereFeriepengeutbetalinger: List<Feriepengeutbetaling>
    ) {
        private fun oppdrag(fagområde: Fagområde, forrigeOppdrag: Oppdrag?, beløp: Int): Oppdrag {
            val (klassekode, mottaker) = when (fagområde) {
                Fagområde.SykepengerRefusjon -> Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig to orgnummer
                Fagområde.Sykepenger -> Klassekode.SykepengerArbeidstakerFeriepenger to personidentifikator.toString()
            }
            val fagsystemId = forrigeOppdrag?.fagsystemId() ?: genererUtbetalingsreferanse(UUID.randomUUID())

            val nyttOppdrag = Oppdrag(
                mottaker = mottaker,
                fagområde = fagområde,
                linjer = listOf(
                    Utbetalingslinje(
                        fom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atDay(1),
                        tom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atEndOfMonth(),
                        satstype = Satstype.Engang,
                        beløp = beløp,
                        aktuellDagsinntekt = null,
                        grad = null,
                        klassekode = klassekode,
                    )
                ),
                fagsystemId = fagsystemId,
                sisteArbeidsgiverdag = null,
            )

            if (forrigeOppdrag == null) return nyttOppdrag
            if (beløp == 0) forrigeOppdrag.annuller(utbetalingshistorikkForFeriepenger)
            return nyttOppdrag.minus(forrigeOppdrag, utbetalingshistorikkForFeriepenger)
        }

        private fun skalSendeOppdrag(forrigeOppdrag: Oppdrag?, beløp: Int): Boolean {
            if (forrigeOppdrag == null) return beløp != 0
            return beløp != forrigeOppdrag.totalbeløp() || beløp == 0
        }

        private val Double.finere get() = "$this".padStart(10, ' ')
        private val List<Periode>.finere get() = joinToString(separator = "\n\t\t\t\t\t", prefix = "\n\t\t\t\t\t") {"$it (${it.count()} dager)" }

        internal fun build(): Feriepengeutbetaling {
            // Arbeidsgiver
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

            val infotrygdFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer)
            val spleisFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(orgnummer)
            val totaltFeriepengebeløpArbeidsgiver: Double = feriepengeberegner.beregnFeriepengerForArbeidsgiver(orgnummer)
            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Double = feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(orgnummer)
            val arbeidsgiverbeløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd.roundToInt()

            val forrigeSendteArbeidsgiverOppdrag =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendTilOppdrag }
                    ?.oppdrag
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val arbeidsgiveroppdrag = oppdrag(Fagområde.SykepengerRefusjon, forrigeSendteArbeidsgiverOppdrag, arbeidsgiverbeløp)

            if (arbeidsgiverbeløp != 0 && orgnummer == "0") sikkerLogg.warn("Forventer ikke arbeidsgiveroppdrag til orgnummer \"0\", aktørId=$aktørId.")

            val sendArbeidsgiveroppdrag = skalSendeOppdrag(forrigeSendteArbeidsgiverOppdrag, arbeidsgiverbeløp)

            // Person
            val infotrygdHarUtbetaltTilPerson = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilPerson()
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer)
            if (hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson.roundToInt() != 0 &&
                hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson.roundToInt() !in infotrygdHarUtbetaltTilPerson
            ) {
                sikkerLogg.info(
                    """
                    Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
                    AktørId: $aktørId
                    Arbeidsgiver: $orgnummer
                    Infotrygd har utbetalt $infotrygdHarUtbetaltTilPerson
                    Vi har beregnet at infotrygd har utbetalt ${hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson.roundToInt()}
                    """.trimIndent()
                )
            }

            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson = feriepengeberegner.beregnFeriepengedifferansenForPerson(orgnummer)
            val infotrygdFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForInfotrygdPerson(orgnummer)
            val spleisFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForSpleisPerson(orgnummer)
            val totaltFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForPerson(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdPersonForEnArbeidsgiver(orgnummer)
            val personbeløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson.roundToInt()

            val forrigeSendtePersonOppdrag =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendPersonoppdragTilOS }
                    ?.personoppdrag
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val personoppdrag = oppdrag(Fagområde.Sykepenger, forrigeSendtePersonOppdrag, personbeløp)

            val sendPersonoppdrag = skalSendeOppdrag(forrigeSendtePersonOppdrag, personbeløp)

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

            // Logging
            sikkerLogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                AktørId: $aktørId
                Arbeidsgiver: $orgnummer
                
                - ARBEIDSGIVER:
                Alle feriepengeutbetalinger fra Infotrygd (alle ytelser): $infotrygdHarUtbetaltTilArbeidsgiver (NB! Dette bruks ikke i beregning, bare til logging)
                Vår beregning av hva Infotrygd ville utbetalt i en verden uten Spleis: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver
                Men siden Spleis finnes:
                    Infotrygd skal betale:                      ${infotrygdFeriepengebeløpArbeidsgiver.finere}
                    Spleis skal betale:                         ${spleisFeriepengebeløpArbeidsgiver.finere}
                    Totalt feriepengebeløp:                     ${totaltFeriepengebeløpArbeidsgiver.finere}
                    Infotrygd-utbetalingen må korrigeres med:   ${differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd.finere}
                
                - PERSON:
                Alle feriepengeutbetalinger fra Infotrygd (alle ytelser): $infotrygdHarUtbetaltTilPerson (NB! Dette bruks ikke i beregning, bare til logging)
                Vår beregning av hva Infotrygd ville utbetalt i en verden uten Spleis: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
                Men siden Spleis finnes:
                    Infotrygd skal betale:                      ${infotrygdFeriepengebeløpPerson.finere}
                    Spleis skal betale:                         ${spleisFeriepengebeløpPerson.finere}
                    Totalt feriepengebeløp:                     ${totaltFeriepengebeløpPerson.finere}
                    Infotrygd-utbetalingen må korrigeres med:   ${differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson.finere}

                - GENERELT:         
                ${feriepengeberegner.feriepengedatoer().let { datoer -> "Datoer vi skal utbetale feriepenger for (${datoer.size}): ${datoer.grupperSammenhengendePerioder().finere}"}}
                
                - OPPDRAG:
                Skal sende arbeidsgiveroppdrag til OS: $sendArbeidsgiveroppdrag
                Differanse fra forrige sendte arbeidsgoiveroppdrag: ${forrigeSendteArbeidsgiverOppdrag?.totalbeløp()?.minus(arbeidsgiveroppdrag.totalbeløp())}
                Arbeidsgiveroppdrag: ${arbeidsgiveroppdrag.toHendelseMap()}
                
                Skal sende personoppdrag til OS: $sendPersonoppdrag
                Differanse fra forrige sendte personoppdrag: ${forrigeSendtePersonOppdrag?.totalbeløp()?.minus(personoppdrag.totalbeløp())}
                Personoppdrag: ${personoppdrag.toHendelseMap()}
                """
            )

            return Feriepengeutbetaling(
                feriepengeberegner = feriepengeberegner,
                infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
                oppdrag = arbeidsgiveroppdrag,
                personoppdrag = personoppdrag,
                utbetalingId = UUID.randomUUID(),
                sendTilOppdrag = sendArbeidsgiveroppdrag,
                sendPersonoppdragTilOS = sendPersonoppdrag,
            )
        }
    }
}
