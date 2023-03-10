package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.utbetalingslinjer.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import kotlin.properties.Delegates

val Oppdrag.inspektør get() = OppdragInspektør(this)

class OppdragInspektør(oppdrag: Oppdrag) : UtbetalingVisitor {
    private var linjeteller = 0
    private val endringskoder = mutableListOf<Endringskode>()
    private lateinit var fagsystemId: String
    lateinit var fagområde: Fagområde
        private set
    lateinit var endringskode: Endringskode
        private set
    lateinit var mottaker: String
        private set
    private val totalBeløp = mutableListOf<Int>()
    var nettoBeløp by Delegates.notNull<Int>()
        private set
    private val fom = mutableListOf<LocalDate>()
    private val tom = mutableListOf<LocalDate>()
    private val grad = mutableListOf<Int?>()
    private val beløp = mutableListOf<Int?>()
    private val datoStatusFom = mutableListOf<LocalDate?>()
    private val delytelseIder = mutableListOf<Int>()
    private val refDelytelseIder = mutableListOf<Int?>()
    private val refFagsystemIder = mutableListOf<String?>()
    var sisteArbeidsgiverdag: LocalDate? = null
    var overføringstidspunkt: LocalDateTime? = null
    var avstemmingsnøkkel: Long? = null
    private var status: Oppdragstatus? = null
    private var simuleringsResultat: SimuleringResultat? = null
    var periode: Periode = LocalDate.MIN.somPeriode()
        private set

    init {
        oppdrag.accept(this)
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        sisteArbeidsgiverdag: LocalDate?,
        stønadsdager: Int,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: SimuleringResultat?
    ) {
        this.fagsystemId = fagsystemId
        this.fagområde = fagområde
        this.endringskode = endringskode
        this.mottaker = mottaker
        this.nettoBeløp = nettoBeløp
        this.status = status
        this.simuleringsResultat = simuleringsResultat
        this.sisteArbeidsgiverdag = sisteArbeidsgiverdag
        this.avstemmingsnøkkel = avstemmingsnøkkel
        this.overføringstidspunkt = overføringstidspunkt
        sisteArbeidsgiverdag?.somPeriode()?.also {
            this.periode = it
        }
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        stønadsdager: Int,
        totalbeløp: Int,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Int?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
        linjeteller += 1
        endringskoder.add(endringskode)
        delytelseIder.add(delytelseId)
        refDelytelseIder.add(refDelytelseId)
        refFagsystemIder.add(refFagsystemId)
        this.fom.add(fom)
        this.tom.add(tom)
        this.grad.add(grad)
        this.beløp.add(beløp)
        this.datoStatusFom.add(datoStatusFom)
        datoStatusFom?.also { this.periode = this.periode.oppdaterFom(it) }
        this.periode = this.periode.oppdaterTom(tom)
    }

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
