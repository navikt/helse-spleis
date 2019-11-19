package no.nav.helse.sak

class SakskjemaForGammelt(private val aktørId: String, private val skjemaVersjon: Int, private val currentSkjemaVersjon: Int) :
        RuntimeException( "Sak for person ${aktørId} har skjemaversjon ${skjemaVersjon}, men kun versjon $currentSkjemaVersjon er støttet"){

}
