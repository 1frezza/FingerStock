package br.com.fingerstock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Imagem enviada para um produto. Fica em tabela própria para não ser carregada
 * nas listagens, e no banco (RDS) para entrar nos mesmos backups dos demais dados.
 */
@Entity
@Table(name = "produto_imagens")
public class ImagemProduto {

    @Id
    @Column(name = "produto_id")
    private Long produtoId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Lob
    @Column(nullable = false, length = 16_777_215)
    private byte[] dados;

    protected ImagemProduto() {
    }

    public ImagemProduto(Long produtoId, String contentType, byte[] dados) {
        this.produtoId = produtoId;
        this.contentType = contentType;
        this.dados = dados;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getDados() {
        return dados;
    }
}
