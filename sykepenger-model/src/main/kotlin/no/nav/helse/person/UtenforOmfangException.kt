package no.nav.helse.person

@Deprecated("Skal bruke Aktivitetslogger.error()")
class UtenforOmfangException(message: String, private val event: ArbeidstakerHendelse) : RuntimeException(message)
