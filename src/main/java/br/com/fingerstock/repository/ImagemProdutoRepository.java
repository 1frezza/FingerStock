package br.com.fingerstock.repository;

import br.com.fingerstock.model.ImagemProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagemProdutoRepository extends JpaRepository<ImagemProduto, Long> {
}
