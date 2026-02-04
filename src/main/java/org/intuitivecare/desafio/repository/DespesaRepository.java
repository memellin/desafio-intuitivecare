package org.intuitivecare.desafio.repository;

import org.intuitivecare.desafio.model.Despesa;
import org.springframework.data.repository.CrudRepository;


//Repositório jpa para o banco
public interface DespesaRepository extends CrudRepository<Despesa, Long> {
}
