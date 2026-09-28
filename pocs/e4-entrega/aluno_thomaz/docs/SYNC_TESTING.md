# Sincronização SQLite (Room) <-> Firestore

O app mantém o cache local (Room/SQLite) sincronizado com o Firestore, funcionando
tanto online quanto offline. Este documento descreve o mecanismo e como testá-lo.
(Migrado de `pocs/e2-cad/aluno_melissaa/SYNC_POC.md` para o módulo principal.)

## Como funciona

Cada entidade (`Produto`, `Negociante`, `Avaliacao` — em `data/`) tem 4 campos de controle:

- `firestoreId`: id do documento no Firestore depois que o registro é enviado (`null` enquanto só existe localmente).
- `updatedAt`: timestamp usado para resolver conflitos (last write wins).
- `pendingSync`: `true` enquanto o registro tem alterações locais ainda não enviadas para a nuvem.
- `pendingDelete`: `true` quando o usuário apagou o registro localmente e a exclusão ainda precisa ser propagada.

Fluxo **nuvem -> SQLite** (`data/sync/*SyncRepository.kt`, método `startListening`):
cada repositório assina um `addSnapshotListener` na coleção do Firestore. Toda vez que um
documento é criado/alterado/removido na nuvem (por este ou outro dispositivo), o listener
atualiza a linha correspondente no Room, casando pelo `firestoreId`.

Fluxo **SQLite -> nuvem** (método `pushPendingChanges`): busca as linhas com `pendingSync = 1`
e `pendingDelete = 1` e envia para o Firestore (`add`/`set`/`delete`). Ao confirmar o envio,
marca a linha local como sincronizada.

O `SyncManager` (`data/sync/SyncManager.kt`) coordena os três repositórios e expõe o status
(`OCIOSO` / `SINCRONIZANDO` / `ERRO`) para a UI. O `CadastroViewModel` (`ui/CadastroViewModel.kt`):

1. Liga os listeners em tempo real assim que a tela abre.
2. Observa a conectividade (`data/sync/NetworkMonitor.kt`) e chama `syncAgora()`
   automaticamente sempre que a internet volta — cobrindo o caso de uso offline (cadastra
   sem internet, o registro fica marcado como pendente, e é enviado sozinho quando a conexão
   retorna).
3. Também permite forçar uma sincronização manual pelo botão na aba "Sincronização".

## Antes de rodar

1. No [console do Firebase](https://console.firebase.google.com/), abra o projeto
   `marketplace-ddm2-94de8` (o mesmo já usado pelas PoCs de autenticação e cadastro) e
   registre um novo app Android com o pacote `edu.ifsp.marketplace` (o `applicationId`
   real do módulo `app`).
2. Baixe o `google-services.json` gerado e substitua o arquivo placeholder em
   `app/google-services.json` (o arquivo atual tem `mobilesdk_app_id`/`api_key` fake e
   **não funciona** até ser trocado).
3. No console, confira que o banco Firestore tem as coleções `produtos`, `negociantes` e
   `avaliacoes` — elas são criadas automaticamente no primeiro envio, não precisa criar
   campos manualmente.
4. Para o login funcionar, habilite o provedor **E-mail/senha** em
   Authentication > Sign-in method, e crie ao menos um usuário de teste em
   Authentication > Users (ou peça pro app permitir cadastro, que hoje só faz login).

## Como testar

1. **Sincronização básica online**: com Wi-Fi/dados ligados, faça login e cadastre um
   produto na aba "Produtos". Ele aparece imediatamente no Firestore Console (coleção
   `produtos`) e o card mostra "☁ Sincronizado".
2. **Fila offline**: desligue o Wi-Fi/dados do aparelho/emulador e cadastre outro produto.
   O card mostra " Pendente de sincronização" (isso é o SQLite local guardando a
   alteração com `pendingSync = true`). Religue a internet — o app detecta a reconexão
   (`NetworkMonitor`) e sincroniza sozinho; ou force pela aba "Sincronização" >
   "Sincronizar agora".
3. **Nuvem -> app em tempo real**: com o app aberto, edite um documento diretamente no
   Firestore Console (ex.: mude o `nome` de um produto). A lista no app atualiza sozinha,
   sem precisar reabrir — vem do `addSnapshotListener`.
4. **Exclusão propagada**: toque em "Excluir" em um produto. Internamente isso só marca
   `pendingDelete = true` (`markPendingDelete`); a exclusão real no Firestore acontece na
   próxima sincronização, e só então a linha some do Room.
5. **Conflito last-write-wins**: edite o mesmo registro quase ao mesmo tempo no app
   (offline) e no Firestore Console; ao reconectar, o registro com `updatedAt` mais recente
   vence (ver `applyRemoteChange` em cada `*SyncRepository`).

### Dicas de inspeção

- Para ver o SQLite local, use o **App Inspection > Database Inspector** do Android
  Studio enquanto o app roda (procure o arquivo `marketplace.db`), ou `adb pull`/`adb shell`
  se preferir linha de comando.
- Para ver o Firestore, use a aba **Firestore Database** do console do Firebase — dá pra
  editar/excluir documentos manualmente ali mesmo para testar os fluxos acima.
