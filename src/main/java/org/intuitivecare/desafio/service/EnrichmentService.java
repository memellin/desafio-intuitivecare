package org.intuitivecare.desafio.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.intuitivecare.desafio.model.Despesa;
import org.intuitivecare.desafio.model.Operadora;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnrichmentService {

    private static final String URL_CADASTRO = "https://dadosabertos.ans.gov.br/FTP/PDA/operadoras_de_plano_de_saude_ativas/Relatorio_cadop.csv";



    // Cache em memória: Chave = RegistroANS, Valor = Dados da Operadora
    private Map<String, Operadora> cacheOperadoras = new HashMap<>();


    public void baixarECarregarOperadoras() throws IOException {
        System.out.println("--- [2.2] Iniciando Download do Cadastro de Operadoras ---");

        // Baixar direto para memória ou arquivo temp. Como é pequeno, stream direto funciona bem.
        // Aparentemente o arquivo da ANS é CSV separado por ponto e vírgula, encoding ISO-8859-1

        try (BufferedInputStream in = new BufferedInputStream(new URL(URL_CADASTRO).openStream());
             // O arquivo CADOP geralmente usa ISO-8859-1
             InputStreamReader reader = new InputStreamReader(in, Charset.forName("ISO-8859-1"));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setDelimiter(';')
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            // DEBUG: Imprime os cabeçalhos para conferirmos se mudaram de nome
            System.out.println("   [DEBUG] Cabeçalhos do Cadop: " + parser.getHeaderNames());

            for (CSVRecord record : parser) {
                // "REGISTRO_OPERADORA" que é o nome real que apareceu no log
                String reg = getValor(record, "REGISTRO_OPERADORA", "Registro_ANS", "Registro");

                String cnpj = getValor(record, "CNPJ", "Cnpj");
                String razao = getValor(record, "Razao_Social", "RazaoSocial", "NM_RAZAO_SOCIAL");
                String modalidade = getValor(record, "Modalidade", "Descricao_Modalidade");
                String uf = getValor(record, "UF", "Uf");

        // Debug para garantir que está pegando
                if (cacheOperadoras.size() == 0 && reg != null) {
                    System.out.println("   [DEBUG] Primeira operadora encontrada: " + razao + " (Reg: " + reg + ")");
                }

                if (reg != null) {
                    // Remove aspas ou formatação extra se houver (ex: "41234" -> 41234)
                    reg = reg.replaceAll("[^0-9]", "");

                    Operadora op = new Operadora(reg, cnpj, razao, modalidade, uf);
                    cacheOperadoras.put(reg, op);
                }
            }
        }
        System.out.println("   Total de operadoras carregadas no cache: " + cacheOperadoras.size());
    }

    // Método auxiliar simples
    private String getValor(CSVRecord record, String... opcoes) {
        for (String opcao : opcoes) {
            if (record.isMapped(opcao)) return record.get(opcao);
        }
        return null;
    }
    // Método auxlilar
    public Map<String, Operadora> getCacheOperadoras() {
        return cacheOperadoras;
    }

    /**
     * Enriquece a lista de despesas com os dados da operadora
     */
    public void enriquecerDespesas(List<Despesa> despesas) {
        System.out.println("   Cruzando dados (Join em memória)...");
        int semMatch = 0;

        for (Despesa d : despesas) {
            // Busca no mapa pelo Registro ANS
            Operadora op = cacheOperadoras.get(d.getRegistroAns());

            if (op != null) {
                d.setRazaoSocial(op.getRazaoSocial());
                // Setar no objeto
                d.setRegistroAns(op.getCnpj());
            } else {
                semMatch++;
                d.setRazaoSocial("OPERADORA NÃO ENCONTRADA");
            }
        }

        if (semMatch > 0) {
            System.err.println("   ALERTA: " + semMatch + " registros não tiveram correspondência no cadastro (Inconsistência identificada).");
        }

    }


}