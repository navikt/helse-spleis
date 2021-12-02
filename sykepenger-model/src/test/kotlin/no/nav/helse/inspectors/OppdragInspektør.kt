package no.nav.helse.inspectors

import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.*
import java.time.LocalDate
import java.time.LocalDateTime

internal val Oppdrag.inspektør get() = OppdragInspektør(this)

internal class OppdragInspektør(oppdrag: Oppdrag) : UtbetalingVisitor {
    private var linjeteller = 0
    private val fagsystemIder = mutableListOf<String>()
    private val totalBeløp = mutableListOf<Int>()
    private val nettoBeløp = mutableListOf<Int>()
    private val fom = mutableListOf<LocalDate>()
    private val tom = mutableListOf<LocalDate>()
    private val datoStatusFom = mutableListOf<LocalDate?>()
    private val delytelseIder = mutableListOf<Int>()
    private val refDelytelseIder = mutableListOf<Int?>()
    private val refFagsystemIder = mutableListOf<String?>()
    private var status: Oppdragstatus? = null
    private var simuleringsResultat: Simulering.SimuleringResultat? = null

    init {
        oppdrag.accept(this)
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        simuleringsResultat: Simulering.SimuleringResultat?
    ) {
        fagsystemIder.add(oppdrag.fagsystemId())
        this.nettoBeløp.add(nettoBeløp)
        this.status = status
        this.simuleringsResultat = simuleringsResultat
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Double?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        klassekode: Klassekode
    ) {
        linjeteller += 1
        delytelseIder.add(delytelseId)
        refDelytelseIder.add(refDelytelseId)
        refFagsystemIder.add(refFagsystemId)
        this.fom.add(fom)
        this.tom.add(tom)
        this.datoStatusFom.add(datoStatusFom)
    }

    fun antallLinjer() = linjeteller
    fun fagsystemId(indeks: Int) = fagsystemIder.elementAt(indeks)
    fun delytelseId(indeks: Int) = delytelseIder.elementAt(indeks)
    fun refDelytelseId(indeks: Int) = refDelytelseIder.elementAt(indeks)
    fun refFagsystemId(indeks: Int) = refFagsystemIder.elementAt(indeks)
    fun fom(indeks: Int) = fom.elementAt(indeks)
    fun tom(indeks: Int) = tom.elementAt(indeks)
    fun datoStatusFom(indeks: Int) = datoStatusFom.elementAt(indeks)
    fun totalBeløp(indeks: Int) = totalBeløp.elementAt(indeks)
    fun nettoBeløp(indeks: Int) = nettoBeløp.elementAt(indeks)
    fun status() = status
    fun simuleringsResultat() = simuleringsResultat
}
