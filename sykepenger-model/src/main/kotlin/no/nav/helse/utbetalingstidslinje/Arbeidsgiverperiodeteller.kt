package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import java.time.LocalDate

internal fun interface Forlengelsestrategi {
    fun erArbeidsgiverperiodenGjennomførtFør(dagen: LocalDate): Boolean

    companion object {
        val Ingen = Forlengelsestrategi { false }
    }
}

internal interface Arbeidsgiverperiodestrategi {
    fun dagenInngårIArbeidsgiverperiodetelling(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {}
    fun dagenInngårIkkeIArbeidsgiverperiodetelling(arbeidsgiverperiode: Arbeidsgiverperiode?, dagen: LocalDate) {}

    class Default(
        private val dagenInngårIArbeidsgiverperiodetellingFun: (arbeidsgiverperiode: Arbeidsgiverperiode) -> Unit,
        private val dagenInngårIkkeIArbeidsgiverperiodetellingFun: () -> Unit
    ) : Arbeidsgiverperiodestrategi {
        override fun dagenInngårIArbeidsgiverperiodetelling(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {
            dagenInngårIArbeidsgiverperiodetellingFun(arbeidsgiverperiode)
        }

        override fun dagenInngårIkkeIArbeidsgiverperiodetelling(arbeidsgiverperiode: Arbeidsgiverperiode?, dagen: LocalDate) {
            dagenInngårIkkeIArbeidsgiverperiodetellingFun()
        }
    }

    companion object {
        val Empty = object : Arbeidsgiverperiodestrategi {}
    }
}

internal class Arbeidsgiverperiode(private val perioder: List<Periode>) : Iterable<LocalDate>, Comparable<LocalDate> {
    override fun compareTo(other: LocalDate) =
        perioder.first().start.compareTo(other)

    override fun iterator(): Iterator<LocalDate> {
        return object : Iterator<LocalDate> {
            private val periodeIterators = perioder.map { it.iterator() }.iterator()
            private var current: Iterator<LocalDate>? = null

            override fun hasNext(): Boolean {
                val iterator = current
                if (iterator != null && iterator.hasNext()) return true
                if (!periodeIterators.hasNext()) return false
                current = periodeIterators.next()
                return true
            }

            override fun next(): LocalDate {
                return current?.next() ?: throw NoSuchElementException()
            }
        }
    }
}

/**
 * Forstår hvordan man teller arbeidsgiverperiode
 *
 * Arbeidsgiverperiode telles hovedsaklig oppover (sykedager) eller nedover (oppholdsdager).
 * Om vi har nok sykedager ihht. loven (f.eks. 16 dager for arbeidstakere) da er arbeidsgiverperioden ferdig.
 * Om vi har for mange oppholdsdager ihht. loven (16 dager), da skal arbeidsgiverperiodetellingen starte på nytt.
 */
internal class Arbeidsgiverperiodeteller(private val regler: ArbeidsgiverRegler, private val forlengelsestrategi: Forlengelsestrategi = Forlengelsestrategi.Ingen) {
    private var arbeidsgiverperiodedager = 0
    private var oppholdsdager = 0
    private var tilstand: Tilstand = HarIkkeArbeidsgiverperiode
    private val observatører = mutableListOf<Observatør>()
    private val usikker = mutableListOf<Pair<LocalDate, Arbeidsgiverperiodestrategi>>()

    private val builder = ArbeidsgiverperiodeBuilder()
    private var arbeidsgiverperiode: Arbeidsgiverperiode? = null

    internal fun observatør(observatør: Observatør) {
        observatører.add(observatør)
    }

    // inkrementerer hver sykedag
    internal fun inkrementer(dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi = Arbeidsgiverperiodestrategi.Empty) {
        tilstand.inkrementerUsikker(this)
        tilstand.inkrementer(this, dagen, strategi)
    }

    internal fun avslutt() {
        usikker.onEach { (dagen, strategi) -> strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(arbeidsgiverperiode, dagen) }.clear()
    }

    // fridager kan telles som en inkrement (del av arbeidsgiverperioden) eller dekrement (telles som oppholdsdag)
    internal fun inkrementEllerDekrement(dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi = Arbeidsgiverperiodestrategi.Empty) {
        tilstand.inkrementEllerDekrement(this, dagen, strategi)
    }

    // dekrementerer hver arbeidsdag, frisk dag, osv
    internal fun dekrementer(dagen: LocalDate) {
        tilstand.dekrementerUsikker(this)
        tilstand.dekrementerer(this, dagen)
    }

    private fun tilstand(tilstand: Tilstand, dagen: LocalDate) {
        this.tilstand.leaving(this, dagen)
        this.tilstand = tilstand
        this.tilstand.entering(this, dagen)
    }

    private fun fortell(dagen: LocalDate, observatørhendelse: Observatør.(LocalDate) -> Unit) {
        observatører.forEach { it.observatørhendelse(dagen) }
    }

    private fun _inkrementer(dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
        oppholdsdager = 0
        arbeidsgiverperiodedager += 1
        builder.nyDag(dagen)
        strategi.dagenInngårIArbeidsgiverperiodetelling(builder.build(), dagen)
        if (!regler.arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedager)) return
        arbeidsgiverperiode = builder.build()
        tilstand(HarFullstendigArbeidsgiverperiode, dagen)
        fortell(dagen) { arbeidsgiverperiodeFerdig(arbeidsgiverperiode!!, dagen) }
    }

    private fun _dekrementer(dagen: LocalDate) {
        oppholdsdager += 1
        if (!regler.burdeStarteNyArbeidsgiverperiode(oppholdsdager)) return
        tilstand(HarIkkeArbeidsgiverperiode, dagen)
    }

    private class ArbeidsgiverperiodeBuilder {
        private val perioder = mutableListOf<Periode>()
        private val siste get() = perioder.last()

        internal fun nyDag(dagen: LocalDate) {
            if (perioder.isNotEmpty() && siste.endInclusive.plusDays(1) == dagen) {
                perioder[perioder.size - 1] = siste.oppdaterTom(dagen)
            } else {
                perioder.add(dagen til dagen)
            }
        }

        internal fun reset() {
            perioder.clear()
        }

        internal fun build(): Arbeidsgiverperiode {
            return Arbeidsgiverperiode(perioder.toList())
        }
    }

    internal interface Observatør {
        // *dagen* medførte at arbeidsgiverperioden ble tilbakestilt, altså at vi må telle nye 16 dager (pga for mange oppholdsdager)
        fun ingenArbeidsgiverperiode(dagen: LocalDate) {}
        // *dagen* medførte at arbeidsgiverperioden ble ferdig, aka. siste arbeidsgiverperiode dag
        fun arbeidsgiverperiodeFerdig(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {}
    }

    private interface Tilstand {
        fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {}
        fun inkrementerUsikker(teller: Arbeidsgiverperiodeteller) {}
        fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {}
        fun inkrementer(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {}
        fun dekrementerUsikker(teller: Arbeidsgiverperiodeteller) {}
        fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {}
        fun leaving(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {}
    }

    private object HarIkkeArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 0
            teller.arbeidsgiverperiodedager = 0
            teller.builder.reset()
            teller.arbeidsgiverperiode = null
            teller.fortell(dagen, Observatør::ingenArbeidsgiverperiode)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            if (teller.forlengelsestrategi.erArbeidsgiverperiodenGjennomførtFør(dagen)) {
                strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
                return teller.tilstand(HarFullstendigInfotrygdArbeidsgiverperiode, dagen)
            }

            teller._inkrementer(dagen, strategi)
            teller.tilstand(PåbegyntArbeidsgiverperiode, dagen)
        }
    }

    private object PåbegyntArbeidsgiverperiode : Tilstand {
        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            teller.usikker.add(dagen to strategi)
        }

        override fun dekrementerUsikker(teller: Arbeidsgiverperiodeteller) {
            teller.usikker.onEach { (dagen, strategi) ->
                teller.tilstand.dekrementerer(teller, dagen)
                strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
            }.clear()
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.tilstand(HarOppholdIPåbegyntArbeidsgiverperiode, dagen)
        }

        override fun inkrementerUsikker(teller: Arbeidsgiverperiodeteller) {
            teller.usikker.onEach { (dagen, strategi) ->
                teller.tilstand.inkrementer(teller, dagen, strategi)
            }.clear()
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            teller._inkrementer(dagen, strategi)
        }
    }

    private object HarOppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 1
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller._dekrementer(dagen)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            dekrementerer(teller, dagen)
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(teller.builder.build(), dagen)
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            teller.tilstand(PåbegyntArbeidsgiverperiode, dagen)
            teller._inkrementer(dagen, strategi)
        }
    }

    private object HarFullstendigArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 0 // om oppholdsdager > 0 betyr det at vi kommer tilbake fra et opphold i arbeidsgiverperioden
            teller.arbeidsgiverperiodedager = 0
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.tilstand(HarOppholdIFullstendigArbeidsgiverperiode, dagen)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            inkrementer(teller, dagen, strategi)
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(teller.arbeidsgiverperiode!!, dagen)
        }
    }

    private object HarOppholdIFullstendigArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 1
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            teller.tilstand(HarFullstendigArbeidsgiverperiode, dagen)
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(teller.arbeidsgiverperiode!!, dagen)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            dekrementerer(teller, dagen)
            // teller.arbeidsgiverperiode kan være null, om *dagen* medfører tilbakestilling
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(teller.arbeidsgiverperiode, dagen)
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller._dekrementer(dagen)
        }
    }

    private object HarFullstendigInfotrygdArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 0 // om oppholdsdager > 0 betyr det at vi kommer tilbake fra et opphold i arbeidsgiverperioden
            check(teller.arbeidsgiverperiodedager == 0)
            check(teller.arbeidsgiverperiode == null)
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.tilstand(HarOppholdIFullstendigInfotrygdArbeidsgiverperiode, dagen)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            inkrementer(teller, dagen, strategi)
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
        }
    }

    private object HarOppholdIFullstendigInfotrygdArbeidsgiverperiode : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller.oppholdsdager = 1
        }

        override fun inkrementer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate, strategi: Arbeidsgiverperiodestrategi) {
            teller.tilstand(HarFullstendigInfotrygdArbeidsgiverperiode, dagen)
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
        }

        override fun inkrementEllerDekrement(
            teller: Arbeidsgiverperiodeteller,
            dagen: LocalDate,
            strategi: Arbeidsgiverperiodestrategi
        ) {
            dekrementerer(teller, dagen)
            strategi.dagenInngårIkkeIArbeidsgiverperiodetelling(null, dagen)
        }

        override fun dekrementerer(teller: Arbeidsgiverperiodeteller, dagen: LocalDate) {
            teller._dekrementer(dagen)
        }
    }
}

