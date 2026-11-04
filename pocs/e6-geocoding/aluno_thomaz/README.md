# PoC — Endereços a partir de coordenadas (reverse geocoding)

**Aluno:** Thomaz Segreto
**Entrega:** Entrega 7, 04/11/2026 (projeto #2 — Marketplace)

---

## Objetivo

Provar que o aplicativo consegue transformar latitude e longitude em um endereço
legível, e que lida bem com a falta de resposta do serviço.

No app principal, isso atende o requisito do perfil **Entregador**:

> Registrar entrega de produto com foto, coordenadas da entrega e também endereço
> fornecido por API de localização (reverse geocoding API).

---

## O que a PoC exercita

1. Consultar o `Geocoder` do Android, com a API assíncrona a partir do Android 13.
2. Recorrer ao Photon (OpenStreetMap) quando o aparelho não tem provedor ou ele não responde.
3. Rejeitar coordenadas inválidas antes de consultar qualquer serviço.
4. Mostrar uma mensagem quando nenhum dos dois serviços responde.

A PoC usa coordenadas digitadas: não exige GPS, login, Firebase nem chave do Maps.

---

## Como executar

1. Abrir **esta pasta** (`aluno_thomaz`) no Android Studio e rodar `app`.
2. Tocar em **Consultar endereço** com as coordenadas iniciais (Praça da Sé, São Paulo).
3. Informar latitude `91`: a tela rejeita sem consultar o serviço.
4. Ativar o modo avião e consultar: aparece a mensagem de indisponibilidade. O
   provedor do aparelho pode ter cache, então o modo avião nem sempre causa a falha.

---

## Integração no app principal

O `GeocodificacaoService` foi copiado para
`app/src/main/java/edu/ifsp/marketplace/util/GeocodificacaoService.kt`. O uso em
cada tela está em [docs/GEOCODIFICACAO_REVERSA.md](../../../docs/GEOCODIFICACAO_REVERSA.md).
