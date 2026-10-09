package br.com.fingerstock.config;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.Produto;
import br.com.fingerstock.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Popula o estoque com alguns produtos de exemplo quando a tabela está vazia. */
@Component
public class DadosIniciais implements CommandLineRunner {

    private final ProdutoRepository repository;

    public DadosIniciais(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Produto("Fingerboard Completo Pro 34mm", Categoria.SKATE_COMPLETO, "Tech Deck",
                        new BigDecimal("89.90"), 18, "Skate de dedo montado, shape 34mm com lixa de espuma."),
                new Produto("Fingerboard Completo Iniciante 29mm", Categoria.SKATE_COMPLETO, "Tech Deck",
                        new BigDecimal("39.90"), 42, "Modelo de entrada com shape plástico."),
                new Produto("Shape Maple 5 Lâminas 33mm", Categoria.SHAPE, "Berlinwood",
                        new BigDecimal("219.00"), 6, "Shape de madeira maple, concave médio."),
                new Produto("Shape Maple 34mm Popsicle", Categoria.SHAPE, "Blackriver",
                        new BigDecimal("249.00"), 4, "Shape profissional, formato popsicle."),
                new Produto("Truck Pro 32mm Prata", Categoria.TRUCK, "Blackriver",
                        new BigDecimal("329.00"), 3, "Par de trucks 32mm com bushings e pivot cups."),
                new Produto("Truck 34mm Preto", Categoria.TRUCK, "Dynamic",
                        new BigDecimal("159.90"), 9, "Par de trucks 34mm, kingpin invertido."),
                new Produto("Rodinhas Uretano com Rolamento 7.5mm", Categoria.RODINHA, "FlatFace",
                        new BigDecimal("179.00"), 12, "Jogo com 4 rodinhas de uretano, rolamento duplo."),
                new Produto("Rodinhas Plásticas Coloridas", Categoria.RODINHA, "Genérica",
                        new BigDecimal("14.90"), 60, "Jogo com 4 rodinhas de plástico."),
                new Produto("Lixa de Espuma Riptape Classic", Categoria.LIXA, "Riptape",
                        new BigDecimal("34.90"), 35, "Tape de espuma 1mm, 1 folha."),
                new Produto("Corrimão Quadrado 30cm", Categoria.RAMPA_OBSTACULO, "Blackriver Ramps",
                        new BigDecimal("149.00"), 5, "Rail de metal com base antiderrapante."),
                new Produto("Mini Ramp Half Pipe MDF", Categoria.RAMPA_OBSTACULO, "FingerPark",
                        new BigDecimal("289.00"), 2, "Mini ramp em MDF com coping de metal."),
                new Produto("Chave Multiuso para Fingerboard", Categoria.FERRAMENTA, "Tech Deck",
                        new BigDecimal("19.90"), 25, "Chave philips + chave de porca."),
                new Produto("Kit Parafusos e Porcas", Categoria.ACESSORIO, "Dynamic",
                        new BigDecimal("24.90"), 0, "8 parafusos e 4 lock nuts.")
        ));
    }
}
