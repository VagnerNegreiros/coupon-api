# Coupon API

API de cupons de desconto (Create, Get e Delete) construída com **Java 21 + Spring Boot 4**, seguindo
**Arquitetura Hexagonal (Ports & Adapters)** e **DDD tático** (entidade rica + value objects).

O foco do projeto não é a quantidade de endpoints, e sim que as **regras de negócio** sejam
corretas, **encapsuladas no domínio** e **comprovadas por testes de comportamento**.

---

## Como executar

Pré-requisito: **JDK 21**. Não é preciso instalar Maven (o projeto usa o Maven Wrapper) nem banco
de dados (H2 em memória).

```bash
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

| Recurso      | URL                                                                                            |
|--------------|------------------------------------------------------------------------------------------------|
| API          | `http://localhost:8080/coupon`                                                                 |
| Swagger UI   | `http://localhost:8080/swagger-ui.html`                                                        |
| Console H2   | `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:coupondb`, usuário `sa`, sem senha) |

Para usar outra porta: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`

### Testes

```bash
./mvnw test
```

94 testes: domínio, use cases, integração HTTP + banco, concorrência e regras de arquitetura.

---

## Endpoints

| Método   | Rota           | Sucesso                         | Erros                                                         |
|----------|----------------|---------------------------------|---------------------------------------------------------------|
| `POST`   | `/coupon`      | `201 Created` + header Location | `400` dados inválidos / JSON malformado                       |
| `GET`    | `/coupon/{id}` | `200 OK`                        | `400` id não é UUID · `404` não encontrado                    |
| `DELETE` | `/coupon/{id}` | `204 No Content`                | `400` id não é UUID · `404` não encontrado · `409` já deletado |

### Exemplo

```bash
curl -X POST http://localhost:8080/coupon -H "Content-Type: application/json" -d '{
  "code": "ABC-123",
  "description": "Desconto de boas-vindas",
  "discountValue": 0.8,
  "expirationDate": "2030-11-04T17:14:45.180Z",
  "published": false
}'
```

```json
{
  "id": "53e93080-4a57-4f5c-b260-5d22934c1ed4",
  "code": "ABC123",
  "description": "Desconto de boas-vindas",
  "discountValue": 0.8,
  "expirationDate": "2030-11-04T17:14:45.180Z",
  "status": "ACTIVE",
  "published": false,
  "redeemed": false
}
```

Os erros seguem o padrão **Problem Details (RFC 9457)**. Erros de regra de negócio indicam o campo:

```json
{
  "status": 400,
  "title": "Dados do cupom inválidos",
  "detail": "O valor de desconto deve ser no mínimo 0.5. Recebido: 0.4.",
  "instance": "/coupon",
  "field": "discountValue"
}
```

---

## Regras de negócio: onde estão e como são garantidas

| Regra                                                              | Implementação                                   | Testes                                                       |
|--------------------------------------------------------------------|-------------------------------------------------|--------------------------------------------------------------|
| `code`, `description`, `discountValue`, `expirationDate` obrigatórios | `Coupon.create`, `CouponCode`, `DiscountValue` | `CouponTest`, `CouponApiIntegrationTest`                     |
| Código alfanumérico com 6 caracteres                               | `CouponCode` (value object)                     | `CouponCodeTest`                                             |
| Caracteres especiais aceitos, mas removidos antes de salvar/retornar | `CouponCode.of` (sanitização encapsulada)     | `CouponCodeTest`, `persistsTheSanitizedCode`                 |
| Desconto mínimo de 0.5, sem máximo                                 | `DiscountValue` (value object)                  | `DiscountValueTest`, `hasNoUpperLimitForDiscountValue`       |
| Expiração nunca no passado                                         | `Coupon.create`                                 | `CouponTest`, `CreateCouponUseCaseTest`                      |
| Pode ser criado já publicado (`published` opcional, padrão `false`) | `CreateCouponRequest` → `Coupon.create`        | `canBeCreatedAlreadyPublished`, `publishedDefaultsToFalse…`  |
| Pode ser deletado a qualquer momento                               | `Coupon.delete` (sem restrição de estado)      | `canBeDeletedEvenIfPublishedOrExpired`                       |
| Soft delete preservando os dados do cadastro                       | `Coupon.delete` → status `DELETED` + `deletedAt` | `softDeletesTheCoupon`, `DeleteCouponUseCaseTest`          |
| Não é possível deletar um cupom já deletado                        | `Coupon.delete` lança `CouponAlreadyDeletedException` | `cannotDeleteTwice`, `CouponConcurrencyIntegrationTest` |

---

## Arquitetura

```
br.com.desafio.coupon
├── domain                       ← regras de negócio. Java puro, zero dependências externas
│   ├── coupon/                     Coupon (aggregate root), CouponCode, DiscountValue, CouponStatus
│   └── exception/                  DomainException, InvalidCouponException, CouponAlreadyDeletedException
│
├── application                  ← orquestra os casos de uso. Não conhece Spring, JPA nem HTTP
│   ├── usecase/                    CreateCouponUseCase, DeleteCouponUseCase, GetCouponUseCase
│   ├── port/out/                   CouponRepository (interface = porta de saída)
│   ├── dto/                        CreateCouponCommand (entrada), CouponOutput (saída)
│   └── exception/                  CouponNotFoundException, ConcurrentCouponModificationException
│
└── infrastructure               ← detalhes técnicos (adapters)
    ├── web/                        CouponController, Request/Response, ApiExceptionHandler
    ├── persistence/                CouponEntity (JPA), SpringDataCouponRepository, JpaCouponRepositoryAdapter
    └── config/                     UseCaseConfig (composition root: instancia os use cases)
```

**Regra de dependência:** `infrastructure → application → domain`. Nunca o contrário.

```
HTTP ─▶ CouponController ─▶ CreateCouponUseCase ─▶ Coupon.create()  (regras)
                                     │
                                     └─▶ CouponRepository (porta) ◀── JpaCouponRepositoryAdapter ─▶ H2
```

Essas regras são **verificadas automaticamente** pelo `ArchitectureTest` (ArchUnit). O build quebra se:

- o domínio depender de qualquer coisa fora de `java.*`;
- a camada application importar Spring, Jakarta/JPA, Hibernate ou a infraestrutura;
- um use case tiver mais de um método público ou um método público que não seja `execute`;
- um use case depender de uma classe concreta em vez de uma interface;
- uma porta não for interface;
- existir qualquer classe `*Service`.

---

## Decisões técnicas

- **Entidade rica, sem setters.** `Coupon` só muda de estado por comportamento (`create`, `delete`).
  Não há como colocar um cupom em estado inválido por fora do domínio.
- **Value objects** (`CouponCode`, `DiscountValue`, como `record`) concentram validação e
  normalização. O construtor também valida, então nem `new CouponCode("AB-123")` cria um código inválido.
- **Use cases de intenção única** com um só método público `execute`, em vez de um
  `CouponService` genérico. O use case orquestra (busca, delega ao domínio, persiste) e não contém `if`
  de regra de negócio.
- **Use cases sem anotações do Spring.** São instanciados em `UseCaseConfig` (composition root), o
  que mantém a camada application independente de framework e de canal (web, fila, CLI).
- **Modelo de domínio separado do modelo JPA** (`Coupon` x `CouponEntity`). O ORM exige construtor
  vazio e campos mutáveis, e isso não deve vazar para as regras de negócio.
- **Tempo injetado** via `java.time.InstantSource` (interface do JDK). O domínio recebe `Instant now`
  e fica determinístico. Os testes usam relógio fixo.
- **`BigDecimal` para dinheiro/desconto**, nunca `double`. A coluna usa `DECFLOAT` (precisão
  arbitrária) para respeitar a regra "sem máximo predeterminado".
- **Lock otimista (`@Version`)** para a regra "não deletar duas vezes" também valer com requisições
  simultâneas. Sem ele, duas requisições leriam `ACTIVE` ao mesmo tempo e ambas "deletariam". A
  requisição perdedora recebe `409`.
- **Cliente não controla campos de sistema.** `id`, `status` e `redeemed` enviados no POST são
  ignorados; o request só expõe os campos permitidos.
- **GET retorna cupons deletados** com `status: DELETED`. O soft delete existe para preservar
  histórico, e o contrato da API já prevê `DELETED` no enum de status.
- **Flyway** versiona o schema; o Hibernate roda com `ddl-auto=validate` (apenas confere o mapeamento).
- **Testes sem mocks.** Use cases são testados com um `InMemoryCouponRepository` (implementação real
  da porta que guarda cópias, como um banco). A API é testada ponta a ponta com MockMvc + H2.

## Possíveis evoluções

- Unicidade do `code` entre cupons ativos (não exigida no enunciado, por isso não implementada).
- Banco PostgreSQL via Docker Compose e Testcontainers nos testes de integração.
- Status `INACTIVE` automático para cupons expirados e fluxo de resgate (`redeemed`).
- Idempotency-Key no POST para evitar cupons duplicados em retentativas.
