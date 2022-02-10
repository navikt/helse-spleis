package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.Teller

/*
    Forstår hvordan man teller arbeidsgiverperioden
    Telling økes for hver sykedag (eller feriedag som inngår i sykdom),
    mens alle oppholdsdager (eller feriedag som ikke inngår i sykdom) minkes.

    For hver sykedag nullstilles oppholdstelleren
    Frem til grensen for arbeidsgiverperiodetelling er nådd telles alle sykedager som del av arbeidsgiverperioden
    Dersom grensen for oppholdstelleren nås nullstilles arbeidsgiverperiodetelleren
 */
internal class Arbeidsgiverperiodeteller private constructor(
    private val oppholdsteller: Teller,
    private val sykedagteller: Teller
) {
    companion object {
        val NormalArbeidstaker get() = Arbeidsgiverperiodeteller(Teller(16), Teller(16))
        val IngenArbeidsgiverperiode get() = Arbeidsgiverperiodeteller(Teller(16), Teller(0))
    }

    private val initiell: Tilstand = if (sykedagteller.ferdig()) IngenTelling else Initiell
    private var state: Tilstand = initiell
    private var observatør: Observatør = Observatør.nullObserver

    init {
        sykedagteller.observer(Sykedagobservatør())
        oppholdsteller.observer(Oppholdsdagobservatør())
    }

    fun observer(observatør: Observatør) {
        this.observatør = observatør
    }

    fun inc() = sykedagteller.inc()
    fun dec() = oppholdsteller.inc()

    fun fullfør() {
        state = ArbeidsgiverperiodeFerdig
    }

    private fun state(state: Tilstand) {
        if (state == this.state) return
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    private inner class Sykedagobservatør : Teller.Observer {
        override fun onInc() {
            state.sykedag(this@Arbeidsgiverperiodeteller)
            oppholdsteller.reset()
        }

        override fun onGrense() {
            state.ferdig(this@Arbeidsgiverperiodeteller)
        }

        override fun onReset() {
            // arbeidsgiverperiodetelling avbrutt av oppholdsdager
            state.reset(this@Arbeidsgiverperiodeteller)
        }
    }

    private inner class Oppholdsdagobservatør : Teller.Observer {
        override fun onInc() {}

        override fun onGrense() {
            sykedagteller.reset()
        }
    }

    internal interface Observatør {
        companion object {
            val nullObserver = object : Observatør {}
        }
        fun arbeidsgiverperiodeFerdig() {}
        fun arbeidsgiverperiodeAvbrutt() {}
        fun arbeidsgiverperiodedag() {}
        fun sykedag() {}
    }

    private interface Tilstand {
        fun entering(teller: Arbeidsgiverperiodeteller) {}
        fun sykedag(teller: Arbeidsgiverperiodeteller) {}
        fun ferdig(teller: Arbeidsgiverperiodeteller) {}
        fun reset(teller: Arbeidsgiverperiodeteller) {
            teller.state(Initiell)
        }
        fun leaving(teller: Arbeidsgiverperiodeteller) {}
    }
    private object Initiell : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodeAvbrutt()
        }

        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodedag()
            teller.state(PåbegyntArbeidsgiverperiode)
        }
    }
    private object PåbegyntArbeidsgiverperiode : Tilstand {
        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodedag()
        }
        override fun ferdig(teller: Arbeidsgiverperiodeteller) {
            teller.state(ArbeidsgiverperiodeFerdig)
        }
    }
    private object ArbeidsgiverperiodeFerdig : Tilstand {
        override fun entering(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodeFerdig()
        }

        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.sykedag()
        }
    }
    private object IngenTelling : Tilstand {
        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.sykedag()
        }

        override fun reset(teller: Arbeidsgiverperiodeteller) {}
    }
}
