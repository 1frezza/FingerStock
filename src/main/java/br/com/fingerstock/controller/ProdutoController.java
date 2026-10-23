package br.com.fingerstock.controller;

import br.com.fingerstock.model.Categoria;
import br.com.fingerstock.model.ImagemProduto;
import br.com.fingerstock.model.Produto;
import br.com.fingerstock.repository.ImagemProdutoRepository;
import br.com.fingerstock.repository.ProdutoRepository;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Controller
public class ProdutoController {

    private static final Set<String> TIPOS_DE_IMAGEM = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final long TAMANHO_MAXIMO_IMAGEM = 2 * 1024 * 1024;

    private final ProdutoRepository repository;
    private final ImagemProdutoRepository imagens;

    public ProdutoController(ProdutoRepository repository, ImagemProdutoRepository imagens) {
        this.repository = repository;
        this.imagens = imagens;
    }

    @ModelAttribute("categorias")
    Categoria[] categorias() {
        return Categoria.values();
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
    @Transactional
    public String criar(@Valid @ModelAttribute("produto") Produto produto,
                        BindingResult erros,
                        @RequestParam(required = false) MultipartFile arquivo,
                        Model model,
                        RedirectAttributes redirect) throws IOException {
        String erroImagem = validarImagem(arquivo);
        if (erros.hasErrors() || erroImagem != null) {
            model.addAttribute("erroImagem", erroImagem);
            return "produtos/form";
        }
        produto.setId(null);
        produto.setImagemVersao(null);
        repository.save(produto);
        salvarImagem(produto, arquivo);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" cadastrado.");
        return "redirect:/produtos";
    }

    @GetMapping("/produtos/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("produto", buscar(id));
        return "produtos/form";
    }

    @PostMapping("/produtos/{id}")
    @Transactional
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("produto") Produto form,
                            BindingResult erros,
                            @RequestParam(required = false) MultipartFile arquivo,
                            @RequestParam(defaultValue = "false") boolean removerImagem,
                            Model model,
                            RedirectAttributes redirect) throws IOException {
        Produto produto = buscar(id);
        String erroImagem = validarImagem(arquivo);
        if (erros.hasErrors() || erroImagem != null) {
            form.setId(id);
            form.setImagemVersao(produto.getImagemVersao());
            model.addAttribute("erroImagem", erroImagem);
            return "produtos/form";
        }
        produto.setNome(form.getNome());
        produto.setCategoria(form.getCategoria());
        produto.setMarca(form.getMarca());
        produto.setPreco(form.getPreco());
        produto.setQuantidade(form.getQuantidade());
        produto.setDescricao(form.getDescricao());
        if (temArquivo(arquivo)) {
            salvarImagem(produto, arquivo);
        } else if (removerImagem && produto.getImagemVersao() != null) {
            imagens.deleteById(id);
            produto.setImagemVersao(null);
        }
        repository.save(produto);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" atualizado.");
        return "redirect:/produtos";
    }

    @PostMapping("/produtos/{id}/excluir")
    @Transactional
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        Produto produto = buscar(id);
        if (produto.getImagemVersao() != null) {
            imagens.deleteById(id);
        }
        repository.delete(produto);
        redirect.addFlashAttribute("mensagem", "Produto \"" + produto.getNome() + "\" excluído.");
        return "redirect:/produtos";
    }

    @GetMapping("/produtos/{id}/imagem")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> imagem(@PathVariable Long id) {
        ImagemProduto imagem = imagens.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada"));
        // A URL leva ?v=<versão>, então o navegador pode guardar a imagem por bastante tempo
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem.getDados());
    }

    private static boolean temArquivo(MultipartFile arquivo) {
        return arquivo != null && !arquivo.isEmpty();
    }

    private static String validarImagem(MultipartFile arquivo) {
        if (!temArquivo(arquivo)) {
            return null;
        }
        if (!TIPOS_DE_IMAGEM.contains(arquivo.getContentType())) {
            return "Envie uma imagem PNG, JPG, WEBP ou GIF.";
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_IMAGEM) {
            return "A imagem deve ter no máximo 2 MB.";
        }
        return null;
    }

    private void salvarImagem(Produto produto, MultipartFile arquivo) throws IOException {
        if (!temArquivo(arquivo)) {
            return;
        }
        imagens.save(new ImagemProduto(produto.getId(), arquivo.getContentType(), arquivo.getBytes()));
        produto.setImagemVersao(System.currentTimeMillis());
    }

    private Produto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
    }
}
