package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.tournament.dagTurnering
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal abstract class Dag internal constructor(
    internal val dagen: LocalDate,
    internal val hendelse: SykdomstidslinjeHendelse
) :
    Sykdomstidslinje() {

    internal val erstatter: MutableList<Dag> = mutableListOf()

    internal abstract fun dagType(): JsonDagType

    internal fun toJsonDag(): JsonDag {
        val hendelseId = hendelse.hendelseId()
        return JsonDag(
            dagType(),
            dagen,
            hendelseId,
            erstatter.map { it.toJsonDag() })

    }

    internal fun toJsonHendelse(): List<SykdomstidslinjeHendelse> {
        val alleHendelser = mutableListOf(hendelse)
        alleHendelser.addAll(erstatter.flatMap { it.toJsonHendelse() })
        return alleHendelser
    }

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun hendelser(): Set<SykdomstidslinjeHendelse> = setOf(hendelse) + erstatter.flatMap { it.hendelser() }
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate) = if (dato == dagen) this else null

    internal fun erstatter(vararg dager: Dag): Dag {
        erstatter.addAll(dager
            .filterNot { it is ImplisittDag }
            .flatMap { it.erstatter + it })
        return this
    }

    fun dagerErstattet(): List<Dag> = erstatter

    internal open fun beste(other: Dag): Dag = dagTurnering.slåss(this, other)

    fun erHelg() = dagen.dayOfWeek == DayOfWeek.SATURDAY || dagen.dayOfWeek == DayOfWeek.SUNDAY

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse

    enum class NøkkelHendelseType {
        Sykmelding,
        Søknad,
        Inntektsmelding
    }

    internal enum class Nøkkel {
        I,
        WD_A,
        WD_IM,
        S_SM,
        S_A,
        V_A,
        V_IM,
        Le_Areg,
        Le_A,
        SW,
        SRD_IM,
        SRD_A,
        EDU,
        OI_Int,
        OI_A,
        DA,
        Undecided,
    }

    internal abstract fun nøkkel(): Nøkkel

    companion object {
        internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        internal fun fromJsonRepresentation(jsonDag: JsonDag, hendelseMap: Map<String, SykdomstidslinjeHendelse>): Dag =
            jsonDag.type.creator(
                jsonDag.dato,
                hendelseMap.getOrElse(jsonDag.hendelseId,
                    { throw RuntimeException("hendelse med id ${jsonDag.hendelseId} finnes ikke") })
            ).also {
                it.erstatter.addAll(jsonDag.erstatter.map { erstatterJsonDag ->
                    fromJsonRepresentation(
                        erstatterJsonDag,
                        hendelseMap
                    )
                })
            }
    }
}
