package org.intuitivecare.desafio;

import org.intuitivecare.desafio.model.Despesa;
import org.intuitivecare.desafio.repository.DespesaRepository;
import org.intuitivecare.desafio.repository.OperadoraRepository;
import org.intuitivecare.desafio.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestExecutionRunner implements CommandLineRunner {

    private final AnsScrapper ansScrapper;
    private final ExtractScrapper extractScrapper;
    private final CsvParserService csvParserService;
    private final CsvWriterService csvWriterService;
    private final EnrichmentService enrichmentService;
    private final DespesaRepository despesaRepository;
    private final OperadoraRepository operadoraRepository;

    // Injeção de Dependência 100% via Construtor (Padrão Ouro do Spring)
    @Autowired
    public TestExecutionRunner(AnsScrapper ansScrapper,
                               ExtractScrapper extractScrapper,
                               CsvParserService csvParserService,
                               CsvWriterService csvWriterService,
                               EnrichmentService enrichmentService,
                               DespesaRepository despesaRepository,
                               OperadoraRepository operadoraRepository) {
        this.ansScrapper = ansScrapper;
        this.extractScrapper = extractScrapper;
        this.csvParserService = csvParserService;
        this.csvWriterService = csvWriterService;
        this.enrichmentService = enrichmentService;
        this.despesaRepository = despesaRepository;
        this.operadoraRepository = operadoraRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> INICIANDO O DESAFIO INTUITIVE CARE <<<");

        // --- ETAPA 1: Identificar Links ---
        System.out.println("--- [1.1] Buscando links na ANS ---");
        List<String> links = ansScrapper.buscarLinksUltimos3Trimestres();

        if (links.isEmpty()) {
            System.err.println("Nenhum link encontrado. Abortando.");
            return;
        }

        // --- ETAPA 2: Baixar e Extrair ---
        System.out.println("--- [1.2] Baixando e Extraindo Arquivos ---");
        List<File> arquivosProntos = extractScrapper.baixarEExtrairArquivos(links);
        System.out.println("Arquivos prontos para processamento: " + arquivosProntos.size());

        // --- Carregando o cache de operadoras (Crucial fazer antes do loop principal) ---
        enrichmentService.baixarECarregarOperadoras();

        // --- ETAPA 3: Parser e Enriquecimento ---
        List<Despesa> todasDespesas = new ArrayList<>();
        System.out.println("--- [1.2 & 2.2] Processando e Enriquecendo Despesas ---");

        for (File arquivo : arquivosProntos) {
            // Parser (Lê o CSV bruto)
            List<Despesa> despesasArquivo = csvParserService.parse(arquivo);

            // Adiciona na lista principal
            todasDespesas.addAll(despesasArquivo);
        }

        System.out.println("Total de despesas lidas: " + todasDespesas.size());

        //  --- Etapa 4: Enriquecimento (Aplicando Join em memória) ---
        enrichmentService.enriquecerDespesas(todasDespesas);

        //  --- Etapa 5: Gerar CSV Consolidado Final ---
        // Geramos apenas uma vez, já com os dados completos (CNPJ e Razão Social preenchidos)
        csvWriterService.gerarArquivoConsolidado(todasDespesas);

        System.out.println(">>> TESTES 1 e 2 (CSV) CONCLUÍDOS! <<<");

        // --- ETAPA 6: Persistência no Banco ---
        System.out.println("--- [3.3] Salvando no Banco de Dados (PostgreSQL) ---");

        // 1. Salvar Operadoras
        if (!enrichmentService.getCacheOperadoras().isEmpty()) {
            operadoraRepository.saveAll(enrichmentService.getCacheOperadoras().values());
            System.out.println("   Operadoras salvas.");
        }

        // 2. Salvar Despesas
        if (!todasDespesas.isEmpty()) {
            despesaRepository.saveAll(todasDespesas);
            System.out.println("   Despesas salvas.");
        }

        System.out.println(">>> FLUXO COMPLETO FINALIZADO COM SUCESSO! <<<");
    }
}