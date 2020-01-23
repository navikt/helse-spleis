package no.nav.helse.person

import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsestype: Hendelsestype
) : Comparable<ArbeidstakerHendelse>, IAktivitetslogger {

    enum class Hendelsestype {
        Ytelser,
        Vilkårsgrunnlag,
        ManuellSaksbehandling,
        Utbetaling,
        Inntektsmelding,
        NySøknad,
        SendtSøknad,
        Påminnelse
    }

    fun hendelseId() = hendelseId
    fun hendelsetype() = hendelsestype

    open fun kanBehandles() = true

    abstract fun rapportertdato(): LocalDateTime

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    override fun compareTo(other: ArbeidstakerHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    override fun equals(other: Any?) =
        other is ArbeidstakerHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()

    override fun info(melding: String, vararg params: Any) {}

    override fun warn(melding: String, vararg params: Any) {}

    override fun error(melding: String, vararg params: Any) {}

    override fun severe(melding: String, vararg params: Any): Nothing { Aktivitetslogger().severe(melding, params) }

    override fun hasMessages(): Boolean { return false }

    override fun hasErrors(): Boolean { return false }

    override fun addAll(other: Aktivitetslogger, label: String) {}

    override fun expectNoErrors(): Boolean { return true }
}
