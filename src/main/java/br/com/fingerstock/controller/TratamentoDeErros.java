package br.com.fingerstock.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class TratamentoDeErros {

    /**
     * Arquivo maior que o limite do servidor (spring.servlet.multipart.max-file-size).
     * O erro acontece antes de a requisição chegar ao controller, por isso é tratado aqui.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrandeDemais(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "A imagem enviada é grande demais. O limite é de 2 MB.");
        return "redirect:/produtos";
    }
}
