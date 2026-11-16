# Geofencing: aviso de entregador próximo (Entrega 8)

O manual pede, para o perfil Entregador:

> Enviar notificação para o cliente informando que o entregador está próximo do
> endereço de entrega (geofencing).

O entregador e o cliente (o negociante comprador) usam aparelhos diferentes. O
perímetro é vigiado no aparelho do entregador, e o aviso chega ao comprador pelo
Firestore, que o app já usa para sincronizar os pedidos.

## Fluxo

```
Aparelho do entregador                         Aparelho do comprador
──────────────────────                         ─────────────────────
"Retirar pacote"
  └─ GeofenceEntrega.registrar
     (círculo de 500 m no destino)
          │
   entra no perímetro
          │
GeofenceReceiver  ──(ou a tela de rota aberta)
  └─ ChegadaEntregador.registrar
       pedidos.entregadorProximoEm = agora  (SQLite, pendingSync = 1)
          │
     SyncManager ───── Firestore ─────►  listener de pedidos
                                           └─ NotificacaoEntrega.avisarSeForComprador
                                                "Seu entregador está chegando"
```

## Peças

| Arquivo | Papel |
|---|---|
| `geofencing/GeofenceEntrega.kt` | Registra e remove o geofence do pedido (`GeofencingClient`) |
| `geofencing/GeofenceReceiver.kt` | Recebe a entrada no perímetro, mesmo com o app fechado |
| `geofencing/ChegadaEntregador.kt` | Marca o pedido uma vez só, desliga o geofence e sincroniza |
| `geofencing/NotificacaoEntrega.kt` | Canal e notificação; só notifica a conta do comprador |
| `data/sync/PedidoSyncRepository.kt` | Percebe `entregadorProximoEm` chegando da nuvem |
| `ui/entregador/RotaEntregaScreen.kt` | Registra o geofence e confere a distância com a tela aberta |
| `ui/negociante/MeusPedidosScreen.kt` | Mostra "Entregador a menos de 500 m desde HH:mm" |

## Decisões

- **Raio de 500 m** (`Pedido.RAIO_ENTREGADOR_PROXIMO_METROS`). O Google recomenda
  ao menos 100–150 m; 500 m dá alguns minutos de antecedência ao comprador.
- **Sem Firebase Cloud Messaging.** Mandar push de um aparelho a outro exige um
  servidor (Cloud Functions, plano pago). O campo no pedido segue pelo mesmo
  caminho SQLite → Firestore do resto do app, e o comprador recebe a notificação
  enquanto o processo do app estiver vivo (aberto ou em segundo plano recente).
- **Uma vez por pedido.** `marcarEntregadorProximo` só altera pedidos
  `EM_TRANSPORTE` com o campo vazio, então o geofence e a tela de rota podem
  disparar juntos sem notificar duas vezes. Avisos com mais de 30 min são ignorados
  (aparelho que ficou offline e sincroniza depois).
- **Duas fontes de chegada.** A partir do Android 10, o geofence só funciona com a
  localização **o tempo todo**. Sem ela, a tela de rota acompanha a posição
  (`LocalizacaoService.atualizacoes`) e faz a mesma conta enquanto está aberta.
  A tela informa qual modo está valendo e oferece o pedido da permissão.
- **Mesma conta nos dois perfis.** Se o comprador for a conta logada no próprio
  aparelho do entregador (demonstração com um aparelho só), a notificação sai ali mesmo.

## Como demonstrar

Com dois emuladores (ou aparelhos) e duas contas:

1. Conta A, negociante: marca o ponto de entrega e compra um produto da conta B.
2. Aparelho do entregador: perfil Entregador → **Rota** → **Retirar pacote**.
   Se aparecer "Permitir localização em segundo plano", tocar e escolher
   **Permitir o tempo todo**.
3. No emulador do entregador, em **Extended controls → Location**, mover a
   posição para perto do ponto de entrega (menos de 500 m).
4. Em segundos, o aparelho da conta A recebe "Seu entregador está chegando", e o
   pedido em **Pedidos** mostra "Entregador a menos de 500 m do destino desde HH:mm".

Com um aparelho só: a mesma conta compra (de outro negociante), troca para o
perfil Entregador e repete os passos 2 e 3.
