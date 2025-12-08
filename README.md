
# 📚 VanDerLer – Backend

> API REST em **Java 21 + Spring Boot** para a plataforma de biblioteca digital **VanDerLer**.  
> Responsável por autenticação (JWT / WebAuthn), gestão de usuários, livros e integração com reconhecimento facial via Azure.

---

## 🧭 Sumário

- [Visão geral](#-visão-geral)
- [Arquitetura da solução](#-arquitetura-da-solução)
- [Stack tecnológica](#-stack-tecnológica)
- [Estrutura de pacotes](#-estrutura-de-pacotes)
- [Requisitos](#-requisitos)
- [Configuração de ambiente](#-configuração-de-ambiente)
  - [Banco de dados](#banco-de-dados)
  - [JWT](#jwt-configuração)
  - [Azure Face API](#azure-face-api)
- [Rodando o projeto](#-rodando-o-projeto)
- [Segurança e autenticação](#-segurança-e-autenticação)
  - [Fluxo JWT](#fluxo-jwt)
  - [WebAuthn](#webauthn)
- [Reconhecimento facial – comportamento da validação](#-reconhecimento-facial--comportamento-da-validação)
- [Endpoints principais (resumo)](#-endpoints-principais-resumo)
- [Boas práticas de desenvolvimento](#-boas-práticas-de-desenvolvimento)
- [Roadmap](#-roadmap)
- [Licença](#-licença)

---

## 🔍 Visão geral

O **VanDerLer Backend** é a API que sustenta a aplicação de biblioteca digital **VanDerLer**, consumida pelo frontend em React + Vite (**https://vanderler.netlify.app/**).

Principais responsabilidades:

- Cadastro, autenticação e gestão de usuários;
- Emissão e validação de **tokens JWT**;
- Suporte a **WebAuthn** (credenciais sem senha);
- Cadastro, listagem e associação de **livros** a usuários;
- Integração com a **Azure Face API** para reconhecimento facial (modo real ou simulado);
- Exposição de endpoints REST para serem consumidos pelo frontend.

---

## 🧱 Arquitetura da solução

Arquitetura em camadas, organizada por pacotes:

- **controller** → expõe a API REST (camada HTTP);
- **service** → regra de negócio;
- **repository** → acesso ao banco (Spring Data JPA);
- **model** → entidades JPA mapeando as tabelas;
- **dto** → objetos de transferência de dados (requests/responses);
- **security** → configuração de autenticação/autorização (JWT, `UserDetails`, filtros);
- **config** → configurações globais de segurança (CORS, HTTP, etc.).

Fluxo clássico:

> Frontend → Controller → Service → Repository → Banco de Dados  
> Frontend → Controller → Service (AzureFaceService / WebAuthnService) → Serviço externo → Decisão de negócio

---

## 🧰 Stack tecnológica

- ☕ **Java 21**
- 🌱 **Spring Boot**
- 🌐 **Spring Web** (REST)
- 🗄 **Spring Data JPA**
- 🔐 **Spring Security + JWT**
- 🔐 **WebAuthn** (credenciais FIDO2/sem senha)
- 🐬 **MySQL** (ou compatível)
- ☁️ Integração com **Azure Face API**
- 📦 **Maven** (build e dependências)

---

## 🗂 Estrutura de pacotes

De acordo com o projeto:

```text
src/main/java
└─ com.vanderler.vanderler_backend
   ├─ VanderlerBackendApplication.java
   ├─ config
   │  └─ SecurityConfig.java
   ├─ controller
   │  ├─ AuthController.java
   │  ├─ BookController.java
   │  ├─ FaceController.java
   │  ├─ LibraryController.java
   │  ├─ UserController.java
   │  └─ WebAuthnController.java
   ├─ dto
   │  ├─ AuthDtos.java
   │  ├─ BookDtos.java
   │  ├─ FaceImageDTO.java
   │  └─ UpdateNameRequest.java
   ├─ model
   │  ├─ Book.java
   │  ├─ Role.java
   │  ├─ User.java
   │  ├─ UserBook.java
   │  └─ WebAuthnCredential.java
   ├─ repository
   │  ├─ BookRepository.java
   │  ├─ UserBookRepository.java
   │  ├─ UserRepository.java
   │  └─ WebAuthnCredentialRepository.java
   ├─ security
   │  ├─ CustomUserDetails.java
   │  ├─ CustomUserDetailsService.java
   │  ├─ JwtAuthenticationFilter.java
   │  └─ JwtService.java
   └─ service
      ├─ AzureFaceService.java
      ├─ BookService.java
      ├─ UserService.java
      └─ WebAuthnService.java

src/main/resources
└─ application.properties
```

---

## 💻 Requisitos

- **Java 21**
- **Maven 3+**
- Banco **MySQL** acessível
- (Opcional, mas recomendado) Key válida da **Azure Face API** para validação facial real

---

## ⚙️ Configuração de ambiente

O projeto está preparado para ler as configurações sensíveis via **variáveis de ambiente**, usando placeholders no `application.properties`.

Arquivo `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.open-in-view=false

# JWT
vanderler.jwt.secret=${JWT_SECRET}
vanderler.jwt.expiration=86400000

# Azure Face
azure.face.endpoint=${AZURE_FACE_ENDPOINT}
azure.face.key=${AZURE_FACE_KEY}
azure.face.region=${AZURE_FACE_REGION}
azure.face.mock=true
```

### Variáveis de ambiente esperadas

- `DB_URL` → URL JDBC do banco (ex.: `jdbc:mysql://localhost:3306/vanderler?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`)
- `DB_USERNAME` → usuário do banco
- `DB_PASSWORD` → senha do banco
- `JWT_SECRET` → segredo usado para assinar tokens JWT
- `AZURE_FACE_ENDPOINT` → endpoint da Azure Face API
- `AZURE_FACE_KEY` → key da Azure Face API
- `AZURE_FACE_REGION` → região da Azure (ex.: `brazilsouth`, `eastus`, etc.)

> Em ambiente local você pode exportar essas variáveis no terminal, usar um `.env` no IntelliJ/Eclipse/VSCode ou configurar direto no perfil de execução.

---

### 🔐 JWT (configuração)

Trecho relevante já mostrado acima:

```properties
vanderler.jwt.secret=${JWT_SECRET}
vanderler.jwt.expiration=86400000
```

- `vanderler.jwt.secret` → segredo usado em `JwtService` para assinar/verificar tokens;
- `vanderler.jwt.expiration` → tempo de expiração em milissegundos (ex.: `86400000` = 24h).

---

### ☁️ Azure Face API

Configuração:

```properties
azure.face.endpoint=${AZURE_FACE_ENDPOINT}
azure.face.key=${AZURE_FACE_KEY}
azure.face.region=${AZURE_FACE_REGION}
azure.face.mock=true
```

- `azure.face.endpoint` → URL base do serviço de Face;
- `azure.face.key` → chave de autenticação;
- `azure.face.region` → região do recurso;
- `azure.face.mock`:
  - `true` → força o **modo simulação** (o back aceita qualquer foto sem chamar a Azure de verdade, ideal para demo/TCC);
  - `false` → tenta validar de fato na Azure (e cai no fallback descrito abaixo se a Azure não responder direito).

---

## ▶️ Rodando o projeto

### 1. Clonar o repositório

```bash
git clone https://github.com/VanDerLer/Backend.git
cd Backend
```

*(ajuste para a URL real do repositório)*

### 2. Definir variáveis de ambiente

Exemplo em ambiente local (Linux/macOS, adaptando para Windows PowerShell/CMD se necessário):

```bash
export DB_URL="jdbc:mysql://localhost:3306/vanderler?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC"
export DB_USERNAME="root"
export DB_PASSWORD="sua_senha"
export JWT_SECRET="uma_chave_secreta_bem_grande"
export AZURE_FACE_ENDPOINT="https://SEU_ENDPOINT.cognitiveservices.azure.com/"
export AZURE_FACE_KEY="SUA_KEY"
export AZURE_FACE_REGION="brazilsouth"
```

Se quiser rodar em **modo totalmente simulado** para o rosto, basta manter `azure.face.mock=true` no `application.properties`.

### 3. Rodar com Maven

```bash
mvn spring-boot:run
```

Por padrão, a API sobe em:

> http://localhost:8080

---

## 🔐 Segurança e autenticação

### Fluxo JWT

Baseado em:

- `JwtService` → geração e validação do token;
- `JwtAuthenticationFilter` → extrai o token do header `Authorization: Bearer <token>` e autentica o usuário;
- `CustomUserDetails` / `CustomUserDetailsService` → carregam o usuário e suas roles.

Fluxo:

1. `AuthController` recebe as credenciais;
2. Validamos usuário/senha;
3. Se ok, geramos o JWT com `JwtService` usando `JWT_SECRET`;
4. O cliente usa o token em chamadas subsequentes.

### WebAuthn

O projeto possui suporte a WebAuthn:

- Modelo: `WebAuthnCredential`;
- Repositório: `WebAuthnCredentialRepository`;
- Serviço: `WebAuthnService`;
- Controller: `WebAuthnController`.

Ele gerencia o fluxo de registro/autenticação de credenciais FIDO2, permitindo login mais seguro do que apenas e-mail/senha (dependendo de como o frontend integra).

---

## 🧬 Reconhecimento facial – comportamento da validação

A validação facial é centralizada em `AzureFaceService` e exposta via `FaceController`.

Comportamento geral:

1. O frontend envia uma imagem (`FaceImageDTO`) para o endpoint de face;
2. `FaceController` chama `AzureFaceService`;
3. O serviço decide como validar:

### 1. Modo simulação explícito (`azure.face.mock=true`)

- O sistema **não tenta** chamar a Azure;
- Qualquer foto enviada é tratada como válida;
- Útil para:
  - período pós-expiração do teste gratuito;
  - demonstrações;
  - ambiente acadêmico onde não se quer depender da Azure.

### 2. Modo real (`azure.face.mock=false`)

- O serviço tenta chamar a Azure Face API usando:
  - `AZURE_FACE_ENDPOINT`
  - `AZURE_FACE_KEY`
  - `AZURE_FACE_REGION`
- Se a Azure responder normalmente, a decisão de **match**/não-match é tomada com base na resposta.

#### Fallback automático

Mesmo com `azure.face.mock=false`, se a Azure:

- não responder;
- retornar erro;
- retornar resposta vazia ou não utilizável;

o backend entende que o serviço externo **não está funcional** e:

- entra em modo de **simulação automática**;
- **aceita toda foto** para não travar o fluxo da aplicação.

> Em resumo:  
> - `azure.face.mock=true` → simulação sempre.  
> - `azure.face.mock=false` → tenta validar de verdade, mas se a Azure falhar, cai em simulação.

---

## 📡 Endpoints principais (resumo)

### Auth (`AuthController`)

- `POST /auth/register` – cadastro de usuário;
- `POST /auth/login` – autenticação e emissão de token JWT.

### Usuário (`UserController`)

- `GET /users/me` – dados do usuário autenticado;
- `PUT /users/me` – atualização de dados básicos (ex.: nome).

### Livros / Biblioteca (`BookController` / `LibraryController`)

- `GET /books` – lista livros;
- `GET /books/{id}` – detalhes de um livro;
- `POST /books` – cria livro (restrito, conforme regra de auth);
- `POST /books/{id}/associate` – associa livro ao usuário (`UserBook`);
- `GET /library/my-books` – livros do usuário logado.

### Reconhecimento facial (`FaceController`)

- `POST /face/register` – registra/associa face a um usuário;
- `POST /face/verify` – verifica se a face enviada bate com o usuário (real ou simulado, conforme explicado).

### WebAuthn (`WebAuthnController`)

- Endpoints para:
  - iniciar registro da credencial;
  - finalizar registro;
  - iniciar autenticação;
  - finalizar autenticação.

---

## 🧹 Boas práticas de desenvolvimento

- Manter Entities e DTOs separados;
- Validar inputs com Bean Validation (`@Valid`, `@NotNull`, etc.);
- Centralizar erro em `@ControllerAdvice`;
- Usar logs claros no `AzureFaceService` e no fluxo de autenticação;
- Nunca commitar secrets (DB_PASSWORD, JWT_SECRET, Azure keys).

---

## 🛣 Roadmap

- [ ] Swagger / OpenAPI documentando todos os endpoints;
- [ ] Paginação, ordenação e filtros em `/books`;
- [ ] Entidade de categorias/gêneros;
- [ ] Histórico de leitura e estatísticas;
- [ ] Métricas com Spring Actuator/Prometheus;
- [ ] Políticas mais avançadas de roles/permissões.

---

> _“Ler transforma. Codar também.”_ 💜  
> _VanDerLer – uma biblioteca que cabe no seu navegador._
