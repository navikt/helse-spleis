package no.nav.helse.testhelpers

import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

/* Dette er en hjelpeklasse for å generere testdata. Det er viktig at defaults her holdes til absolutt minimum, slik at
 * en ikke ender opp med tester som er avhengig av sære defaults i inntektsmeldingen
 */

internal fun <Type, Builder> inntektsmelding(
    buildertype: Buildertype<Type, Builder>,
    block: Builder.() -> Unit
) = build(buildertype, block)

internal object InntektsmeldingHendelseWrapper :
    Buildertype<InntektsmeldingHendelse, InntektsmeldingkontraktBuilder> {
    override fun build(block: InntektsmeldingkontraktBuilder.() -> Unit) =
        InntektsmeldingHendelseBuilder.build { inntektsmelding(block) }
}

internal class InntektsmeldingHendelseBuilder {
    private lateinit var inntektsmelding: Inntektsmeldingkontrakt

    internal fun inntektsmelding(block: InntektsmeldingkontraktBuilder.() -> Unit) {
        inntektsmelding = InntektsmeldingkontraktBuilder.build(block)
    }

    private fun build() = InntektsmeldingHendelse(inntektsmelding.toJsonNode())

    internal companion object Type :
        Buildertype<InntektsmeldingHendelse, InntektsmeldingHendelseBuilder> {
        override fun build(block: InntektsmeldingHendelseBuilder.() -> Unit) =
            InntektsmeldingHendelseBuilder().apply(block).build()
    }
}

internal class InntektsmeldingkontraktBuilder {
    private val arbeidsgiverperioder = mutableListOf<Periode>()

    internal fun arbeidsgiverperiode(block: ArbeidsgiverperiodeBuilder.() -> Unit) {
        arbeidsgiverperioder.add(ArbeidsgiverperiodeBuilder().apply(block).build())
    }

    fun build() = Inntektsmeldingkontrakt(
        arbeidsgiverperioder = arbeidsgiverperioder,
        arbeidstakerAktorId = "aktørId",
        arbeidstakerFnr = "fødselsnummer",
        virksomhetsnummer = "123456789",
        beregnetInntekt = 666.toBigDecimal(),
        foersteFravaersdag = arbeidsgiverperioder.map { it.fom }.min(),
        ferieperioder = emptyList(),
        refusjon = Refusjon(
            beloepPrMnd = 666.toBigDecimal(),
            opphoersdato = null
        ),
        endringIRefusjoner = emptyList(),
        inntektsmeldingId = "inntektsmeldingId",
        arbeidsgiverFnr = "arbeidsgiverFødselsnummer",
        arbeidsgiverAktorId = "arbeidsgiverAktørId",
        arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
        arbeidsforholdId = "arbeidsforholdId",
        opphoerAvNaturalytelser = emptyList(),
        gjenopptakelseNaturalytelser = emptyList(),
        status = Status.GYLDIG,
        arkivreferanse = "arkivreferanse",
        mottattDato = arbeidsgiverperioder.map { it.tom }.max()?.atStartOfDay()
            ?: LocalDateTime.now()
    )

    internal companion object Type :
        Buildertype<Inntektsmeldingkontrakt, InntektsmeldingkontraktBuilder> {
        override fun build(block: InntektsmeldingkontraktBuilder.() -> Unit) =
            InntektsmeldingkontraktBuilder().apply(block).build()
    }
}

internal class ArbeidsgiverperiodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal var periode
        set(value) {
            fom = value.first
            tom = value.second
        }
        get() = fom to tom

    internal fun build() = Periode(fom, tom)
}
