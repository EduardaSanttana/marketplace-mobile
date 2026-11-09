# Geração de rotas

Documentação técnica da funcionalidade de rotas do perfil **Entregador**.

> Requisito do manual: *"Gerar rota da localização atual até o endereço de entrega."*

PoC de origem: `pocs/e5-rotas/aluno_eduarda` (Eduarda Santana).

---

## Serviço escolhido

| Função | Serviço | Chave de API |
|---|---|---|
| Exibição do mapa | Google Maps | sim (`MAPS_API_KEY`) |
| Cálculo da rota | OSRM / OpenStreetMap | não |

A Directions API do Google exige faturamento ativo. O OSRM é público e
gratuito, e o manual permite a escolha entre Google Maps e OpenStreetMap.

**Limitação conhecida:** o servidor `router.project-osrm.org` é de demonstração,
tem limite de requisições e não garante disponibilidade. Para uso real, as
opções são hospedar uma instância própria do OSRM ou contratar um serviço
equivalente.

---

## Endpoint

```
GET https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=polyline
```

> O OSRM espera **longitude,latitude** — ordem inversa à do `LatLng` do
> Google Maps. Trocar a ordem faz a rota cair no lugar errado.

Resposta relevante:

| Campo | Significado |
|---|---|
| `code` | `"Ok"` quando encontrou rota |
| `routes[0].geometry` | traçado, como polyline codificada |
| `routes[0].distance` | distância em metros |
| `routes[0].duration` | duração estimada em segundos |

---

## Arquivos

| Arquivo | Papel |
|---|---|
| `app/.../util/RotaService.kt` | Chama o OSRM, decodifica a polyline, expõe `Rota` |
| `app/.../ui/entregador/RotaEntregaScreen.kt` | Tela do mapa com o traçado |
| `app/.../ui/entregador/EntregasScreen.kt` | Lista de entregas; botão **Rota** |

---

## Decodificação da polyline

O campo `geometry` vem como texto compactado no formato *encoded polyline* do
Google (precisão 5): cada coordenada é guardada como a diferença em relação à
anterior, em blocos de 5 bits.

`RotaService.decodificarPolyline()` implementa a decodificação à mão. A
biblioteca `android-maps-utils` oferece `PolyUtil.decode()` caso o time prefira
trocar por ela.

---

## Permissões

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

A permissão é pedida em tempo de execução ao abrir a tela de rota. Negada, a
tela informa que não é possível gerar o percurso.

---

## Pré-condição

O percurso tem duas etapas, e cada uma precisa do seu ponto:

| Situação do pedido | A rota vai até | Campos usados |
|---|---|---|
| Aguardando retirada | O vendedor | `latitudeRetirada`, `longitudeRetirada` |
| Em transporte | O comprador | `latitudeEntrega`, `longitudeEntrega` |

Os dois pontos vêm do endereço que cada negociante marca na aba **Entrega**. Um
pedido aguardando retirada sem endereço do vendedor pode ser retirado mesmo assim;
a tela avisa e a rota começa na segunda etapa. O botão **Rota** só fica desabilitado
quando não há ponto para nenhuma das etapas disponíveis.

Pedidos criados antes desta mudança não têm local de retirada, até o vendedor salvar
o endereço de novo.

---

## Como testar

Cada conta age como um negociante só, então o teste usa duas contas (ou, com
uma só, **Não sou eu** na Vitrine para trocar de negociante entre os passos).

1. Conta do vendedor, perfil **Negociante**: aba **Entrega**, marcar e salvar o ponto;
   aba **Produtos**, cadastrar um produto.
2. Conta do comprador: aba **Entrega**, marcar e salvar o ponto.
3. Aba **Vitrine**: comprar o produto.
4. Trocar para o perfil **Entregador** (botão de setas na barra superior).
5. Na entrega criada, tocar em **Rota**: o traçado vai até o vendedor, e **Ligar para
   vendedor** abre o discador com o telefone dele.
6. Tocar em **Retirar pacote**: o traçado é recalculado até o comprador.

No emulador, definir a localização em *Extended Controls → Location* antes do
passo 5 — sem isso o GPS não devolve posição e a rota não é calculada.
