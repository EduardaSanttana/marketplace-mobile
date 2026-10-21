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

A rota só é gerada se o pedido tiver `latitudeEntrega` e `longitudeEntrega`
preenchidas — ou seja, se o negociante já marcou o ponto de entrega na aba
**Entrega**. Sem isso, o botão **Rota** fica desabilitado e o cartão da entrega
mostra o aviso.

---

## Como testar

1. Perfil **Negociante** → aba **Entrega**: marcar o ponto de um negociante.
2. Aba **Meus Produtos**: cadastrar um produto por outro negociante.
3. Aba **Vitrine**: comprar o produto com o negociante que tem ponto definido.
4. Trocar para o perfil **Entregador** (botão de setas na barra superior).
5. Na entrega criada, tocar em **Rota**.

No emulador, definir a localização em *Extended Controls → Location* antes do
passo 5 — sem isso o GPS não devolve posição e a rota não é calculada.
