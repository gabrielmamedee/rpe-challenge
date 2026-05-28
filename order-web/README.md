# order-web

Frontend da plataforma de gestão de ordens de pagamento, desenvolvido como parte do teste técnico RPE para a vaga de Desenvolvedor Fullstack Pleno.

Consome a API do `order-service` e oferece uma interface para autenticação de usuários, criação e acompanhamento de ordens de pagamento, com suporte a múltiplos meios de pagamento (PIX, Crédito e Débito).

---

## Recursos implementados

### Autenticação JWT

O fluxo de autenticação utiliza tokens JWT emitidos pelo `order-service`.

- Após o login, o token é armazenado no `localStorage` via `AuthContext` (`src/contexts/AuthContext.tsx`).
- A cada navegação, o contexto decodifica o token com `jwt-decode` e verifica a expiração (`exp`). Se expirado ou ausente, o usuário é redirecionado para `/login`.
- O `api.ts` (`src/services/api.ts`) configura um interceptor no Axios que injeta o header `Authorization: Bearer <token>` em todas as requisições autenticadas automaticamente, e trata respostas `401` redirecionando para o login sem expor o erro ao usuário.
- O logout limpa o token do `localStorage` e desfaz o estado do contexto, forçando nova autenticação.

### Idempotência na criação de ordens

Para evitar que o usuário crie ordens duplicadas ao clicar mais de uma vez ou ao retentar após uma falha de rede, cada tentativa de criação de ordem é protegida por um `Idempotency-Key`.

- A chave é gerada como UUID v4 **uma única vez** quando o formulário é montado (`useState(() => uuidv4())`), e não a cada clique.
- Ela é enviada no header `Idempotency-Key` da requisição para o `order-service`.
- Se o backend retornar `409 Conflict` (chave já processada), a interface exibe um aviso claro sem criar nova ordem.
- A chave só é renovada após uma criação bem-sucedida, garantindo que retentativas da mesma tentativa usem sempre a mesma chave.

### Polling automático para PIX

Pagamentos via PIX são processados de forma assíncrona pelo `order-service`. O status inicial retornado é `PENDENTE_PAGAMENTO`, e a confirmação ocorre em background.

- Assim que uma ordem PIX é criada, um `setInterval` de 5 segundos é iniciado no `HomePage`.
- A cada ciclo, o endpoint de listagem é consultado com o CPF do comprador e o resultado é filtrado pelo ID da ordem.
- O card exibe o status em tempo real até a resolução do pagamento — com estados visuais distintos para cada desfecho: spinner amarelo enquanto pendente, verde para `PAGO`, vermelho para `REPROVADO` e cinza para `CANCELADO`.
- Quando o status é resolvido, o polling é interrompido automaticamente.

---

## O que é possível fazer no frontend

| Funcionalidade | Descrição |
|---|---|
| **Login** | Autenticar com usuário e senha. A tela suporta alternar entre login e criação de conta sem navegar para outra rota. |
| **Criar conta** | Cadastrar novo usuário diretamente na tela de login, com validação de confirmação de senha. Após o cadastro, o login é realizado automaticamente. |
| **Cadastrar usuário (autenticado)** | Usuários autenticados podem acessar `/register` para criar outras contas e visualizar os dados do usuário criado. |
| **Criar ordem** | Preencher os dados de uma nova ordem (item, valor, comprador, CPF, meio de pagamento) e submetê-la à API. |
| **Selecionar meio de pagamento** | A lista de meios disponíveis (PIX, Crédito, Débito) é carregada dinamicamente do `order-service`. |
| **Gerar ID do item** | Botão para gerar automaticamente um UUID v4 válido para o campo "ID do Item". |
| **Acompanhar status PIX** | O card da ordem exibe o status em tempo real via polling até a resolução do pagamento. |
| **Consultar ordens por CPF** | Buscar e listar todas as ordens associadas a um CPF. A tabela mantém polling ativo enquanto houver ordens pendentes. |
| **Logout** | Encerrar a sessão e limpar o token armazenado. |

---

## Primeiros passos

Após subir os serviços com `docker compose up --build`, siga o fluxo abaixo:

**1. Crie sua conta**

Acesse `http://localhost:3000` e clique em **"Criar agora"** na tela de login. Preencha um usuário e senha (mínimo 8 caracteres) e confirme a senha. O login é realizado automaticamente após o cadastro.

**2. Explore a plataforma**

Após autenticar, você estará na página principal onde é possível:
- Criar ordens de pagamento
- Acompanhar o status de pagamentos PIX em tempo real
- Consultar ordens por CPF do comprador

**3. Criando uma ordem**

- Preencha os dados da ordem (ID do item pode ser gerado pelo botão **"Gerar"**)
- Selecione o meio de pagamento: **PIX**, **Crédito** ou **Débito**
- Clique em **"Criar Ordem"**

Para **Débito** e **Crédito**, o pagamento é confirmado imediatamente.
Para **PIX**, o card exibirá "Validando PIX..." e atualizará automaticamente até a resolução.

---

## Stack

| Tecnologia | Uso |
|---|---|
| React 19 + TypeScript | Interface e tipagem |
| Vite | Build e dev server |
| Tailwind CSS v4 | Estilização |
| Axios | Comunicação HTTP com o `order-service` |
| React Router v7 | Roteamento client-side |
| jwt-decode | Decodificação e validação do token JWT |
| uuid | Geração do `Idempotency-Key` |

---

## Executando localmente

**1. Instale as dependências:**

```bash
cd order-web
npm install
```

**2. Configure as variáveis de ambiente:**

```bash
cp .env.example .env
```

```env
VITE_API_URL=http://localhost:8080
```

**3. Inicie o servidor de desenvolvimento:**

```bash
npm run dev
```

A aplicação estará disponível em `http://localhost:5173`.

> O `order-service` precisa estar rodando em `http://localhost:8080`.

---

## Executando com Docker Compose

Na raiz do monorepo, suba todos os serviços:

```bash
docker compose up --build
```

| Serviço | URL |
|---|---|
| order-web | http://localhost:3000 |
| order-service | http://localhost:8080 |
| payment-processor | http://localhost:8081 |

```bash
docker compose down
```

---

## Estrutura do projeto

```
src/
├── components/
│   ├── layout/
│   │   └── Header.tsx           # Cabeçalho com navegação e logout
│   └── ui/                      # Componentes reutilizáveis (Button, Input, Badge, etc.)
├── contexts/
│   └── AuthContext.tsx          # Gerenciamento de JWT e estado de autenticação
├── pages/
│   ├── Home/
│   │   ├── HomePage.tsx         # Página principal com polling do card PIX
│   │   ├── OrderForm.tsx        # Formulário de criação de ordem com idempotência
│   │   └── OrderList.tsx        # Tabela de ordens com polling automático
│   ├── Login/
│   │   └── LoginPage.tsx        # Login e criação de conta na mesma tela
│   └── Register/
│       └── RegisterPage.tsx     # Cadastro de usuário (rota protegida)
├── routes/
│   └── index.tsx                # Rotas públicas e privadas
├── services/
│   ├── api.ts                   # Axios com interceptors de auth e 401
│   ├── auth.service.ts          # Login e registro
│   ├── orders.service.ts        # Criação e listagem de ordens
│   └── payment.service.ts       # Listagem de meios de pagamento
└── types/
    └── index.ts                 # Interfaces e tipos TypeScript
```
