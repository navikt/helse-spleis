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
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
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

    companion object {
        fun List<Feriepengeutbetaling>.gjelderFeriepengeutbetaling(hendelse: UtbetalingHendelse) = any { hendelse.fagsystemId == it.oppdrag.fagsystemId || hendelse.fagsystemId == it.personoppdrag.fagsystemId }

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
                arbeidsgiverOppdrag = PersonObserver.FeriepengerUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(oppdrag),
                personOppdrag = PersonObserver.FeriepengerUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(personoppdrag)
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
        private fun oppdrag(mottaker: String, fagområde: Fagområde, klassekode: Klassekode, forrigeOppdrag: Feriepengeoppdrag?, beløp: Int): Feriepengeoppdrag {
            if (forrigeOppdrag == null) {
                val maiMåned = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY)
                val linjer = if (beløp == 0)
                    emptyList()
                else
                    listOf(
                        Feriepengeutbetalingslinje(
                            fom = maiMåned.atDay(1),
                            tom = maiMåned.atEndOfMonth(),
                            beløp = beløp,
                            klassekode = klassekode,
                        )
                    )
                return Feriepengeoppdrag(
                    mottaker = mottaker,
                    fagområde = fagområde,
                    fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID()),
                    endringskode = Endringskode.NY,
                    linjer = linjer,
                    tidsstempel = LocalDateTime.now()
                )
            }

            if (beløp == 0) return forrigeOppdrag.annuller()
            if (beløp == forrigeOppdrag.linjer.single().beløp) return forrigeOppdrag.copy(
                endringskode = Endringskode.UEND,
                linjer = listOf(
                    forrigeOppdrag.linjer.single().copy(endringskode = Endringskode.UEND)
                )
            )
            return forrigeOppdrag.copy(
                endringskode = Endringskode.ENDR,
                linjer = listOf(
                    forrigeOppdrag.linjer.single().copy(
                        endringskode = Endringskode.NY,
                        beløp = beløp,
                        delytelseId = forrigeOppdrag.linjer.single().delytelseId + 1,
                        refDelytelseId = forrigeOppdrag.linjer.single().delytelseId,
                        refFagsystemId = forrigeOppdrag.fagsystemId
                    )
                )
            )
        }

        private fun skalSendeOppdrag(nyttOppdrag: Feriepengeoppdrag): Boolean {
            return nyttOppdrag.linjer.singleOrNull()?.endringskode != Endringskode.UEND
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
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val arbeidsgiveroppdrag = oppdrag(
                mottaker = orgnummer,
                fagområde = Fagområde.SykepengerRefusjon,
                klassekode = Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
                forrigeOppdrag = forrigeSendteArbeidsgiverOppdrag,
                beløp = arbeidsgiverbeløp
            )

            if (arbeidsgiverbeløp != 0 && orgnummer == "0") aktivitetslogg.info("Forventer ikke arbeidsgiveroppdrag til orgnummer \"0\".")

            val sendArbeidsgiveroppdrag = skalSendeOppdrag(arbeidsgiveroppdrag)

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
                    ?.takeIf { it.linjerUtenOpphør().isNotEmpty() }

            val personoppdrag = oppdrag(
                mottaker = personidentifikator.toString(),
                fagområde = Fagområde.Sykepenger,
                klassekode = Klassekode.SykepengerArbeidstakerFeriepenger,
                forrigeOppdrag = forrigeSendtePersonOppdrag,
                beløp = personbeløp
            )

            val sendPersonoppdrag = skalSendeOppdrag(personoppdrag)

            if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < -499 || differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson > 100) aktivitetslogg.info(
                """
                ${if (differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson < 0) "Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale" else "Utbetalt for lite i Infotrygd"} for person & orgnr-kombo:
                Arbeidsgiver: $orgnummer
                Diff: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygdTilPerson
                Hva vi har beregnet at IT har utbetalt til person for denne AG: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilPersonForDenneAktuelleArbeidsgiver
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                """.trimIndent()
            )

            val arbeidsgiveroppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(arbeidsgiveroppdrag).toString()
            val personoppdragdetaljer = PersonObserver.FeriepengerUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(personoppdrag).toString()
            // Logging
            aktivitetslogg.info(
                """
                Nøkkelverdier om feriepengeberegning
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
                ${feriepengeberegner.feriepengedatoer().let { datoer -> "Datoer vi skal utbetale feriepenger for (${datoer.size}): ${datoer.grupperSammenhengendePerioder().finere}" }}
                
                - OPPDRAG:
                Skal sende arbeidsgiveroppdrag til OS: $sendArbeidsgiveroppdrag
                Differanse fra forrige sendte arbeidsgoiveroppdrag: ${forrigeSendteArbeidsgiverOppdrag?.totalbeløp()?.minus(arbeidsgiveroppdrag.totalbeløp())}
                Arbeidsgiveroppdrag: $arbeidsgiveroppdragdetaljer
                
                Skal sende personoppdrag til OS: $sendPersonoppdrag
                Differanse fra forrige sendte personoppdrag: ${forrigeSendtePersonOppdrag?.totalbeløp()?.minus(personoppdrag.totalbeløp())}
                Personoppdrag: $personoppdragdetaljer
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
