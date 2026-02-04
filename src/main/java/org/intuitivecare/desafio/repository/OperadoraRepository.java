package org.intuitivecare.desafio.repository;

import org.intuitivecare.desafio.model.Operadora;
import org.springframework.data.repository.CrudRepository;

//Repositório jpa para o banco
public interface OperadoraRepository extends CrudRepository<Operadora, Integer> {
}
