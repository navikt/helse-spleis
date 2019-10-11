package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.tournament.dagTurnering
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

abstract class Dag internal constructor(
    internal val dagen: LocalDate,
    internal val hendelse: Sykdomshendelse
) :
    Sykdomstidslinje() {
    private val anyDag = null as KClass<Dag>?
    private val anyEvent = null as KClass<Sykdomshendelse>?
    private val nySøknad = NySykepengesøknad::class
    private val sendtSøknad = SendtSykepengesøknad::class
    private val inntektsmelding = Inntektsmelding::class

    private val nulldag = ImplisittDag::class
    private val sykedag = Sykedag::class
    private val feriedag = Feriedag::class
    private val utenlandsdag = Utenlandsdag::class
    private val arbeidsdag = Arbeidsdag::class

    internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    internal val erstatter: MutableList<Dag> = mutableListOf()

    internal abstract fun dagType(): JsonDagType
    override fun jsonRepresentation(): List<JsonDag> {
        val hendelseType = hendelse.hendelsetype()
        val hendelseJson = hendelse.toJson()
        return listOf(JsonDag(dagType(), dagen, JsonHendelse(hendelseType.name, hendelseJson), erstatter.flatMap { it.jsonRepresentation() }))
    }

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate, hendelse: Sykdomshendelse) = if (dato == dagen) this else ImplisittDag(
        dato,
        hendelse
    )

    internal fun erstatter(vararg dager: Dag): Dag {
        dager.filterNot { it is ImplisittDag }
            .forEach { erstatter.addAll(it.erstatter + it) }
        return this
    }

    fun dagerErstattet(): List<Dag> = erstatter

    internal fun beste(other: Dag): Dag = dagTurnering.slåss(this, other)

    private fun sisteDag(other: Dag) =
        if (this.hendelse.rapportertdato() > other.hendelse.rapportertdato()) this.also { this.erstatter(other) } else other.also {
            other.erstatter(
                this
            )
        }

    internal open fun tilDag() = this

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse

    internal enum class Nøkkel{
        WD_I,
        WD_A,
        WD_IM,
        S,
        V_A,
        V_IM,
        W,
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
        internal fun fromJsonRepresentation(jsonDag: JsonDag): Dag = jsonDag.type.creator(jsonDag).also {
            it.erstatter.addAll(jsonDag.erstatter.map { erstatterJsonDag -> fromJsonRepresentation(erstatterJsonDag) })
        }
    }
}
