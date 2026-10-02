# Plano Financeiro

Frontend servido pelo Spring Boot e API REST com Spring Security e MySQL.

## Requisitos

- Java 21 ou superior
- MySQL 8 para persistência normal

## Demo local

Execute na raiz do projeto para experimentar sem instalar MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:8080`. O perfil `demo` usa H2 em memória; os dados são apagados quando o servidor é encerrado.

## MySQL

Crie o banco `financeiro`, configure as variáveis e execute na raiz do projeto:

```powershell
$env:DB_USER = "seu_usuario"
$env:DB_PASSWORD = "sua_senha"
.\mvnw.cmd spring-boot:run
```

A URL padrão aponta para `localhost:3306/financeiro`; configure `DB_URL` se necessário. Em produção com HTTPS, defina `COOKIE_SECURE=true`.

## Recuperação de senha

O perfil `demo` escreve o link temporário no log do servidor para permitir testar o fluxo sem e-mail. Com MySQL, configure o SMTP usando `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLE` e `MAIL_FROM`; defina `APP_BASE_URL` para o endereço público da aplicação. O link expira em 30 minutos e só pode ser usado uma vez.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes usam H2 e não precisam de MySQL. Senhas são armazenadas com BCrypt, a sessão usa cookie HttpOnly e alterações são protegidas por CSRF. Cada consulta financeira é limitada ao usuário autenticado.

## Publicar no Vercel

O Vercel hospeda o frontend e a função proxy em `api/[...path].js`; ele não mantém o processo Java do Spring Boot. Publique o backend separado em um serviço que aceite `Dockerfile` (por exemplo, Railway), adicione um MySQL e configure no backend:

- `DB_URL`: URL JDBC do MySQL remoto, começando com `jdbc:mysql://`
- `DB_USER` e `DB_PASSWORD`: credenciais do banco
- `COOKIE_SECURE=true`
- `APP_BASE_URL`: URL pública do frontend Vercel, para os links de redefinição de senha
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLE` e `MAIL_FROM` para recuperação por e-mail

Depois, em **Vercel > Project Settings > Environment Variables**, configure `BACKEND_URL` com o domínio HTTPS público do backend, sem `/api` no final, e faça um novo deploy. A função encaminha `/api/...` ao Spring e preserva cookies de sessão e CSRF. Para conferir o backend, abra `https://SEU_BACKEND/api/health`; deve responder `{"status":"ok"}`.
