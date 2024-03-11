package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.memento.EndringskodeMemento
import no.nav.helse.memento.FagområdeMemento
import no.nav.helse.memento.OppdragMemento
import no.nav.helse.memento.OppdragstatusMemento
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OS_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OS_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AVVIST
import no.nav.helse.utbetalingslinjer.Oppdragstatus.FEIL
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.kjedeSammenLinjer
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.kobleTil
import no.nav.helse.utbetalingslinjer.Utbetalingslinje.Companion.normaliserLinjer

class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: MutableList<Utbetalingslinje>,
    private val fagsystemId: String,
    private val endringskode: Endringskode,
    private var nettoBeløp: Int = linjer.sumOf { it.totalbeløp() },
    private var overføringstidspunkt: LocalDateTime? = null,
    private var avstemmingsnøkkel: Long? = null,
    private var status: Oppdragstatus? = null,
    private val tidsstempel: LocalDateTime,
    private var erSimulert: Boolean = false,
    private var simuleringsResultat: SimuleringResultat? = null
) : List<Utbetalingslinje> by linjer, Aktivitetskontekst {
    companion object {
        fun periode(vararg oppdrag: Oppdrag): Periode? {
            return oppdrag
                .mapNotNull { it.linjeperiode }
                .takeIf(List<*>::isNotEmpty)
                ?.reduce(Periode::plus)
        }

        fun List<Oppdrag>.trekkerTilbakePenger() = sumOf { it.nettoBeløp() } < 0

        fun stønadsdager(vararg oppdrag: Oppdrag): Int {
            return Utbetalingslinje.stønadsdager(oppdrag.toList().flatten())
        }

        fun synkronisert(vararg oppdrag: Oppdrag): Boolean {
            val endrede = oppdrag.filter { it.harUtbetalinger() }
            return endrede.all { it.status == endrede.first().status }
        }

        fun ingenFeil(vararg oppdrag: Oppdrag) = oppdrag.none { it.status in listOf(AVVIST, FEIL) }
        fun harFeil(vararg oppdrag: Oppdrag) = oppdrag.any { it.status in listOf(AVVIST, FEIL) }
        fun kanIkkeForsøkesPåNy(vararg oppdrag: Oppdrag) = oppdrag.any { it.status == AVVIST }
        fun ferdigOppdrag(
            mottaker: String,
            from: Fagområde,
            utbetalingslinjer: List<Utbetalingslinje>,
            fagsystemId: String,
            endringskode: Endringskode,
            nettoBeløp: Int,
            overføringstidspunkt: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            status: Oppdragstatus?,
            tidsstempel: LocalDateTime,
            erSimulert: Boolean,
            simuleringResultat: SimuleringResultat?
        ): Oppdrag = Oppdrag(
            mottaker = mottaker,
            fagområde = from,
            linjer = utbetalingslinjer.toMutableList(),
            fagsystemId = fagsystemId,
            endringskode = endringskode,
            nettoBeløp = nettoBeløp,
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            status = status,
            tidsstempel = tidsstempel,
            erSimulert = erSimulert,
            simuleringsResultat = simuleringResultat
        )

        internal fun List<Oppdrag>.valider(aktivitetslogg: IAktivitetslogg) {
            if (all { it.nettoBeløp >= 0 }) return
            aktivitetslogg.varsel(RV_UT_23)
        }
    }

    private val linjeperiode get() = firstOrNull()?.let { (it.datoStatusFom() ?: it.fom) til last().tom }

    constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID())
    ) : this(
        mottaker,
        fagområde,
        normaliserLinjer(fagsystemId, linjer).toMutableList(),
        fagsystemId,
        Endringskode.NY,
        tidsstempel = LocalDateTime.now()
    )

    fun accept(visitor: OppdragVisitor) {
        visitor.preVisitOppdrag(
            this,
            fagområde,
            fagsystemId,
            mottaker,
            stønadsdager(),
            totalbeløp(),
            nettoBeløp,
            tidsstempel,
            endringskode,
            avstemmingsnøkkel,
            status,
            overføringstidspunkt,
            erSimulert,
            simuleringsResultat
        )
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(
            this,
            fagområde,
            fagsystemId,
            mottaker,
            stønadsdager(),
            totalbeløp(),
            nettoBeløp,
            tidsstempel,
            endringskode,
            avstemmingsnøkkel,
            status,
            overføringstidspunkt,
            erSimulert,
            simuleringsResultat
        )
    }

    fun fagsystemId() = fagsystemId
    fun mottaker() = mottaker

    private operator fun contains(other: Oppdrag) = this.tilhører(other) || this.overlapperMed(other)

    fun tilhører(other: Oppdrag) = this.fagsystemId == other.fagsystemId && this.fagområde == other.fagområde
    private fun overlapperMed(other: Oppdrag) =
        this.linjeperiode?.let { other.linjeperiode?.overlapperMed(it) } ?: false

    fun overfør(
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate?,
        saksbehandler: String
    ) {
        if (status == Oppdragstatus.AKSEPTERT) return aktivitetslogg.info("Overfører ikke oppdrag som allerede er akseptert for fagområde=$fagområde med fagsystemId=$fagsystemId")
        if (!harUtbetalinger()) return aktivitetslogg.info("Overfører ikke oppdrag uten endring for fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        aktivitetslogg.kontekst(this)
        aktivitetslogg.behov(Behovtype.Utbetaling, "Trenger å sende utbetaling til Oppdrag", behovdetaljer(saksbehandler, maksdato))
    }

    fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String) {
        if (!harUtbetalinger()) return aktivitetslogg.info("Simulerer ikke oppdrag uten endring fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        check(status == null)
        aktivitetslogg.kontekst(this)
        aktivitetslogg.behov(Behovtype.Simulering, "Trenger simulering fra Oppdragssystemet", behovdetaljer(saksbehandler, maksdato))
    }

    override fun toSpesifikkKontekst() = SpesifikkKontekst("Oppdrag", mapOf("fagsystemId" to fagsystemId))

    private fun behovdetaljer(saksbehandler: String, maksdato: LocalDate?): MutableMap<String, Any> {
        return mutableMapOf(
            "mottaker" to mottaker,
            "fagområde" to "$fagområde",
            "linjer" to kopierKunLinjerMedEndring().map(Utbetalingslinje::behovdetaljer),
            "fagsystemId" to fagsystemId,
            "endringskode" to "$endringskode",
            "saksbehandler" to saksbehandler
        ).apply {
            maksdato?.let {
                put("maksdato", maksdato.toString())
            }
        }
    }

    fun totalbeløp() = linjerUtenOpphør().sumOf { it.totalbeløp() }
    fun stønadsdager() = sumOf { it.stønadsdager() }

    fun nettoBeløp() = nettoBeløp

    private fun nettoBeløp(tidligere: Oppdrag) {
        nettoBeløp = this.totalbeløp() - tidligere.totalbeløp()
    }

    fun harUtbetalinger() = any(Utbetalingslinje::erForskjell)

    fun erRelevant(fagsystemId: String, fagområde: Fagområde) =
        this.fagsystemId == fagsystemId && this.fagområde == fagområde

    fun valider(simulering: Simulering) =
        simulering.valider(this)

    private fun kopierKunLinjerMedEndring() = kopierMed(filter(Utbetalingslinje::erForskjell))

    private fun kopierUtenOpphørslinjer() = kopierMed(linjerUtenOpphør())

    fun linjerUtenOpphør() = filter { !it.erOpphør() }

    fun annuller(aktivitetslogg: IAktivitetslogg): Oppdrag {
        return tomtOppdrag().minus(this, aktivitetslogg)
    }

    private fun tomtOppdrag(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId
        )

    fun begrensFra(førsteDag: LocalDate): Oppdrag {
        val (senereLinjer, tidligereLinjer) = this.linjer
            .filterNot { it.erOpphør() }
            .partition { it.fom >= førsteDag }
        val delvisOverlappendeFørsteLinje = tidligereLinjer
            .lastOrNull()
            ?.takeIf { it.tom >= førsteDag }
            ?.begrensFra(førsteDag)
        return kopierMed(listOfNotNull(delvisOverlappendeFørsteLinje) + senereLinjer)
    }

    fun begrensTil(sisteDato: LocalDate, other: Oppdrag? = null): Oppdrag {
        val (tidligereLinjer, senereLinjer) = this.linjer.partition { it.tom <= sisteDato }
        val delvisOverlappendeSisteLinje = senereLinjer
            .firstOrNull()
            ?.takeIf { it.fom <= sisteDato }
            ?.begrensTil(sisteDato)
        other?.also { kobleTil(it) }
        return kopierMed(tidligereLinjer + listOfNotNull(delvisOverlappendeSisteLinje))
    }

    operator fun plus(other: Oppdrag): Oppdrag {
        check(none { linje -> other.any { it.periode.overlapperMed(linje.periode) } }) {
            "ikke støttet: kan ikke overlappe med annet oppdrag"
        }
        if (this.isNotEmpty() && other.isNotEmpty() && this.fomHarFlyttetSegFremover(other)) return other + this
        return kopierMed((slåSammenOppdrag(other)))
    }

    private fun slåSammenOppdrag(other: Oppdrag): List<Utbetalingslinje> {
        if (this.isEmpty()) return other.linjer
        if (other.isEmpty()) return this.linjer
        val sisteLinje = this.first()
        val første = other.first()
        val mellomlinje = sisteLinje.slåSammenLinje(første) ?: return this.linjer + other.linjer
        return this.linjer.dropLast(1) + listOf(mellomlinje) + other.linjer.drop(1)
    }

    fun minus(eldre: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
        // overtar fagsystemId fra tidligere Oppdrag uten utbetaling, gitt at det er samme arbeidsgiverperiode
        if (eldre.erTomt()) return medFagsystemId(eldre)
        return when {
            // om man trekker fra et utbetalt oppdrag med et tomt oppdrag medfører det et oppdrag som opphører (les: annullerer) hele fagsystemIDen
            erTomt() -> annulleringsoppdrag(eldre)
            eldre.ingenUtbetalteDager() -> kjørFrem(eldre)
            // "fom" kan flytte seg fremover i tid dersom man, eksempelvis, revurderer en utbetalt periode til å starte med ikke-utbetalte dager (f.eks. ferie)
            fomHarFlyttetSegFremover(eldre.kopierUtenOpphørslinjer()) -> {
                aktivitetslogg.varsel(RV_OS_2)
                returførOgKjørFrem(eldre.kopierUtenOpphørslinjer())
            }
            // utbetaling kan endres til å starte tidligere, eksempelvis via revurdering der feriedager egentlig er sykedager
            fomHarFlyttetSegBakover(eldre.kopierUtenOpphørslinjer()) -> {
                aktivitetslogg.varsel(RV_OS_2)
                kjørFrem(eldre.kopierUtenOpphørslinjer())
            }
            // fom er lik, men endring kan oppstå overalt ellers
            else -> endre(eldre.kopierUtenOpphørslinjer(), aktivitetslogg)
        }.also { it.nettoBeløp(eldre) }
    }

    private fun ingenUtbetalteDager() = linjerUtenOpphør().isEmpty()

    private fun erTomt() = this.isEmpty()

    // Vi har oppdaget utbetalingsdager tidligere i tidslinjen
    private fun fomHarFlyttetSegBakover(eldre: Oppdrag) = this.first().fom < eldre.first().fom

    // Vi har endret tidligere utbetalte dager til ikke-utbetalte dager i starten av tidslinjen
    private fun fomHarFlyttetSegFremover(eldre: Oppdrag) = this.first().fom > eldre.first().fom

    // man opphører (annullerer) et annet oppdrag ved å lage en opphørslinje som dekker hele perioden som er utbetalt
    // om det forrige oppdraget også var et opphør så kopieres siste linje for å bevare
    // delytelseId-rekkefølgen slik at det nye oppdraget kan bygges videre på
    private fun annulleringsoppdrag(tidligere: Oppdrag) =
        if (tidligere.kopierUtenOpphørslinjer().erTomt()) kopierMed(
            linjer = listOf(tidligere.last().markerUendret(tidligere.last())),
            fagsystemId = tidligere.fagsystemId,
            endringskode = Endringskode.UEND
        )
        else kopierMed(
            linjer = listOf(tidligere.last().opphørslinje(tidligere.kopierUtenOpphørslinjer().first().fom)),
            fagsystemId = tidligere.fagsystemId,
            endringskode = Endringskode.ENDR
        )
    // når man oppretter en NY linje med dato-intervall "(a, b)" vil oppdragsystemet
    // automatisk opphøre alle eventuelle linjer med fom > b.
    //
    // Eksempel:
    // Oppdrag 1: 5. januar til 31. januar (linje 1)
    // Oppdrag 2: 1. januar til 10. januar
    // Fordi linje "1. januar - 10. januar" opprettes som NY, medfører dette at oppdragsystemet opphører 11. januar til 31. januar automatisk
    private fun kjørFrem(tidligere: Oppdrag): Oppdrag {
        val sammenkoblet = this.kobleTil(tidligere)
        val linjer = kjedeSammenLinjer(sammenkoblet, tidligere.last())
        return sammenkoblet.kopierMed(linjer)
    }


    private fun endre(avtroppendeOppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg) =
        DifferanseBuilder(this).kalkulerDifferanse(avtroppendeOppdrag, aktivitetslogg)

    // når man oppretter en NY linje vil Oppdragsystemet IKKE ta stilling til periodene FØR.
    // Man må derfor eksplisitt opphøre evt. perioder tidligere, som i praksis vil medføre at
    // oppdraget kjøres tilbake, så fremover
    private fun returførOgKjørFrem(tidligere: Oppdrag): Oppdrag {
        val deletion = this.opphørOppdrag(tidligere)
        val kjørtFrem = this.kjørFrem(tidligere)
        return kjørtFrem.kopierMed(listOf(deletion) + kjørtFrem.linjer)
    }

    private fun opphørOppdrag(tidligere: Oppdrag) =
        tidligere.last().opphørslinje(tidligere.first().fom)


    private fun medFagsystemId(other: Oppdrag) = kopierMed(this.linjer, fagsystemId = other.fagsystemId)

    private fun kopierMed(linjer: List<Utbetalingslinje>, fagsystemId: String = this.fagsystemId, endringskode: Endringskode = this.endringskode) = Oppdrag(
        mottaker = mottaker,
        fagområde = fagområde,
        linjer = linjer.map { it.kopier() }.toMutableList(),
        fagsystemId = fagsystemId,
        endringskode = endringskode,
        overføringstidspunkt = overføringstidspunkt,
        avstemmingsnøkkel = avstemmingsnøkkel,
        status = status,
        tidsstempel = tidsstempel,
        erSimulert = erSimulert,
        simuleringsResultat = simuleringsResultat
    )

    private fun kobleTil(tidligere: Oppdrag) = kopierMed(
        linjer.kobleTil(tidligere.fagsystemId),
        tidligere.fagsystemId,
        Endringskode.ENDR
    )

    fun lagreOverføringsinformasjon(hendelse: UtbetalingHendelse) {
        if (!hendelse.erRelevant(fagsystemId)) return
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = hendelse.avstemmingsnøkkel
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = hendelse.overføringstidspunkt
        this.status = hendelse.status
    }
    fun håndter(simulering: Simulering) {
        if (!simulering.erSimulert(fagområde, fagsystemId)) return
        this.erSimulert = true
        this.simuleringsResultat = simulering.simuleringResultat
    }
    fun erKlarForGodkjenning() = !harUtbetalinger() || erSimulert

    private class DifferanseBuilder(
        private val påtroppendeOppdrag: Oppdrag
    ) {
        private lateinit var tilstand: Tilstand
        private lateinit var sisteLinjeITidligereOppdrag: Utbetalingslinje

        private lateinit var linkTo: Utbetalingslinje

        // forsøker så langt det lar seg gjøre å endre _siste_ linje, dersom mulig *)
        // ellers lager den NY linjer fra og med linja før endringen oppstod
        // *) en linje kan endres dersom "tom"-dato eller grad er eneste forskjell
        //    ulik dagsats eller fom-dato medfører enten at linjen får status OPPH, eller at man overskriver
        //    ved å sende NY linjer
        fun kalkulerDifferanse(avtroppendeOppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
            this.linkTo = avtroppendeOppdrag.last()
            val kobletTil = påtroppendeOppdrag.kobleTil(avtroppendeOppdrag)
            val medLinkeLinjer = kopierLikeLinjer(kobletTil, avtroppendeOppdrag, aktivitetslogg)
            if (medLinkeLinjer.last().erForskjell()) return medLinkeLinjer
            return medLinkeLinjer.kopierMed(medLinkeLinjer.linjer, endringskode = Endringskode.UEND)
        }

        private fun opphørTidligereLinjeOgOpprettNy(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje,
            aktivitetslogg: IAktivitetslogg,
            datoStatusFom: LocalDate = tidligere.fom
        ): List<Utbetalingslinje> {
            linkTo = tidligere
            val opphørslinje = tidligere.opphørslinje(datoStatusFom)
            val linketTilForrige = nåværende.kobleTil(linkTo)
            linkTo = linketTilForrige
            tilstand = Ny()
            aktivitetslogg.varsel(RV_OS_3)
            return listOf(opphørslinje, linketTilForrige)
        }

        private fun kopierLikeLinjer(nytt: Oppdrag, tidligere: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
            tilstand = if (tidligere.last().tom > nytt.last().tom) Slett(nytt.last()) else Identisk()
            sisteLinjeITidligereOppdrag = tidligere.last()
            val linjer = nytt.zip(tidligere).map { (a, b) -> tilstand.håndterForskjell(a, b, aktivitetslogg) }.flatten()
            val remaining = (nytt.size - minOf(nytt.size, tidligere.size)).coerceAtLeast(0)
            val nyeLinjer = nytt.takeLast(remaining)
            return nytt.kopierMed(linjer + kjedeSammenLinjer(nyeLinjer, linjer.last()))
        }

        private fun håndterUlikhet(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg): List<Utbetalingslinje> {
            return when {
                nåværende.kanEndreEksisterendeLinje(tidligere, sisteLinjeITidligereOppdrag) -> listOf(nåværende.endreEksisterendeLinje(tidligere))
                nåværende.skalOpphøreOgErstatte(tidligere, sisteLinjeITidligereOppdrag) -> opphørTidligereLinjeOgOpprettNy(nåværende, tidligere, aktivitetslogg)
                nåværende.fom > tidligere.fom -> opphørTidligereLinjeOgOpprettNy(nåværende, sisteLinjeITidligereOppdrag, aktivitetslogg, tidligere.fom)
                else -> listOf(opprettNyLinje(nåværende))
            }
        }

        private fun opprettNyLinje(nåværende: Utbetalingslinje): Utbetalingslinje {
            val nyLinje = nåværende.kobleTil(linkTo)
            linkTo = nyLinje
            tilstand = Ny()
            return nyLinje
        }


        private interface Tilstand {
            fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg): List<Utbetalingslinje>
        }

        private inner class Identisk : Tilstand {
            override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg): List<Utbetalingslinje> {
                if (nåværende == tidligere) return listOf(nåværende.markerUendret(tidligere))
                return håndterUlikhet(nåværende, tidligere, aktivitetslogg)
            }
        }

        private inner class Slett(private val sisteLinjeINyttOppdrag: Utbetalingslinje) : Tilstand {
            override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg): List<Utbetalingslinje> {
                if (nåværende == tidligere) {
                    if (nåværende == sisteLinjeINyttOppdrag) return listOf(nåværende.kobleTil(linkTo))
                    return listOf(nåværende.markerUendret(tidligere))
                }
                return håndterUlikhet(nåværende, tidligere, aktivitetslogg)
            }
        }

        private inner class Ny : Tilstand {
            override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg): List<Utbetalingslinje> {
                val nyLinje = nåværende.kobleTil(linkTo)
                linkTo = nyLinje
                return listOf(nyLinje)
            }
        }
    }

    fun memento() = OppdragMemento(
        mottaker = mottaker,
        fagområde = when (fagområde) {
            Fagområde.SykepengerRefusjon -> FagområdeMemento.SPREF
            Fagområde.Sykepenger -> FagområdeMemento.SP
        },
        linjer = linjer.map { it.memento() },
        fagsystemId = fagsystemId,
        endringskode = when (endringskode) {
            Endringskode.NY -> EndringskodeMemento.NY
            Endringskode.UEND -> EndringskodeMemento.UEND
            Endringskode.ENDR -> EndringskodeMemento.ENDR
        },
        nettoBeløp = nettoBeløp,
        overføringstidspunkt = overføringstidspunkt,
        avstemmingsnøkkel = avstemmingsnøkkel,
        status = when (status) {
            Oppdragstatus.OVERFØRT -> OppdragstatusMemento.OVERFØRT
            Oppdragstatus.AKSEPTERT -> OppdragstatusMemento.AKSEPTERT
            Oppdragstatus.AKSEPTERT_MED_FEIL -> OppdragstatusMemento.AKSEPTERT_MED_FEIL
            Oppdragstatus.AVVIST -> OppdragstatusMemento.AVVIST
            Oppdragstatus.FEIL -> OppdragstatusMemento.FEIL
            null -> null
        },
        tidsstempel = tidsstempel,
        erSimulert = erSimulert,
        simuleringsResultat = simuleringsResultat
    )
}

enum class Oppdragstatus { OVERFØRT, AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL }