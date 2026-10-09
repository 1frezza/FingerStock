package br.com.fingerstock.repository;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("""
            select p from Produto p
            where (lower(p.nome) like lower(concat('%', :busca, '%'))
                or lower(coalesce(p.marca, '')) like lower(concat('%', :busca, '%')))
              and (:categoria is null or p.categoria = :categoria)
            order by p.nome
            """)
    List<Produto> filtrar(@Param("busca") String busca, @Param("categoria") Categoria categoria);
}
