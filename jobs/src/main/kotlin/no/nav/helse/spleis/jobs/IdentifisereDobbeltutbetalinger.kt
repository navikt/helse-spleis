package no.nav.helse.spleis.jobs

import java.security.MessageDigest
import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.serde.SerialisertPerson
import org.slf4j.MDC

fun finneDobbelutbetalinger(arbeidId: String) {
    opprettOgUtførArbeid(arbeidId, size = 10) { session, fnr ->
        hentPerson(session, fnr).let { data ->
            try {
                val dto = SerialisertPerson(data).tilPersonDto()
                dto.finnDobbeltutbetalinger()
            } catch (err: Exception) {
                log.info("person lar seg ikke serialisere: ${err.message}")
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun String.hash256(): String {
    val bytes = this.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(bytes).toHexString()
}

fun PersonInnDto.finnDobbeltutbetalinger() {
    MDC.putCloseable("fnr", fødselsnummer.hash256()).use {
        arbeidsgivere.forEach {
            it.finnDobbeltutbetalinger()
        }
    }
}

private fun List<UtbetalingInnDto>.aktive(): List<Utbetalingsak> {
    return this
        .groupBy { it.korrelasjonsId }
        .map { (_, utbetalinger) -> Utbetalingsak(utbetalinger) }
        .filter { utbetalinger -> utbetalinger.harVærtUtbetaltFør }
        .filterNot { utbetalinger -> utbetalinger.erAnnullert }
}

private data class Utbetalingsak(private val utbetalinger: List<UtbetalingInnDto>) {
    val sortertEtterTidsstempel = utbetalinger.sortedBy { it.tidsstempel }

    val fom = sortertEtterTidsstempel.last().periode.fom
    val tom = sortertEtterTidsstempel.last().periode.tom

    val sisteRefusjonOppdraglinje = sortertEtterTidsstempel
        .last()
        .arbeidsgiverOppdrag
        .linjer
        .filter { it.datoStatusFom == null }
        .maxOfOrNull { it.tom }
    val sistePersonOppdraglinje = sortertEtterTidsstempel
        .last()
        .personOppdrag
        .linjer
        .filter { it.datoStatusFom == null }
        .maxOfOrNull { it.tom }

    val sisteOppdragslinjeTom = listOfNotNull(sisteRefusjonOppdraglinje, sistePersonOppdraglinje).maxOfOrNull { it }

    val korrelasjonsId = utbetalinger.first().korrelasjonsId
    val sisteOverførte = sortertEtterTidsstempel.lastOrNull {
        when (it.tilstand) {
            UtbetalingTilstandDto.ANNULLERT,
            UtbetalingTilstandDto.GODKJENT,
            UtbetalingTilstandDto.OVERFØRT,
            UtbetalingTilstandDto.UTBETALT -> true

            UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING,
            UtbetalingTilstandDto.IKKE_GODKJENT,
            UtbetalingTilstandDto.IKKE_UTBETALT,
            UtbetalingTilstandDto.FORKASTET,
            UtbetalingTilstandDto.NY -> false
        }
    }
    val harVærtUtbetaltFør = sisteOverførte != null
    val erAnnullert = sisteOverførte?.tilstand == UtbetalingTilstandDto.ANNULLERT
}

private fun ArbeidsgiverInnDto.finnDobbeltutbetalinger() {
    val aktive = utbetalinger
        .aktive()
        .sortedBy { it.fom }

    val kø = aktive.toMutableList()
    while (kø.isNotEmpty()) {
        val sjekk = kø.removeAt(0)

        val overlappendeRefusjoner = oppdragOverlapper(kø, sjekk) { it.sisteOverførte!!.arbeidsgiverOppdrag }
        val overlappendeBruker = oppdragOverlapper(kø, sjekk) { it.sisteOverførte!!.personOppdrag }

        val overlappendeUtbetalinger = overlappendeRefusjoner + overlappendeBruker

        overlappendeUtbetalinger.onEach { kø.remove(it) }
    }
}

private fun ArbeidsgiverInnDto.oppdragOverlapper(aktive: List<Utbetalingsak>, denneUtbetalingen: Utbetalingsak, oppdragvelger: (Utbetalingsak) -> OppdragInnDto): List<Utbetalingsak> {
    val andreUtbetalinger = aktive
        .filter { it !== denneUtbetalingen }

    val oppdraget = oppdragvelger(denneUtbetalingen)
    val utbetalteDager = oppdraget
        .linjer
        .filter { it.datoStatusFom == null }
        .flatMap { linje ->
            linje
                .fom
                .datesUntil(linje.tom.plusDays(1))
                .filter { !it.erHelg() }
                .toList()
        }

    val utbetaltIAndreOppdrag = utbetalteDager
        .mapNotNull { utbetaltDag ->
            val utbetaltDobbelt = andreUtbetalinger
                .mapNotNull { annenUtbetaling ->
                    oppdragvelger(annenUtbetaling)
                        .linjer
                        .filter { it.datoStatusFom == null }
                        .firstOrNull { annenLinje -> utbetaltDag >= annenLinje.fom && utbetaltDag <= annenLinje.tom }
                        ?.let {
                            annenUtbetaling to it.beløp
                        }
                }
            val beløpUtbetaltDobbelt = utbetaltDobbelt.sumOf { it.second }
            if (beløpUtbetaltDobbelt == 0) return@mapNotNull null
            val utbetalingene = utbetaltDobbelt.map { it.first }
            Triple(utbetaltDag, beløpUtbetaltDobbelt, utbetalingene)
        }

    if (utbetaltIAndreOppdrag.isEmpty()) return emptyList()
    val utbetaltePerioder = utbetaltIAndreOppdrag
        .map { it.first }
        .grupperSammenhengendePerioderMedHensynTilHelg()
    val totaltUtbetaltDobbelt = utbetaltIAndreOppdrag.sumOf { it.second }

    val korreringen = denneUtbetalingen
        .sortertEtterTidsstempel
        .last { it.tilstand == UtbetalingTilstandDto.IKKE_UTBETALT }

    val deAndreUtbetalingene = utbetaltIAndreOppdrag
        .flatMap { it.third }
        .distinctBy { it.korrelasjonsId }

    deAndreUtbetalingene.forEach { annenUtbetaling ->
        // printer ut én linje per dobbelutbetaling
        val csvlinje = buildList {
            add(organisasjonsnummer)
            add(annenUtbetaling.sisteOverførte!!.id.toString())
            add(annenUtbetaling.fom.toString())
            add(annenUtbetaling.tom.toString())
            add(oppdraget.fagområde.toString())
            add(oppdragvelger(annenUtbetaling).linjer.sumOf { linje ->
                val virkedager = linje
                    .fom
                    .datesUntil(linje.tom.plusDays(1))
                    .filter { !it.erHelg() }
                    .count()
                linje.beløp * virkedager
            }.toString())
            add(annenUtbetaling.sisteOverførte.overføringstidspunkt!!.toLocalDate().toString())
            add(annenUtbetaling.sisteOverførte.avsluttet?.toLocalDate().toString())
        }
        sikkerlogg.info(csvlinje.joinToString(";"))
    }

    // printer ut én linje for hele dobbelutbetalingen
    /*val csvlinje = buildList {
        add(person.fødselsnummer)
        add(organisasjonsnummer)
        add(vedtaksperioden.id.toString())
        add(vedtaksperioden.periode.start.toString())
        add(vedtaksperioden.periode.endInclusive.toString())
        add(korreringen.id.toString())
        add(oppdraget.fagområde.toString())
        add(totaltUtbetaltDobbelt.toString())
        add(utbetaltePerioder.joinToString { "${it.start}-${it.endInclusive}" })
        add(deAndreUtbetalingene.joinToString { "${it.id}" })
    }
    println(csvlinje.joinToString(";"))*/
    return deAndreUtbetalingene
}
