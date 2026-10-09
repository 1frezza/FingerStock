package br.com.fingerstock.model;

public enum Categoria {
    SKATE_COMPLETO("Skate completo"),
    SHAPE("Shape"),
    TRUCK("Truck"),
    RODINHA("Rodinha"),
    LIXA("Lixa / Tape"),
    RAMPA_OBSTACULO("Rampa / Obstáculo"),
    FERRAMENTA("Ferramenta"),
    ACESSORIO("Acessório");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Ilustração padrão da categoria, usada quando o produto não tem imagem própria. */
    public String getImagem() {
        return "/img/categorias/" + name().toLowerCase().replace('_', '-') + ".svg";
    }
}
