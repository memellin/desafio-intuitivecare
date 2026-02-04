package org.intuitivecare.desafio.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ExtractScrapper {

    private static final String DOWNLOAD_DIR = "dados_downloads";
    private static final String EXTRACT_DIR = "dados_extraidos";

    public ExtractScrapper() {
        // Garante que os diretórios de trabalho existam
        criarDiretorios(DOWNLOAD_DIR);
        criarDiretorios(EXTRACT_DIR);
    }

    /**
     * Orquestra o download e extração dos arquivos a partir das URLs.
     * @param urlsDiretorios Lista de URLs identificadas pelo Scraper
     * @return Lista de arquivos extraídos prontos para o parser.
     */
    public List<File> baixarEExtrairArquivos(List<String> urlsDiretorios) {
        List<File> arquivosRelevantes = new ArrayList<>();

        for (String url : urlsDiretorios) {
            try {
                System.out.println("Processando URL: " + url);
                String urlZip = null;

                // Lógica de resiliência: Verifica se a URL já é o arquivo ZIP ou um diretório
                if (url.toLowerCase().endsWith(".zip")) {
                    System.out.println("   URL identificada como arquivo direto.");
                    urlZip = url;
                } else {
                    // Se for diretório, faz scraping para encontrar o link do .zip
                    urlZip = encontrarLinkZip(url);
                }

                if (urlZip == null) {
                    System.err.println("Nenhum arquivo .zip encontrado em: " + url);
                    continue;
                }

                File arquivoZip = downloadFile(urlZip);
                List<File> extraidos = extrairArquivosRelevantes(arquivoZip);
                arquivosRelevantes.addAll(extraidos);

            } catch (Exception e) {
                System.err.println("Erro ao processar URL " + url + ": " + e.getMessage());
                e.printStackTrace(); // Importante para debug em ambiente de teste
            }
        }
        return arquivosRelevantes;
    }

    private String encontrarLinkZip(String urlDir) throws IOException {
        // Timeout aumentado para garantir conexão com servidores instáveis
        Document doc = Jsoup.connect(urlDir).timeout(10000).get();
        Elements links = doc.select("a[href$=.zip]");

        if (!links.isEmpty()) {
            String href = links.first().attr("href");
            // Tratamento para URLs relativas
            if (!href.startsWith("http")) {
                if (!urlDir.endsWith("/")) urlDir += "/";
                return urlDir + href;
            }
            return href;
        }
        return null;
    }

    private File downloadFile(String urlString) throws IOException {
        String nomeArquivo = urlString.substring(urlString.lastIndexOf("/") + 1);
        File destino = new File(DOWNLOAD_DIR, nomeArquivo);

        System.out.println("   Baixando: " + nomeArquivo + "...");

        // Utiliza BufferedInputStream para evitar carregar o arquivo inteiro em memória (Heap Space)
        try (BufferedInputStream in = new BufferedInputStream(new URL(urlString).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destino)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        System.out.println("   Download concluído: " + destino.getAbsolutePath());
        return destino;
    }

    private List<File> extrairArquivosRelevantes(File arquivoZip) throws IOException {
        List<File> arquivosExtraidos = new ArrayList<>();
        System.out.println("   Extraindo arquivos relevantes de: " + arquivoZip.getName());

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(arquivoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String nome = entry.getName().toLowerCase();

                if (isArquivoInteresse(nome)) {
                    File destino = new File(EXTRACT_DIR, entry.getName());

                    // Cria subdiretórios se necessário
                    new File(destino.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(destino)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    System.out.println("      -> Extraído: " + entry.getName());
                    arquivosExtraidos.add(destino);
                }
            }
        }
        return arquivosExtraidos;
    }

    /**
     * Aplica regras de negócio para filtrar apenas arquivos de dados contábeis (CSV/TXT/XLSX),
     * ignorando manuais, layouts e PDFs.
     */
    private boolean isArquivoInteresse(String nomeArquivo) {
        String nomeLower = nomeArquivo.toLowerCase();

        // 1. Validação de extensão
        boolean isExtensaoValida = nomeLower.endsWith(".csv") || nomeLower.endsWith(".txt") || nomeLower.endsWith(".xlsx");

        // 2. Exclusão explícita de documentação
        boolean isNaoDocumentacao = !nomeLower.contains("leiaute") && !nomeLower.contains("manual") && !nomeLower.endsWith(".pdf");

        // 3. Busca por termos chaves comuns nos arquivos da ANS
        // Nota: Arquivos genéricos são aceitos se passarem nos critérios acima para garantir cobertura.
        return isExtensaoValida && isNaoDocumentacao;
    }

    private void criarDiretorios(String caminho) {
        File dir = new File(caminho);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}