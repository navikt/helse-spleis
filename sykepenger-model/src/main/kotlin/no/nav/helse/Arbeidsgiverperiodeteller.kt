package no.nav.helse

/*
    Forstår hvordan man teller arbeidsgiverperioden

    En sykedag nullstiller oppholdstelling
    En sykedag øker arbeidsgiverperiodetelling, hvis grensen ikke er nådd allerede
    En oppholdsdag nullstiller arbeidsgiverperiodetelling hvis grensen er nådd
 */
internal class Arbeidsgiverperiodeteller(
    private val oppholdsteller: Teller,
    private val sykedagteller: Teller
) {
    companion object {
        val NormalArbeidstaker get() = Arbeidsgiverperiodeteller(Teller(16), Teller(16))
    }

    private var state: Tilstand = Initiell
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

    private fun state(state: Tilstand) {
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
            state(ArbeidsgiverperiodeFerdig)
        }

        override fun onReset() {
            // arbeidsgiverperiodetelling avbrutt av oppholdsdager
            state(Initiell)
        }
    }

    private inner class Oppholdsdagobservatør : Teller.Observer {
        override fun onInc() {
            state.oppholdsdag(this@Arbeidsgiverperiodeteller)
        }

        override fun onGrense() {
            sykedagteller.reset()
        }
    }

    internal interface Observatør {
        companion object {
            val nullObserver = object : Observatør {}
        }
        fun arbeidsgiverperiodeFerdig() {}
        fun arbeidsgiverperiodedag() {}
        fun sykedag() {}
    }

    private interface Tilstand {
        fun entering(teller: Arbeidsgiverperiodeteller) {}
        fun sykedag(teller: Arbeidsgiverperiodeteller) {}
        fun oppholdsdag(teller: Arbeidsgiverperiodeteller) {}
        fun leaving(teller: Arbeidsgiverperiodeteller) {}
    }
    private object Initiell : Tilstand {
        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodedag()
            teller.state(PåbegyntArbeidsgiverperiode)
        }
    }
    private object PåbegyntArbeidsgiverperiode : Tilstand {
        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodedag()
        }

        override fun oppholdsdag(teller: Arbeidsgiverperiodeteller) {
            teller.state(OppholdIPåbegyntArbeidsgiverperiode)
        }
    }
    // Denne tilstanden har egentlig ingen praktisk betydning for tellingen
    private object OppholdIPåbegyntArbeidsgiverperiode : Tilstand {
        override fun sykedag(teller: Arbeidsgiverperiodeteller) {
            teller.observatør.arbeidsgiverperiodedag()
            teller.state(PåbegyntArbeidsgiverperiode)
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
}
