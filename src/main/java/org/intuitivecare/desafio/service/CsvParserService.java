package org.intuitivecare.desafio.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.intuitivecare.desafio.model.Despesa;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserService {

    /**
     * Processa um arquivo desconhecido, tentando detectar formato e encoding.
     */
    public List<Despesa> parse(File arquivo) throws IOException {
        System.out.println("   Iniciando parser do arquivo: " + arquivo.getName());

        // 1. Detecção de Encoding (Simples)
        // A ANS costuma usar ISO-8859-1 (Windows) ou UTF-8. Vamos testar ISO primeiro que é mais comum em governos antigos.
        Charset encoding = StandardCharsets.ISO_8859_1;

        // 2. Detecção de Separador (Sniffing)
        char separador = detectarSeparador(arquivo, encoding);
        System.out.println("      -> Detectado: Separador=['" + separador + "'] Encoding=[" + encoding + "]");

        List<Despesa> despesas = new ArrayList<>();

        // Configura o parser do Apache Commons CSV
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setIgnoreEmptyLines(true)
                .setHeader() // Assume que a primeira linha é cabeçalho
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(arquivo), encoding);
             CSVParser csvParser = new CSVParser(reader, format)) {
            // Dentro do try (csvParser)
            System.out.println("      [DEBUG] Cabeçalhos encontrados: " + csvParser.getHeaderNames());
            for (CSVRecord record : csvParser) {
                // Lógica de Extração Resiliente
                // Verifica se é uma linha de 'EVENTOS' ou 'DESPESA'
                // Adaptei os nomes das colunas conforme o CSV
                try {
                    // Tenta mapear colunas comuns. A ANS muda nomes as vezes (ex: "CD_OPERADORA" vs "REG_ANS")
                    String regAns = getValorSeguro(record, "REG_ANS", "CD_OPERADORA", "Registro");
                    String nome = getValorSeguro(record, "RAZAO_SOCIAL", "NM_RAZAO_SOCIAL", "Operadora");
                    String valorStr = getValorSeguro(record, "VL_SALDO_FINAL", "VALOR", "Vl_Saldo_Final");
                    String conta = getValorSeguro(record, "CD_CONTA_CONTABIL", "CD_CONTA", "Conta");

                    // FILTRO: Apenas contas de DESPESA (geralmente começam com '4' no plano de contas da ANS)
                    // Ajuste essa regra se necessário ao olhar o CSV
                    if (conta != null && conta.startsWith("4")) {
                        Despesa d = new Despesa();
                        d.setRegistroAns(regAns);
                        d.setRazaoSocial(nome);
                        d.setValor(parseValor(valorStr));

                        // Extrai ano/trimestre do nome do arquivo (ex: 1T2025.csv)
                        preencherDataPeloNomeArquivo(d, arquivo.getName());

                        despesas.add(d);
                    }
                } catch (Exception e) {
                    // Loga erro mas não para o processamento (Resiliência)
                    // System.err.println("Erro linha " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("      -> Linhas processadas com sucesso: " + despesas.size());
        return despesas;
    }

    private char detectarSeparador(File arquivo, Charset encoding) throws IOException {
        // Lê as primeiras linhas para "cheirar" o separador
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), encoding))) {
            String linha = reader.readLine();
            if (linha != null) {
                if (linha.contains(";")) return ';';
                if (linha.contains(",")) return ',';
            }
        }
        return ';'; // Padrão Brasil
    }

    private String getValorSeguro(CSVRecord record, String... colunasPossiveis) {
        for (String col : colunasPossiveis) {
            if (record.isMapped(col)) {
                return record.get(col);
            }
        }
        return null;
    }

    private BigDecimal parseValor(String valorStr) {
        if (valorStr == null) return BigDecimal.ZERO;
        try {
            // Converte formato brasileiro (1.000,00) para BigDecimal
            String limpo = valorStr.replace(".", "").replace(",", ".");
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void preencherDataPeloNomeArquivo(Despesa d, String nomeArquivo) {
        // Ex: "1T2025.csv" -> Trimestre 1, Ano 2025
        try {
            String nome = nomeArquivo.toUpperCase();
            int ano = 0;
            int tri = 0;

            if (nome.contains("2025")) ano = 2025;
            else if (nome.contains("2024")) ano = 2024;
            else if (nome.contains("2023")) ano = 2023;

            if (nome.contains("1T")) tri = 1;
            else if (nome.contains("2T")) tri = 2;
            else if (nome.contains("3T")) tri = 3;
            else if (nome.contains("4T")) tri = 4;

            d.setAno(ano);
            d.setTrimestre(tri);
        } catch (Exception e) {
            // Ignora
        }
    }
}