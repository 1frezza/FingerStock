package br.com.fingerstock.controller;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.Produto;
import br.com.fingerstock.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vitrine pública: mostra os produtos do estoque e dá baixa a cada compra. */
@Controller
public class LojaController {

    private final ProdutoRepository repository;

    public LojaController(ProdutoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/loja";
    }

    @GetMapping("/loja")
    public String vitrine(@RequestParam(defaultValue = "") String busca,
                          @RequestParam(required = false) Categoria categoria,
                          Model model) {
        model.addAttribute("produtos", repository.filtrar(busca.trim(), categoria));
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaSelecionada", categoria);
        return "loja/vitrine";
    }

    @GetMapping("/loja/{id}")
    public String produto(@PathVariable Long id, Model model) {
        Produto produto = buscar(id);
        model.addAttribute("produto", produto);
        model.addAttribute("relacionados",
                repository.findTop4ByCategoriaAndIdNotOrderByNome(produto.getCategoria(), id));
        return "loja/produto";
    }

    @PostMapping("/loja/{id}/comprar")
    @Transactional
    public String comprar(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String busca,
                          @RequestParam(required = false) Categoria categoria,
                          @RequestParam(defaultValue = "false") boolean detalhe,
                          RedirectAttributes redirect) {
        Produto produto = buscar(id);

        if (repository.baixarEstoque(id) == 1) {
            redirect.addFlashAttribute("mensagem", "Compra realizada: " + produto.getNome() + ".");
        } else {
            redirect.addFlashAttribute("erro", "\"" + produto.getNome() + "\" acabou de esgotar.");
        }

        // Compra feita na página do produto volta para ela; feita na vitrine, volta para a vitrine filtrada
        if (detalhe) {
            return "redirect:/loja/" + id;
        }
        if (!busca.isBlank()) {
            redirect.addAttribute("busca", busca);
        }
        if (categoria != null) {
            redirect.addAttribute("categoria", categoria);
        }
        return "redirect:/loja";
    }

    private Produto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
