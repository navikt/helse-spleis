package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.AktivitetsloggAsserts
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.Varslersamler
import no.nav.helse.dsl.a1
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode

internal fun Aktivitetslogg.assertInfo(forventet: String, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertInfo(forventet, filter)

internal fun Aktivitetslogg.assertIngenInfo(forventet: String, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertIngenInfo(forventet, filter)

internal fun Aktivitetslogg.assertVarsler(varsel: List<Varselkode>, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertVarsler(varsel, filter)

internal fun Aktivitetslogg.assertIngenVarsler() = assertVarsler(emptyList())

internal fun Aktivitetslogg.assertVarsel(varsel: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertVarsel(varsel, filter)

internal fun Aktivitetslogg.assertFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertFunksjonellFeil(varselkode, filter)

internal fun Aktivitetslogg.assertFunksjonelleFeil(filter: AktivitetsloggFilter, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertFunksjonelleFeil(filter)

internal fun Aktivitetslogg.assertIngenFunksjonellFeil(varselkode: Varselkode, filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertIngenFunksjonellFeil(varselkode, filter)

internal fun Aktivitetslogg.assertIngenFunksjonelleFeil(filter: AktivitetsloggFilter = AktivitetsloggFilter.Alle, assertetVarsler: Varslersamler.AssertetVarsler = Varslersamler.AssertetVarsler()) =
    AktivitetsloggAsserts(this, assertetVarsler).assertIngenFunksjonelleFeil(filter)

internal fun interface AktivitetsloggFilter {
    companion object {
        internal val Alle = AktivitetsloggFilter { true }
        internal fun UUID.filter() = vedtaksperiode(this)

        internal fun vedtaksperiode(vedtaksperiodeId: UUID): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstType == "Vedtaksperiode" && kontekst.kontekstMap["vedtaksperiodeId"] == vedtaksperiodeId.toString()
        }

        internal fun person(personidentifikator: Personidentifikator = UNG_PERSON_FNR_2018): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstMap["fødselsnummer"] == personidentifikator.toString()
        }

        internal fun arbeidsgiver(orgnummer: String): AktivitetsloggFilter = AktivitetsloggFilter { kontekst ->
            kontekst.kontekstType == "Arbeidsgiver" && kontekst.kontekstMap["organisasjonsnummer"] == orgnummer
        }
    }

    fun filtrer(kontekst: SpesifikkKontekst): Boolean
}


