package no.nav.helse

/**
 * Teller teller telleting
 */
class Teller(private val grense: Int) {
    private var observer: Observer = Observer.nullObserver
    private var rest = grense
    private val initiell = if (grense == 0) Ferdig else Initiell
    private var state: Telletilstand = initiell

    init {
        require(grense >= 0) { "grense må være større eller lik 0" }
    }

    fun reset() {
        state(initiell)
    }

    fun observer(observer: Observer) {
        this.observer = observer
    }

    fun ferdig() = state == Ferdig

    fun inc() {
        observer.onInc()
        state.inc(this)
    }

    private fun state(state: Telletilstand) {
        this.state.leaving(this)
        this.state = state
        this.state.entering(this)
    }

    private interface Telletilstand {
        fun entering(teller: Teller) {}
        fun inc(teller: Teller)
        fun leaving(teller: Teller) {}
    }

    private object Initiell : Telletilstand {
        override fun entering(teller: Teller) {
            teller.observer.onReset()
            teller.rest = teller.grense
        }

        override fun inc(teller: Teller) {
            teller.rest -= 1
            if (teller.rest > 0) return
            teller.state(Ferdig)
            teller.observer.onGrense()
        }
    }

    private object Ferdig : Telletilstand {
        override fun entering(teller: Teller) {
            teller.rest = 0
        }

        override fun inc(teller: Teller) {}
    }

    interface Observer {
        companion object {
            val nullObserver = object : Observer {}
        }

        fun onInc() {}
        fun onGrense() {}
        fun onReset() {}
    }
}
