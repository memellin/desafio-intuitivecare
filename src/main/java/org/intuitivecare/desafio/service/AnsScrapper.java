package org.intuitivecare.desafio.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AnsScrapper {

    private static final String ANS_ROOT_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/";
    // Regex para identificar anos (ex: 2023, 2024)
    private static final Pattern YEAR_PATTERN = Pattern.compile("^20\\d{2}$");

    /* Método principal que orquestra a busca pelos 3 ultimo trimestres. */
    public List<String> buscarLinksUltimos3Trimestres() throws IOException {
        System.out.println(">>> Iniciando varredura na ANS...");

        // 1. Acessa a raiz e encontra a pasta de Demonstrações Contábeis
        String demonstracoesUrl = encontrarLinkDemonstracoes(ANS_ROOT_URL);
        if (demonstracoesUrl == null) {
            throw new IOException("Pasta 'demonstracoes_contabeis' não encontrada na raiz.");
        }
        System.out.println("Pasta de demonstrações encontrada: " + demonstracoesUrl);

        // 2. Varre todos os anos e trimestres disponíveis
        List<PeriodoAns> todosPeriodos = listarTodosPeriodos(demonstracoesUrl);

        // 3. Ordena do mais recente para o mais antigo
        Collections.sort(todosPeriodos);

        // 4. Seleciona os 3 primeiros (Top 3)
        List<String> linksFinais = new ArrayList<>();
        int limite = Math.min(3, todosPeriodos.size());

        System.out.println(">>> Trimestres selecionados para download:");
        for (int i = 0; i < limite; i++) {
            PeriodoAns p = todosPeriodos.get(i);
            System.out.println("   " + (i+1) + "º: " + p.getAno() + "/" + p.getTrimestre() + " -> " + p.getUrl());
            linksFinais.add(p.getUrl());
        }

        return linksFinais;
    }

    /**
     * Passo 1: Encontra o link correto para Demonstrações Contábeis
     */
    private String encontrarLinkDemonstracoes(String baseUrl) throws IOException {
        Document doc = Jsoup.connect(baseUrl).get();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            // Busca por palavras-chave, ignorando case, para ser resiliente a mudanças de nome
            if (href.toLowerCase().contains("demonstraco") && href.toLowerCase().contains("contabeis")) {
                // Resolve URL relativa se necessário
                return resolveUrl(baseUrl, href);
            }
        }
        return null;
    }

    /**
     * Passo 2: Navega recursivamente: Pasta Raiz -> Pastas de Anos -> Pastas de Trimestres
     */
    private List<PeriodoAns> listarTodosPeriodos(String baseUrl) throws IOException {
        List<PeriodoAns> periodos = new ArrayList<>();

        // Acessa a pasta de Demonstrações
        Document docAnos = Jsoup.connect(baseUrl).get();
        Elements linksAnos = docAnos.select("a[href]");

        for (Element linkAno : linksAnos) {
            String hrefAno = linkAno.attr("href").replace("/", ""); // Remove barra final se houver

            // Verifica se é uma pasta de ano (ex: 2024)
            if (YEAR_PATTERN.matcher(hrefAno).matches()) {
                int ano = Integer.parseInt(hrefAno);
                String urlAno = resolveUrl(baseUrl, linkAno.attr("href"));

                // Dentro do Ano, busca os trimestres
                periodos.addAll(listarTrimestresDoAno(urlAno, ano));
            }
        }
        return periodos;
    }

    private List<PeriodoAns> listarTrimestresDoAno(String urlAno, int ano) throws IOException {
        List<PeriodoAns> periodosDoAno = new ArrayList<>();
        Document docTrim = Jsoup.connect(urlAno).get();
        Elements linksTrim = docTrim.select("a[href]");

        for (Element linkTrim : linksTrim) {
            String texto = linkTrim.text(); // Ex: "1T2024", "1T", "202401"
            String href = linkTrim.attr("href");

            // Lógica para identificar o número do trimestre
            Integer trimestre = extrairNumeroTrimestre(texto, href);

            if (trimestre != null) {
                String finalUrl = resolveUrl(urlAno, href);
                periodosDoAno.add(new PeriodoAns(ano, trimestre, finalUrl));
            }
        }
        return periodosDoAno;
    }

    private String resolveUrl(String base, String relative) {
        if (relative.startsWith("http")) return relative;
        if (!base.endsWith("/")) base += "/";
        return base + relative;
    }

    /**
     * Tenta identificar se é 1º, 2º, 3º ou 4º trimestre baseado no texto do link.
     */
    private Integer extrairNumeroTrimestre(String texto, String href) {
        String raw = (texto + " " + href).toLowerCase();

        if (raw.contains("1t") || raw.contains("1º") || raw.contains("01")) return 1;
        if (raw.contains("2t") || raw.contains("2º") || raw.contains("02")) return 2;
        if (raw.contains("3t") || raw.contains("3º") || raw.contains("03")) return 3;
        if (raw.contains("4t") || raw.contains("4º") || raw.contains("04")) return 4;

        return null; // Não identificado como trimestre
    }

    /**
     * Classe auxiliar para armazenar e ordenar os períodos.
     * Implementa Comparable para facilitar a ordenação (Ano DESC, Trimestre DESC).
     */
    public static class PeriodoAns implements Comparable<PeriodoAns> {
        private int ano;
        private int trimestre;
        private String url;

        public PeriodoAns(int ano, int trimestre, String url) {
            this.ano = ano;
            this.trimestre = trimestre;
            this.url = url;
        }

        public int getAno() { return ano; }
        public int getTrimestre() { return trimestre; }
        public String getUrl() { return url; }

        @Override
        public int compareTo(PeriodoAns outro) {
            // Ordenação decrescente de Ano
            int anoCompare = Integer.compare(outro.ano, this.ano);
            if (anoCompare != 0) return anoCompare;

            // Se ano igual, ordenação decrescente de Trimestre
            return Integer.compare(outro.trimestre, this.trimestre);
        }
    }
}