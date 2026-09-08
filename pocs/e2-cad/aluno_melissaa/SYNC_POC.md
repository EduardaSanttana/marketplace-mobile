# PoC: Sincronização SQLite (Room) <-> Firebase Firestore

Esta PoC estende o app de cadastro já existente para manter o cache local (Room/SQLite)
sincronizado com o Firestore, funcionando tanto online quanto offline.

## Como funciona

Cada entidade (`Produto`, `Negociante`, `Avaliacao`) ganhou 4 campos de controle:

- `firestoreId`: id do documento no Firestore depois que o registro é enviado (null enquanto só existe localmente).
- `updatedAt`: timestamp usado para resolver conflitos (last write wins).
- `pendingSync`: true enquanto o registro tem alterações locais ainda não enviadas para a nuvem.
- `pendingDelete`: true quando o usuário apagou o registro localmente e a exclusão ainda precisa ser propagada.

Fluxo **nuvem -> SQLite** (`data/sync/*SyncRepository.kt`, método `startListening`):
cada repositório assina um `addSnapshotListener` na coleção do Firestore. Toda vez que um
documento é criado/alterado/removido na nuvem (por este ou outro dispositivo), o listener
atualiza a linha correspondente no Room, casando pelo `firestoreId`.

Fluxo **SQLite -> nuvem** (método `pushPendingChanges`): busca as linhas com `pendingSync = 1`
e `pendingDelete = 1` e envia para o Firestore (`add`/`set`/`delete`). Ao confirmar o envio,
marca a linha local como sincronizada.

O `SyncManager` (`data/sync/SyncManager.kt`) coordena os três repositórios e expõe o status
(`OCIOSO` / `SINCRONIZANDO` / `ERRO`) para a UI. O `CadastroViewModel`:

1. Liga os listeners em tempo real assim que a tela abre.
2. Observa a conectividade (`data/sync/NetworkMonitor.kt`) e chama `syncAgora()`
   automaticamente sempre que a internet volta — cobrindo o caso de uso offline (cadastra
   sem internet, o registro fica marcado como pendente, e é enviado sozinho quando a conexão
   retorna).
3. Também permite forçar uma sincronização manual pelo botão na aba "Sincronização".

## Antes de rodar

1. No [console do Firebase](https://console.firebase.google.com/), abra o projeto
   `marketplace-ddm2-94de8` (o mesmo já usado pela PoC de autenticação) e registre um novo
   app Android com o pacote `com.example.aluno_melissaa`.
2. Baixe o `google-services.json` gerado e substitua o arquivo placeholder em
   `app/google-services.json` (o arquivo atual tem `api_key`/`mobilesdk_app_id` fake e
   **não funciona** até ser trocado).
3. No console, crie o banco Firestore (modo teste é suficiente para a PoC) nas coleções
   `produtos`, `negociantes` e `avaliacoes` — elas são criadas automaticamente no primeiro
   envio, não precisa criar campos manualmente.

## Como testar

- Cadastre um produto com internet ligada: ele aparece imediatamente no Firestore Console
  e o card mostra "☁ Sincronizado".
- Desligue o Wi-Fi/dados, cadastre outro produto: o card mostra "⏳ Pendente de sincronização".
  Religue a internet e observe que ele sincroniza sozinho (ou use o botão "Sincronizar agora"
  na aba Sincronização).
- Edite um documento diretamente no Firestore Console: o app atualiza a lista sozinho, sem
  precisar reabrir (isso vem do `addSnapshotListener`).
- Toque em "Excluir" em um produto: a exclusão é propagada para o Firestore na próxima
  sincronização, e só então a linha some do Room.
