# Marketplace

Projeto **#2 — Marketplace** da disciplina Desenvolvimento para Dispositivos Móveis 2.

Aplicativo Android em que negociantes compram e vendem produtos e entregadores
são guiados no processo de entrega.

## Equipe:

- Bruno Possar
- Eduarda Santana
- Luana Melissa
- Thomaz Segreto

---

## Perfis de usuário

O perfil ativo é trocado pelo botão de setas na barra superior.

### Conta e negociante

Cada conta do Firebase Auth age como **um** negociante. No primeiro acesso como
negociante, a tela **Quem é você?** oferece criar o próprio cadastro (**Ainda não
tenho cadastro**) ou escolher um já existente; quem só entrega toca em **Sou só
entregador**. O entregador não cadastra negociantes: cada um se cadastra. O vínculo
fica no cadastro do negociante e segue para a nuvem, então vale em qualquer
aparelho. Negociantes já ligados a outra conta aparecem bloqueados. Na Vitrine,
**Meus dados** corrige nome, e-mail e telefone, e **Não sou eu** desfaz o vínculo.

### Negociante

| Aba | O que faz |
|---|---|
| Vitrine | Busca por produto ou vendedor e compra, em uma ou mais unidades, produtos de outros negociantes, com a nota média das avaliações |
| Produtos | Cadastra, edita e exclui os próprios produtos, com foto |
| Pedidos | Acompanha compras e vendas (rastreamento) numa linha do tempo com a hora da compra, da retirada e da entrega, vê o destino no mapa, cancela pedidos que aguardam retirada e apaga os entregues |
| Entrega | Indica no mapa, por CEP ou toque, o endereço onde recebe as compras e de onde saem as vendas |
| Avaliações | Avalia produtos comprados |

### Entregador

| Aba | O que faz |
|---|---|
| Entregas | Lista os pedidos em aberto ou entregues, retira o pacote, liga para o vendedor ou o comprador e registra a entrega com foto, coordenadas e endereço |

A partir de uma entrega, o botão **Rota** traça o caminho da posição atual até
o vendedor, para retirar o pacote. Ao tocar em **Retirar pacote**, a rota é
recalculada até o ponto de entrega do comprador, e a mesma tela registra a
entrega ao chegar. A mais de 300 m do ponto marcado, o app pergunta antes de
registrar.

---

## Fluxo do pedido

```
Negociante compra          Entregador retira        Entregador registra
      |                           |                   foto + coordenadas
      v                           v                           v
  AGUARDANDO  ----------->  EM TRANSPORTE  -------------->  ENTREGUE
```

O negociante acompanha esses três estágios na aba **Pedidos**.

O ponto marcado no mapa é o destino da rota, e o endereço do local real da entrega
é comparado com esse ponto. Detalhes em
[docs/GEOCODIFICACAO_REVERSA.md](docs/GEOCODIFICACAO_REVERSA.md).

---

## Arquitetura

- **Kotlin + Jetpack Compose** (Material 3)
- **Room/SQLite** como cache local dos dados estruturados
- **Firebase Auth** para o controle de acesso
- **Cloud Firestore** como armazenamento definitivo, sincronizado de forma assíncrona
- **Fotos** gravadas apenas no sistema de arquivos do dispositivo
- **Google Maps** para exibição do mapa
- **OSRM / OpenStreetMap** para cálculo de rotas
- **Geocoder do Android / Photon (OpenStreetMap)** para obter endereços a partir de coordenadas

Os dados são sempre gravados primeiro no SQLite e enviados ao Firestore quando
houver conectividade, conforme exigido pelo manual do curso. Uma faixa sob a
barra superior mostra isso acontecendo: sem conexão, conta quantas alterações
estão salvas no aparelho esperando envio; quando tudo chega à nuvem, some.

Cada referência entre registros (produto → vendedor, pedido → produto, comprador
e vendedor, avaliação → produto) guarda o **id do documento no Firestore**, que é o
mesmo em todos os aparelhos. O id do SQLite muda de aparelho para aparelho e é
só a tradução local, recalculada sempre que um dado chega da nuvem
(`data/sync/Vinculos.kt`). Registros de antes dessa mudança são religados pelo
nome copiado no pedido, quando ele é único.

As fotos ficam só no aparelho que as tirou; nos outros, o pedido mostra
"Foto registrada no aparelho do entregador".

---

## Configuração

Criar o arquivo `local.properties` na raiz com a chave do Google Maps:

```properties
MAPS_API_KEY=sua_chave_aqui
```

O arquivo `app/google-services.json` do Firebase também é necessário.

---

## Estrutura do repositório

```
/
├── app/                        Projeto principal Android
├── pocs/                       Provas de Conceito
│   ├── e1-auth/aluno_eduarda/      Firebase Auth
│   ├── e2-cad/aluno_melissaa/      Cadastros + SQLite
│   ├── e3-foto/aluno_bruno/        Fotos no sistema de arquivos
│   ├── e4-entrega/aluno_thomaz/    Ponto de entrega no mapa
│   ├── e5-rotas/aluno_eduarda/     Geração de rotas
│   └── e6-geocoding/aluno_thomaz/  Endereços a partir de coordenadas
├── docs/                       Documentação técnica
├── .gitignore
└── README.md
```
