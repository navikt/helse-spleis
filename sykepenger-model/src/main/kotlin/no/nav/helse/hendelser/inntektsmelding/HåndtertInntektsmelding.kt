package no.nav.helse.hendelser.inntektsmelding

import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.inneholder

internal class HåndtertInntektsmelding(private val inntektsmelding: Inntektsmelding) {
    private val håndtertInntektsmeldingNå = mutableSetOf<UUID>()

    internal fun håndtertNå(vedtaksperiodeId: UUID) = apply { håndtertInntektsmeldingNå.add(vedtaksperiodeId) }

    internal companion object {
        private fun Person.signaliserInntektmeldingHåndtert(
            inntektsmelding: Inntektsmelding,
            vedtaksperioder: List<Vedtaksperiode>,
            håndtertInntektsmeldingNå: Set<UUID>
        ) {
            // Nå vet vi at minst én aktiv vedtaksperiode har håndtert en del av inntektsmeldingen. Enten tidligere, nå, eller en kombinasjon.
            // Om det er noen deler som er blitt håndtert _nå_ sender vi ut signal om at innektsmelding er håndtert av de respektive vedtaksperiodene.
            // De eventuelle vedtaksperiodene som håndtere en del nå, men også ble forkastet sender _ikke_ signal
            håndtertInntektsmeldingNå.filter { vedtaksperioder.inneholder(it) }.forEach {
               emitInntektsmeldingHåndtert(inntektsmelding.meldingsreferanseId(), it, inntektsmelding.organisasjonsnummer())
            }
        }

        private fun Person.signaliserInntektsmeldingFørSøknad(
            inntektsmelding: Inntektsmelding,
            overlappendeSykmeldingsperioder: List<Periode>
        ) {
            emitInntektsmeldingFørSøknadEvent(inntektsmelding, overlappendeSykmeldingsperioder, inntektsmelding.organisasjonsnummer())
            inntektsmelding.info("Inntektsmelding overlapper med sykmeldingsperioder $overlappendeSykmeldingsperioder")
        }

        private fun Person.signaliserInntektsmeldingIkkeHåndtert(
            inntektsmelding: Inntektsmelding
        ) {
            emitInntektsmeldingIkkeHåndtert(inntektsmelding, inntektsmelding.organisasjonsnummer())
            inntektsmelding.info("Inntektsmelding ikke håndtert")
        }

        internal fun List<HåndtertInntektsmelding>.signaliser(
            person: Person,
            vedtaksperioder: List<Vedtaksperiode>,
            sykmeldingsperioder: Sykmeldingsperioder
        ) {
            if (isEmpty()) return
            val inntektsmelding = first().inntektsmelding
            val hendelseId = inntektsmelding.meldingsreferanseId()
            check(all { it.inntektsmelding.meldingsreferanseId() == hendelseId }) { "Kan ikke signalisere på tvers av inntektsmeldinger" }

            val håndtertInntektsmeldingNå = map { it.håndtertInntektsmeldingNå }.flatten().toSet()
            if (vedtaksperioder.any { hendelseId in it.hendelseIder() }) return person.signaliserInntektmeldingHåndtert(inntektsmelding, vedtaksperioder, håndtertInntektsmeldingNå)

            val overlappendeSykmeldingsperioder = sykmeldingsperioder.overlappendePerioder(inntektsmelding)
            if (overlappendeSykmeldingsperioder.isNotEmpty())  return person.signaliserInntektsmeldingFørSøknad(inntektsmelding, overlappendeSykmeldingsperioder)

            person.signaliserInntektsmeldingIkkeHåndtert(inntektsmelding)
        }
    }
}