package no.nav.helse.person

import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

sealed interface Yrkesaktivitet {
    companion object {
        const val Frilans = "FRILANS"
        const val Selvstendig = "SELVSTENDIG"
        const val Arbeidsledig = "ARBEIDSLEDIG"

        fun String.tilYrkesaktivitet() = when (this) {
            Selvstendig -> Selvstendig()
            Frilans -> Frilans()
            Arbeidsledig -> Arbeidsledig()
            else -> Arbeidstaker(this)
        }
    }

    fun identifikator(): String

    fun jurist(other: MaskinellJurist): MaskinellJurist =
        other.medOrganisasjonsnummer(this.toString())

    fun erYrkesaktivitetenIkkeStøttet(hendelse: IAktivitetslogg): Boolean

    class Arbeidstaker(private val organisasjonsnummer: String) : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(hendelse: IAktivitetslogg) = false

        override fun hashCode(): Int {
            throw NotImplementedError()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Arbeidstaker) return false
            return this.organisasjonsnummer == other.organisasjonsnummer
        }

        override fun identifikator() = organisasjonsnummer
        override fun toString() = identifikator()
    }
    class Frilans : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(hendelse: IAktivitetslogg): Boolean {
            hendelse.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun hashCode(): Int {
            throw NotImplementedError()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Frilans
        }

        override fun identifikator() = Frilans
        override fun toString() = identifikator()
    }
    class Selvstendig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(hendelse: IAktivitetslogg): Boolean {
            hendelse.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun hashCode(): Int {
            throw NotImplementedError()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Selvstendig
        }
        override fun identifikator() = Selvstendig
        override fun toString() = identifikator()
    }
    class Arbeidsledig : Yrkesaktivitet {
        override fun erYrkesaktivitetenIkkeStøttet(hendelse: IAktivitetslogg): Boolean {
            hendelse.funksjonellFeil(Varselkode.RV_SØ_39)
            return true
        }

        override fun hashCode(): Int {
            throw NotImplementedError()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Arbeidsledig
        }
        override fun identifikator() = Arbeidsledig
        override fun toString() = identifikator()
    }
}
