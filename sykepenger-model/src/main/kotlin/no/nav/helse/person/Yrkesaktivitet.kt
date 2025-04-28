package no.nav.helse.person

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

const val Frilanstype = "FRILANS"
const val Selvstendigtype = "SELVSTENDIG"
const val Arbeidsledigtype = "ARBEIDSLEDIG"

internal sealed interface Yrkesaktivitet {
    companion object {
        fun String.tilYrkesaktivitet() = when (this) {
            Selvstendigtype -> Selvstendig
            Frilanstype -> Frilans
            Arbeidsledigtype -> Arbeidsledig
            else -> Arbeidstaker(this)
        }

        fun Behandlingsporing.Yrkesaktivitet.tilYrkesaktivitet() = when (this) {
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> organisasjonsnummer.tilYrkesaktivitet()
        }
    }

    fun identifikator(): String

    fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean
    fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg, sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.lagre(sykmelding, aktivitetslogg)
    }

    data class Arbeidstaker(val organisasjonsnummer: String) : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg) = false

        override fun identifikator() = organisasjonsnummer
        override fun toString() = identifikator()
    }

    data object Frilans : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun identifikator() = Frilanstype
        override fun toString() = identifikator()
    }

    data object Selvstendig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun identifikator() = Selvstendigtype
        override fun toString() = identifikator()
    }

    data object Arbeidsledig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(aktivitetslogg: IAktivitetslogg): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg, sykmeldingsperioder: Sykmeldingsperioder) {
            aktivitetslogg.info("Lagrer _ikke_ sykmeldingsperiode ${sykmelding.periode()} ettersom det er en sykmelding som arbeidsledig.")
        }

        override fun identifikator() = Arbeidsledigtype
        override fun toString() = identifikator()
    }
}

internal fun Yrkesaktivitet.erLik(other: Yrkesaktivitet) = when (this) {
    is Yrkesaktivitet.Arbeidstaker -> other is Yrkesaktivitet.Arbeidstaker && this.organisasjonsnummer == other.organisasjonsnummer

    is Yrkesaktivitet.Arbeidsledig -> other is Yrkesaktivitet.Arbeidsledig
    is Yrkesaktivitet.Frilans -> other is Yrkesaktivitet.Frilans
    is Yrkesaktivitet.Selvstendig -> other is Yrkesaktivitet.Selvstendig
}
