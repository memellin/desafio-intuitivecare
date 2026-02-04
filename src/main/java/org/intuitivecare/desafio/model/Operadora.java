package org.intuitivecare.desafio.model;


import jakarta.persistence.*;

@Entity
@Table(name = "operadoras")
public class Operadora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registro_ans")
    private String registroAns;
    private String cnpj;
    @Transient // Não salva no banco, já que vamos pegar da tabela Operadoras via Join
    private String razaoSocial;
    private String modalidade;
    private String uf;

    // Construtores, Getters e Setters
    public Operadora(String registroAns, String cnpj, String razaoSocial, String modalidade, String uf) {
        this.registroAns = registroAns;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.modalidade = modalidade;
        this.uf = uf;
    }

    // Adicione os Getters aqui...
    public String getRegistroAns() { return registroAns; }
    public String getCnpj() { return cnpj; }
    public String getRazaoSocial() { return razaoSocial; }
    public String getModalidade() { return modalidade; }
    public String getUf() { return uf; }

}