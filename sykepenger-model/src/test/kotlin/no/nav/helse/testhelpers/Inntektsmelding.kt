package no.nav.helse.testhelpers

import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.*
import java.time.LocalDate
import java.time.LocalDateTime

/* Dette er en hjelpeklasse for å generere testdata. Det er viktig at defaults her holdes til absolutt minimum, slik at
 * en ikke ender opp med tester som er avhengig av sære defaults i inntektsmeldingen
 */
internal class InntektsmeldingBuilder {
    private val arbeidsgiverperioder = mutableListOf<Periode>()

    internal fun arbeidsgiverperiode(block: ArbeidsgiverperiodeBuilder.() -> Unit) {
        arbeidsgiverperioder.add(ArbeidsgiverperiodeBuilder().apply(block).build())
    }

    fun build() = Inntektsmelding(
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
}

internal class ArbeidsgiverperiodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal fun build() = Periode(fom, tom)
}

internal fun inntektsmelding(block: InntektsmeldingBuilder.() -> Unit) =
    InntektsmeldingBuilder().apply(block).build()

internal fun Inntektsmelding.asHendelse() =
    InntektsmeldingHendelse(no.nav.helse.hendelser.inntektsmelding.Inntektsmelding(this.toJsonNode()))
