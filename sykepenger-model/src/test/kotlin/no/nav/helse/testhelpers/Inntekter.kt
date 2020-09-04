package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Inntektsvurdering.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
import no.nav.helse.hendelser.Inntektsvurdering.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.YearMonth

internal fun inntektperioder(block: Inntektperioder.() -> Unit) = Inntektperioder(block).inntekter()

internal class Inntektperioder(block: Inntektperioder.() -> Unit) {
    private val liste = mutableListOf<Pair<String, List<ArbeidsgiverInntekt.MånedligInntekt>>>()
    internal var inntektsgrunnlag: Inntektsvurdering.Inntektsgrunnlag = SYKEPENGEGRUNNLAG

    init {
        block()
    }

    internal fun inntekter(): List<ArbeidsgiverInntekt> = liste
        .groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekter) ->
            inntekter
        }
        .map { (arbeidsgiver, inntekter) -> ArbeidsgiverInntekt(arbeidsgiver, inntekter.flatten()) }

    internal infix fun Periode.inntekter(block: Inntekter.() -> Unit) =
        this.map(YearMonth::from)
            .distinct()
            .flatMap { yearMonth ->
                Inntekter(block).toList().groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekt) ->
                    ArbeidsgiverInntekt.MånedligInntekt(
                        yearMonth,
                        inntekt,
                        LØNNSINNTEKT,
                        inntektsgrunnlag,
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
        private val liste = mutableListOf<Pair<String, Inntekt>>()

        init {
            block()
        }

        internal fun toList() = liste.toList()

        infix fun String.inntekt(inntekt: Int) = this inntekt inntekt.månedlig
        infix fun String.inntekt(inntekt: Inntekt) = liste.add(this to inntekt)
    }
}
