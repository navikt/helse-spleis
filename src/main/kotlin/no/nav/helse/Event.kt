package no.nav.helse

interface Event {

    enum class Type {
        SendtSykepengesøknad,
        NySykepengesøknad,
        Inntektsmelding
    }

    fun eventType(): Type
}
