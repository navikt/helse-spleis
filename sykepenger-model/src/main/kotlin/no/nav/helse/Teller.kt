package no.nav.helse

internal class Teller(private val grense: Int) {
    private var observer: Observer = Observer.nullObserver
    private var rest = grense
    private var state: Telletilstand = Initiell

    fun reset() { state(Initiell) }
    fun observer(observer: Observer) { this.observer = observer }

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
            if (teller.rest == 1) return teller.state(Ferdig)
            teller.rest -= 1
        }
    }

    private object Ferdig : Telletilstand {
        override fun entering(teller: Teller) {
            teller.rest = 0
            teller.observer.onGrense()
        }

        override fun inc(teller: Teller) {}
    }

    internal interface Observer {
        companion object {
            val nullObserver = object : Observer {}
        }
        fun onInc() {}
        fun onGrense() {}
        fun onReset() {}
    }
}
