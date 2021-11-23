package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal const val WARN_FORLENGER_OPPHØRT_OPPDRAG = "Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen."
internal const val WARN_OPPDRAG_FOM_ENDRET = "Utbetaling fra og med dato er endret. Kontroller simuleringen"

internal class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: MutableList<Utbetalingslinje>,
    private var fagsystemId: String,
    private var endringskode: Endringskode,
    private val sisteArbeidsgiverdag: LocalDate?,
    private var nettoBeløp: Int = linjer.sumOf { it.totalbeløp() },
    private var overføringstidspunkt: LocalDateTime? = null,
    private var avstemmingsnøkkel: Long? = null,
    private var status: Oppdragstatus? = null,
    private val tidsstempel: LocalDateTime
) : MutableList<Utbetalingslinje> by linjer {
    internal companion object {
        internal fun periode(vararg oppdrag: Oppdrag): Periode {
            return oppdrag
                .filter(Oppdrag::isNotEmpty)
                .takeIf(List<*>::isNotEmpty)
                ?.let { liste -> Periode(liste.minOf { it.førstedato }, liste.maxOf { it.sistedato }) }
                ?: Periode(LocalDate.MIN, LocalDate.MAX)
        }

        internal fun stønadsdager(vararg oppdrag: Oppdrag): Int {
            return Utbetalingslinje.stønadsdager(oppdrag.toList().flatten())
        }

        internal fun synkronisert(vararg oppdrag: Oppdrag): Boolean {
            val endrede = oppdrag.filter { it.harUtbetalinger() }
            return endrede.all { it.status == endrede.first().status }
        }
    }

    internal val førstedato get() = linjer.firstOrNull()?.let { it.datoStatusFom() ?: it.fom } ?: LocalDate.MIN
    internal val sistedato get() = linjer.lastOrNull()?.tom ?: LocalDate.MIN

    internal constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
        sisteArbeidsgiverdag: LocalDate?
    ) : this(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        Endringskode.NY,
        sisteArbeidsgiverdag,
        tidsstempel = LocalDateTime.now()
    )

    internal constructor(mottaker: String, fagområde: Fagområde) :
        this(mottaker, fagområde, sisteArbeidsgiverdag = null)

    internal fun accept(visitor: OppdragVisitor) {
        visitor.preVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel, endringskode, avstemmingsnøkkel, status, overføringstidspunkt,)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel, endringskode, avstemmingsnøkkel, status, overføringstidspunkt)
    }

    internal fun mottaker() = mottaker
    internal fun fagområde() = fagområde
    internal fun fagsystemId() = fagsystemId

    internal operator fun contains(other: Oppdrag) = this.tilhører(other) || this.overlapperMed(other)

    private fun tilhører(other: Oppdrag) = this.fagsystemId == other.fagsystemId && this.fagområde == other.fagområde
    private fun overlapperMed(other: Oppdrag) = maxOf(this.førstedato, other.førstedato) <= minOf(this.sistedato, other.sistedato) || sammeArbeidsgiverperiode(other)

    private fun sammeArbeidsgiverperiode(other: Oppdrag) =
        this.sisteArbeidsgiverdag != null && this.sisteArbeidsgiverdag == other.sisteArbeidsgiverdag

    internal fun overfør(
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate?,
        saksbehandler: String
    ) {
        if (!harUtbetalinger()) return aktivitetslogg.info("Overfører ikke oppdrag uten endring for fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        fagområde.overfør(aktivitetslogg, behovdetaljer(saksbehandler, maksdato))
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String) {
        if (!harUtbetalinger()) return aktivitetslogg.info("Simulerer ikke oppdrag uten endring fagområde=$fagområde med fagsystemId=$fagsystemId")
        check(endringskode != Endringskode.UEND)
        fagområde.simuler(aktivitetslogg, behovdetaljer(saksbehandler, maksdato))
    }

    private fun behovdetaljer(saksbehandler: String, maksdato: LocalDate?): MutableMap<String, Any> {
        return mutableMapOf(
            "mottaker" to mottaker,
            "fagområde" to "$fagområde",
            "linjer" to kopierKunLinjerMedEndring().map(Utbetalingslinje::toMap),
            "fagsystemId" to fagsystemId,
            "endringskode" to "$endringskode",
            "saksbehandler" to saksbehandler
        ).apply {
            maksdato?.let {
                put("maksdato", maksdato.toString())
            }
        }
    }

    internal fun totalbeløp() = linjerUtenOpphør().sumOf { it.totalbeløp() }
    internal fun stønadsdager() = sumOf { it.stønadsdager() }

    internal fun nettoBeløp() = nettoBeløp

    internal fun nettoBeløp(tidligere: Oppdrag) {
        nettoBeløp = this.totalbeløp() - tidligere.totalbeløp()
    }

    internal fun harUtbetalinger() = any(Utbetalingslinje::erForskjell)

    internal fun erRelevant(fagsystemId: String, fagområde: Fagområde) =
        this.fagsystemId == fagsystemId && this.fagområde == fagområde

    internal fun sammenlignMed(simulering: Simulering) =
        simulering.valider(kopierKunLinjerMedEndring())

    private fun kopierKunLinjerMedEndring() = kopierMed(filter(Utbetalingslinje::erForskjell))

    private fun kopierUtenOpphørslinjer() = kopierMed(linjerUtenOpphør())

    internal fun linjerUtenOpphør() = filter { !it.erOpphør() }

    internal fun erForskjelligFra(resultat: Simulering.SimuleringResultat): Boolean {
        return dagSatser().zip(dagSatser(resultat, førstedato, sistedato)).any { (oppdrag, simulering) ->
            oppdrag.first != simulering.first || oppdrag.second != simulering.second
        }
    }

    private fun dagSatser() = linjerUtenOpphør().flatMap { linje -> linje.dager().map { it to linje.beløp } }

    private fun dagSatser(resultat: Simulering.SimuleringResultat, fom: LocalDate, tom: LocalDate) =
        resultat.perioder.flatMap {
            it.utbetalinger.flatMap {
                it.detaljer.flatMap { detalj ->
                    detalj.periode.start.datesUntil(detalj.periode.endInclusive.plusDays(1))
                        .filter { it >= fom && it <= tom }
                        .filter { !it.erHelg() }
                        .map { it to detalj.sats.sats }
                        .toList()
                }
            }
        }

    internal fun annuller(aktivitetslogg: IAktivitetslogg): Oppdrag {
        return tomtOppdrag().minus(this, aktivitetslogg)
    }

    private fun tomtOppdrag(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            sisteArbeidsgiverdag = sisteArbeidsgiverdag
        )

    internal fun minus(eldre: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
        return when {
            harIngenKoblingTilTidligereOppdrag(eldre) -> this
            // overtar fagsystemId fra tidligere Oppdrag uten utbetaling, gitt at det er samme arbeidsgiverperiode
            eldre.erTomt() -> this.also { this.fagsystemId = eldre.fagsystemId }
            // Vi ønsker ikke å forlenge et oppdrag vi ikke overlapper med, eller et tomt oppdrag
            // om man trekker fra et utbetalt oppdrag med et tomt oppdrag medfører det et oppdrag som opphører (les: annullerer) hele fagsystemIDen
            erTomt() -> annulleringsoppdrag(eldre)
            // "fom" kan flytte seg fremover i tid dersom man, eksempelvis, revurderer en utbetalt periode til å starte med ikke-utbetalte dager (f.eks. ferie)
            eldre.ingenUtbetalteDager() -> {
                aktivitetslogg.warn(WARN_FORLENGER_OPPHØRT_OPPDRAG)
                kjørFrem(eldre)
            }
            fomHarFlyttetSegFremover(eldre.kopierUtenOpphørslinjer()) -> {
                aktivitetslogg.warn("Utbetaling opphører tidligere utbetaling. Kontroller simuleringen")
                returførOgKjørFrem(eldre.kopierUtenOpphørslinjer())
            }
            // utbetaling kan endres til å starte tidligere, eksempelvis via revurdering der feriedager egentlig er sykedager
            fomHarFlyttetSegBakover(eldre.kopierUtenOpphørslinjer()) -> {
                aktivitetslogg.warn(WARN_OPPDRAG_FOM_ENDRET)
                kjørFrem(eldre.kopierUtenOpphørslinjer())
            }
            // fom er lik, men endring kan oppstå overalt ellers
            else -> endre(eldre.kopierUtenOpphørslinjer(), aktivitetslogg)
        }
    }

    private fun harIngenKoblingTilTidligereOppdrag(eldre: Oppdrag) = (eldre.erTomt() && !sammeArbeidsgiverperiode(eldre)) || this !in eldre

    private fun ingenUtbetalteDager() = linjerUtenOpphør().isEmpty()

    private fun erTomt() = this.isEmpty()

    // Vi har oppdaget utbetalingsdager tidligere i tidslinjen
    private fun fomHarFlyttetSegBakover(eldre: Oppdrag) = this.førstedato < eldre.førstedato

    // Vi har endret tidligere utbetalte dager til ikke-utbetalte dager i starten av tidslinjen
    private fun fomHarFlyttetSegFremover(eldre: Oppdrag) = this.førstedato > eldre.førstedato

    // man opphører (annullerer) et annet oppdrag ved å lage en opphørslinje som dekker hele perioden som er utbetalt
    private fun annulleringsoppdrag(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        linjer.add(tidligere.last().opphørslinje(tidligere.first().fom))
    }

    // når man oppretter en NY linje med dato-intervall "(a, b)" vil oppdragsystemet
    // automatisk opphøre alle eventuelle linjer med fom > b.
    //
    // Eksempel:
    // Oppdrag 1: 5. januar til 31. januar (linje 1)
    // Oppdrag 2: 1. januar til 10. januar
    // Fordi linje "1. januar - 10. januar" opprettes som NY, medfører dette at oppdragsystemet opphører 11. januar til 31. januar automatisk
    private fun kjørFrem(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        nåværende.first().kobleTil(tidligere.last())
        nåværende.zipWithNext { a, b -> b.kobleTil(a) }
    }
    private lateinit var tilstand: Tilstand
    private lateinit var sisteLinjeITidligereOppdrag: Utbetalingslinje

    private lateinit var linkTo: Utbetalingslinje

    // forsøker så langt det lar seg gjøre å endre _siste_ linje, dersom mulig *)
    // ellers lager den NY linjer fra og med linja før endringen oppstod
    // *) en linje kan endres dersom "tom"-dato eller grad er eneste forskjell
    //    ulik dagsats eller fom-dato medfører enten at linjen får status OPPH, eller at man overskriver
    //    ved å sende NY linjer
    private fun endre(avtroppendeOppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg) =
        this.also { påtroppendeOppdrag ->
            this.linkTo = avtroppendeOppdrag.last()
            påtroppendeOppdrag.kobleTil(avtroppendeOppdrag)
            påtroppendeOppdrag.kopierLikeLinjer(avtroppendeOppdrag, aktivitetslogg)
            påtroppendeOppdrag.håndterLengreNåværende(avtroppendeOppdrag)
            if (!påtroppendeOppdrag.last().erForskjell()) påtroppendeOppdrag.endringskode = Endringskode.UEND
        }

    // når man oppretter en NY linje vil Oppdragsystemet IKKE ta stilling til periodene FØR.
    // Man må derfor eksplisitt opphøre evt. perioder tidligere, som i praksis vil medføre at
    // oppdraget kjøres tilbake, så fremover
    private fun returførOgKjørFrem(tidligere: Oppdrag) = this.also { nåværende ->
        val deletion = nåværende.opphørOppdrag(tidligere)
        nåværende.kjørFrem(tidligere)
        nåværende.add(0, deletion)
    }

    private fun opphørTidligereLinjeOgOpprettNy(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
        linkTo = tidligere
        add(this.indexOf(nåværende), tidligere.opphørslinje(tidligere.fom))
        nåværende.kobleTil(linkTo)
        tilstand = Ny()
        aktivitetslogg.warn("Endrer tidligere oppdrag. Kontroller simuleringen.")
    }

    private fun opphørOppdrag(tidligere: Oppdrag) =
        tidligere.last().opphørslinje(tidligere.førstedato)

    private fun kopierMed(linjer: List<Utbetalingslinje>) = Oppdrag(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        endringskode,
        sisteArbeidsgiverdag,
        overføringstidspunkt = overføringstidspunkt,
        avstemmingsnøkkel = avstemmingsnøkkel,
        status = status,
        tidsstempel = tidsstempel
    )

    private fun kopierLikeLinjer(tidligere: Oppdrag, aktivitetslogg: IAktivitetslogg) {
        tilstand = if (tidligere.sistedato > this.sistedato) Slett() else Identisk()
        sisteLinjeITidligereOppdrag = tidligere.last()
        this.zip(tidligere).forEach { (a, b) -> tilstand.håndterForskjell(a, b, aktivitetslogg) }
    }

    private fun håndterLengreNåværende(tidligere: Oppdrag) {
        if (this.size <= tidligere.size) return
        this[tidligere.size].kobleTil(linkTo)
        this
            .subList(tidligere.size, this.size)
            .zipWithNext { a, b -> b.kobleTil(a) }
    }

    private fun kobleTil(tidligere: Oppdrag) {
        this.fagsystemId = tidligere.fagsystemId
        this.forEach { it.refFagsystemId = tidligere.fagsystemId }
        this.endringskode = Endringskode.ENDR
    }

    private fun håndterUlikhet(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
        when {
            nåværende.kanEndreEksisterendeLinje(tidligere, sisteLinjeITidligereOppdrag) -> nåværende.endreEksisterendeLinje(tidligere)
            nåværende.skalOpphøreOgErstatte(tidligere, sisteLinjeITidligereOppdrag) -> opphørTidligereLinjeOgOpprettNy(nåværende, tidligere, aktivitetslogg)
            else -> opprettNyLinje(nåværende)
        }
    }

    private fun opprettNyLinje(nåværende: Utbetalingslinje) {
        nåværende.kobleTil(linkTo)
        linkTo = nåværende
        tilstand = Ny()
    }

    internal fun toMap() = mapOf(
        "mottaker" to mottaker,
        "fagområde" to "$fagområde",
        "linjer" to map(Utbetalingslinje::toMap),
        "fagsystemId" to fagsystemId,
        "endringskode" to "$endringskode",
        "sisteArbeidsgiverdag" to sisteArbeidsgiverdag,
        "tidsstempel" to tidsstempel,
        "nettoBeløp" to nettoBeløp,
        "stønadsdager" to stønadsdager(),
        "avstemmingsnøkkel" to avstemmingsnøkkel?.let { "$it" },
        "status" to status?.let { "$it" },
        "overføringstidspunkt" to overføringstidspunkt,
        "fom" to førstedato,
        "tom" to sistedato
    )

    internal fun lagreOverføringsinformasjon(hendelse: UtbetalingOverført) {
        if (!hendelse.erRelevant(fagsystemId)) return
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = hendelse.avstemmingsnøkkel
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = hendelse.overføringstidspunkt
        this.status = Oppdragstatus.OVERFØRT
    }

    internal fun lagreOverføringsinformasjon(hendelse: UtbetalingHendelse) {
        if (!hendelse.erRelevant(fagsystemId)) return
        if (this.avstemmingsnøkkel == null) this.avstemmingsnøkkel = hendelse.avstemmingsnøkkel
        if (this.overføringstidspunkt == null) this.overføringstidspunkt = hendelse.overføringstidspunkt
        this.status = hendelse.status
    }

    private interface Tilstand {
        fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg)
    }

    private inner class Identisk : Tilstand {
         override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            if (nåværende == tidligere) return nåværende.markerUendret(tidligere)
            håndterUlikhet(nåværende, tidligere, aktivitetslogg)
        }
    }

    private inner class Slett : Tilstand {
        override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            if (nåværende == tidligere) {
                if (nåværende == last()) return nåværende.kobleTil(linkTo)
                return nåværende.markerUendret(tidligere)
            }
            håndterUlikhet(nåværende, tidligere, aktivitetslogg)
        }
    }

    private inner class Ny : Tilstand {
        override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            nåværende.kobleTil(linkTo)
            linkTo = nåværende
        }
    }
}

