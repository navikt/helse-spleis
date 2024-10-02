package no.nav.helse.inspectors

import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus

val Oppdrag.inspektør get() = OppdragInspektør(this)

class OppdragInspektør(oppdrag: Oppdrag) {
    private val linjeteller = oppdrag.size
    private val endringskoder = oppdrag.map { linje -> linje.endringskode }
    val fagsystemId: String = oppdrag.fagsystemId
    val fagområde: Fagområde = oppdrag.fagområde
    val mottaker: String = oppdrag.mottaker
    val endringskode: Endringskode = oppdrag.endringskode
    private val totalBeløp = oppdrag.map { linje -> linje.totalbeløp() }
    val nettoBeløp = oppdrag.nettoBeløp
    private val fom = oppdrag.map { linje -> linje.fom }
    private val tom = oppdrag.map { linje -> linje.tom }
    private val grad = oppdrag.map { linje -> linje.grad }
    private val beløp = oppdrag.map { linje -> linje.beløp }
    private val datoStatusFom = oppdrag.map { linje -> linje.datoStatusFom }
    private val delytelseIder = oppdrag.map { linje -> linje.delytelseId }
    private val refDelytelseIder = oppdrag.map { linje -> linje.refDelytelseId }
    private val refFagsystemIder = oppdrag.map { linje -> linje.refFagsystemId }
    val overføringstidspunkt = oppdrag.overføringstidspunkt
    val avstemmingsnøkkel = oppdrag.avstemmingsnøkkel
    val status: Oppdragstatus? = oppdrag.status
    val simuleringsResultat: SimuleringResultatDto? = oppdrag.simuleringsResultat
    val periode: Periode? = oppdrag.linjeperiode

    fun antallLinjer() = linjeteller
    fun endringskoder() = endringskoder.toList()
    fun fagsystemId() = fagsystemId
    fun delytelseId(indeks: Int) = delytelseIder.elementAt(indeks)
    fun refDelytelseId(indeks: Int) = refDelytelseIder.elementAt(indeks)
    fun refFagsystemId(indeks: Int) = refFagsystemIder.elementAt(indeks)
    fun fom(indeks: Int) = fom.elementAt(indeks)
    fun tom(indeks: Int) = tom.elementAt(indeks)
    fun beløp(indeks: Int) = beløp.elementAt(indeks)
    fun grad(indeks: Int) = grad.elementAt(indeks)
    fun endringskode(indeks: Int) = endringskoder.elementAt(indeks)
    fun datoStatusFom(indeks: Int) = datoStatusFom.elementAt(indeks)
    fun totalBeløp(indeks: Int) = totalBeløp.elementAt(indeks)
    fun status() = status
    fun simuleringsResultat() = simuleringsResultat
}
