package no.nav.helse.sak

class SakskjemaForGammelt(val skjemaVersjon: Int, currentSkjemaVersjon: Int) : RuntimeException( "Sak har skjemaversjon $skjemaVersjon, men kun versjon $currentSkjemaVersjon er støttet")
