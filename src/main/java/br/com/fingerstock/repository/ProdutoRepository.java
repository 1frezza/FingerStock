package br.com.fingerstock.repository;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Outros produtos da mesma categoria, para a seção "Veja também" da página do produto. */
    List<Produto> findTop4ByCategoriaAndIdNotOrderByNome(Categoria categoria, Long id);

    /** Retira uma unidade do estoque, se houver. Retorna 1 quando a baixa foi feita e 0 se o produto está esgotado. */
    @Modifying
    @Query("update Produto p set p.quantidade = p.quantidade - 1 where p.id = :id and p.quantidade > 0")
    int baixarEstoque(@Param("id") Long id);
}
