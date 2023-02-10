```mermaid
classDiagram

    api --> model
    api --> primitiver
    api --> etterlevelse
    api --> utbetaling
    api --> aktivitetslogg
    etterlevelse --> primitiver
    inntekt --> aktivitetslogg
    inntekt --> primitiver
    inntekt --> etterlevelse
    mediators --> model
    mediators --> utbetaling
    mediators --> etterlevelse
    mediators --> primitiver
    mediators --> aktivitetslogg
    model --> primitiver
    model --> utbetaling
    model --> aktivitetslogg
    model --> etterlevelse
    model --> inntekt
    utbetaling --> primitiver
    utbetaling --> aktivitetslogg

```