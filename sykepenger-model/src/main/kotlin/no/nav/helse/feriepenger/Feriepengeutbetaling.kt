package no.nav.helse.feriepenger

import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.util.*
import kotlin.math.roundToInt
import no.nav.helse.Personidentifikator
import no.nav.helse.dto.deserialisering.FeriepengeInnDto
import no.nav.helse.dto.serialisering.FeriepengeUtDto
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.genererUtbetalingsreferanse

internal class Feriepengeutbetaling private constructor(
    private val feriepengegrunnlag: Feriepengeutbetalinggrunnlag,
    private val infotrygdFeriepengebeløpPerson: Double,
    private val infotrygdFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpPerson: Double,
    private val oppdrag: Feriepengeoppdrag,
    private val personoppdrag: Feriepengeoppdrag,
    private val utbetalingId: UUID,
    private val sendTilOppdrag: Boolean,
    private val sendPersonoppdragTilOS: Boolean,
) : Aktivitetskontekst {
    var overføringstidspunkt: LocalDateTime? = null
    var avstemmingsnøkkel: Long? = null

    private val maiMåned = feriepengegrunnlag.opptjeningsår.plusYears(1).atMonth(Month.MAY)
    val fom = maiMåned.atDay(1)
    val tom = maiMåned.atEndOfMonth()

    companion object {
        internal fun gjenopprett(dto: FeriepengeInnDto): Feriepengeutbetaling {
            return Feriepengeutbetaling(
                feriepengegrunnlag = Feriepengeutbetalinggrunnlag(
                    opptjeningsår = dto.feriepengeberegner.opptjeningsår,
                    utbetalteDager = dto.feriepengeberegner.utbetalteDager.map {
                        Feriepengeutbetalinggrunnlag.UtbetaltDag.gjenopprett(it)
                    },
                    feriepengedager = dto.feriepengeberegner.feriepengedager.map {
                        Feriepengeutbetalinggrunnlag.UtbetaltDag.gjenopprett(it)
                    }
                ),
                infotrygdFeriepengebeløpPerson = dto.infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = dto.infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = dto.spleisFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpPerson = dto.spleisFeriepengebeløpPerson,
                oppdrag = Feriepengeoppdrag.gjenopprett(dto.oppdrag),
                personoppdrag = Feriepengeoppdrag.gjenopprett(dto.personoppdrag),
                utbetalingId = dto.utbetalingId,
                sendTilOppdrag = dto.sendTilOppdrag,
                sendPersonoppdragTilOS = dto.sendPersonoppdragTilOS
            )
        }
    }

    internal fun view() = FeriepengeutbetalingView(
        infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
        infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
        spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
        spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
        oppdrag = oppdrag,
        personoppdrag = personoppdrag
    )

    fun håndter(utbetalingHendelse: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg, organisasjonsnummer: String, person: Person) {
        if (utbetalingHendelse.utbetalingId != this.utbetalingId || utbetalingHendelse.fagsystemId !in setOf(oppdrag.fagsystemId, personoppdrag.fagsystemId)) return

        aktivitetslogg.info("Behandler svar fra Oppdrag/UR/spenn for feriepenger")
        when (utbetalingHendelse.status) {
            Oppdragstatus.OVERFØRT,
            Oppdragstatus.AKSEPTERT -> {
            } // all is good
            Oppdragstatus.AKSEPTERT_MED_FEIL -> aktivitetslogg.info("Utbetalingen ble gjennomført, men med advarsel")
            Oppdragstatus.AVVIST,
            Oppdragstatus.FEIL -> aktivitetslogg.info("Utbetaling feilet med status ${utbetalingHendelse.status}. Feilmelding fra Oppdragsystemet: ${utbetalingHendelse.melding}")
        }
        val utbetaltOk = !aktivitetslogg.harFunksjonelleFeilEllerVerre()
        lagreInformasjon(utbetalingHendelse, aktivitetslogg, utbetaltOk)

        if (!utbetaltOk) {
            return aktivitetslogg.info("Utbetaling av feriepenger med utbetalingId $utbetalingId og fagsystemIder ${oppdrag.fagsystemId} og ${personoppdrag.fagsystemId} feilet.")
        }

        person.feriepengerUtbetalt(
            PersonObserver.FeriepengerUtbetaltEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
                fom = fom,
                tom = tom,
                arbeidsgiverOppdrag = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(oppdrag),
                personOppdrag = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(personoppdrag)
            )
        )
    }

    private fun lagreInformasjon(hendelse: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg, gikkBra: Boolean) {
        overføringstidspunkt = hendelse.overføringstidspunkt
        avstemmingsnøkkel = hendelse.avstemmingsnøkkel
        aktivitetslogg.info("Data for feriepenger fra Oppdrag/UR: tidspunkt: $overføringstidspunkt, avstemmingsnøkkel $avstemmingsnøkkel og utbetalt ok: ${if (gikkBra) "ja" else "nei"}")
    }

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Feriepengeutbetaling", mapOf("utbetalingId" to "$utbetalingId"))

    internal fun overfør(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(this)
        if (sendTilOppdrag) oppdrag.overfør(aktivitetsloggMedUtbetalingkontekst, "SPLEIS")
        if (sendPersonoppdragTilOS) personoppdrag.overfør(aktivitetsloggMedUtbetalingkontekst, "SPLEIS")
    }

    internal fun gjelderForÅr(år: Year) = feriepengegrunnlag.opptjeningsår == år

    internal class Builder(
        private val personidentifikator: Personidentifikator,
        private val orgnummer: String,
        private val feriepengeberegner: Feriepengeberegner,
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        private val tidligereFeriepengeutbetalinger: List<Feriepengeutbetaling>
    ) {
        private fun oppdrag(mottaker: String, fagområde: Feriepengerfagområde, klassekode: Feriepengerklassekode, forrigeOppdrag: Feriepengeoppdrag?, beløp: Int): Feriepengeoppdrag {
            if (forrigeOppdrag == null) {
                val maiMåned = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY)
                val linje = if (beløp == 0)
                    null
                else
                    Feriepengeutbetalingslinje(
                        fom = maiMåned.atDay(1),
                        tom = maiMåned.atEndOfMonth(),
                        beløp = beløp,
                        klassekode = klassekode,
                    )
                return Feriepengeoppdrag(
                    mottaker = mottaker,
                    fagområde = fagområde,
                    fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID()),
                    endringskode = Feriepengerendringskode.NY,
                    linje = linje,
                    tidsstempel = LocalDateTime.now()
                )
            }

            return forrigeOppdrag.endreBeløp(beløp)
        }

        private val Double.finere get() = "$this".padStart(10, ' ')
        private val List<Periode>.finere get() = joinToString(separator = "\n\t\t\t\t\t", prefix = "\n\t\t\t\t\t") { "$it (${it.count()} dager)" }

        internal fun build(aktivitetslogg: IAktivitetslogg): Feriepengeutbetaling {
            // Arbeidsgiver
            val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)

            if (hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() != 0 &&
                hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() !in infotrygdHarUtbetaltTilArbeidsgiver
            ) {
                aktivitetslogg.info(
                    """
                    Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp
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

            val arbeidsgiveroppdrag = oppdrag(
                mottaker = orgnummer,
                fagområde = Feriepengerfagområde.SykepengerRefusjon,
                klassekode = Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
                forrigeOppdrag = forrigeSendteArbeidsgiverOppdrag,
                beløp = arbeidsgiverbeløp
            )

            if (arbeidsgiverbeløp != 0 && orgnummer == "0") aktivitetslogg.info("Forventer ikke arbeidsgiveroppdrag til orgnummer \"0\".")

            val sendArbeidsgiveroppdrag = arbeidsgiveroppdrag.skalSendeOppdrag

            // Person
            val infotrygdHarUtbetaltTilPerson = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilPerson()
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer)
            if (hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson.roundToInt() != 0 &&
                hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPerson.roundToInt() !in infotrygdHarUtbetaltTilPerson
            ) {
                aktivitetslogg.info(
                    """
                    Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
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

            val personoppdrag = oppdrag(
                mottaker = personidentifikator.toString(),
                fagområde = Feriepengerfagområde.Sykepenger,
                klassekode = Feriepengerklassekode.SykepengerArbeidstakerFeriepenger,
                forrigeOppdrag = forrigeSendtePersonOppdrag,
                beløp = personbeløp
            )

            val sendPersonoppdrag = personoppdrag.skalSendeOppdrag

            if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < -499 || differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson > 100) aktivitetslogg.info(
                """
                ${if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < 0) "Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale" else "Utbetalt for lite i Infotrygd"} for person & orgnr-kombo:
                Arbeidsgiver: $orgnummer
                Diff: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson
                Hva vi har beregnet at IT har utbetalt til person for denne AG: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                """.trimIndent()
            )

            val arbeidsgiveroppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(arbeidsgiveroppdrag).toString()
            val personoppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(personoppdrag).toString()
            // Logging
            aktivitetslogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                Arbeidsgiver: $orgnummer
                
                ${oppsummeringArbeidsgiver(infotrygdHarUtbetaltTilArbeidsgiver, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver, infotrygdFeriepengebeløpArbeidsgiver, spleisFeriepengebeløpArbeidsgiver, totaltFeriepengebeløpArbeidsgiver, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd)}
                
                ${oppsummeringPerson(infotrygdHarUtbetaltTilPerson, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver, infotrygdFeriepengebeløpPerson, spleisFeriepengebeløpPerson, totaltFeriepengebeløpPerson, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson)}

                - GENERELT:         
                ${feriepengeberegner.feriepengedatoer().let { datoer -> "Datoer vi skal utbetale feriepenger for (${datoer.size}): ${datoer.grupperSammenhengendePerioder().finere}" }}
                
                ${oppsummeringArbeidsgiveroppdrag(sendArbeidsgiveroppdrag, forrigeSendteArbeidsgiverOppdrag, arbeidsgiveroppdrag, arbeidsgiveroppdragdetaljer)}
                
                ${oppsummeringPersonoppdrag(sendPersonoppdrag, forrigeSendtePersonOppdrag, personoppdrag, personoppdragdetaljer)}
                """
            )

            return Feriepengeutbetaling(
                feriepengegrunnlag = feriepengeberegner.grunnlag(),
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

        private fun oppsummeringArbeidsgiveroppdrag(sendArbeidsgiveroppdrag: Boolean, forrigeSendteArbeidsgiverOppdrag: Feriepengeoppdrag?, arbeidsgiveroppdrag: Feriepengeoppdrag, arbeidsgiveroppdragdetaljer: String): String {
            return """- ARBEIDSGIVEROPPDRAG:
                Skal sende arbeidsgiveroppdrag til OS: $sendArbeidsgiveroppdrag
                Differanse fra forrige sendte arbeidsgoiveroppdrag: ${forrigeSendteArbeidsgiverOppdrag?.totalbeløp?.minus(arbeidsgiveroppdrag.totalbeløp)}
                Arbeidsgiveroppdrag: $arbeidsgiveroppdragdetaljer"""
        }

        private fun oppsummeringPersonoppdrag(sendPersonoppdrag: Boolean, forrigeSendtePersonOppdrag: Feriepengeoppdrag?, personoppdrag: Feriepengeoppdrag, personoppdragdetaljer: String): String {
            return """- PERSONOPPDRAG:
                Skal sende personoppdrag til OS: $sendPersonoppdrag
                Differanse fra forrige sendte personoppdrag: ${forrigeSendtePersonOppdrag?.totalbeløp?.minus(personoppdrag.totalbeløp)}
                Personoppdrag: $personoppdragdetaljer"""
        }

        private fun oppsummeringArbeidsgiver(infotrygdHarUtbetaltTilArbeidsgiver: List<Int>, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver: Double, infotrygdFeriepengebeløpArbeidsgiver: Double, spleisFeriepengebeløpArbeidsgiver: Double, totaltFeriepengebeløpArbeidsgiver: Double, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Double): String {
            return """- ARBEIDSGIVER:
                ${oppsummering(infotrygdHarUtbetaltTilArbeidsgiver, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver, infotrygdFeriepengebeløpArbeidsgiver, spleisFeriepengebeløpArbeidsgiver, totaltFeriepengebeløpArbeidsgiver, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd)}"""
        }

        private fun oppsummeringPerson(infotrygdHarUtbetaltTilPerson: List<Int>, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver: Double, infotrygdFeriepengebeløpPerson: Double, spleisFeriepengebeløpPerson: Double, totaltFeriepengebeløpPerson: Double, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson: Double): String {
            return """- PERSON:
                ${oppsummering(infotrygdHarUtbetaltTilPerson, hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver, infotrygdFeriepengebeløpPerson, spleisFeriepengebeløpPerson, totaltFeriepengebeløpPerson, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson)}"""
        }

        private fun oppsummering(infotrygdHarUtbetalt: List<Int>, hvaViHarBeregnetAtInfotrygdHarUtbetaltForDenneAktuelleArbeidsgiver: Double, infotrygdFeriepengebeløp: Double, spleisFeriepengebeløp: Double, totaltFeriepengebeløp: Double, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTil: Double): String {
            return """Alle feriepengeutbetalinger fra Infotrygd (alle ytelser): $infotrygdHarUtbetalt (NB! Dette bruks ikke i beregning, bare til logging)
                Vår beregning av hva Infotrygd ville utbetalt i en verden uten Spleis: $hvaViHarBeregnetAtInfotrygdHarUtbetaltForDenneAktuelleArbeidsgiver
                Men siden Spleis finnes:
                    Infotrygd skal betale:                      ${infotrygdFeriepengebeløp.finere}
                    Spleis skal betale:                         ${spleisFeriepengebeløp.finere}
                    Totalt feriepengebeløp:                     ${totaltFeriepengebeløp.finere}
                    Infotrygd-utbetalingen må korrigeres med:   ${differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTil.finere}"""
        }
    }

    internal fun dto() = FeriepengeUtDto(
        feriepengeberegner = feriepengegrunnlag.dto(),
        infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
        infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
        spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
        spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
        oppdrag = this.oppdrag.dto(),
        personoppdrag = this.personoppdrag.dto(),
        utbetalingId = this.utbetalingId,
        sendTilOppdrag = this.sendTilOppdrag,
        sendPersonoppdragTilOS = this.sendPersonoppdragTilOS
    )
}

internal data class FeriepengeutbetalingView(
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: Feriepengeoppdrag,
    val personoppdrag: Feriepengeoppdrag
)
