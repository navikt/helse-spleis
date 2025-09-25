package no.nav.helse.person.tilstandsmaskin

import java.time.Period
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal data object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_BLOKKERENDE_PERIODE
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(!vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
            "Periode i avventer blokkerende har ikke tilstrekkelig informasjon til utbetaling! VedtaksperiodeId = ${vedtaksperiode.id}."
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    fun venterpå(vedtaksperiode: Vedtaksperiode) = when (val t = tilstand(vedtaksperiode)) {
        ForventerIkkeInntekt,
        KlarForBeregning,
        KlarForVilkårsprøving,
        ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag -> VenterPå.Nestemann

        AvventerTidligereEllerOverlappendeSøknad -> VenterPå.SegSelv(Venteårsak.SØKNAD)
        is TrengerInntektsmelding -> VenterPå.SegSelv(Venteårsak.INNTEKTSMELDING)
        is TrengerInntektsmeldingAnnenPeriode -> VenterPå.AnnenPeriode(t.trengerInntektsmelding.venter(), Venteårsak.INNTEKTSMELDING)
    }

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (vedtaksperiode.skalBehandlesISpeil()) return vedtaksperiode.håndterKorrigerendeInntektsmelding(
            dager,
            aktivitetslogg
        )
        vedtaksperiode.håndterDager(dager, aktivitetslogg)
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return vedtaksperiode.forkast(
            dager.hendelse,
            aktivitetslogg
        )
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) =
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, hendelse, aktivitetslogg)

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        tilstand(vedtaksperiode).håndterPåminnelse(vedtaksperiode, påminnelse, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) vedtaksperiode.tilstand(
            aktivitetslogg,
            AvventerInntektsmelding
        )
    }

    private fun tilstand(
        vedtaksperiode: Vedtaksperiode,
    ): Tilstand {
        val førstePeriodeSomTrengerInntektsmelding = vedtaksperiode.førstePeriodeSomTrengerInntektsmelding()
        return when {
            !vedtaksperiode.skalBehandlesISpeil() -> ForventerIkkeInntekt
            vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() -> ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag
            vedtaksperiode.person.avventerSøknad(vedtaksperiode.periode) -> AvventerTidligereEllerOverlappendeSøknad
            førstePeriodeSomTrengerInntektsmelding != null -> when (førstePeriodeSomTrengerInntektsmelding) {
                vedtaksperiode -> TrengerInntektsmelding(førstePeriodeSomTrengerInntektsmelding)
                else -> TrengerInntektsmeldingAnnenPeriode(førstePeriodeSomTrengerInntektsmelding)
            }

            vedtaksperiode.vilkårsgrunnlag == null -> KlarForVilkårsprøving
            else -> KlarForBeregning
        }
    }

    private sealed interface Tilstand {
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg)
        fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {}
    }

    private data object AvventerTidligereEllerOverlappendeSøknad : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding som er før eller overlapper med vedtaksperioden")
        }

        override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
            if (påminnelse.når(Påminnelse.Predikat.VentetMinst(Period.ofMonths(3))) || påminnelse.når(
                    Påminnelse.Predikat.Flagg(
                        "forkastOverlappendeSykmeldingsperioderAndreArbeidsgivere"
                    )
                )) {
                aktivitetslogg.varsel(Varselkode.RV_SY_4)
                vedtaksperiode.person.fjernSykmeldingsperiode(vedtaksperiode.periode)
            }
        }
    }

    private data object ForventerIkkeInntekt : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvsluttetUtenUtbetaling)
        }
    }

    private data object ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SV_2)
            vedtaksperiode.forkast(hendelse, aktivitetslogg)
        }
    }

    private data class TrengerInntektsmelding(val segSelv: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Går tilbake til Avventer inntektsmelding fordi perioden mangler inntekt og/eller refusjonsopplysninger")
            vedtaksperiode.tilstand(aktivitetslogg, AvventerInntektsmelding)
        }
    }

    private data class TrengerInntektsmeldingAnnenPeriode(val trengerInntektsmelding: Vedtaksperiode) :
        Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én overlappende periode venter på nødvendig opplysninger fra arbeidsgiver")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerVilkårsprøving)
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerHistorikk)
        }
    }
}
