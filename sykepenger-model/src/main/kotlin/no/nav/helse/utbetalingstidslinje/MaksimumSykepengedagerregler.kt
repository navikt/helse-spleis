package no.nav.helse.utbetalingstidslinje


internal interface MaksimumSykepengedagerregler {
    fun maksSykepengedager(): Int
    fun maksSykepengedagerOver67(): Int

    companion object {
        internal object NormalArbeidstaker : MaksimumSykepengedagerregler {
            override fun maksSykepengedager() = 248
            override fun maksSykepengedagerOver67() = 60
        }
    }
}
