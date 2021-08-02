package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal class Sykepengegrunnlag(
    internal val sykepengegrunnlag: Inntekt,
    private val arbeidsgiverInntektsopplysning: List<ArbeidsgiverInntektsopplysning>,
    internal val grunnlagForSykepengegrunnlag: Inntekt
) {

    constructor(
        arbeidsgiverInntektsopplysning: List<ArbeidsgiverInntektsopplysning>,
        skjæringstidspunkt: LocalDate,
        personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden: LocalDate
    ) : this(
        minOf(arbeidsgiverInntektsopplysning.inntekt(), Grunnbeløp.`6G`.beløp(skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden)),
        arbeidsgiverInntektsopplysning,
        arbeidsgiverInntektsopplysning.inntekt()
    )

    internal fun avviksprosent(sammenligningsgrunnlag: Inntekt) = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
    internal fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning> = arbeidsgiverInntektsopplysning.inntektsopplysningPerArbeidsgiver()

}
