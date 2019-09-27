package no.nav.helse

interface Event {

    enum class Type {
        Sykepengesøknad,
        Sykmelding,
        Inntektsmelding
    }

    fun name() = Type.valueOf(this.javaClass.simpleName)
}
