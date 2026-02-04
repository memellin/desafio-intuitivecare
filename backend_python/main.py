from fastapi import FastAPI, HTTPException
from sqlalchemy import create_engine, Column, Integer, String, Numeric, Date, text, BigInteger, select
from sqlalchemy.orm import sessionmaker, declarative_base
from fastapi.middleware.cors import CORSMiddleware


def corrigir_texto(texto):
    if not texto:
        return ""
    try:
        # O famoso "pulo do gato": reverte latin-1 e força utf-8
        return texto.encode('latin-1').decode('utf-8')
    except:
        return texto


# --- CONFIGURAÇÃO ---
app = FastAPI(title="Intuitive Care API", description="Versão Final - Psycopg3")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- TROCA DE DRIVER ---
# Usamos 'postgresql+psycopg' (v3) em vez de pg8000
# Forçamos 127.0.0.1 para evitar problemas de IPv6
SQLALCHEMY_DATABASE_URL = "postgresql+psycopg://postgres:123456@127.0.0.1:5432/intuitive_db"

print(">>> TENTANDO CONECTAR AO BANCO...")
try:
    engine = create_engine(SQLALCHEMY_DATABASE_URL)
    # Teste rápido de conexão
    with engine.connect() as connection:
        result = connection.execute(text("SELECT 1"))
        print(">>> CONEXÃO COM BANCO BEM SUCEDIDA! 🎉")
except Exception as e:
    print(f">>> ERRO FATAL NA CONEXÃO DO BANCO: {e}")

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# --- MODELOS ---
class Operadora(Base):
    __tablename__ = "operadoras"
    id = Column(BigInteger, primary_key=True)
    registro_ans = Column(String)
    cnpj = Column(String)
    uf = Column(String)
    modalidade = Column(String)

class Despesa(Base):
    __tablename__ = "despesas"
    registro_ans = Column(String, primary_key=True)
    data_evento = Column("data", Date)
    ano = Column(Integer)
    trimestre = Column(Integer)
    valor = Column(Numeric)
    descricao = Column(String)
    razao_social = Column(String)

# --- ROTAS ---
@app.get("/")
def home():
    return {"status": "API Online! Acesse /docs para ver a documentação."}

@app.get("/api/operadoras")
def listar_operadoras(page: int = 1, limit: int = 10, search: str = ""):
    db = SessionLocal()
    offset = (page - 1) * limit
    
    subquery_nome = (
        select(Despesa.razao_social)
        .where(Despesa.registro_ans == Operadora.cnpj)
        .limit(1)
        .scalar_subquery()
    )

    query = db.query(
        Operadora.registro_ans,
        Operadora.cnpj,
        Operadora.uf,
        Operadora.modalidade,
        subquery_nome.label("razao_social")
    )
    
    if search:
        query = query.filter(Operadora.cnpj.ilike(f"%{search}%"))
    
    total = query.count()
    resultados = query.offset(offset).limit(limit).all()
    
    data_json = []
    for row in resultados:
        data_json.append({
            "registro_ans": row.registro_ans,
            "cnpj": row.cnpj,
            "uf": row.uf,
            "modalidade": corrigir_texto(row.modalidade), # <--- AQUI
            "razao_social": corrigir_texto(row.razao_social) if row.razao_social else "NOME INDISPONÍVEL" # <--- E AQUI
        })

    db.close()
    
    return {
        "data": data_json,
        "meta": {"page": page, "limit": limit, "total": total}
    }

@app.get("/api/estatisticas")
def estatisticas():
    db = SessionLocal()
    try:
        total = db.execute(text("SELECT SUM(valor) FROM despesas")).scalar() or 0
        
        ufs = db.execute(text("""
            SELECT o.uf, SUM(d.valor) 
            FROM despesas d 
            JOIN operadoras o ON d.registro_ans = o.cnpj 
            WHERE o.uf IS NOT NULL 
            GROUP BY o.uf 
            ORDER BY SUM(d.valor) DESC
        """)).fetchall()
        
        distribuicao_uf = [{"uf": row[0], "valor": float(row[1])} for row in ufs]
        
        return {"total_geral": float(total), "distribuicao_uf": distribuicao_uf}
    except Exception as e:
        return {"error": str(e)}
    finally:
        db.close()