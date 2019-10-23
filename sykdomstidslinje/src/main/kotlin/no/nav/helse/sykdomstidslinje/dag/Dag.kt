package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.InntektsmeldingMottatt
import no.nav.helse.hendelse.NySøknadOpprettet
import no.nav.helse.hendelse.SendtSøknadMottatt
import no.nav.helse.hendelse.DokumentMottattHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.tournament.dagTurnering
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

abstract class Dag internal constructor(
    internal val dagen: LocalDate,
    internal val hendelse: DokumentMottattHendelse
) :
    Sykdomstidslinje() {
    private val anyDag = null as KClass<Dag>?
    private val anyEvent = null as KClass<DokumentMottattHendelse>?
    private val nySøknad = NySøknadOpprettet::class
    private val sendtSøknad = SendtSøknadMottatt::class
    private val inntektsmelding = InntektsmeldingMottatt::class

    private val nulldag = ImplisittDag::class
    private val sykedag = Sykedag::class
    private val feriedag = Feriedag::class
    private val utenlandsdag = Utenlandsdag::class
    private val arbeidsdag = Arbeidsdag::class

    internal val erstatter: MutableList<Dag> = mutableListOf()

    internal abstract fun dagType(): JsonDagType

    internal fun toJsonDag(): JsonDag {
        val hendelseType = hendelse.hendelsetype()
        val hendelseId = hendelse.hendelseId()
        return JsonDag(
            dagType(),
            dagen,
            JsonHendelsesReferanse(hendelseType.name, hendelseId),
            erstatter.map { it.toJsonDag() })

    }

    internal fun toJsonHendelse(): List<JsonHendelse> {
        val alleHendelser = mutableListOf<JsonHendelse>(
            JsonHendelse(
                hendelse.hendelsetype().name,
                hendelse.toJson()
            )
        )
        alleHendelser.addAll(erstatter.flatMap { it.toJsonHendelse() })
        return alleHendelser
    }

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate, hendelse: DokumentMottattHendelse) = if (dato == dagen) this else ImplisittDag(
        dato,
        hendelse
    )

    internal fun erstatter(vararg dager: Dag): Dag {
        erstatter.addAll(dager
            .filterNot { it is ImplisittDag }
            .flatMap { it.erstatter + it })
        return this
    }

    fun dagerErstattet(): List<Dag> = erstatter

    internal open fun beste(other: Dag): Dag = dagTurnering.slåss(this, other)

    private fun sisteDag(other: Dag) =
        if (this.hendelse.rapportertdato() > other.hendelse.rapportertdato()) this.also { this.erstatter(other) } else other.also {
            other.erstatter(
                this
            )
        }

    fun erHelg() = dagen.dayOfWeek == DayOfWeek.SATURDAY || dagen.dayOfWeek == DayOfWeek.SUNDAY

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse

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

        internal fun fromJsonRepresentation(jsonDag: JsonDag, hendelseMap: Map<String, Sykdomshendelse>): Dag =
            jsonDag.type.creator(
                jsonDag.dato,
                hendelseMap.getOrElse(jsonDag.hendelse.hendelseId,
                    { throw RuntimeException("hendelse med id ${jsonDag.hendelse.hendelseId} finnes ikke") })
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
