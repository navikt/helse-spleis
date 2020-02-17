package no.nav.helse.person

import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsestype: Hendelsestype,
    internal val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) : Comparable<ArbeidstakerHendelse>, IAktivitetslogger by aktivitetslogger, IAktivitetslogg by aktivitetslogg {
    private val hendelseObservers = mutableListOf<HendelseObserver>()

    @Deprecated("Enum brukes til (de)serialisering og bør ikke ligge i modell-objektene")
    enum class Hendelsestype {
        Ytelser,
        Vilkårsgrunnlag,
        ManuellSaksbehandling,
        Utbetaling,
        Inntektsmelding,
        NySøknad,
        SendtSøknad,
        Påminnelse,
        GjennopptaBehandling
    }

    fun addObserver(hendelseObserver: HendelseObserver) = hendelseObservers.add(hendelseObserver)

    fun hendelseId() = hendelseId

    @Deprecated("Henger igjen fra Epic-1")
    fun hendelsestype() = hendelsestype

    abstract fun rapportertdato(): LocalDateTime

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    @Deprecated("Henger igjen fra Epic-1")
    override fun compareTo(other: ArbeidstakerHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    @Deprecated("Henger igjen fra Epic-1")
    override fun equals(other: Any?) =
        other is ArbeidstakerHendelse && other.hendelseId == this.hendelseId

    @Deprecated("Henger igjen fra Epic-1")
    override fun hashCode() = hendelseId.hashCode()

    internal fun need(behov: BehovType) {
        aktivitetslogg.need(melding = behov.navn)
        hendelseObservers.forEach { it.onBehov(behov) }
    }
}
