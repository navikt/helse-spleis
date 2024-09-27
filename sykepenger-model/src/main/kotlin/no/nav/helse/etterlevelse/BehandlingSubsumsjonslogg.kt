package no.nav.helse.etterlevelse

import java.util.UUID

class BehandlingSubsumsjonslogg(
    private val parent: Subsumsjonslogg,
    private val kontekster: List<Subsumsjonskontekst>
) : Subsumsjonslogg {

    constructor(subsumsjonslogg: Subsumsjonslogg) : this(subsumsjonslogg, emptyList())

    override fun logg(subsumsjon: Subsumsjon) {
        sjekkKontekster()
        if (tomPeriode(subsumsjon)) return
        leggTil(subsumsjon.copy(kontekster = kontekster))
    }

    private fun tomPeriode(subsumsjon: Subsumsjon): Boolean {
        if (subsumsjon.type != Subsumsjon.Subsumsjonstype.PERIODISERT) return false
        if ("perioder" !in subsumsjon.output) return true
        val perioder = subsumsjon.output["perioder"] as List<*>
        return perioder.isEmpty()
    }

    private fun leggTil(subsumsjon: Subsumsjon) {
        parent.logg(subsumsjon)
    }

    private fun sjekkKontekster() {
        val kritiskeTyper = setOf(KontekstType.Fødselsnummer, KontekstType.Organisasjonsnummer)
        check(kritiskeTyper.all { kritiskType ->
            kontekster.count { it.type == kritiskType } == 1
        }) {
            "en av $kritiskeTyper mangler/har duplikat:\n${kontekster.joinToString(separator = "\n")}"
        }
        // todo: sjekker for mindre enn 1 også ettersom noen subsumsjoner skjer på arbeidsgivernivå. det burde vi forsøke å flytte/fikse slik at
        // alt kan subsummeres i kontekst av en behandling.
        check(kontekster.count { it.type == KontekstType.Vedtaksperiode } <= 1) {
            "det er flere kontekster av ${KontekstType.Vedtaksperiode}:\n${kontekster.joinToString(separator = "\n")}"
        }
    }

    fun medFødselsnummer(personidentifikator: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Fødselsnummer, personidentifikator)))

    fun medOrganisasjonsnummer(organisasjonsnummer: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Organisasjonsnummer, organisasjonsnummer)))

    fun medVedtaksperiode(vedtaksperiodeId: UUID, hendelseIder: List<Subsumsjonskontekst>) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Vedtaksperiode, vedtaksperiodeId.toString())) + hendelseIder)

    fun medInntektsmelding(inntektsmeldingId: UUID) = kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Inntektsmelding, inntektsmeldingId.toString())))

    private fun kopierMedKontekst(kontekster: List<Subsumsjonskontekst>) =
        BehandlingSubsumsjonslogg(this, this.kontekster + kontekster)
}