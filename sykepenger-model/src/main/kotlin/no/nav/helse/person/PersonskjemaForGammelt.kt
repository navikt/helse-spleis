package no.nav.helse.person

class PersonskjemaForGammelt(val skjemaVersjon: Int, currentSkjemaVersjon: Int) : RuntimeException("Person har skjemaversjon $skjemaVersjon, men kun versjon $currentSkjemaVersjon er støttet")
