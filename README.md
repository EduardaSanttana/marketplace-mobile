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

### Negociante

| Aba | O que faz |
|---|---|
| Vitrine | Compra produtos anunciados por outros negociantes |
| Meus Produtos | Cadastra produtos à venda, com foto |
| Pedidos | Acompanha o estágio de cada entrega (rastreamento) |
| Entrega | Define o ponto de entrega no mapa, por CEP ou toque |
| Avaliações | Avalia produtos comprados |

### Entregador

| Aba | O que faz |
|---|---|
| Entregas | Lista os pedidos, retira o pacote e registra a entrega com foto, coordenadas e endereço |
| Negociantes | Consulta os negociantes cadastrados |

A partir de uma entrega, o botão **Rota** traça o caminho da posição atual
até o ponto de entrega.

---

## Fluxo do pedido

```
Negociante compra          Entregador retira        Entregador registra
      |                           |                   foto + coordenadas
      v                           v                           v
  AGUARDANDO  ----------->  EM TRANSPORTE  -------------->  ENTREGUE
```

O negociante acompanha esses três estágios na aba **Pedidos**.

---

## Arquitetura

- **Kotlin + Jetpack Compose** (Material 3)
- **Room/SQLite** como cache local dos dados estruturados
- **Firebase Auth** para o controle de acesso
- **Cloud Firestore** como armazenamento definitivo, sincronizado de forma assíncrona
- **Fotos** gravadas apenas no sistema de arquivos do dispositivo
- **Google Maps** para exibição do mapa
- **OSRM / OpenStreetMap** para cálculo de rotas

Os dados são sempre gravados primeiro no SQLite e enviados ao Firestore quando
houver conectividade, conforme exigido pelo manual do curso.

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
│   └── e5-rotas/aluno_eduarda/     Geração de rotas
├── docs/                       Documentação técnica
├── .gitignore
└── README.md
```
