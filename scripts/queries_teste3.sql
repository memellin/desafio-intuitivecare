-- TRADE-OFF: NORMALIZAÇÃO
-- Escolha: Opção B (Tabelas Normalizadas)
-- Justificativa: Separei 'operadoras' de 'despesas'.
-- 1. Redução de Redundância: A Razão Social e UF repetem-se milhares de vezes. Normalizando, economizamos armazenamento.
-- 2. Integridade: Se uma operadora mudar de nome, atualizamos em apenas um lugar.
-- 3. Performance: Queries analíticas apenas em valores numéricos (tabela despesas) são mais rápidas sem as strings de texto pesado.

CREATE TABLE operadoras (
                            registro_ans VARCHAR(20) PRIMARY KEY,
                            cnpj VARCHAR(20),
                            razao_social VARCHAR(255),
                            modalidade VARCHAR(100),
                            uf CHAR(2)
);

-- TRADE-OFF: TIPOS DE DADOS
-- Valor: DECIMAL(15,2) -> Essencial para dados monetários. FLOAT tem problemas de precisão em centavos.
-- Data: DATE -> Suficiente, pois a análise é trimestral (dia/mês/ano), não precisamos de precisão de segundos (TIMESTAMP).

CREATE TABLE despesas (
                          id SERIAL PRIMARY KEY,
                          registro_ans VARCHAR(20),
                          data_evento DATE, -- Representa o trimestre (ex: 2025-01-01 para 1T2025)
                          trimestre INT,
                          ano INT,
                          valor NUMERIC(15,2),
                          descricao VARCHAR(255),
                          CONSTRAINT fk_operadora FOREIGN KEY (registro_ans) REFERENCES operadoras(registro_ans)
);

CREATE INDEX idx_despesas_data ON despesas(ano, trimestre);
CREATE INDEX idx_despesas_operadora ON despesas(registro_ans);

WITH despesas_trimestrais AS (
    SELECT
        d.registro_ans,
        o.razao_social,
        SUM(CASE WHEN d.ano = 2025 AND d.trimestre = 1 THEN d.valor ELSE 0 END) as despesa_t1,
        SUM(CASE WHEN d.ano = 2025 AND d.trimestre = 3 THEN d.valor ELSE 0 END) as despesa_ult_t
    FROM despesas d
             JOIN operadoras o ON d.registro_ans = o.registro_ans
    GROUP BY d.registro_ans, o.razao_social
)
SELECT
    registro_ans,
    razao_social,
    despesa_t1,
    despesa_ult_t,
    ((despesa_ult_t - despesa_t1) / NULLIF(despesa_t1, 0)) * 100 as crescimento_pct
FROM despesas_trimestrais
WHERE despesa_t1 > 0 -- Evita divisão por zero e distorções de quem começou agora
ORDER BY crescimento_pct DESC
    LIMIT 5;

SELECT
    o.uf,
    SUM(d.valor) as total_despesas,
    AVG(d.valor) as media_por_lancamento -- Ou média por operadora, dependendo da interpretação
FROM despesas d
         JOIN operadoras o ON d.registro_ans = o.registro_ans
GROUP BY o.uf
ORDER BY total_despesas DESC
    LIMIT 5;

WITH medias_gerais AS (
    SELECT ano, trimestre, AVG(valor) as media_geral
    FROM despesas
    GROUP BY ano, trimestre
),
     comparativo AS (
         SELECT
             d.registro_ans,
             d.ano,
             d.trimestre,
             SUM(d.valor) as total_op,
             m.media_geral
         FROM despesas d
                  JOIN medias_gerais m ON d.ano = m.ano AND d.trimestre = m.trimestre
         GROUP BY d.registro_ans, d.ano, d.trimestre, m.media_geral
     )
SELECT registro_ans
FROM comparativo
WHERE total_op > media_geral
GROUP BY registro_ans
HAVING COUNT(*) >= 2; -- Pelo menos 2 trimestres acima da média