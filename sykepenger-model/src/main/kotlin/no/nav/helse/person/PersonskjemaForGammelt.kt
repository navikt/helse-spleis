package no.nav.helse.person

class PersonskjemaForGammelt(private val aktørId: String, private val skjemaVersjon: Int, private val currentSkjemaVersjon: Int) :
        RuntimeException( "Person ${aktørId} har skjemaversjon ${skjemaVersjon}, men kun versjon $currentSkjemaVersjon er støttet"){

}
