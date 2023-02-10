```mermaid
classDiagram
  aktivitetslogg
  primitiver
  api --> model
  api --> primitiver
  api --> etterlevelse
  api --> utbetaling
  api --> aktivitetslogg
  etterlevelse --> primitiver
  etterlevelse --> inntekt
  inntekt --> primitiver
  inntekt --> aktivitetslogg
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