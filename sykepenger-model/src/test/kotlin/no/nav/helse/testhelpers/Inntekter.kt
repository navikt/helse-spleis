package no.nav.helse.testhelpers

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.InntektCreator
import no.nav.helse.hendelser.Periode
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.YearMonth

internal fun inntektperioderForSammenligningsgrunnlag(block: Inntektperioder.() -> Unit) = Inntektperioder(ArbeidsgiverInntekt.MånedligInntekt::Sammenligningsgrunnlag, block).inntekter()
internal fun inntektperioderForSykepengegrunnlag(block: Inntektperioder.() -> Unit) = Inntektperioder(ArbeidsgiverInntekt.MånedligInntekt::Sykepengegrunnlag, block).inntekter()

internal class Inntektperioder(private val inntektCreator: InntektCreator, block: Inntektperioder.() -> Unit) {
    private val liste = mutableListOf<Pair<Organisasjonsnummer, List<ArbeidsgiverInntekt.MånedligInntekt>>>()

    init {
        block()
    }

    internal fun inntekter(): List<ArbeidsgiverInntekt> = liste
        .groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekter) ->
            inntekter
        }
        .map { (arbeidsgiver, inntekter) -> ArbeidsgiverInntekt(arbeidsgiver.toString(), inntekter.flatten()) }

    internal infix fun Periode.inntekter(block: Inntekter.() -> Unit) =
        this.map(YearMonth::from)
            .distinct()
            .flatMap { yearMonth ->
                Inntekter(block).toList().groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekt) ->
                    inntektCreator(
                        yearMonth,
                        inntekt,
                        ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        "kontantytelse",
                        "fastloenn"
                    )
                }.toList()
            }
            .groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekt) -> inntekt }
            .map { (arbeidsgiver, inntekter) ->
                arbeidsgiver to inntekter.flatten()
            }
            .also { liste.addAll(it) }

    internal class Inntekter(block: Inntekter.() -> Unit) {
        private val liste = mutableListOf<Pair<Organisasjonsnummer, Inntekt>>()

        init {
            block()
        }

        internal fun toList() = liste.toList()

        infix fun String.inntekt(inntekt: Int) = this inntekt inntekt.månedlig
        infix fun String.inntekt(inntekt: Inntekt) = liste.add(this.somOrganisasjonsnummer() to inntekt)

        infix fun Organisasjonsnummer.inntekt(inntekt: Int) = this inntekt inntekt.månedlig
        infix fun Organisasjonsnummer.inntekt(inntekt: Inntekt) = liste.add(this to inntekt)
    }
}
