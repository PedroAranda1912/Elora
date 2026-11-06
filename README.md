# Elora

Marketplace de produtos e serviços feito com Java 21, Spring Boot, PostgreSQL e frontend HTML, CSS e JavaScript.

## Requisitos

- JDK 21 e Maven 3.8 ou superior.
- PostgreSQL local ou serviço PostgreSQL Aiven.
- Docker Desktop apenas para executar o PostgreSQL local com Docker Compose.
- `pg_dump` e `pg_restore` apenas para transferir os dados do Neon.

Configure `backend/.env` com a URI PostgreSQL completa do banco em `DB_URL` e um segredo JWT forte em `JWT_SECRET`. Para iniciar o backend:

```powershell
cd backend
mvn spring-boot:run
```

## Dados de demonstração

Para apresentar o marketplace com as vitrines cheias, inicie o backend com `ELORA_DEMO_SEED=true`:

```powershell
cd backend
$env:ELORA_DEMO_SEED = "true"; mvn spring-boot:run
```

Na primeira inicialização, o app cria 12 contas, 37 produtos de todas as categorias, 18 serviços com pacotes,
8 projetos com propostas, pedidos, avaliações, conversas, negociações, perguntas, favoritos e notificações.
Se as contas de demonstração já existirem, nada é alterado, então a variável pode ficar ligada.

Todas as contas usam a senha `Elora@2026` (mude com `ELORA_DEMO_PASSWORD`). As principais para a apresentação:

| Conta | Papel na demo |
|---|---|
| `marina@elora.demo` | Compradora: pedidos em todos os status, favoritos, negociações, um projeto aberto com propostas e um serviço aguardando aprovação |
| `rafael@elora.demo` | Vendedor de eletrônicos: vendas para enviar, pergunta sem resposta, proposta de compra pendente e estoque baixo no painel |
| `juliana@elora.demo` | Designer com 11 trabalhos concluídos (nível "Experiente"), portfólio e uma solicitação nova para aceitar |

As outras contas (`bruno`, `camila`, `lucas`, `diego`, `larissa`, `thiago`, `patricia`, `fernanda` e `gabriel`, todas `@elora.demo`)
completam o catálogo e o histórico. Os dados ficam em `backend/src/main/resources/demo/demo-data.json`, e as fotos,
com os créditos, em `frontend/assets/demo/`. As URLs das fotos usam `http://localhost:8080`; para outro endereço,
defina `ELORA_DEMO_IMAGE_BASE_URL` antes da primeira inicialização com a seed.

Para usar o PostgreSQL local do Docker Compose, execute na raiz do projeto:

```powershell
docker compose up --build
```

## Testes

Execute os comandos na pasta `backend`. Todos os testes usam H2 e não acessam o Aiven:

```powershell
cd backend
mvn test
```

Para executar todos os testes de uma classe:

```powershell
mvn -Dtest=PostgresDatabaseUrlTest test
mvn -Dtest=MarketplaceFlowIntegrationTest test
```

Para executar um teste individual, use `mvn "-Dtest=Classe#metodo" test`:

```powershell
mvn "-Dtest=PostgresDatabaseUrlTest#convertsAivenUriAndDecodesEmbeddedCredentials" test
mvn "-Dtest=PostgresDatabaseUrlTest#preservesExistingJdbcUrlsAndTheirSeparateCredentials" test
mvn "-Dtest=PostgresDatabaseUrlTest#rejectsUnsupportedOrIncompleteUris" test
mvn "-Dtest=MarketplaceFlowIntegrationTest#cadastroCatalogoFavoritosCompraEEstoqueFuncionamDePontaAPonta" test
mvn "-Dtest=MarketplaceFlowIntegrationTest#mensagensContextoNaoLidasLeituraEReviewsDeCompraConcluidaFuncionam" test
mvn "-Dtest=MarketplaceFlowIntegrationTest#pacotesDeServicoEFluxoDeProjetoComPropostaFuncionam" test
mvn "-Dtest=MarketplaceFlowIntegrationTest#negociacaoDeProdutoENotificacoesRespeitamAsPartes" test
mvn "-Dtest=MarketplaceFlowIntegrationTest#portfolioPermiteGerenciarSomenteItensDoProprioPerfil" test
```

## Rodar no dia da apresentação

Passo a passo para o notebook da apresentação. O Java padrão do Windows nele é o 17, e o projeto precisa do 21 ou mais novo,
por isso o bloco abaixo aponta para o JDK 22 e usa o Maven que já está baixado.

1. Abra a pasta `Elora-Final` no Explorador de Arquivos, clique com o botão direito num espaço vazio e escolha **Abrir no Terminal**.
2. Cole este bloco e aperte Enter:

   ```powershell
   cd backend
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
   $env:ELORA_DEMO_SEED = "true"
   & (Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Recurse -Filter mvn.cmd | Select-Object -First 1).FullName spring-boot:run
   ```

3. Espere aparecer `Started EloraApplication` (cerca de 30 segundos). Deixe o terminal aberto durante toda a apresentação.
4. Abra **http://localhost:8080**.

Para mostrar compra e venda ao mesmo tempo, entre com uma conta numa janela normal e com a outra numa **janela anônima**
(`Ctrl+Shift+N`). Na mesma janela, as abas trocam de conta entre si.

| Conta | Senha |
|---|---|
| `marina@elora.demo` (compradora) | `Elora@2026` |
| `rafael@elora.demo` (vendedor) | `Elora@2026` |
| `juliana@elora.demo` (prestadora) | `Elora@2026` |

Para encerrar, aperte `Ctrl+C` no terminal.

**Se algo der errado**

| O terminal mostra | O que fazer |
|---|---|
| `Port 8080 was already in use` | O Elora já está rodando em outro terminal. Use esse ou feche-o com `Ctrl+C` e rode de novo. |
| `The connection attempt failed` ou erro de conexão com `aivencloud.com` | A rede bloqueia o banco. Conecte o notebook ao hotspot do celular e rode de novo. |
| A página abre, mas sem anúncios | Confira se o bloco foi colado inteiro, com a linha `ELORA_DEMO_SEED`. A seed não apaga nem duplica nada. |
