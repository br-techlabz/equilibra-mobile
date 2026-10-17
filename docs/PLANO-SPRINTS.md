# 📋 Plano de Desenvolvimento — Equilibra Mobile (Android)

> App Android nativo para o sistema financeiro doméstico **Equilibra** (backend Spring Boot 4 + MySQL, API REST com JWT).
> Stack: **Kotlin + Jetpack Compose (Material 3)**, Hilt, Retrofit/OkHttp, Room, DataStore, Navigation Compose, MPAndroidChart.

---

## 1. Levantamento de Funcionalidades da API

API REST **stateless com JWT** (Bearer token), organizada em **11 domínios**, **28 controladores**, **~130 endpoints**.
IDs são **UUIDs**, valores monetários são `BigDecimal`, datas usam `Instant`/`LocalDate` (ISO 8601).

### 🔐 Autenticação (`/auth`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastro (email + senha) |
| POST | `/auth/login` | Login → `accessToken`, `tokenType`, `expiresIn` |
| POST | `/auth/refresh` | Renova token via cookie |
| POST | `/auth/logout` | Revoga sessão |
| GET | `/users/me` | Dados do usuário autenticado |

### 🏦 Contas de Ativo (`/asset-accounts`)
- CRUD de contas (corrente, poupança, dinheiro, investimento, digital, outras)
- Ativar/desativar conta
- **Reconciliação** bancária (informa saldo → sistema calcula diferença e ajusta)

### 📂 Categorias (`/categories`)
- CRUD com aplicabilidade: `EXPENSE`, `INCOME`, `BOTH`
- Ativar/desativar

### 🏷️ Tags (`/tags`)
- CRUD de tags + ativar/desativar

### 💰 Despesas (`/expenses`)
- CRUD de despesas (descrição, data, conta, valor, categoria, notas, tags)
- Cancelar despesa

### 💵 Receitas (`/incomes`)
- CRUD de receitas (mesma estrutura) + cancelar

### 🔄 Transferências (`/transfers`)
- Transferência entre contas do próprio usuário + cancelar

### 📜 Histórico Unificado (`/transactions`)
- Lista paginada com filtros: tipo, status, período, conta, categoria, tags

### 💳 Cartão de Crédito
- **Cartões** (`/credit-cards`): CRUD, ativar/desativar (instituição, limite, fechamento, vencimento, timezone)
- **Compras** (`/credit-card-purchases`): compra parcelada (até 60x), listar por cartão, cancelar
- **Faturas** (`/credit-card-statements`): fatura atual, histórico, fechar fatura por ciclo
- **Pagamento** (`/credit-card-statements/{id}/payments`): pagar fatura via conta bancária (com idempotency key)

### 🎯 Metas Financeiras (`/goals`)
- Criar meta (nome, descrição, valor alvo, data alvo)
- Registrar contribuições
- Completar/arquivar meta

### 📅 Compromissos (`/commitments`)
- Compromissos de despesa/receita com vencimento
- **Gerar compromissos** a partir de regra de recorrência (`/generate`)
- Liquidar (settle) / cancelar
- **Indicadores**: atrasados, hoje, próximos, futuros

### 🔁 Regras de Recorrência (`/recurrences`)
- Criar regra (tipo, frequência mensal/anual, data fim, dia de vencimento)
- Pausar, retomar, encerrar, cancelar

### 📊 Orçamentos (`/budgets`)
- **Orçamento por categoria/mês** (CRUD)
- **Resumo** com status: `ON_TRACK`, `WARNING`, `EXCEEDED`

### 📈 Dashboard (`/dashboard`)
- Resumo do período (receita, despesa, saldo líquido, patrimônio)
- Saldo por conta · Transações recentes · Evolução de saldo
- Despesas/receitas por categoria
- Resumo de cartão (pendente, próximo vencimento, limite)

### 📑 Relatórios (`/reports`)
- **Financeiro**: abertura, totais, transferências, fechamento + detalhes paginados
- **Por categoria**: totais agrupados
- **Auditoria**: log de transações com tags e anexos
- **Exportação**: `PDF` e `CSV`

### 💵 Fluxo de Caixa (`/cash-flow`)
- Projeção read-only (abertura, projeções, fechamento, saldo mínimo, eventos)

### 📎 Anexos (`/transactions/{id}/attachments`)
- Upload multipart, listar, baixar conteúdo, excluir

---

## 2. Arquitetura do App

```
Kotlin + Jetpack Compose (Material 3)
├── ui/            → Telas + ViewModels (MVVM)
├── domain/        → Casos de uso + modelos de domínio
├── data/
│   ├── remote/    → Retrofit (API REST)
│   ├── local/     → Room (cache de leitura)
│   └── session/   → DataStore encrypted (token + usuário)
├── di/            → Hilt (injeção de dependência)
└── navigation/    → Jetpack Navigation Compose
```

**Bibliotecas:** Retrofit + OkHttp (interceptor JWT + refresh), Hilt, Compose, Navigation, MPAndroidChart, Room, DataStore (Preferences + encrypted), Coroutines/Flow, kotlinx.serialization.

### Regra de ouro: Repository como fonte única

```
UI (Compose) → ViewModel → UseCase → Repository
                                         ├── Remote (Retrofit)  ← fonte da verdade
                                         └── Local (Room)        ← cache de leitura
```

---

## 3. Estratégia de Persistência — API-first com cache Room

| Dado | Fonte primária | Cache local (Room) | Quando sincroniza |
|---|---|---|---|
| **Token JWT** | — | `DataStore` (encrypted) | Login, refresh, logout |
| **Usuário logado** | `/users/me` | `DataStore` (id, email) | Login + splash |
| **Contas** | `/asset-accounts` | Tabela `accounts` | Pull-to-refresh + após criar/editar |
| **Categorias** | `/categories` | Tabela `categories` | Pull-to-refresh + após criar/editar |
| **Tags** | `/tags` | Tabela `tags` | Pull-to-refresh + após criar/editar |
| **Transações** | `/expenses`, `/incomes`, `/transfers` | Tabela `transactions` (últimas ~100) | Pull-to-refresh + após criar/editar/cancelar |
| **Cartões** | `/credit-cards` | Tabela `credit_cards` | Pull-to-refresh + após criar/editar |
| **Compras de cartão** | `/credit-card-purchases` | Tabela `purchases` | Pull-to-refresh + após criar/cancelar |
| **Faturas** | `/credit-card-statements` | Tabela `statements` | Pull-to-refresh + após fechar/pagar |
| **Metas** | `/goals` | Tabela `goals` | Pull-to-refresh + após criar/contribuir |
| **Compromissos** | `/commitments` | Tabela `commitments` | Pull-to-refresh + após criar/liquidar |
| **Recorrências** | `/recurrences` | Tabela `recurrences` | Pull-to-refresh + após criar/pausar |
| **Orçamentos** | `/budgets` | Tabela `budgets` | Pull-to-refresh + após criar/editar |
| **Dashboard** | `/dashboard` | ❌ Sem cache (sempre fresco) | Sempre da API |
| **Relatórios** | `/reports/*` | ❌ Sem cache (sempre fresco) | Sempre da API |
| **Anexos** | API (multipart) | ❌ Sem cache (binários) | Sempre da API |

### Regras de cache
1. **Abertura do app**: lê do Room primeiro (tela abre instantânea) → busca na API em background → atualiza Room.
2. **Pull-to-refresh**: busca na API → atualiza Room → atualiza UI.
3. **Ações de escrita** (criar/editar/cancelar): chamam a API → se sucesso, atualizam o Room.
4. **Dashboard e relatórios**: sempre da API (agregados, mudam com frequência).
5. **Anexos**: nunca em Room (arquivos binários ficam só no backend).

### Tabelas Room (esboço)

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,            // CHECKING, SAVINGS, CASH, INVESTMENT, DIGITAL, OTHER
    val initialBalance: String,  // BigDecimal como String
    val currentBalance: String,
    val active: Boolean,
    val createdAt: String,       // Instant como String
    val updatedAt: String,
    val syncedAt: Long           // timestamp da última sync
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val applicability: String,   // EXPENSE, INCOME, BOTH
    val active: Boolean,
    val syncedAt: Long
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean,
    val syncedAt: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,            // EXPENSE, INCOME, TRANSFER, STATEMENT_PAYMENT
    val description: String,
    val occurredAt: String,
    val amount: String,
    val categoryId: String?,
    val sourceAccountId: String?,
    val destinationAccountId: String?,
    val notes: String?,
    val status: String,          // ACTIVE, CANCELLED
    val syncedAt: Long
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String,
    val nickname: String?,
    val totalLimit: String,
    val closingDay: Int,
    val dueDay: Int,
    val timezone: String?,
    val status: String,          // ACTIVE, INACTIVE
    val syncedAt: Long
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val creditCardId: String,
    val categoryId: String,
    val description: String,
    val totalAmount: String,
    val purchaseDate: String,
    val installmentCount: Int,
    val cycleMonth: String,
    val status: String,         // ACTIVE, REVERSED, CANCELLED
    val syncedAt: Long
)

@Entity(tableName = "statements")
data class StatementEntity(
    @PrimaryKey val id: String,
    val creditCardId: String,
    val cycleStart: String,
    val cycleEnd: String,
    val dueDate: String,
    val status: String,         // OPEN, CLOSED, OVERDUE, PARTIALLY_PAID, PAID, CANCELLED
    val totalAmount: String,
    val paidAmount: String,
    val outstandingAmount: String,
    val syncedAt: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val targetAmount: String,
    val targetDate: String,
    val status: String,          // ACTIVE, COMPLETED, ARCHIVED
    val progressAmount: String,
    val remainingAmount: String,
    val progressPercentage: String,
    val syncedAt: Long
)

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val type: String,            // EXPENSE, INCOME
    val description: String,
    val plannedAmount: String,
    val dueDate: String,
    val accountId: String,
    val categoryId: String,
    val recurrenceRuleId: String?,
    val status: String,          // PENDING, SETTLED, CANCELLED
    val overdue: Boolean,
    val syncedAt: Long
)

@Entity(tableName = "recurrences")
data class RecurrenceEntity(
    @PrimaryKey val id: String,
    val type: String,            // EXPENSE, INCOME
    val description: String,
    val amount: String,
    val categoryId: String,
    val accountId: String,
    val frequency: String,       // MONTHLY, YEARLY
    val startDate: String,
    val endDate: String?,
    val dueDay: Int,
    val status: String,          // ACTIVE, PAUSED, ENDED, CANCELLED
    val syncedAt: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val month: String,           // yyyy-MM
    val plannedAmount: String,
    val syncedAt: Long
)
```

---

## 4. Plano por Sprints

### **Sprint 1 — Fundação & Autenticação**
*Base técnica: projeto, rede, DI, tema, sessão.*

| # | Task | Entrega |
|---|---|---|
| 1.1 | Criar projeto Android (Kotlin, Compose, Material 3, minSdk 26) | Projeto buildando |
| 1.2 | Configurar Hilt + estrutura de pacotes (`ui`, `domain`, `data`, `di`) | DI funcional |
| 1.3 | Implementar `OkHttp` com interceptor JWT + refresh automático | Token anexado em todas as chamadas |
| 1.4 | Criar `DataStore` encrypted para `accessToken`, `refreshToken`, `userId`, `email` | Sessão persistente |
| 1.5 | Criar `AuthApi` (login, register, refresh, logout, me) + DTOs | Camada de dados de auth |
| 1.6 | Tela de Login (email + senha, validação, loading, erro) | UI de login |
| 1.7 | Tela de Registro (email + senha com regras de validação) | UI de registro |
| 1.8 | Tela de Splash / roteamento por estado de auth (lê DataStore) | Navegação inicial |
| 1.9 | Logout (revoga cookie no backend + limpa DataStore) | Encerramento de sessão |
| 1.10 | Navegação base: NavHost com rotas `splash`, `login`, `register`, `home` | Estrutura de rotas |

---

### **Sprint 2 — Dashboard**
*Visão geral financeira + esqueleto de navegação.*

| # | Task | Entrega |
|---|---|---|
| 2.1 | BottomNavigation (Dashboard, Transações, Cartões, Relatórios, Mais) | Navegação principal |
| 2.2 | `DashboardApi` + DTOs (summary, contas, recentes, evolução, categorias, cartão) | Camada de dados |
| 2.3 | Tela Dashboard: cards de resumo (receita, despesa, saldo líquido, patrimônio) | Resumo financeiro |
| 2.4 | Tela Dashboard: lista de saldos por conta | Saldo por conta |
| 2.5 | Tela Dashboard: transações recentes | Feed de recentes |
| 2.6 | Tela Dashboard: gráfico de evolução de saldo (MPAndroidChart) | Gráfico de evolução |
| 2.7 | Tela Dashboard: despesas/receitas por categoria (gráfico de barras/rosca) | Breakdown visual |
| 2.8 | Resumo de cartão no dashboard (pendente, próximo vencimento, limite) | Card cartão |
| 2.9 | Filtro de período (mês atual, anterior, intervalo customizado) | Seleção de datas |
| 2.10 | *Dashboard é sempre fresco (sem cache Room)* | Decisão de design |

---

### **Sprint 3 — Contas, Categorias & Tags (com cache Room)**
*Cadastros base que alimentam todo o sistema. Primeira camada Room.*

| # | Task | Entrega |
|---|---|---|
| 3.1 | Configurar Room (banco `equilibra.db`, Hilt module, estratégia de migração) | Room ativo |
| 3.2 | Criar `AccountEntity` + `AccountDao` (CRUD + `getAll`) | Camada local contas |
| 3.3 | Criar `AssetAccountApi` + DTOs (conta, reconcile) | Camada remota contas |
| 3.4 | `AccountRepository`: remoto como fonte da verdade, sincroniza Room | Repository contas |
| 3.5 | Tela de lista de contas (lê Room primeiro → atualiza da API em background) | Lista de contas |
| 3.6 | Tela de criação de conta (nome, tipo, saldo inicial) → API → atualiza Room | Nova conta |
| 3.7 | Tela de edição + ativar/desativar → API → atualiza Room | Gestão de conta |
| 3.8 | Tela de reconciliação (data/hora/saldo → ver diferença e ajuste) | Reconciliação |
| 3.9 | Criar `CategoryEntity` + `CategoryDao` + `CategoryApi` + DTOs | Camada categorias |
| 3.10 | `CategoryRepository` (API → Room) + tela de categorias: lista + criar + editar + ativar/desativar | CRUD categorias |
| 3.11 | Criar `TagEntity` + `TagDao` + `TagApi` + DTOs | Camada tags |
| 3.12 | `TagRepository` (API → Room) + tela de tags: lista + criar + editar + ativar/desativar | CRUD tags |
| 3.13 | Dialog de seleção de categoria/tag reutilizável (lê Room) | Componente selecionador |
| 3.14 | Padrão de UI: lista com pull-to-refresh + estado vazio + erro | UX padrão |

---

### **Sprint 4 — Transações (Despesas, Receitas, Transferências)**
*Core financeiro do app.*

| # | Task | Entrega |
|---|---|---|
| 4.1 | Criar `TransactionEntity` + `TransactionDao` (CRUD, `getRecent`, `getAllByType`) | Camada local transações |
| 4.2 | Criar `ExpenseApi`, `IncomeApi`, `TransferApi` + DTOs | Camada remota transações |
| 4.3 | `TransactionRepository` (API → Room, cache das últimas ~100) | Repository transações |
| 4.4 | Componente reutilizável de formulário de transação (descrição, valor, data, conta, categoria, tags, notas) | Formulário base |
| 4.5 | Tela de lista de despesas (paginada, filtros, pull-to-refresh) | Lista despesas |
| 4.6 | Tela de criar/editar despesa → API → atualiza Room | Nova despesa |
| 4.7 | Tela de lista de receitas (paginada, filtros incl. canceladas) | Lista receitas |
| 4.8 | Tela de criar/editar receita → API → atualiza Room | Nova receita |
| 4.9 | Tela de transferência (origem → destino) → API → atualiza Room | Nova transferência |
| 4.10 | Cancelar transação (despesa, receita, transferência) → API → atualiza Room | Cancelamento |
| 4.11 | Histórico unificado (`/transactions`) com filtros combinados (lê API, sem cache) | Histórico geral |
| 4.12 | Tela de detalhe da transação (info completa, anexos, status) | Detalhe transação |
| 4.13 | Formatação BR (R$, dd/MM/yyyy) + cores semânticas (vermelho despesa, verde receita) | UI financeira |

---

### **Sprint 5 — Anexos em Transações**
| # | Task | Entrega |
|---|---|---|
| 5.1 | `AttachmentApi` (upload multipart, listar, baixar, excluir) | Camada de dados |
| 5.2 | Seletor de arquivo (Storage Access Framework) | Upload |
| 5.3 | Tela de anexos da transação: upload, lista, download (salva via SAF) | Gestão de anexos |
| 5.4 | Preview de imagem/PDF quando aplicável | Visualização |
| 5.5 | *Anexos nunca ficam em Room* | Decisão de design |

---

### **Sprint 6 — Cartões de Crédito (1/2): Cartões & Compras**
| # | Task | Entrega |
|---|---|---|
| 6.1 | Criar `CreditCardEntity` + `CreditCardDao` + `CreditCardApi` + DTOs | Camada cartões |
| 6.2 | `CreditCardRepository` (API → Room) | Repository cartões |
| 6.3 | Tela de lista de cartões (limite total, disponível, status) | Lista cartões |
| 6.4 | Tela de criar cartão (nome, instituição, limite, fechamento, vencimento, timezone) | Novo cartão |
| 6.5 | Tela de edição + ativar/desativar → API → atualiza Room | Gestão cartão |
| 6.6 | Criar `PurchaseEntity` + `PurchaseDao` + `CreditCardPurchaseApi` + DTOs | Camada compras |
| 6.7 | `PurchaseRepository` (API → Room) | Repository compras |
| 6.8 | Tela de compras: lista por cartão, criar compra parcelada (1–60x), cancelar | Gestão compras |
| 6.9 | Tela de detalhe da compra (parcelas, valores, ciclo) | Detalhe compra |

---

### **Sprint 7 — Cartões de Crédito (2/2): Faturas & Pagamento**
| # | Task | Entrega |
|---|---|---|
| 7.1 | Criar `StatementEntity` + `StatementDao` + `CreditCardStatementApi` + DTOs | Camada faturas |
| 7.2 | `StatementRepository` (API → Room) | Repository faturas |
| 7.3 | Tela de faturas do cartão: atual + histórico (lê Room) | Lista faturas |
| 7.4 | Fechar fatura manualmente (`/close`) → API → atualiza Room | Fechamento |
| 7.5 | Tela de detalhe da fatura (itens, total, pago, pendente, vencimento) | Detalhe fatura |
| 7.6 | Pagar fatura (selecionar conta, valor, data) + idempotency key → API | Pagamento |
| 7.7 | Atualizar card do dashboard: faturas pendentes, próximo vencimento, limite | Resumo cartão |

---

### **Sprint 8 — Compromissos & Recorrências**
| # | Task | Entrega |
|---|---|---|
| 8.1 | Criar `CommitmentEntity` + `CommitmentDao` + `CommitmentApi` + DTOs | Camada compromissos |
| 8.2 | `CommitmentRepository` (API → Room) + tela de indicadores (atrasados, hoje, próximos, futuros) | Painel indicadores |
| 8.3 | Tela de lista de compromissos (filtros: tipo, status, período) | Lista compromissos |
| 8.4 | Tela de criar compromisso (tipo, descrição, valor, vencimento, conta, categoria) | Novo compromisso |
| 8.5 | Liquidar compromisso (data + valor) / cancelar → API → atualiza Room | Liquidação/cancelamento |
| 8.6 | Gerar compromissos em lote a partir de recorrência (`/generate`) | Geração em lote |
| 8.7 | Criar `RecurrenceEntity` + `RecurrenceDao` + `RecurrenceApi` + DTOs | Camada recorrências |
| 8.8 | `RecurrenceRepository` (API → Room) + tela de regras: criar, pausar, retomar, encerrar, cancelar | Gestão recorrências |

---

### **Sprint 9 — Metas Financeiras**
| # | Task | Entrega |
|---|---|---|
| 9.1 | Criar `GoalEntity` + `GoalDao` + `GoalApi` + DTOs | Camada metas |
| 9.2 | `GoalRepository` (API → Room) | Repository metas |
| 9.3 | Tela de lista de metas (progresso, % concluído, status) | Lista metas |
| 9.4 | Tela de criar meta (nome, descrição, valor, data alvo) | Nova meta |
| 9.5 | Registrar contribuição (valor, data, nota) → API → atualiza Room | Contribuição |
| 9.6 | Completar/arquivar meta → API → atualiza Room | Ações de meta |
| 9.7 | Barra de progresso visual com % | UI de progresso |

---

### **Sprint 10 — Orçamentos & Fluxo de Caixa**
| # | Task | Entrega |
|---|---|---|
| 10.1 | Criar `BudgetEntity` + `BudgetDao` + `BudgetApi` + `BudgetSummaryApi` + DTOs | Camada orçamentos |
| 10.2 | `BudgetRepository` (API → Room) | Repository orçamentos |
| 10.3 | Tela de lista de orçamentos por mês (lê Room) | Lista orçamentos |
| 10.4 | Criar/editar orçamento por categoria/mês → API → atualiza Room | CRUD orçamento |
| 10.5 | Resumo de orçamento (planejado vs realizado, % consumo, status `ON_TRACK`/`WARNING`/`EXCEEDED`) | Resumo visual |
| 10.6 | `CashFlowApi` + DTOs + tela de fluxo de caixa projetado (abertura → fechamento, eventos) | Projeção |
| 10.7 | Gráfico de projeção diária | Gráfico fluxo |

---

### **Sprint 11 — Relatórios & Exportação**
| # | Task | Entrega |
|---|---|---|
| 11.1 | `ReportApi` (financeiro, categoria, auditoria, exportação) + DTOs | Camada relatórios |
| 11.2 | Tela de relatório financeiro (abertura, totais, transferências, fechamento + detalhes paginados) | Relatório financeiro |
| 11.3 | Tela de relatório por categoria (grupos com totais) | Relatório categoria |
| 11.4 | Tela de auditoria (lista paginada com filtros: tipo, status, conta, categoria, tags) | Auditoria |
| 11.5 | Exportar PDF e CSV (compartilhar/salvar via SAF) | Exportação |
| 11.6 | *Relatórios sempre frescos (sem cache)* | Decisão de design |

---

### **Sprint 12 — Polish & Qualidade**
| # | Task | Entrega |
|---|---|---|
| 12.1 | Revisão geral de sincronização Room ↔ API em todas as telas | Consistência de cache |
| 12.2 | Pull-to-refresh em todas as listas | UX |
| 12.3 | Tema claro/escuro (Material 3 dynamic + fallback) | Tema |
| 12.4 | Localização pt-BR (moeda, datas, textos) | Localização |
| 12.5 | Tratamento de erros global (rede, validação, 401 → logout, 403) | Robustez |
| 12.6 | Testes unitários (ViewModels, UseCases, Repositories com Room em memória) | Cobertura |
| 12.7 | Testes de instrumentação (telas críticas: login, criar transação, dashboard) | E2E |
| 12.8 | Build release + assinatura + distribuição (APK / Play Console interna) | Distribuição |

---

## 5. Resumo da Cobertura

| Domínio | Endpoints | Sprint |
|---|---|---|
| Auth / Usuário | 6 | 1 |
| Dashboard | 1 | 2 |
| Contas | 7 | 3 |
| Categorias + Tags | 10 | 3 |
| Despesas / Receitas / Transferências | 12 | 4 |
| Histórico unificado | 1 | 4 |
| Anexos | 4 | 5 |
| Cartões + Compras + Faturas + Pagamento | 14 | 6–7 |
| Compromissos + Recorrências | 14 | 8 |
| Metas | 6 | 9 |
| Orçamentos + Fluxo de Caixa | 4 | 10 |
| Relatórios + Exportação | 7 | 11 |
| Qualidade/Polish | — | 12 |

**Total estimado:** ~12–14 semanas (1 sprint = 1 semana), **~100 tasks** granulares.

---

## 6. Decisões de Design Registradas

| Decisão | Escolha | Justificativa |
|---|---|---|
| Estratégia de dados | API-first com cache Room | Backend é a fonte da verdade; Room acelera abertura e permite leitura offline |
| Dashboard / Relatórios | Sem cache (sempre fresco) | Dados agregados mudam com frequência; cache geraria inconsistência |
| Anexos | Sem cache (binários) | Arquivos grandes ficam só no backend; download sob demanda |
| Sessão | DataStore encrypted | Token JWT + dados do usuário de forma segura |
| Sincronização | Pull-to-refresh + pós-escrita | Simples e previsível para o usuário |
