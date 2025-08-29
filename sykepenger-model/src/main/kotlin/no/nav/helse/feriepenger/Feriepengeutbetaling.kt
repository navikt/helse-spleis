package no.nav.helse.feriepenger

import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.util.UUID
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

        private val Number.finere get() = "$this".padStart(10, ' ')
        private val List<Periode>.finere get() = joinToString(separator = "\n\t\t\t\t\t", prefix = "\n\t\t\t\t\t") { "$it (${it.count()} dager)" }

        internal fun build(aktivitetslogg: IAktivitetslogg): Feriepengeutbetaling {
            // Arbeidsgiver
            val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)

            val (feriepengeberegningsresultat, grunnlag) = feriepengeberegner.beregnFeriepenger(orgnummer)

            if (feriepengeberegningsresultat.arbeidsgiver.hvaViHarBeregnetAtInfotrygdHarUtbetalt != 0 && feriepengeberegningsresultat.arbeidsgiver.hvaViHarBeregnetAtInfotrygdHarUtbetalt !in infotrygdHarUtbetaltTilArbeidsgiver) {
                aktivitetslogg.info(
                    """
                    Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp
                    Arbeidsgiver: $orgnummer
                    Infotrygd har utbetalt $infotrygdHarUtbetaltTilArbeidsgiver
                    Vi har beregnet at infotrygd har utbetalt ${feriepengeberegningsresultat.arbeidsgiver.hvaViHarBeregnetAtInfotrygdHarUtbetalt}
                    """.trimIndent()
                )
            }

            val forrigeSendteArbeidsgiverOppdrag =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendTilOppdrag }
                    ?.oppdrag

            val arbeidsgiveroppdrag = oppdrag(
                mottaker = orgnummer,
                fagområde = Feriepengerfagområde.SykepengerRefusjon,
                klassekode = Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
                forrigeOppdrag = forrigeSendteArbeidsgiverOppdrag,
                beløp = feriepengeberegningsresultat.arbeidsgiver.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd
            )

            if (feriepengeberegningsresultat.arbeidsgiver.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd != 0 && orgnummer == "0") aktivitetslogg.info("Forventer ikke arbeidsgiveroppdrag til orgnummer \"0\".")

            val sendArbeidsgiveroppdrag = arbeidsgiveroppdrag.skalSendeOppdrag

            // Person
            val infotrygdHarUtbetaltTilPerson = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilPerson()
            if (feriepengeberegningsresultat.person.hvaViHarBeregnetAtInfotrygdHarUtbetalt != 0 && feriepengeberegningsresultat.person.hvaViHarBeregnetAtInfotrygdHarUtbetalt !in infotrygdHarUtbetaltTilPerson) {
                aktivitetslogg.info(
                    """
                    Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
                    Arbeidsgiver: $orgnummer
                    Infotrygd har utbetalt $infotrygdHarUtbetaltTilPerson
                    Vi har beregnet at infotrygd har utbetalt ${feriepengeberegningsresultat.person.hvaViHarBeregnetAtInfotrygdHarUtbetalt}
                    """.trimIndent()
                )
            }

            val forrigeSendtePersonOppdrag =
                tidligereFeriepengeutbetalinger
                    .lastOrNull { it.gjelderForÅr(utbetalingshistorikkForFeriepenger.opptjeningsår) && it.sendPersonoppdragTilOS }
                    ?.personoppdrag

            val personoppdrag = oppdrag(
                mottaker = personidentifikator.toString(),
                fagområde = Feriepengerfagområde.Sykepenger,
                klassekode = Feriepengerklassekode.SykepengerArbeidstakerFeriepenger,
                forrigeOppdrag = forrigeSendtePersonOppdrag,
                beløp = feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd
            )

            val sendPersonoppdrag = personoppdrag.skalSendeOppdrag

            if (feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd < -499 || feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd > 100) aktivitetslogg.info(
                """
                ${if (feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd < 0) "Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale" else "Utbetalt for lite i Infotrygd"} for person & orgnr-kombo:
                Arbeidsgiver: $orgnummer
                Diff: ${feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd}
                Hva vi har beregnet at IT har utbetalt til person for denne AG: ${feriepengeberegningsresultat.person.hvaViHarBeregnetAtInfotrygdHarUtbetalt}
                IT sin personandel: ${feriepengeberegningsresultat.person.infotrygdFeriepengebeløp}
                """.trimIndent()
            )

            val arbeidsgiveroppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(arbeidsgiveroppdrag).toString()
            val personoppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.mapOppdrag(personoppdrag).toString()
            // Logging
            aktivitetslogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                Arbeidsgiver: $orgnummer
                
                - ARBEIDSGIVER:
                ${oppsummering(infotrygdHarUtbetaltTilArbeidsgiver, feriepengeberegningsresultat.arbeidsgiver.hvaViHarBeregnetAtInfotrygdHarUtbetalt, feriepengeberegningsresultat.arbeidsgiver.infotrygdFeriepengebeløp, feriepengeberegningsresultat.arbeidsgiver.spleisFeriepengebeløp, feriepengeberegningsresultat.arbeidsgiver.totaltFeriepengebeløp, feriepengeberegningsresultat.arbeidsgiver.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd)}
                
                - PERSON:
                ${oppsummering(infotrygdHarUtbetaltTilPerson, feriepengeberegningsresultat.person.hvaViHarBeregnetAtInfotrygdHarUtbetalt, feriepengeberegningsresultat.person.infotrygdFeriepengebeløp, feriepengeberegningsresultat.person.spleisFeriepengebeløp, feriepengeberegningsresultat.person.totaltFeriepengebeløp, feriepengeberegningsresultat.person.differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd)}

                - GENERELT:         
                ${grunnlag.datoer.let { datoer -> "Datoer vi skal utbetale feriepenger for (${datoer.size}): ${datoer.grupperSammenhengendePerioder().finere}" }}
                
                - ARBEIDSGIVEROPPDRAG:
                ${oppdragoppsummering(true, sendArbeidsgiveroppdrag, forrigeSendteArbeidsgiverOppdrag, arbeidsgiveroppdrag, arbeidsgiveroppdragdetaljer)}
                
                - PERSONOPPDRAG:
                ${oppdragoppsummering(false, sendPersonoppdrag, forrigeSendtePersonOppdrag, personoppdrag, personoppdragdetaljer)}
                """
            )

            return Feriepengeutbetaling(
                feriepengegrunnlag = grunnlag,
                infotrygdFeriepengebeløpPerson = feriepengeberegningsresultat.person.infotrygdFeriepengebeløp,
                infotrygdFeriepengebeløpArbeidsgiver = feriepengeberegningsresultat.arbeidsgiver.infotrygdFeriepengebeløp,
                spleisFeriepengebeløpArbeidsgiver = feriepengeberegningsresultat.arbeidsgiver.spleisFeriepengebeløp,
                spleisFeriepengebeløpPerson = feriepengeberegningsresultat.person.spleisFeriepengebeløp,
                oppdrag = arbeidsgiveroppdrag,
                personoppdrag = personoppdrag,
                utbetalingId = UUID.randomUUID(),
                sendTilOppdrag = sendArbeidsgiveroppdrag,
                sendPersonoppdragTilOS = sendPersonoppdrag,
            )
        }

        private fun oppdragoppsummering(arbeidsgiver: Boolean, sendOppdrag: Boolean, forrigeSendteOppdrag: Feriepengeoppdrag?, oppdrag: Feriepengeoppdrag, oppdragdetaljer: String): String {
            val nettobeløp = oppdrag.totalbeløp - (forrigeSendteOppdrag?.totalbeløp ?: 0)
            val label = if (nettobeløp < 0)
                "(kreve tilbake fra ${if (arbeidsgiver) "arbeidsgiver" else "person"})"
            else if (nettobeløp == 0)
                "(ingen endring)"
            else
                "(betale mer til ${if (arbeidsgiver) "arbeidsgiver" else "person"})"

            return """Skal sende ${if (arbeidsgiver) "arbeidsgiveroppdrag" else "personoppdrag"} til OS: $sendOppdrag
                Differanse fra forrige sendte ${if (arbeidsgiver) "arbeidsgiveroppdrag" else "personoppdrag"}: $nettobeløp $label
                ${if (arbeidsgiver) "Arbeidsgiveroppdrag" else "Personoppdrag"}: $oppdragdetaljer"""
        }

        private fun oppsummering(infotrygdHarUtbetalt: List<Int>, hvaViHarBeregnetAtInfotrygdHarUtbetaltForDenneAktuelleArbeidsgiver: Int, infotrygdFeriepengebeløp: Double, spleisFeriepengebeløp: Double, totaltFeriepengebeløp: Double, differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTil: Int): String {
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
