# PoC — Geração de rotas

**Aluna:** Eduarda Santana
**Entrega:** Geração de rotas (projeto #2 — Marketplace)

---

## Objetivo

Provar que o aplicativo consegue traçar uma rota entre a localização atual do
dispositivo e um ponto de destino escolhido no mapa, exibindo o traçado,
a distância e o tempo estimado de viagem.

No app principal, essa funcionalidade atende o requisito do perfil **Entregador**:

> Gerar rota da localização atual até o endereço de entrega.

---

## O que a PoC exercita

1. Pedir permissão de localização em tempo de execução (`ACCESS_FINE_LOCATION`).
2. Capturar a posição atual do aparelho com `FusedLocationProviderClient`.
3. Deixar o usuário escolher o destino tocando no mapa.
4. Chamar uma API de rotas e receber o percurso.
5. Decodificar a *polyline* devolvida pela API.
6. Desenhar o traçado sobre o `GoogleMap` com `Polyline`.
7. Exibir distância e duração estimadas.

---

## Decisão técnica: por que OSRM e não Google Directions

A Directions API do Google exige chave **com faturamento ativo** (cartão de
crédito cadastrado). Para uma PoC de disciplina isso não se justifica.

O **OSRM** (Open Source Routing Machine) é um serviço público baseado em dados
do OpenStreetMap que calcula rotas sem exigir chave de API. O manual do curso
permite a escolha:

> Para as funcionalidades que envolvam mapas e endereços, será permitida a
> escolha entre Google Maps e OpenStreetMap.

A combinação adotada foi:

| Função | Serviço |
|---|---|
| Exibição do mapa | Google Maps (já usado no app) |
| Cálculo da rota | OSRM / OpenStreetMap |

**Limitação:** o servidor de demonstração `router.project-osrm.org` tem limite de
uso e não deve ser usado em produção. Para o app final, as opções são hospedar
uma instância própria do OSRM ou contratar um serviço como o Geoapify.

---

## Endpoint utilizado

```
GET https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=polyline
```

> Atenção: o OSRM espera as coordenadas na ordem **longitude,latitude** —
> o inverso do `LatLng` do Google Maps. Trocar a ordem foi o erro mais comum
> durante o desenvolvimento.

Resposta (resumida):

```json
{
  "code": "Ok",
  "routes": [
    {
      "geometry": "fnr~Cn}k{GaA}@...",
      "distance": 5432.1,
      "duration": 684.3
    }
  ]
}
```

---

## Decodificação da polyline

O campo `geometry` não vem como lista de coordenadas, e sim como um texto
compactado no formato *encoded polyline* do Google (precisão 5). Cada
coordenada é guardada como a **diferença** em relação à anterior, em blocos de
5 bits, o que reduz bastante o tamanho da resposta.

O decodificador foi escrito à mão em `RotaService.decodificarPolyline()` para
entender o formato. A biblioteca `android-maps-utils` tem a função
`PolyUtil.decode()` pronta, caso o time prefira usá-la no app principal.

---

## Como executar

1. Criar o arquivo `local.properties` na raiz desta PoC com a chave do Maps:

   ```properties
   MAPS_API_KEY=sua_chave_aqui
   ```

2. Abrir a pasta `aluno_eduarda` no Android Studio e rodar no emulador ou
   em um aparelho físico.

3. Conceder a permissão de localização quando o app pedir.

4. No emulador, definir uma localização em
   *Extended Controls → Location* (o GPS do emulador começa sem posição).

5. Tocar em um ponto do mapa para marcar o destino e depois em **Gerar rota**.

---

## Resultado

- Marcador azul: origem (localização atual)
- Marcador vermelho: destino escolhido
- Linha azul: traçado devolvido pelo OSRM
- Rodapé: distância, tempo estimado e número de pontos do traçado

---

## Dificuldades encontradas

- **Ordem das coordenadas.** O OSRM usa `longitude,latitude`; o Google Maps usa
  `LatLng(latitude, longitude)`. Invertido, a rota cai no oceano.
- **Localização nula no emulador.** `getCurrentLocation` devolve `null` até que
  uma posição seja definida nos Extended Controls.
- **Enquadramento da rota.** Depois de calcular, foi preciso usar
  `LatLngBounds` + `newLatLngBounds` para que o percurso inteiro coubesse na tela.

---

## Integração no app principal

O `RotaService` foi copiado para o app principal em
`app/src/main/java/edu/ifsp/marketplace/util/RotaService.kt` e é usado pela tela
de rota do Entregador.
