package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.InntektHistorie
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class InntektReflect {
    fun getSome() {
        val inntektHistorie = InntektHistorie().apply {
            add(
                ModelInntektsmelding(
                    hendelseId = UUID.randomUUID(),
                    refusjon = ModelInntektsmelding.Refusjon(null, 30000.00, emptyList()),
                    orgnummer = "organisasjonsnummer",
                    fødselsnummer = "fødselsnummer",
                    aktørId = "aktørId",
                    mottattDato = LocalDateTime.now(),
                    førsteFraværsdag = LocalDate.now(),
                    beregnetInntekt = 30000.00,
                    aktivitetslogger = Aktivitetslogger(),
                    originalJson = "{}",
                    arbeidsgiverperioder = listOf(LocalDate.now()..LocalDate.now().plusDays(16)),
                    ferieperioder = emptyList()
                )
            )
        }

        inntektHistorie::class.memberProperties.forEach {
            println(it.name)
        }

        val kClass: KClass<InntektHistorie> = InntektHistorie::class
        val inntekter: KProperty1<InntektHistorie, List<InntektHistorie.Inntekt>> = kClass.memberProperties.first { it.name == "inntekter" }.apply { isAccessible = true } as KProperty1<InntektHistorie, List<InntektHistorie.Inntekt>>
        inntekter.get(inntektHistorie).forEach {
            println(it.fom)
        }
    }
}

//private inline fun <reified T, reified R> T.getProp(name: String): R {
////    val l: KClass<*> = T::class.memberProperties
//    l.memberProperties
//}
