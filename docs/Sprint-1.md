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