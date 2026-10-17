# Prompts de implementação — Equilibra Mobile (Android)

> Documento gerado a partir do `PLANO-SPRINTS.md`. Use os prompts na ordem das sprints e execute uma task por vez no Claude Code.

## Contexto e instruções globais para o Claude Code

Você está implementando o aplicativo Android nativo **Equilibra Mobile**, cliente do sistema de finanças domésticas Equilibra. Stack definida no planejamento: **Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit/OkHttp, Room, DataStore, Navigation Compose, Coroutines/Flow, kotlinx.serialization e MPAndroidChart**. Backend: Spring Boot 4, MySQL, API REST stateless com JWT. IDs UUID; valores monetários BigDecimal; datas Instant/LocalDate em ISO 8601.

### Regras obrigatórias
1. **Antes de alterar código**, inspecione a estrutura, convenções, arquivos, dependências, estado do Git e implementação existente. Não recrie nem sobrescreva trabalho existente.
2. **Contrato real da API prevalece.** Procure OpenAPI, documentação, código backend disponível ou exemplos de requests/responses. Não invente endpoint, campo, enum, paginação, regra financeira ou formato de token. Se não houver evidência suficiente, implemente apenas o que for possível e liste a dúvida objetiva no relatório final.
3. **Arquitetura:** Compose UI → ViewModel → UseCase quando útil → Repository → API/Room. Repository é a fonte única para UI; API é fonte da verdade; Room é cache de leitura para os domínios definidos. Não colocar regra de negócio em composables.
4. **Persistência:** dinheiro no Room como String decimal para preservar precisão; IDs UUID como String; datas ISO ou timestamps definidos no modelo. Dashboard, relatórios e anexos binários não ficam em Room. Anexos são obtidos sob demanda.
5. **Segurança:** tokens nunca em logs; armazenamento seguro apoiado em mecanismos atuais do Android; não versionar credenciais, keystores, arquivos `.env` ou segredos. Não desabilitar TLS para contornar erros.
6. **UX:** textos em pt-BR; estados de loading, sucesso, vazio, erro e retry quando aplicável; acessibilidade, suporte a telas pequenas, prevenção de duplo envio e confirmação para ações destrutivas. Reutilize componentes existentes.
7. **Qualidade:** escreva/atualize testes proporcionais à task, execute testes e build relevantes, corrija regressões introduzidas e não esconda falhas. Não marque uma etapa como concluída se build/teste não passou.
8. **Escopo:** implemente somente a task solicitada e dependências estritamente necessárias. Não avance para a próxima task. Não faça refatorações amplas ou migrações destrutivas sem necessidade e autorização.
9. **Relatório final obrigatório:** resumo do que foi feito; arquivos principais alterados; testes/build executados e resultados; decisões/limitações; passos manuais ou dúvidas pendentes.

### Formato de uso
Copie o prompt global acima uma vez no início da sessão de trabalho (ou mantenha-o como contexto do projeto) e depois copie **um bloco de task por vez**. Se iniciar uma nova sessão do Claude Code, forneça novamente o contexto global para manter consistência.

---

# Sprint 1 — Fundação & Autenticação
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 1.1 — Criar projeto Android

**Prompt para Claude Code**

> Implemente a **Task 1.1 — Criar projeto Android** do Equilibra Mobile. Objetivo: Inicializar o projeto nativo Equilibra Mobile com Kotlin, Jetpack Compose, Material 3 e minSdk 26. Inspecione o ambiente e convenções já existentes; configure Gradle Kotlin DSL, package name coerente, version catalog se aplicável, build types e configuração de Compose. Não sobrescreva um projeto existente sem verificar seu estado.
>
> Critérios de aceite: Projeto compila em debug; estrutura mínima executa no emulador; versões de dependências compatíveis e documentadas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.2 — Hilt e estrutura de pacotes

**Prompt para Claude Code**

> Implemente a **Task 1.2 — Hilt e estrutura de pacotes** do Equilibra Mobile. Objetivo: Configurar injeção de dependência e camadas ui, domain, data e di. Configure Application class, plugins/dependências Hilt e KSP/KAPT conforme compatibilidade; defina pacotes para telas/ViewModels, casos de uso/modelos, remote/local/session e módulos DI.
>
> Critérios de aceite: Aplicativo compila; Hilt resolve uma dependência de exemplo; responsabilidades ficam separadas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.3 — OkHttp JWT e refresh

**Prompt para Claude Code**

> Implemente a **Task 1.3 — OkHttp JWT e refresh** do Equilibra Mobile. Objetivo: Implementar cliente HTTP com Bearer JWT e renovação automática de token. Inspecione o contrato real de autenticação e refresh no backend. Evite loops de refresh, concorrência de múltiplos refreshes e retry infinito; não faça refresh recursivo pelo mesmo interceptor. Trate 401 após refresh e logout seguro.
>
> Critérios de aceite: Token anexado somente a chamadas protegidas; refresh conforme contrato real; testes para sucesso, falha, concorrência e sessão expirada.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.4 — DataStore de sessão segura

**Prompt para Claude Code**

> Implemente a **Task 1.4 — DataStore de sessão segura** do Equilibra Mobile. Objetivo: Persistir accessToken, refreshToken quando suportado, userId e email. O documento pede DataStore encrypted; verifique as bibliotecas atuais e use solução segura apoiada em Android Keystore para segredos, sem inventar API de criptografia inexistente. Armazene apenas dados mínimos e nunca registre tokens em logs.
>
> Critérios de aceite: Leitura/escrita/limpeza testáveis; segredos protegidos; comportamento de migração/erro documentado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.5 — AuthApi e DTOs

**Prompt para Claude Code**

> Implemente a **Task 1.5 — AuthApi e DTOs** do Equilibra Mobile. Objetivo: Criar API e DTOs para login, cadastro, refresh, logout e usuário atual. Confirme payloads, respostas, status codes, cookies e nomes de campos no backend ou OpenAPI. Use kotlinx.serialization conforme o projeto e mapeie DTOs para modelos de domínio quando necessário.
>
> Critérios de aceite: Todas as operações mapeadas ao contrato real; erros HTTP convertidos em resultado tipado; testes de serialização.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.6 — Tela de Login

**Prompt para Claude Code**

> Implemente a **Task 1.6 — Tela de Login** do Equilibra Mobile. Objetivo: Implementar login com email, senha, validação, loading e erros. Compose Material 3, campos acessíveis, mostrar/ocultar senha, teclado e IME apropriados, prevenção de duplo envio, mensagens pt-BR e navegação após autenticação bem-sucedida.
>
> Critérios de aceite: Validação local; loading e erros visíveis; token persistido pela camada de sessão; testes de ViewModel.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.7 — Tela de Registro

**Prompt para Claude Code**

> Implemente a **Task 1.7 — Tela de Registro** do Equilibra Mobile. Objetivo: Implementar cadastro com email e senha e regras de validação. Descubra regras de senha no backend; não imponha requisitos incompatíveis. Inclua confirmação de senha apenas se apropriado ao contrato/UI, estados de submissão, erros de campo e retorno ao login ou navegação autenticada.
>
> Critérios de aceite: Validações coerentes com backend; erros de conflito/validação tratados; testes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.8 — Splash e roteamento de autenticação

**Prompt para Claude Code**

> Implemente a **Task 1.8 — Splash e roteamento de autenticação** do Equilibra Mobile. Objetivo: Decidir o destino inicial a partir da sessão persistida e validar sessão quando aplicável. Evite mostrar telas protegidas antes de concluir a verificação; trate token ausente, válido, expirado e falha de rede de acordo com política segura. Não bloqueie indefinidamente a UI.
>
> Critérios de aceite: Roteamento determinístico para login/home; estados de carregamento e erro definidos; teste de navegação.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.9 — Logout

**Prompt para Claude Code**

> Implemente a **Task 1.9 — Logout** do Equilibra Mobile. Objetivo: Revogar sessão no backend e limpar dados locais de autenticação. Siga o contrato real de logout e refresh-cookie. Limpe sessão mesmo se a rede falhar, remova back stack protegido e não deixe o usuário retornar à home pelo botão voltar.
>
> Critérios de aceite: Logout idempotente; sessão local removida; falha de rede não mantém acesso local.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 1.10 — Navegação base

**Prompt para Claude Code**

> Implemente a **Task 1.10 — Navegação base** do Equilibra Mobile. Objetivo: Criar NavHost com splash, login, register e home. Use rotas tipadas ou constantes centralizadas conforme dependências; integre fluxo de auth e back stack; evite strings de rota duplicadas.
>
> Critérios de aceite: Todas as rotas navegáveis; back stack coerente; navegação protegida por estado de autenticação.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 2 — Dashboard
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 2.1 — BottomNavigation

**Prompt para Claude Code**

> Implemente a **Task 2.1 — BottomNavigation** do Equilibra Mobile. Objetivo: Criar navegação principal Dashboard, Transações, Cartões, Relatórios e Mais. Crie destinos e ícones Material apropriados; preserve estado das abas quando viável; implemente Mais como ponto de entrada para áreas ainda não entregues, sem links quebrados.
>
> Critérios de aceite: Seleção visual correta; navegação acessível; funciona em telas compactas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.2 — DashboardApi e DTOs

**Prompt para Claude Code**

> Implemente a **Task 2.2 — DashboardApi e DTOs** do Equilibra Mobile. Objetivo: Criar camada remota para resumo, contas, transações recentes, evolução, categorias e cartões. Inspecione contrato real de /dashboard; use modelos explícitos para cada seção e tratamento de campos opcionais/datas/valores.
>
> Critérios de aceite: Chamadas e DTOs aderem ao backend; parsing testado; erros padronizados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.3 — Cards de resumo financeiro

**Prompt para Claude Code**

> Implemente a **Task 2.3 — Cards de resumo financeiro** do Equilibra Mobile. Objetivo: Exibir receita, despesa, saldo líquido e patrimônio no dashboard. Carregue sempre da API; modele loading, erro, retry e conteúdo; formate moeda pt-BR e valores com precisão decimal sem conversão para Float/Double em cálculos financeiros.
>
> Critérios de aceite: Indicadores exibem valores da API e período aplicado; estados completos; acessibilidade.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.4 — Saldos por conta

**Prompt para Claude Code**

> Implemente a **Task 2.4 — Saldos por conta** do Equilibra Mobile. Objetivo: Exibir saldo por conta no dashboard. Use dados do endpoint dashboard, ordenação estável e formatação consistente; ao tocar em conta, navegue para detalhe/lista se já existir destino, sem criar fluxo fictício.
>
> Critérios de aceite: Lista correta, vazia e com erro; não dispara chamadas duplicadas desnecessárias.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.5 — Transações recentes

**Prompt para Claude Code**

> Implemente a **Task 2.5 — Transações recentes** do Equilibra Mobile. Objetivo: Exibir feed de transações recentes. Apresente tipo, descrição, data, conta/categoria disponíveis e valor com semântica visual; diferencie canceladas quando informadas.
>
> Critérios de aceite: Dados vêm da API; ordenação respeita backend; clique abre detalhe somente se implementado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.6 — Gráfico de evolução de saldo

**Prompt para Claude Code**

> Implemente a **Task 2.6 — Gráfico de evolução de saldo** do Equilibra Mobile. Objetivo: Implementar gráfico temporal usando MPAndroidChart. Verifique compatibilidade da biblioteca com Compose; encapsule via AndroidView se necessário. Respeite formato de dados real, estados vazios e legibilidade em telas pequenas.
>
> Critérios de aceite: Eixos/rótulos legíveis; sem dados não quebra; valores correspondem à API.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.7 — Gráficos por categoria

**Prompt para Claude Code**

> Implemente a **Task 2.7 — Gráficos por categoria** do Equilibra Mobile. Objetivo: Exibir despesas e receitas por categoria em barras/rosca. Use dados agregados da API; permita distinguir despesa/receita; inclua legenda, cores semânticas acessíveis e fallback quando não houver categorias.
>
> Critérios de aceite: Totais/legendas consistentes; gráfico adapta-se à largura; sem dados tratado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.8 — Resumo de cartão

**Prompt para Claude Code**

> Implemente a **Task 2.8 — Resumo de cartão** do Equilibra Mobile. Objetivo: Mostrar pendências, próximo vencimento e limite. Use campos reais retornados pelo dashboard; não calcule faturas se backend já fornece agregados. Trate ausência de cartões e faturas.
>
> Critérios de aceite: Resumo fiel à API e navegação para Cartões quando disponível.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.9 — Filtro de período

**Prompt para Claude Code**

> Implemente a **Task 2.9 — Filtro de período** do Equilibra Mobile. Objetivo: Adicionar mês atual, mês anterior e intervalo customizado. Defina datas no fuso esperado pelo backend; serialize ISO 8601; mantenha filtro aplicado às seções relevantes e previna intervalos inválidos.
>
> Critérios de aceite: Mudança de período atualiza dados; intervalo validado; estado exibido ao usuário.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 2.10 — Dashboard sem cache Room

**Prompt para Claude Code**

> Implemente a **Task 2.10 — Dashboard sem cache Room** do Equilibra Mobile. Objetivo: Garantir que dashboard sempre seja carregado da API. Revise implementação e documente a regra: não persistir agregados do dashboard no Room; exibir loading/erro/retry sem substituir por cache obsoleto.
>
> Critérios de aceite: Dashboard sempre consulta API e não depende de cache local.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 3 — Contas, Categorias & Tags
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 3.1 — Configurar Room

**Prompt para Claude Code**

> Implemente a **Task 3.1 — Configurar Room** do Equilibra Mobile. Objetivo: Criar banco equilibra.db, DAOs, módulos Hilt e migrações. Configure versão inicial, exportação de schemas e testes; migrações futuras devem ser explícitas, sem fallback destrutivo em produção.
>
> Critérios de aceite: Banco abre via DI; schema exportado; teste de criação e migração.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.2 — AccountEntity e AccountDao

**Prompt para Claude Code**

> Implemente a **Task 3.2 — AccountEntity e AccountDao** do Equilibra Mobile. Objetivo: Criar entidade e operações locais de conta. Use UUID como String; BigDecimal serializado como String; timestamps ISO/epoch conforme modelo; implemente consultas necessárias para lista e sincronização.
>
> Critérios de aceite: CRUD e getAll testados com Room in-memory; sem perda de precisão.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.3 — AssetAccountApi e DTOs

**Prompt para Claude Code**

> Implemente a **Task 3.3 — AssetAccountApi e DTOs** do Equilibra Mobile. Objetivo: Criar endpoints e DTOs de contas e reconciliação. Inspecione endpoints e contrato de reconcile; cubra listar, obter, criar, atualizar, ativar/desativar e reconciliar conforme API real.
>
> Critérios de aceite: DTOs aderentes ao backend e testes de serialização.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.4 — AccountRepository

**Prompt para Claude Code**

> Implemente a **Task 3.4 — AccountRepository** do Equilibra Mobile. Objetivo: Implementar API como fonte da verdade e sincronização Room. Leitura inicial deve emitir cache e depois atualizar com API; pull-to-refresh consulta API; escritas só atualizam Room após sucesso remoto; erros não apagam cache válido.
>
> Critérios de aceite: Fluxos online/offline e falhas cobertos por testes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.5 — Lista de contas

**Prompt para Claude Code**

> Implemente a **Task 3.5 — Lista de contas** do Equilibra Mobile. Objetivo: Criar tela que lê Room primeiro e sincroniza em background. Inclua pull-to-refresh, loading inicial, atualização silenciosa, vazio, erro recuperável, saldo formatado e status ativo/inativo.
>
> Critérios de aceite: Lista abre com cache e converge para API; estados e refresh funcionam.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.6 — Criar conta

**Prompt para Claude Code**

> Implemente a **Task 3.6 — Criar conta** do Equilibra Mobile. Objetivo: Formulário com nome, tipo e saldo inicial. Tipos suportados: CHECKING, SAVINGS, CASH, INVESTMENT, DIGITAL, OTHER; use labels pt-BR. Validar valor monetário, enviar API e atualizar Room somente em sucesso.
>
> Critérios de aceite: Criação funciona; erros do servidor aparecem; não há duplicação por toque repetido.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.7 — Editar e ativar/desativar conta

**Prompt para Claude Code**

> Implemente a **Task 3.7 — Editar e ativar/desativar conta** do Equilibra Mobile. Objetivo: Implementar edição e mudança de status. Reutilize formulário; confirme desativação se houver impacto; respeite restrições do backend e atualize cache após sucesso.
>
> Critérios de aceite: Editar/status refletem API e Room; falha preserva estado anterior.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.8 — Reconciliação de conta

**Prompt para Claude Code**

> Implemente a **Task 3.8 — Reconciliação de conta** do Equilibra Mobile. Objetivo: Criar tela com data/hora e saldo informado, exibindo diferença e ajuste. Consulte o contrato real; não crie lançamentos localmente se backend gera o ajuste. Mostrar saldo esperado, saldo informado e diferença, e confirmar antes de enviar.
>
> Critérios de aceite: Reconciliação enviada uma vez; valores e data corretos; resultado sincronizado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.9 — CategoryEntity, DAO e API

**Prompt para Claude Code**

> Implemente a **Task 3.9 — CategoryEntity, DAO e API** do Equilibra Mobile. Objetivo: Criar camada local/remota de categorias e DTOs. Aplicabilidade EXPENSE, INCOME, BOTH; estado ativo; IDs UUID. Verifique endpoints reais para CRUD e ativação.
>
> Critérios de aceite: DAO/API/DTOs implementados e testados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.10 — CategoryRepository e tela

**Prompt para Claude Code**

> Implemente a **Task 3.10 — CategoryRepository e tela** do Equilibra Mobile. Objetivo: Implementar sincronização e CRUD de categorias. Lista Room-first com refresh; formulário nome/aplicabilidade; editar e ativar/desativar; evite remover categorias usadas se backend não permitir.
>
> Critérios de aceite: CRUD funcional e cache consistente; estados de tela completos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.11 — TagEntity, DAO e API

**Prompt para Claude Code**

> Implemente a **Task 3.11 — TagEntity, DAO e API** do Equilibra Mobile. Objetivo: Criar persistência local e camada remota de tags. Implemente ID, nome, ativo e syncedAt; siga contrato real; índices/constraints conforme necessário.
>
> Critérios de aceite: Testes DAO e serialização; build funcional.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.12 — TagRepository e tela

**Prompt para Claude Code**

> Implemente a **Task 3.12 — TagRepository e tela** do Equilibra Mobile. Objetivo: Implementar sincronização e CRUD de tags. Tela de lista, criação, edição e ativação; Room-first e atualização pós-escrita.
>
> Critérios de aceite: CRUD funcional, erros tratados e refresh correto.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.13 — Seletor reutilizável de categoria/tag

**Prompt para Claude Code**

> Implemente a **Task 3.13 — Seletor reutilizável de categoria/tag** do Equilibra Mobile. Objetivo: Criar componentes Compose que consultam dados do Room. Permitir seleção única/múltipla conforme o formulário consumidor; filtrar categorias por aplicabilidade e apenas opções ativas; suportar loading/vazio.
>
> Critérios de aceite: Componente reutilizado sem duplicação; seleção retorna IDs estáveis.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 3.14 — Padrão de lista

**Prompt para Claude Code**

> Implemente a **Task 3.14 — Padrão de lista** do Equilibra Mobile. Objetivo: Padronizar pull-to-refresh, vazio e erro nas listas. Crie componentes compartilhados apenas onde reduz duplicação real; acessibilidade, retry e indicadores consistentes; aplique a contas, categorias e tags desta sprint.
>
> Critérios de aceite: Padrão consistente sem quebrar estado/scroll das telas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 4 — Transações
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 4.1 — TransactionEntity e DAO

**Prompt para Claude Code**

> Implemente a **Task 4.1 — TransactionEntity e DAO** do Equilibra Mobile. Objetivo: Criar cache das últimas ~100 transações e consultas por tipo. Inclua tipo, descrição, data, valor como String decimal, categoria, contas, notas, status e syncedAt; considere IDs e campos opcionais; não tente armazenar páginas ilimitadas.
>
> Critérios de aceite: Consultas recentes/tipo e operações locais testadas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.2 — ExpenseApi, IncomeApi e TransferApi

**Prompt para Claude Code**

> Implemente a **Task 4.2 — ExpenseApi, IncomeApi e TransferApi** do Equilibra Mobile. Objetivo: Criar serviços Retrofit e DTOs para despesas, receitas e transferências. Confirme payloads, paginação, filtros, cancelamento e enums no backend; não presuma que endpoints compartilham o mesmo DTO sem validar.
>
> Critérios de aceite: Serviços mapeiam o contrato real; testes de serialização e erros.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.3 — TransactionRepository

**Prompt para Claude Code**

> Implemente a **Task 4.3 — TransactionRepository** do Equilibra Mobile. Objetivo: Unificar acesso remoto e cache de até ~100 transações. API é fonte da verdade; cache para leitura rápida e lista recente; escritas remotas precedem atualização Room; histórico unificado continua sem cache.
>
> Critérios de aceite: Sincronização determinística e testes de sucesso/falha.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.4 — Formulário reutilizável de transação

**Prompt para Claude Code**

> Implemente a **Task 4.4 — Formulário reutilizável de transação** do Equilibra Mobile. Objetivo: Criar formulário base para descrição, valor, data, conta, categoria, tags e notas. Componha campos reutilizáveis com validação e estado hoisted; permita configuração de campos por tipo; use seletores da Sprint 3 e formatação monetária sem arredondamentos indevidos.
>
> Critérios de aceite: Formulário serve despesas e receitas; acessível e testável.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.5 — Lista de despesas

**Prompt para Claude Code**

> Implemente a **Task 4.5 — Lista de despesas** do Equilibra Mobile. Objetivo: Criar lista paginada com filtros e refresh. Use paginação real do backend; filtros por período, status, conta, categoria e tags quando suportados; não carregar todas as páginas de uma vez.
>
> Critérios de aceite: Paginação e filtros corretos; estados loading/vazio/erro/refresh.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.6 — Criar/editar despesa

**Prompt para Claude Code**

> Implemente a **Task 4.6 — Criar/editar despesa** do Equilibra Mobile. Objetivo: Implementar formulário e persistência remota de despesas. Suportar campos do contrato, edição quando API permitir e associação de tags; validar valor positivo e conta/categoria válidas.
>
> Critérios de aceite: Sucesso atualiza Room; falha não altera cache; testes de ViewModel.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.7 — Lista de receitas

**Prompt para Claude Code**

> Implemente a **Task 4.7 — Lista de receitas** do Equilibra Mobile. Objetivo: Criar lista paginada com filtros, incluindo canceladas. Respeite status e filtros reais; diferencie visualmente receita ativa/cancelada e implemente paginação.
>
> Critérios de aceite: Filtro de canceladas funciona conforme API; estados completos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.8 — Criar/editar receita

**Prompt para Claude Code**

> Implemente a **Task 4.8 — Criar/editar receita** do Equilibra Mobile. Objetivo: Implementar formulário de receita com atualização do Room após sucesso. Reutilize formulário base; categorias compatíveis com INCOME/BOTH; validar e mapear resposta da API.
>
> Critérios de aceite: Criar/editar funciona e tem testes de validação/erro.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.9 — Transferência entre contas

**Prompt para Claude Code**

> Implemente a **Task 4.9 — Transferência entre contas** do Equilibra Mobile. Objetivo: Criar tela para conta origem, destino, valor, data e descrição se suportada. Impedir origem igual ao destino; usar contrato real; não modelar transferência como despesa+receita local; atualizar cache após sucesso.
>
> Critérios de aceite: Validações e envio correto; saldo fica sob responsabilidade do backend.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.10 — Cancelar transação

**Prompt para Claude Code**

> Implemente a **Task 4.10 — Cancelar transação** do Equilibra Mobile. Objetivo: Adicionar cancelamento para despesa, receita e transferência. Use endpoints específicos e status real; confirmação para ação destrutiva; evitar duplo envio e sincronizar Room após sucesso.
>
> Critérios de aceite: Cancelamento refletido em listas/detalhes; erros não corrompem cache.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.11 — Histórico unificado

**Prompt para Claude Code**

> Implemente a **Task 4.11 — Histórico unificado** do Equilibra Mobile. Objetivo: Criar tela com /transactions e filtros combinados sem cache. Implemente paginação e filtros tipo/status/período/conta/categoria/tags; API sempre fonte da tela; serialização de parâmetros conforme contrato.
>
> Critérios de aceite: Filtros combinados e paginação corretos; sem leitura/escrita Room.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.12 — Detalhe da transação

**Prompt para Claude Code**

> Implemente a **Task 4.12 — Detalhe da transação** do Equilibra Mobile. Objetivo: Exibir dados completos, status e seção de anexos. Use navegação por ID e carregue detalhes reais; apresente ações disponíveis conforme tipo/status; conectar tela de anexos quando sprint 5 estiver pronta, com estado provisório claro se ainda não estiver.
>
> Critérios de aceite: Detalhes corretos; ações autorizadas; estados de erro/loading.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 4.13 — Formatação financeira pt-BR

**Prompt para Claude Code**

> Implemente a **Task 4.13 — Formatação financeira pt-BR** do Equilibra Mobile. Objetivo: Padronizar R$, datas dd/MM/yyyy e cores semânticas. Centralize formatadores; trate null, valores negativos e timezone; não dependa de cores sozinhas para indicar tipo e preserve acessibilidade em tema claro/escuro.
>
> Critérios de aceite: Componentes financeiros usam os mesmos formatadores; testes de formatação.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 5 — Anexos
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 5.1 — AttachmentApi

**Prompt para Claude Code**

> Implemente a **Task 5.1 — AttachmentApi** do Equilibra Mobile. Objetivo: Criar upload multipart, listar, baixar e excluir anexos. Inspecione contrato e tipos MIME/tamanho aceitos; use streaming para downloads quando possível e erros tipados; binários não passam por Room.
>
> Critérios de aceite: Chamadas testadas; multipart correto; arquivos grandes tratados com segurança.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 5.2 — Seletor de arquivos SAF

**Prompt para Claude Code**

> Implemente a **Task 5.2 — Seletor de arquivos SAF** do Equilibra Mobile. Objetivo: Permitir selecionar arquivo pelo Storage Access Framework. Use contratos Activity Result modernos; não solicite permissões amplas de armazenamento; valide tipo/tamanho e mantenha URI válida durante upload.
>
> Critérios de aceite: Seleciona documentos sem permissão desnecessária; cancelamento tratado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 5.3 — Tela de anexos

**Prompt para Claude Code**

> Implemente a **Task 5.3 — Tela de anexos** do Equilibra Mobile. Objetivo: Implementar upload, listagem e download via SAF. Associar anexos à transação; exibir progresso, erros e confirmação de exclusão; salvar download via seletor de destino do SAF.
>
> Critérios de aceite: Fluxo completo funcional; metadados atualizados; nenhum binário no Room.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 5.4 — Preview de imagem/PDF

**Prompt para Claude Code**

> Implemente a **Task 5.4 — Preview de imagem/PDF** do Equilibra Mobile. Objetivo: Adicionar visualização para formatos suportados. Use visualização segura, sem executar conteúdo; PDF/imagens com fallback para formato não suportado; considere tamanho e lifecycle.
>
> Critérios de aceite: Preview abre formatos suportados e trata falhas sem travar.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 5.5 — Garantir anexos fora do Room

**Prompt para Claude Code**

> Implemente a **Task 5.5 — Garantir anexos fora do Room** do Equilibra Mobile. Objetivo: Auditar e documentar que anexos binários nunca são armazenados localmente no Room. Revise entidades, DAOs e repositórios; somente metadados temporários de UI podem existir em memória; download fica no destino escolhido pelo usuário.
>
> Critérios de aceite: Sem coluna blob/arquivo em Room; política documentada e verificável.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 6 — Cartões e Compras
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 6.1 — CreditCardEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 6.1 — CreditCardEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camadas de dados de cartão. Use limite como decimal em String no Room; campos instituição, apelido, fechamento, vencimento, timezone e status; inspecione contrato real.
>
> Critérios de aceite: DAO/API/DTOs testados; valores precisos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.2 — CreditCardRepository

**Prompt para Claude Code**

> Implemente a **Task 6.2 — CreditCardRepository** do Equilibra Mobile. Objetivo: Implementar API-first e cache Room para cartões. Implemente cache-first para leitura e sincronização pós-escrita; calcule limite disponível somente se o contrato indicar os dados necessários, evitando fórmula divergente do backend.
>
> Critérios de aceite: Testes de sincronização e falhas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.3 — Lista de cartões

**Prompt para Claude Code**

> Implemente a **Task 6.3 — Lista de cartões** do Equilibra Mobile. Objetivo: Exibir limite total, disponível e status. Use dados do backend; cartões ativos/inativos claros; empty/loading/error/refresh; acessibilidade para limite utilizado/disponível.
>
> Critérios de aceite: Lista reflete API e atualiza cache.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.4 — Criar cartão

**Prompt para Claude Code**

> Implemente a **Task 6.4 — Criar cartão** do Equilibra Mobile. Objetivo: Formulário para nome, instituição, limite, fechamento, vencimento e timezone. Validar limite positivo, dias válidos segundo regras reais e timezone suportado; usar seletores adequados e evitar assumir fechamento antes do vencimento sem contrato.
>
> Critérios de aceite: Criação remota e validações; erros de backend apresentados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.5 — Editar e ativar/desativar cartão

**Prompt para Claude Code**

> Implemente a **Task 6.5 — Editar e ativar/desativar cartão** do Equilibra Mobile. Objetivo: Implementar gestão de cartão. Reutilize formulário e confirme desativação; preserve compras/faturas existentes; não apague dados ao desativar.
>
> Critérios de aceite: Edição/status sincronizados e testados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.6 — PurchaseEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 6.6 — PurchaseEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camada de compras de cartão. Modele compra parcelada com card ID, categoria, descrição, valor total, data, quantidade até 60, ciclo e status; confirme o formato real de parcelas na API.
>
> Critérios de aceite: Persistência e parsing testados; decimal sem perda.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.7 — PurchaseRepository

**Prompt para Claude Code**

> Implemente a **Task 6.7 — PurchaseRepository** do Equilibra Mobile. Objetivo: Implementar cache e sincronização de compras. API como fonte da verdade; sincronizar por cartão e após criar/cancelar; evitar atualizar compras localmente antes da confirmação.
>
> Critérios de aceite: Cache consistente e testes de falha.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.8 — Lista e cadastro de compras

**Prompt para Claude Code**

> Implemente a **Task 6.8 — Lista e cadastro de compras** do Equilibra Mobile. Objetivo: Exibir compras por cartão, criar compra parcelada de 1 a 60 vezes e cancelar. Validar parcelas 1–60; apresentar valor total e dados de ciclo conforme backend; não calcular regra de fatura independentemente do servidor.
>
> Critérios de aceite: Lista filtrada por cartão; criação/cancelamento sincronizados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 6.9 — Detalhe da compra

**Prompt para Claude Code**

> Implemente a **Task 6.9 — Detalhe da compra** do Equilibra Mobile. Objetivo: Exibir parcelas, valores e ciclo da compra. Use resposta real da API; identifique valor total e valores/ciclos de parcelas sem inferir campos ausentes; trate compra cancelada.
>
> Critérios de aceite: Detalhe acessível e consistente com backend.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 7 — Faturas e Pagamento
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 7.1 — StatementEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 7.1 — StatementEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camada de faturas. Inclua ciclo, vencimento, status, total, pago e pendente como valores decimais String; respeite enum/status reais da API.
>
> Critérios de aceite: DAO e DTOs testados; precisão preservada.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.2 — StatementRepository

**Prompt para Claude Code**

> Implemente a **Task 7.2 — StatementRepository** do Equilibra Mobile. Objetivo: Implementar API-first e cache Room de faturas. Sincronize após fechamento/pagamento e pull-to-refresh; cache para leitura; estados de fatura não devem ser inferidos pelo cliente.
>
> Critérios de aceite: Testes de sincronização e erros.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.3 — Tela de faturas

**Prompt para Claude Code**

> Implemente a **Task 7.3 — Tela de faturas** do Equilibra Mobile. Objetivo: Exibir fatura atual e histórico com leitura Room-first. Seleção de cartão; ordenação por ciclo/vencimento; status visíveis; atualizar via API em background.
>
> Critérios de aceite: Atual e histórico corretos; estados completos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.4 — Fechar fatura

**Prompt para Claude Code**

> Implemente a **Task 7.4 — Fechar fatura** do Equilibra Mobile. Objetivo: Implementar ação de fechamento manual via endpoint /close. Confirmar ação; evitar chamadas duplicadas; tratar fatura já fechada e sincronizar a resposta do servidor.
>
> Critérios de aceite: Fechamento refletido em cache/lista; erros claros.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.5 — Detalhe da fatura

**Prompt para Claude Code**

> Implemente a **Task 7.5 — Detalhe da fatura** do Equilibra Mobile. Objetivo: Exibir itens, total, pago, pendente e vencimento. Carregue itens conforme contrato; apresente valores e status; separar estado de carregamento e falha.
>
> Critérios de aceite: Detalhes conferem com API e valores agregados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.6 — Pagar fatura

**Prompt para Claude Code**

> Implemente a **Task 7.6 — Pagar fatura** do Equilibra Mobile. Objetivo: Permitir escolher conta, valor e data; enviar idempotency key. Gere chave única por intenção de pagamento e reutilize a mesma chave em retry da mesma operação; não gere uma chave nova em retry cego. Validar saldo apenas se backend fornecer fonte confiável, sem prometer garantia local.
>
> Critérios de aceite: Pagamento não duplica por retry; resultado sincroniza fatura e contas; testes de idempotência.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 7.7 — Atualizar resumo de cartão do dashboard

**Prompt para Claude Code**

> Implemente a **Task 7.7 — Atualizar resumo de cartão do dashboard** do Equilibra Mobile. Objetivo: Mostrar faturas pendentes, próximo vencimento e limite. Conectar ao contrato de dashboard; após pagamento/fechamento, atualizar dashboard por nova consulta, pois ele não usa Room.
>
> Critérios de aceite: Indicadores atualizados por API e estado de erro tratado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 8 — Compromissos e Recorrências
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 8.1 — CommitmentEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 8.1 — CommitmentEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camada de compromissos. Modele tipo, descrição, valor planejado, vencimento, conta, categoria, regra de recorrência, status e overdue; confirme campos no backend.
>
> Critérios de aceite: DAO/DTOs testados e datas consistentes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.2 — CommitmentRepository e indicadores

**Prompt para Claude Code**

> Implemente a **Task 8.2 — CommitmentRepository e indicadores** do Equilibra Mobile. Objetivo: Sincronizar compromissos e exibir atrasados, hoje, próximos e futuros. Use os indicadores da API quando disponíveis; defina limites temporais com timezone coerente; não duplicar contagem local.
>
> Critérios de aceite: Categorias temporais batem com backend; cache sincronizado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.3 — Lista de compromissos

**Prompt para Claude Code**

> Implemente a **Task 8.3 — Lista de compromissos** do Equilibra Mobile. Objetivo: Criar lista com filtros por tipo, status e período. Paginada se suportado; apresentar vencimento, valor, conta, categoria e status; filtros preservam seleção e atualizam resultados.
>
> Critérios de aceite: Filtros e estados funcionais.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.4 — Criar compromisso

**Prompt para Claude Code**

> Implemente a **Task 8.4 — Criar compromisso** do Equilibra Mobile. Objetivo: Formulário tipo, descrição, valor, vencimento, conta e categoria. Validar tipo EXPENSE/INCOME, valores e entidades ativas; usar categorias aplicáveis; mapear datas conforme contrato.
>
> Critérios de aceite: Criação API-first e testes de validação.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.5 — Liquidar ou cancelar compromisso

**Prompt para Claude Code**

> Implemente a **Task 8.5 — Liquidar ou cancelar compromisso** do Equilibra Mobile. Objetivo: Implementar liquidação com data/valor e cancelamento. Confirme ação e use endpoint real; não criar despesa/receita local separadamente se o backend realiza liquidação; sincronize o estado retornado.
>
> Critérios de aceite: Liquidação/cancelamento corretos sem duplicar lançamento.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.6 — Gerar compromissos em lote

**Prompt para Claude Code**

> Implemente a **Task 8.6 — Gerar compromissos em lote** do Equilibra Mobile. Objetivo: Executar geração de compromissos via /generate. Inspecione parâmetros e resposta; solicite confirmação, apresente quantidade/resultado e previna duplo envio; não invente idempotência que a API não oferece.
>
> Critérios de aceite: Geração reporta resultado real e atualiza cache.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.7 — RecurrenceEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 8.7 — RecurrenceEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camada de regras de recorrência. Tipo, descrição, valor, categoria, conta, frequência mensal/anual, início/fim, dia de vencimento e status; alinhar enums e datas à API.
>
> Critérios de aceite: Persistência e parsing testados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 8.8 — Repository e tela de recorrências

**Prompt para Claude Code**

> Implemente a **Task 8.8 — Repository e tela de recorrências** do Equilibra Mobile. Objetivo: Implementar criar, pausar, retomar, encerrar e cancelar regras. Diferencie ações de estado e respeite transições permitidas pelo backend; formulário e lista com status, frequência e datas.
>
> Critérios de aceite: Ações sincronizadas e estados inválidos tratados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 9 — Metas Financeiras
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 9.1 — GoalEntity, DAO, API e DTOs

**Prompt para Claude Code**

> Implemente a **Task 9.1 — GoalEntity, DAO, API e DTOs** do Equilibra Mobile. Objetivo: Criar camada de metas financeiras. Campos nome, descrição, valor alvo, data alvo, status, progresso, restante e percentual; decimais como String no Room.
>
> Critérios de aceite: DAO/API/DTOs testados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.2 — GoalRepository

**Prompt para Claude Code**

> Implemente a **Task 9.2 — GoalRepository** do Equilibra Mobile. Objetivo: Implementar cache e sincronização de metas. Atualizar após criar/contribuir/completar/arquivar; API é fonte da verdade para progresso.
>
> Critérios de aceite: Cache sincronizado e falhas testadas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.3 — Lista de metas

**Prompt para Claude Code**

> Implemente a **Task 9.3 — Lista de metas** do Equilibra Mobile. Objetivo: Exibir progresso, percentual e status. Room-first com atualização API; ordenar por status/data de forma previsível; incluir vazio/erro/refresh.
>
> Critérios de aceite: Lista representa corretamente os valores retornados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.4 — Criar meta

**Prompt para Claude Code**

> Implemente a **Task 9.4 — Criar meta** do Equilibra Mobile. Objetivo: Formulário com nome, descrição, valor e data alvo. Validar nome não vazio, valor positivo e data quando exigido; enviar à API e refletir resposta.
>
> Critérios de aceite: Criação funcional com testes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.5 — Registrar contribuição

**Prompt para Claude Code**

> Implemente a **Task 9.5 — Registrar contribuição** do Equilibra Mobile. Objetivo: Registrar valor, data e nota e atualizar Room. Inspecione endpoint e regra de contribuição; prevenir duplo toque; progresso calculado pelo backend.
>
> Critérios de aceite: Contribuição confirmada e dados atualizados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.6 — Completar ou arquivar meta

**Prompt para Claude Code**

> Implemente a **Task 9.6 — Completar ou arquivar meta** do Equilibra Mobile. Objetivo: Implementar ações de completar/arquivar. Exibir confirmação e habilitar ações conforme status; não assumir que completar altera valores além do contrato.
>
> Critérios de aceite: Transições válidas e cache atualizado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 9.7 — Barra de progresso

**Prompt para Claude Code**

> Implemente a **Task 9.7 — Barra de progresso** do Equilibra Mobile. Objetivo: Criar indicador visual percentual. Use percentual retornado pelo backend; limitar apenas o desenho visual ao intervalo permitido sem alterar valor persistido; incluir texto acessível.
>
> Critérios de aceite: Progresso legível, acessível e correto.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 10 — Orçamentos e Fluxo de Caixa
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 10.1 — BudgetEntity, DAO, APIs e DTOs

**Prompt para Claude Code**

> Implemente a **Task 10.1 — BudgetEntity, DAO, APIs e DTOs** do Equilibra Mobile. Objetivo: Criar camada de orçamentos e resumo. Orçamento por categoria/mês; mês yyyy-MM; valor decimal String; status ON_TRACK/WARNING/EXCEEDED conforme API.
>
> Critérios de aceite: Camadas testadas e contrato conferido.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.2 — BudgetRepository

**Prompt para Claude Code**

> Implemente a **Task 10.2 — BudgetRepository** do Equilibra Mobile. Objetivo: Implementar API-first e cache de orçamentos. Sincronizar após criar/editar e refresh; separar orçamento salvo do resumo agregado quando os endpoints diferirem.
>
> Critérios de aceite: Cache e resultados coerentes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.3 — Lista de orçamentos por mês

**Prompt para Claude Code**

> Implemente a **Task 10.3 — Lista de orçamentos por mês** do Equilibra Mobile. Objetivo: Exibir orçamentos do mês selecionado usando Room primeiro. Permitir navegar meses; mostrar categoria e valor planejado; tratar mês sem orçamento.
>
> Critérios de aceite: Mudança de mês busca/sincroniza dados corretos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.4 — Criar/editar orçamento

**Prompt para Claude Code**

> Implemente a **Task 10.4 — Criar/editar orçamento** do Equilibra Mobile. Objetivo: Implementar CRUD por categoria/mês. Validar categoria elegível, mês e valor; prevenir duplicidade conforme regra real do backend.
>
> Critérios de aceite: Operações sincronizadas e validações cobertas.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.5 — Resumo de orçamento

**Prompt para Claude Code**

> Implemente a **Task 10.5 — Resumo de orçamento** do Equilibra Mobile. Objetivo: Mostrar planejado, realizado, percentual consumido e status. Use endpoint de resumo como fonte; não inventar limiares WARNING/EXCEEDED se backend não os documentar; representar estados visual e textualmente.
>
> Critérios de aceite: Valores e status iguais à API; estados sem dados tratados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.6 — CashFlowApi e tela de projeção

**Prompt para Claude Code**

> Implemente a **Task 10.6 — CashFlowApi e tela de projeção** do Equilibra Mobile. Objetivo: Exibir fluxo projetado: abertura, eventos, projeções, fechamento e saldo mínimo. Dados são read-only e sempre da API; identifique claramente projeção vs realizado; use datas/valores fornecidos pelo backend.
>
> Critérios de aceite: Tela atualiza pela API e não permite edição acidental.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 10.7 — Gráfico de projeção diária

**Prompt para Claude Code**

> Implemente a **Task 10.7 — Gráfico de projeção diária** do Equilibra Mobile. Objetivo: Adicionar gráfico temporal para fluxo de caixa. Utilize pontos retornados pela API, escalas legíveis e tooltip/resumo acessível; diferencie saldo previsto e eventos sem criar projeções client-side.
>
> Critérios de aceite: Gráfico bate com dados da API e funciona sem pontos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 11 — Relatórios e Exportação
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 11.1 — ReportApi e DTOs

**Prompt para Claude Code**

> Implemente a **Task 11.1 — ReportApi e DTOs** do Equilibra Mobile. Objetivo: Criar API de relatórios financeiro, categoria, auditoria e exportação. Confirme parâmetros de filtros, paginação, content types e respostas binárias; trate streams e erros sem carregar arquivos grandes desnecessariamente em memória.
>
> Critérios de aceite: Chamadas aderentes ao contrato; DTOs/headers testados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 11.2 — Relatório financeiro

**Prompt para Claude Code**

> Implemente a **Task 11.2 — Relatório financeiro** do Equilibra Mobile. Objetivo: Exibir abertura, totais, transferências, fechamento e detalhes paginados. Sempre consultar API; filtros de período e paginação; formatação pt-BR; destacar total sem alterar precisão.
>
> Critérios de aceite: Relatório consistente e paginação funcional.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 11.3 — Relatório por categoria

**Prompt para Claude Code**

> Implemente a **Task 11.3 — Relatório por categoria** do Equilibra Mobile. Objetivo: Agrupar totais por categoria. Usar agregados da API; apresentar tipo/período e totais; tratar categorias sem nome e ausência de dados.
>
> Critérios de aceite: Totais e agrupamentos conferem com resposta.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 11.4 — Auditoria

**Prompt para Claude Code**

> Implemente a **Task 11.4 — Auditoria** do Equilibra Mobile. Objetivo: Criar lista paginada com filtros tipo, status, conta, categoria e tags. Sempre da API; incluir anexos/tags apenas se o contrato fornecer; filtros combinados e carregamento incremental.
>
> Critérios de aceite: Paginação e filtros funcionam; sem cache Room.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 11.5 — Exportar PDF e CSV

**Prompt para Claude Code**

> Implemente a **Task 11.5 — Exportar PDF e CSV** do Equilibra Mobile. Objetivo: Permitir salvar/compartilhar exportações via SAF. Baixar conteúdo da API com MIME e nome de arquivo seguros; usar SAF para salvar e Android Sharesheet para compartilhar; não armazenar em Room.
>
> Critérios de aceite: PDF/CSV abrem quando válidos; cancelamento/erro tratados.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 11.6 — Relatórios sempre frescos

**Prompt para Claude Code**

> Implemente a **Task 11.6 — Relatórios sempre frescos** do Equilibra Mobile. Objetivo: Auditar ausência de cache Room para relatórios. Verifique repositórios e ViewModels; cada consulta de relatório deve buscar API e apresentar loading/erro/retry. Documente a regra.
>
> Critérios de aceite: Nenhum relatório depende de cache local.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


# Sprint 12 — Polish & Qualidade
Execute as tasks abaixo em sequência, validando cada entrega antes de iniciar a seguinte.

## Task 12.1 — Revisar sincronização Room ↔ API

**Prompt para Claude Code**

> Implemente a **Task 12.1 — Revisar sincronização Room ↔ API** do Equilibra Mobile. Objetivo: Auditar todas as telas com cache. Monte checklist por domínio; confirme cache-first, refresh, pós-escrita, comportamento de erro e limites de dados; corrija inconsistências com testes.
>
> Critérios de aceite: Todos os domínios cacheados seguem política documentada.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.2 — Pull-to-refresh global

**Prompt para Claude Code**

> Implemente a **Task 12.2 — Pull-to-refresh global** do Equilibra Mobile. Objetivo: Adicionar refresh a todas as listas elegíveis. Use API em todas as listas com fonte remota; Dashboard, relatórios e anexos seguem suas regras específicas; evite refresh duplicado e preserve scroll quando possível.
>
> Critérios de aceite: Todas as listas elegíveis têm refresh consistente.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.3 — Tema claro/escuro

**Prompt para Claude Code**

> Implemente a **Task 12.3 — Tema claro/escuro** do Equilibra Mobile. Objetivo: Implementar Material 3 dynamic color com fallback. Suportar Android 26+ com fallback coerente; respeitar configuração do sistema e garantir contraste, cores semânticas e gráficos legíveis.
>
> Critérios de aceite: Tema aplicado em todas as telas e componentes.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.4 — Localização pt-BR

**Prompt para Claude Code**

> Implemente a **Task 12.4 — Localização pt-BR** do Equilibra Mobile. Objetivo: Centralizar textos, moeda, datas e formatos. Mover strings para recursos; usar locale pt-BR para R$ e datas; datas de API continuam ISO internamente; não traduzir valores/enums de rede.
>
> Critérios de aceite: Sem textos hardcoded críticos; testes de formatação.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.5 — Tratamento global de erros

**Prompt para Claude Code**

> Implemente a **Task 12.5 — Tratamento global de erros** do Equilibra Mobile. Objetivo: Padronizar erros de rede, validação, 401 e 403. 401 após refresh falho encerra sessão e remove back stack protegido; 403 informa falta de permissão sem loop de logout; distinguir offline, timeout e validação.
>
> Critérios de aceite: Tratamento consistente, sem tokens em logs; testes por cenário.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.6 — Testes unitários

**Prompt para Claude Code**

> Implemente a **Task 12.6 — Testes unitários** do Equilibra Mobile. Objetivo: Cobrir ViewModels, UseCases e Repositories com Room in-memory. Use fakes/mocks e dispatcher de coroutines controlável; priorize regras financeiras, refresh, falhas e pós-escrita; não faça testes dependentes de servidor real.
>
> Critérios de aceite: Suíte roda no CI/local; casos principais e limites cobertos.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.7 — Testes de instrumentação

**Prompt para Claude Code**

> Implemente a **Task 12.7 — Testes de instrumentação** do Equilibra Mobile. Objetivo: Testar login, criação de transação e dashboard. Use Compose UI testing; configure ambiente fake/testável para API; teste estados de carregamento, validação, navegação e erros, sem credenciais reais.
>
> Critérios de aceite: Testes críticos automatizados e repetíveis.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


## Task 12.8 — Build release e distribuição

**Prompt para Claude Code**

> Implemente a **Task 12.8 — Build release e distribuição** do Equilibra Mobile. Objetivo: Preparar build de release, assinatura e distribuição interna. Verifique build variant, minification/R8, manifest, permissões e versionamento; nunca inclua keystore/senhas no repositório; use variáveis/segredos locais e documente passos de assinatura e Play Console interna.
>
> Critérios de aceite: Release build validado; instruções de distribuição seguras; nenhum segredo versionado.
>
> Siga todas as instruções globais deste documento. Inspecione o código antes de editar, implemente somente este escopo, adicione/ajuste testes, execute as verificações disponíveis e informe honestamente resultados, arquivos alterados e pendências. Não avance para outra task.


---

## Observações de integração identificadas no planejamento

- **Refresh JWT:** o plano menciona refresh por cookie e também inclui `refreshToken` entre os dados de sessão. O Claude Code deve confirmar o contrato real do backend antes de decidir onde e como persistir/enviar o refresh token.
- **DataStore encrypted:** DataStore Preferences não deve ser tratado como automaticamente criptografado. A implementação precisa usar uma solução de armazenamento de segredos compatível com a versão atual do Android e com o projeto.
- **MPAndroidChart:** confirmar compatibilidade com Compose; encapsular em `AndroidView` se necessário, sem introduzir dependências incompatíveis.
- **Cartões e finanças:** o backend deve ser autoridade para ciclos de fatura, parcelas, limites disponíveis, pagamentos e status. O app exibe e envia operações, mas não deve duplicar regras financeiras no cliente sem contrato explícito.
- **Room:** o planejamento define cache de leitura e atualização após sucesso remoto; não implementar escrita offline otimista ou fila de sincronização sem uma decisão adicional de produto.
- **Contagem de tasks:** o documento contém também tasks de decisão/auditoria (2.10, 5.5, 11.6), que devem resultar em verificação e documentação, não em funcionalidade fictícia.
