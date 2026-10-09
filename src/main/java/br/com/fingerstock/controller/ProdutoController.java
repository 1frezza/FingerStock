package br.com.fingerstock.controller;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.Produto;
import br.com.fingerstock.repository.ProdutoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProdutoController {

    private final ProdutoRepository repository;

    public ProdutoController(ProdutoRepository repository) {
        this.repository = repository;
    }

    @ModelAttribute("categorias")
    Categoria[] categorias() {
        return Categoria.values();
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/produtos";
    }

    @GetMapping("/produtos")
    public String listar(@RequestParam(defaultValue = "") String busca,
                         @RequestParam(required = false) Categoria categoria,
                         Model model) {
        List<Produto> produtos = repository.filtrar(busca.trim(), categoria);

        int totalItens = produtos.stream().mapToInt(Produto::getQuantidade).sum();
        BigDecimal valorTotal = produtos.stream()
                .map(p -> p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long estoqueBaixo = produtos.stream().filter(Produto::isEstoqueBaixo).count();

        model.addAttribute("produtos", produtos);
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaSelecionada", categoria);
        model.addAttribute("totalItens", totalItens);
        model.addAttribute("valorTotal", valorTotal);
        model.addAttribute("estoqueBaixo", estoqueBaixo);
        return "produtos/lista";
    }

    @GetMapping("/produtos/novo")
    public String novo(Model model) {
        model.addAttribute("produto", new Produto());
        return "produtos/form";
    }

    @PostMapping("/produtos")
    public String criar(@Valid @ModelAttribute("produto") Produto produto,
                        BindingResult erros,
                        RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return "produtos/form";
        }
        produto.setId(null);
        repository.save(produto);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" cadastrado.");
        return "redirect:/produtos";
    }

    @GetMapping("/produtos/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("produto", buscar(id));
        return "produtos/form";
    }

    @PostMapping("/produtos/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("produto") Produto form,
                            BindingResult erros,
                            RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            form.setId(id);
            return "produtos/form";
        }
        Produto produto = buscar(id);
        produto.setNome(form.getNome());
        produto.setCategoria(form.getCategoria());
        produto.setMarca(form.getMarca());
        produto.setPreco(form.getPreco());
        produto.setQuantidade(form.getQuantidade());
        produto.setDescricao(form.getDescricao());
        produto.setImagemUrl(form.getImagemUrl());
        repository.save(produto);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" atualizado.");
        return "redirect:/produtos";
    }

    @PostMapping("/produtos/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        Produto produto = buscar(id);
        repository.delete(produto);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" excluído.");
        return "redirect:/produtos";
    }

    private Produto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
