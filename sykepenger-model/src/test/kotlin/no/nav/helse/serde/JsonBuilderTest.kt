package no.nav.helse.serde

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

private const val aktørId = "12345"
private const val fnr = "12020052345"
private const val orgnummer = "987654321"
private var vedtaksperiodeId = "1"

internal class JsonBuilderTest {

    private fun lagPerson() =
        Person(aktørId, fnr).apply {
            addObserver(object : PersonObserver {
                override fun vedtaksperiodeTrengerLøsning(event: Behov) {
                    if (event.hendelsetype() == ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag) {
                        vedtaksperiodeId = event.vedtaksperiodeId()
                    }
                }
            })

            håndter(nySøknad)
            håndter(sendtSøknad)
            håndter(inntektsmelding)
            håndter(vilkårsgrunnlag)
            håndter(ytelser)
            håndter(manuellSaksbehandling)
        }

    @Test
    internal fun `print person som json`() {
        val person = lagPerson()
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        println(jsonBuilder.toString())
    }

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person`() {
        val person = lagPerson()
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = DataClassModelBuilder(json).result()
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        assertEquals(json, json2)
        assertTrue(deepEquals(person, result))
    }

    val checkLog = mutableListOf<Pair<Any, Any>>()

    fun deepEquals(one: Any?, other: Any?): Boolean {
        checkLog.clear()
        if (one == null && other == null) return true
        if (one == null || other == null) {
            println("$one er ulik $other")
            return false
        }
        if (one::class.qualifiedName == null) return true
        checkLog.forEach {
            if (it.first == one && it.second == other) {
                return true
            } // some one else is checking
        }
        checkLog.add(one to other)

        if (one is Collection<*> && other is Collection<*>) {
            val a1 = one.toTypedArray()
            val a2 = other.toTypedArray()
            if (a1.size != a2.size) {
                println("array ${a1.size} er ulik ${a2.size}")
                println("$a1 vs $a2")
                return false
            }
            for (i in 0 until a1.size) {
                if (!deepEquals(a1[i], a2[i])) return false
            }
            return true
        } else if (one is Map<*, *> && other is Map<*, *>) {
            if (one.size != other.size) {
                println("one.size ${one.size} er ulik other.size ${other.size}")
                return false
            }
            one.keys.forEach {
                if (!deepEquals(one[it], other[it])) return false
            }
            return true
        }
        if (one::class != other::class) {
            println("${one::class} er ulik class ${other::class}")
            return false
        }

        if (one is Enum<*> && other is Enum<*>) {
            return one.equals(other)
        }
        val isOurs = one::class.qualifiedName!!.startsWith("no.nav.helse.")
        if (!isOurs) {
            if (one is BigDecimal && other is BigDecimal) {
                if (one.toLong() == other.toLong()) return true
                println("${one::class} vs ${other::class} : $one.toLong er ulik $other.toLong")
                return false
            }
            if (one.equals(other)) return true
            println("${one::class} vs ${other::class} : $one er ulik $other")
            return false
        }

        one::class.memberProperties.map { it.apply { isAccessible = true } }.forEach { prop ->
            if (!prop.name.toLowerCase().endsWith("observers")) {
                if (!deepEquals(prop.call(one), prop.call(other))) return false
            }
        }
        return true
    }
}

private val nySøknad
    get() = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(1.januar, 31.januar, 100)),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

private val sendtSøknad
    get() = ModelSendtSøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        perioder = listOf(
            ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100)
        ),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

private val inntektsmelding
    get() = ModelInntektsmelding(
        hendelseId = UUID.randomUUID(),
        refusjon = ModelInntektsmelding.Refusjon(1.juli, 1000.00, emptyList()),
        orgnummer = orgnummer,
        fødselsnummer = fnr,
        aktørId = aktørId,
        mottattDato = 1.februar.atStartOfDay(),
        førsteFraværsdag = 1.januar,
        beregnetInntekt = 1000.00,
        originalJson = "{}",
        arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
        ferieperioder = emptyList(),
        aktivitetslogger = Aktivitetslogger()
    )

private val vilkårsgrunnlag
    get() = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fnr,
        orgnummer = orgnummer,
        rapportertDato = LocalDateTime.now(),
        inntektsmåneder = (1.rangeTo(12)).map {
            ModelVilkårsgrunnlag.Måned(
                årMåned = YearMonth.of(2018, it),
                inntektsliste = listOf(
                    ModelVilkårsgrunnlag.Inntekt(
                        beløp = 1000.0
                    )
                )
            )
        },
        erEgenAnsatt = false,
        aktivitetslogger = Aktivitetslogger()

    )

private val ytelser
    get() = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = listOf(
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    fom = 1.januar.minusYears(1),
                    tom = 31.januar.minusYears(1),
                    dagsats = 1000
                )
            ),
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = Periode(
                fom = 1.januar.minusYears(2),
                tom = 31.januar.minusYears(2)
            ),
            svangerskapsytelse = Periode(
                fom = 1.juli.minusYears(2),
                tom = 31.juli.minusYears(2)
            ),
            aktivitetslogger = Aktivitetslogger()
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

private val manuellSaksbehandling
    get() = ModelManuellSaksbehandling(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fnr,
        organisasjonsnummer = orgnummer,
        utbetalingGodkjent = true,
        saksbehandler = "en_saksbehandler_ident",
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

