package no.nav.helse.person

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal sealed interface Yrkesaktivitet {
    fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean
    fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg, sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.lagre(sykmelding, aktivitetslogg)
    }

    data object Arbeidstaker : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg) = false
    }

    data object Frilans : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }
    }

    data object Selvstendig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            if (Toggle.SelvstendigNæringsdrivende.enabled) return false
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }
    }

    data object SelvstendigJordbruker : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }
    }

    data object SelvstendigFisker : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }
    }

    data object SelvstendigBarnepasser : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            if (Toggle.SelvstendigNæringsdrivende.enabled) return false
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }
    }

    data object Arbeidsledig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg, sykmeldingsperioder: Sykmeldingsperioder) {
            aktivitetslogg.info("Lagrer _ikke_ sykmeldingsperiode ${sykmelding.periode()} ettersom det er en sykmelding som arbeidsledig.")
        }
    }
}
