package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.objectMapper
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

    private val nulldag = Nulldag::class
    private val sykedag = Sykedag::class
    private val feriedag = Feriedag::class
    private val utenlandsdag = Utenlandsdag::class
    private val arbeidsdag = Arbeidsdag::class

    internal val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    internal val erstatter: MutableList<Dag> = mutableListOf()

    internal abstract fun dagType(): JsonDagType
    internal open fun jsonRepresentation(): JsonDag {
        val hendelseType = (hendelse as Event).eventType()
        val hendelseJson = hendelse.toJson()
        return JsonDag(dagType(), dagen, JsonHendelse(hendelseType.name,hendelseJson), erstatter.map { it.jsonRepresentation() })
    }
    override fun toJson(): String = objectMapper.writeValueAsString(jsonRepresentation())

    override fun startdato() = dagen
    override fun sluttdato() = dagen
    override fun flatten() = listOf(this)
    override fun dag(dato: LocalDate, hendelse: Sykdomshendelse) = if (dato == dagen) this else Nulldag(
        dagen,
        hendelse
    )

    internal fun erstatter(vararg dager: Dag) {
        dager.filterNot { it is Nulldag }
            .forEach { erstatter.addAll(it.erstatter + it) }
    }

    fun dagerErstattet(): List<Dag> = erstatter

    internal fun beste(other: Dag): Dag {
        val helper = Helper(this, other)

        return when {
            helper.doesMatchBidirectional(sykedag, nySøknad, feriedag, inntektsmelding) -> Ubestemtdag(
                this,
                other
            )
            helper.doesMatchBidirectional(sykedag, sendtSøknad, feriedag, inntektsmelding) -> Ubestemtdag(
                this,
                other
            )
            helper.doesMatch(nulldag, anyEvent, anyDag, anyEvent) -> other
            helper.doesMatch(anyDag, anyEvent, nulldag, anyEvent) -> this
            helper.doesMatch(sykedag, anyEvent, sykedag, anyEvent) -> this.sisteDag(other)
            helper.doesMatch(feriedag, anyEvent, sykedag, anyEvent) -> this.also { this.erstatter(other) }
            helper.doesMatch(sykedag, anyEvent, feriedag, anyEvent) -> other.also { other.erstatter(this) }
            helper.doesMatch(feriedag, anyEvent, utenlandsdag, anyEvent) -> this.also { this.erstatter(other) }
            helper.doesMatch(utenlandsdag, anyEvent, feriedag, anyEvent) -> other.also { other.erstatter(this) }
            helper.doesMatch(arbeidsdag, anyEvent, sykedag, anyEvent) -> this.also { this.erstatter(other) }
            helper.doesMatch(sykedag, anyEvent, arbeidsdag, anyEvent) -> other.also { other.erstatter(this) }
            else -> Ubestemtdag(this, other)
        }
    }

    private fun sisteDag(other: Dag) =
        if (this.hendelse.rapportertdato() > other.hendelse.rapportertdato()) this.also { this.erstatter(other) } else other.also {
            other.erstatter(
                this
            )
        }

    internal open fun tilDag() = this

    override fun length() = 1

    override fun sisteHendelse() = this.hendelse

    private class Helper(private val left: Dag, private val right: Dag) {
        fun <S : Dag, T : Sykdomshendelse, U : Dag, V : Sykdomshendelse> doesMatch(
            leftClass: KClass<S>?,
            leftEvent: KClass<T>?,
            rightClass: KClass<U>?,
            rightEvent: KClass<V>?
        ): Boolean {
            return doesMatch(leftClass, left) && doesMatch(leftEvent, left.hendelse) && doesMatch(
                rightClass,
                right
            ) && doesMatch(rightEvent, right.hendelse)
        }

        fun <S : Dag, T : Sykdomshendelse, U : Dag, V : Sykdomshendelse> doesMatchBidirectional(
            leftClass: KClass<S>?,
            leftEvent: KClass<T>?,
            rightClass: KClass<U>?,
            rightEvent: KClass<V>?
        ): Boolean {
            return doesMatch(leftClass, leftEvent, rightClass, rightEvent) || doesMatch(
                rightClass,
                rightEvent,
                leftClass,
                leftEvent
            )
        }

        private fun <T : Any> doesMatch(expectedClass: KClass<T>?, actual: Any) =
            expectedClass?.equals(actual::class) ?: true
    }

    companion object {
        internal fun fromJson(node: JsonNode): Dag = TODO("Oversett json til dag")
    }
}

