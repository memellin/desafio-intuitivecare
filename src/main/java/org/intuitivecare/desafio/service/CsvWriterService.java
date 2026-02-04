package org.intuitivecare.desafio.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.intuitivecare.desafio.model.Despesa;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class CsvWriterService {

    private static final String ARQUIVO_SAIDA = "consolidado_despesas.csv";

    public void gerarArquivoConsolidado(List<Despesa> todasDespesas) throws IOException {
        System.out.println("--- [1.3] Gerando CSV Consolidado ---");
        System.out.println("   Escrevendo " + todasDespesas.size() + " registros em " + ARQUIVO_SAIDA + "...");

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(ARQUIVO_SAIDA));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setHeader("CNPJ", "RazaoSocial", "Trimestre", "Ano", "ValorDespesas")
                     .setDelimiter(';') // Padrão API pública
                     .build())) {

            for (Despesa d : todasDespesas) {
                csvPrinter.printRecord(
                        d.getRegistroAns(), // O teste pede CNPJ, mas nos arquivos de despesa geralmente vem o Registro ANS.
                        d.getRazaoSocial(),
                        d.getTrimestre(),
                        d.getAno(),
                        d.getValor()
                );
            }
            csvPrinter.flush();
        }
        System.out.println("   Arquivo gerado com sucesso: " + Paths.get(ARQUIVO_SAIDA).toAbsolutePath());
    }
}