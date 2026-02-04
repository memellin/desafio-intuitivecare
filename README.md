# Intuitive Care - Desafio Técnico

Solução completa para o teste técnico, abrangendo ETL de dados da ANS, API RESTful e Dashboard interativo.

## Tecnologias Utilizadas

* **Banco de Dados:** PostgreSQL
* **ETL (Extração e Carga):** Java 17 + Maven
* **Backend (API):** Python 3.8+ (FastAPI + SQLAlchemy + Psycopg3)
* **Frontend:** Vue.js 3 + Chart.js

---

## Pré-requisitos

* Java JDK 17 ou superior
* Python 3.8 ou superior
* Node.js e NPM
* PostgreSQL instalado e rodando (Porta 5432)

---

## Como Rodar o Projeto

Siga a ordem abaixo para garantir o funcionamento correto.

### 1. Configuração do Banco de Dados
Certifique-se de que o PostgreSQL está rodando.
* **Banco:** intuitive_db
* **Usuário:** postgres
* **Senha:** 123456
  *(Caso suas credenciais sejam diferentes, ajuste a string de conexão no arquivo src/.../DatabaseConnection.java e no backend_python/main.py)*

### 2. Executar o ETL (Java)
O módulo Java é responsável por baixar os CSVs, tratar os dados e popular o banco.

1.  Abra o projeto na raiz.
2.  Execute a classe principal Main (localizada em src/main/java/com/intuitive/Main.java).
3.  Aguarde a mensagem "Processamento concluído com sucesso!".
    * Isso criará as tabelas operadoras e despesas e inserirá os dados.

### 3. Iniciar a API (Backend Python)
A API expõe os dados do banco para o Frontend.

1.  Entre na pasta do backend:
    cd backend_python

2.  Crie e ative o ambiente virtual (opcional, mas recomendado):
    python -m venv venv
    # Windows:
    .\venv\Scripts\activate
    # Linux/Mac:
    source venv/bin/activate

3.  Instale as dependências:
    pip install -r requirements.txt

4.  Rode o servidor (Porta 8001):
    uvicorn main:app --reload --port 8001
    * Acesse a documentação em: http://127.0.0.1:8001/docs

### 4. Iniciar o Dashboard (Frontend Vue.js)
A interface visual com gráficos e tabelas.

1.  Em um novo terminal, entre na pasta do frontend:
    cd frontend_vue

2.  Instale as dependências:
    npm install

3.  Rode o projeto:
    npm run dev

4.  Acesse o link gerado (geralmente http://localhost:5173).

---

## Funcionalidades Implementadas

* [x] ETL Java: Download automático e parsing de CSVs da ANS.
* [x] Busca Textual: Pesquisa por CNPJ ou Razão Social na tabela.
* [x] Paginação: Controle de navegação na listagem de operadoras.
* [x] Visualização de Dados: Gráfico de barras com as despesas por UF (Top 10).
* [x] API REST: Documentada via Swagger/OpenAPI.
* [x] Tratamento de Encoding: Correção de caracteres especiais vindos do CSV.

---
Desenvolvido por Enock