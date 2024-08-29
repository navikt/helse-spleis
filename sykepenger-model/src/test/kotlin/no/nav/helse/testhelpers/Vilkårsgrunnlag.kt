package no.nav.helse.testhelpers

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Opptjening
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.utbetalingstidslinje.FaktaavklarteInntekter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal data class FiktivInntekt(val orgnr: String, val inntekt: Inntekt, val periode: Periode? = null)
internal fun Map<LocalDate, List<FiktivInntekt>>.faktaavklarteInntekter() = FaktaavklarteInntekter(
    skjæringstidspunkter = map { (skjæringstidspunkt, inntekter) ->
        FaktaavklarteInntekter.VilkårsprøvdSkjæringstidspunkt(
            skjæringstidspunkt = skjæringstidspunkt,
            `6G` = `6G`.beløp(skjæringstidspunkt),
            inntekter = inntekter.map { (orgnummer, inntekt, gjelder) ->
                FaktaavklarteInntekter.VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt(
                    organisasjonsnummer = orgnummer,
                    fastsattÅrsinntekt = inntekt,
                    gjelder = gjelder ?: (skjæringstidspunkt til LocalDate.MAX),
                    refusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), skjæringstidspunkt, null, inntekt).refusjonsopplysninger
                )
            }
        )
    }
)

internal fun List<FiktivInntekt>.faktaavklarteInntekter(skjæringstidspunkt: LocalDate) =
    mapOf(skjæringstidspunkt to this).faktaavklarteInntekter()