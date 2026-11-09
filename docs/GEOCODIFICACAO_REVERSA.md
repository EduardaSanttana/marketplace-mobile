# Geocodificação reversa

Documentação técnica da recuperação de endereços a partir de coordenadas.

> Requisito do manual: *"Registrar entrega de produto com foto, coordenadas da
> entrega e também endereço fornecido por API de localização (reverse geocoding API)."*

PoC de origem: `pocs/e6-geocoding/aluno_thomaz` (Thomaz Segreto).

---

## Como as entregas 5, 6 e 7 se ligam

As três funcionalidades trabalham sobre o mesmo pedido:

```
Entrega 5: mapa           Entrega 6: rota             Entrega 7: endereço
Comprador marca o ponto   Entregador vai da posição   Entregador registra foto,
no mapa                   atual até o ponto           coordenadas e endereço
        |                         |                           |
        v                         v                           v
latitudeEntrega    ------->  destino da rota          latitudeEntregue
longitudeEntrega                                      longitudeEntregue
enderecoEntrega                                       enderecoEntregue
        |                                                     |
        +----------- comparados na aba Pedidos ---------------+
```

| Onde | O que a geocodificação reversa faz |
|---|---|
| Aba **Entrega** (negociante) | Tocar no mapa mostra o endereço do ponto antes de salvar; antes, o pino mudava e o texto continuava o antigo |
| Tela de **rota** (entregador) | Mostra o endereço de partida ao lado do destino |
| Registro da **entrega** | Grava o endereço do local real da entrega |
| Aba **Pedidos** e lista de **entregas** | Mostram endereço, coordenadas e a distância até o ponto marcado |

Outras ligações feitas nesta entrega:

- **Mapa → pedido.** Salvar o ponto na aba **Entrega** vale para as próximas compras
  e atualiza os pedidos do comprador que aguardam retirada. Depois da retirada, o
  destino só muda se o pedido estava sem ponto, para não trocar o destino de um
  pacote já a caminho.
- **Indicar e visualizar.** A aba **Entrega** indica o endereço do negociante; o toque
  só posiciona o pino, e nada muda até salvar. Na aba **Pedidos**, **Ver no mapa**
  mostra o destino daquele pedido, só para consulta.
- **Rota → entrega.** A rota vai primeiro até o vendedor. Ao retirar o pacote, é
  recalculada até o comprador e, ao chegar, a mesma tela registra a entrega.
- **Recalcular** parte da posição atual, não da posição de quando a tela abriu.

---

## Serviço escolhido

| Ordem | Serviço | Chave de API | Observação |
|---|---|---|---|
| 1 | `android.location.Geocoder` | não | Depende de o aparelho ter provedor; emuladores podem não ter |
| 2 | Photon / OpenStreetMap | não | Servidor público, sem garantia de disponibilidade |

```
GET https://photon.komoot.io/reverse?lat={lat}&lon={lon}&limit=1
```

O `Geocoder` tem duas APIs: a assíncrona (`GeocodeListener`, Android 13+) e a
síncrona, que roda em `Dispatchers.IO` nas versões anteriores (o `minSdk` é 30).

**Limitação conhecida:** o número da casa nem sempre vem na resposta, e a precisão
depende do provedor. O Photon pode devolver só a cidade em alguns pontos: na Praça
da Sé (-23.55052, -46.633308), a consulta de 04/11/2026 trouxe apenas "São Paulo".
Para uso real, o Photon público precisaria ser trocado por uma instância própria ou
um serviço contratado.

---

## Sem conexão

Seguindo o manual, tudo é salvo primeiro no aparelho:

1. A foto e as coordenadas são gravadas no SQLite na hora; a entrega já fica concluída.
2. O endereço é consultado em seguida e o pedido é atualizado.
3. Se a consulta falhar, a tela indica o endereço como pendente.
4. Quando a conexão volta, `CadastroViewModel.resolverEnderecosPendentes()` completa
   os endereços e envia ao Firestore.

Sem **posição** a entrega não é registrada: sem coordenadas não há como obter o
endereço depois. A tela pede para verificar o GPS e tentar de novo.

Como o endereço chega depois do primeiro envio, os repositórios de sincronização
usam `marcarSincronizado()`: a pendência só é liberada se o registro não mudou
durante o envio. Antes, o fim do envio sobrescrevia o registro com a versão antiga.

---

## Arquivos

| Arquivo | Papel |
|---|---|
| `app/.../util/GeocodificacaoService.kt` | Consulta o Geocoder e o Photon, devolve o endereço ou null |
| `app/.../util/LocalizacaoService.kt` | Permissões e posição atual, usados na rota e na entrega |
| `app/.../util/Distancia.kt` | Distância entre coordenadas e sua formatação |
| `app/.../ui/entregador/RegistroEntrega.kt` | Fluxo localização → câmera → foto → posição, usado nas duas telas |
| `app/.../ui/ComprovanteEntrega.kt` | Comprovação exibida aos dois perfis |
| `app/.../ui/CadastroViewModel.kt` | Grava primeiro, consulta o endereço depois, resolve pendências |

---

## Como testar

1. Em **Quem é você?**, cada conta cria o próprio cadastro (**Ainda não tenho cadastro**)
   ou escolhe um existente. São dois negociantes: um vende, outro compra.
2. Com a conta do vendedor, perfil **Negociante** → **Produtos**: cadastrar um produto.
3. Com a conta do comprador, **Vitrine**: comprar, **ainda sem ponto de entrega**.
   Com uma conta só, **Não sou eu** na Vitrine troca de negociante.
4. Aba **Entrega**: tocar no mapa. O endereço aparece sozinho;
   tocar em **Salvar ponto de entrega**.
5. Aba **Pedidos**: **Ver no mapa** mostra o destino do pedido. Perfil **Entregador** →
   **Entregas**: o pedido já mostra o destino e a **Rota** está liberada.
6. **Retirar** e abrir a **Rota**: aparecem partida, destino e traçado.
7. **Cheguei: registrar entrega**: conceder as permissões e tirar a foto. A mais de
   300 m do ponto marcado, o app pergunta antes de abrir a câmera.
8. Conferir endereço, coordenadas e distância na lista e na aba **Pedidos** do negociante.

**Sem conexão:** repetir o passo 7 em modo avião. A entrega é registrada com endereço
pendente, que é preenchido ao desligar o modo avião.

No emulador, definir a posição em *Extended Controls → Location* antes dos passos 6 e 7.
