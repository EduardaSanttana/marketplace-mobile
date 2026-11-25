# PoC — Geofencing: entregador próximo

**Aluno:** Bruno Possar
**Entrega:** Entrega 8, 18/11/2026 (projeto #2 — Marketplace)

---

## Objetivo

Provar que o aplicativo consegue vigiar um perímetro em volta do ponto de entrega
e reagir quando o aparelho entra nele, mesmo com o app fechado.

No app principal, isso atende o requisito do perfil **Entregador**:

> Enviar notificação para o cliente informando que o entregador está próximo do
> endereço de entrega (geofencing).

---

## O que a PoC exercita

1. Pedir as três permissões, na ordem que o Android exige: localização, localização
   **o tempo todo** (sem ela o `GeofencingClient` recusa com o erro 1004) e notificações.
2. Registrar um geofence circular com `GeofencingClient.addGeofences`, com
   transições de entrada e saída e `INITIAL_TRIGGER_ENTER` (já dentro ao ativar
   dispara na hora).
3. Receber o evento num `BroadcastReceiver` via `PendingIntent` mutável, com o app
   aberto ou fechado.
4. Transformar o evento numa notificação e num histórico na tela.

---

## Como executar (emulador)

1. Abrir **esta pasta** (`aluno_bruno`) no Android Studio e rodar `app`.
2. Tocar nos três botões de permissão. No segundo, o Android abre as configurações:
   escolher **Permitir o tempo todo** e voltar.
3. Em **Extended controls → Location** do emulador, colocar o aparelho **longe**
   do ponto (ex.: `-23.5800, -46.6400`) e tocar em **Ativar perímetro**.
4. Mover a localização para dentro do raio (ex.: `-23.5510, -46.6330`).
   Em até ~1 min chega a notificação "Entregador próximo" e a linha no histórico.
5. Mover para longe de novo: chega "Entregador saiu do perímetro".

Pelo terminal, o passo 4 é `adb emu geo fix -46.6330 -23.5510` (longitude primeiro).

O geofence depende de o Google Play Services receber posições novas. Se demorar,
abrir o Google Maps no emulador força uma leitura de localização.

---

## Integração no app principal

| PoC | App principal |
|---|---|
| `ativarPerimetro` | `geofencing/GeofenceEntrega.kt`, chamado ao retirar o pacote e ao abrir a rota |
| `GeofenceReceiver` | `geofencing/GeofenceReceiver.kt`: marca o pedido e sincroniza |
| Notificação local | `geofencing/NotificacaoEntrega.kt`, no aparelho do **comprador** |

Os detalhes estão em [docs/GEOFENCING.md](../../../docs/GEOFENCING.md).
