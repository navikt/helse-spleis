package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    fun venterpå(vedtaksperiode: Vedtaksperiode) = when (val t = tilstand(vedtaksperiode)) {
        is TrengerInntektsopplysningerAnnenArbeidsgiver -> VenterPå.AnnenPeriode(t.trengerInntektsmelding.venter(), Venteårsak.INNTEKTSMELDING)

        is TrengerInnteksopplysninger,
        KlarForVilkårsprøving,
        KlarForBeregning -> VenterPå.Nestemann
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun tilstand(vedtaksperiode: Vedtaksperiode): Tilstand {
        return when (vedtaksperiode.vilkårsgrunnlag) {
            null -> when (val førstePeriodeSomTrengerInntekt = vedtaksperiode.førstePeriodeSomVenterPåInntekt()) {
                null -> when {
                    !vedtaksperiode.kanAvklareInntekt() -> TrengerInnteksopplysninger(vedtaksperiode)
                    else -> KlarForVilkårsprøving
                }
                else -> TrengerInntektsopplysningerAnnenArbeidsgiver(førstePeriodeSomTrengerInntekt)
            }

            else -> when (val førstePeriodeSomTrengerRefusjonsopplysninger = vedtaksperiode.førstePeriodeSomVenterPåRefusjonsopplysninger()) {
                null -> KlarForBeregning
                else -> TrengerInntektsopplysningerAnnenArbeidsgiver(førstePeriodeSomTrengerRefusjonsopplysninger)
            }
        }
    }

    private sealed interface Tilstand {
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg)
    }

    private data class TrengerInnteksopplysninger(private val vedtaksperiode: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering. Etterspør inntekt fra skatt")
            vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        }
    }

    private data class TrengerInntektsopplysningerAnnenArbeidsgiver(val trengerInntektsmelding: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering på annen arbeidsgiver.")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerVilkårsprøvingRevurdering) {
                aktivitetslogg.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
            }
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerHistorikkRevurdering)
        }
    }
}
