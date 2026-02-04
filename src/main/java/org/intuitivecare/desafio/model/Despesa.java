package org.intuitivecare.desafio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesas")
public class Despesa {


    @Id
    @Column(name = "registro_ans")
    private String registroAns; // Será usado no join depois
    private String razaoSocial;
    private String descricao;
    private LocalDate data; // Data base do trimestre
    private BigDecimal valor;
    private int ano;
    private int trimestre;
    // Em Despesa.java
    private String cnpj;
    private String modalidade;
    private String uf;

// Gere os Getters e Setters

    // Construtor vazio
    public Despesa() {}

    // Getters e Setters manuais (sem Lombok)
    public String getRegistroAns() { return registroAns; }
    public void setRegistroAns(String registroAns) { this.registroAns = registroAns; }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public int getTrimestre() { return trimestre; }
    public void setTrimestre(int trimestre) { this.trimestre = trimestre; }

    @Override
    public String toString() {
        return "Despesa{ANS='" + registroAns + "', Valor=" + valor + ", Data=" + trimestre + "/" + ano + "}";
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getModalidade() {
        return modalidade;
    }

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}