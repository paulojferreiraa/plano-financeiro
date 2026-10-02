package br.com.portifinanceiro.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.portifinanceiro.application.repository.DespesaRepository;
import br.com.portifinanceiro.application.repository.ReceitaRepository;
import br.com.portifinanceiro.application.repository.TokenRecuperacaoSenhaRepository;
import br.com.portifinanceiro.application.repository.UsuarioRepository;
import br.com.portifinanceiro.application.service.NotificadorRecuperacaoSenha;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndIsolationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ReceitaRepository receitas;

    @Autowired
    private DespesaRepository despesas;

    @Autowired
    private TokenRecuperacaoSenhaRepository tokensRecuperacao;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificadorRecuperacaoSenha notificadorRecuperacao;

    private String csrfToken;
    private Cookie csrfCookie;

    @BeforeEach
    void preparar() throws Exception {
        despesas.deleteAll();
        receitas.deleteAll();
        tokensRecuperacao.deleteAll();
        usuarios.deleteAll();
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString()).get("token").asText();
        csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
    }

    @Test
    void senhaEhArmazenadaComHashELoginCriaSessao() throws Exception {
        MockHttpSession session = criarContaEEntrar("Ana", "ana@example.com");

        org.assertj.core.api.Assertions.assertThat(session).isNotNull();
        org.assertj.core.api.Assertions.assertThat(usuarios.findByEmailIgnoreCase("ana@example.com").orElseThrow().getSenhaHash())
            .isNotEqualTo("SenhaForte123")
            .startsWith("$2");

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void lancamentosFicamIsoladosPorUsuario() throws Exception {
        MockHttpSession sessaoAna = criarContaEEntrar("Ana", "ana@example.com");
        mockMvc.perform(post("/api/lancamentos")
                .session(sessaoAna)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"receita\",\"data\":\"2026-10-05\",\"descricao\":\"Salário\",\"categoria\":\"Trabalho\",\"valor\":2700.00}"))
            .andExpect(status().isCreated());

        MockHttpSession sessaoBruno = criarContaEEntrar("Bruno", "bruno@example.com");
        mockMvc.perform(get("/api/lancamentos").session(sessaoBruno))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        mockMvc.perform(get("/api/lancamentos").session(sessaoAna))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void apiFinanceiraExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/lancamentos"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recuperacaoNaoRevelaContaETokenSoPodeSerUsadoUmaVez() throws Exception {
        String respostaDesconhecida = mockMvc.perform(requisicaoPost(
                "/api/auth/password/forgot",
                "{\"email\":\"naoexiste@example.com\"}"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

        criarContaEEntrar("Ana", "ana@example.com");
        String respostaCadastrada = mockMvc.perform(requisicaoPost(
                "/api/auth/password/forgot",
                "{\"email\":\"ana@example.com\"}"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(respostaCadastrada).isEqualTo(respostaDesconhecida);
        ArgumentCaptor<String> linkCapturado = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(notificadorRecuperacao)
            .enviarLink(org.mockito.Mockito.eq("Ana"), org.mockito.Mockito.eq("ana@example.com"), linkCapturado.capture());
        String token = URI.create(linkCapturado.getValue()).getFragment().substring("recuperar=".length());

        mockMvc.perform(requisicaoPost("/api/auth/password/reset", objectMapper.writeValueAsString(java.util.Map.of(
                "token", token,
                "senha", "NovaSenhaForte456"
            ))))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"senha\":\"SenhaForte123\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(requisicaoPost("/api/auth/password/reset", objectMapper.writeValueAsString(java.util.Map.of(
                "token", token,
                "senha", "OutraSenhaForte789"
            ))))
            .andExpect(status().isBadRequest());
    }

    private MockHttpSession criarContaEEntrar(String nome, String email) throws Exception {
        String senha = "SenhaForte123";
        String cadastro = objectMapper.writeValueAsString(java.util.Map.of(
            "nome", nome,
            "email", email,
            "senha", senha
        ));
        mockMvc.perform(requisicaoPost("/api/auth/register", cadastro))
            .andExpect(status().isCreated());

        String login = objectMapper.writeValueAsString(java.util.Map.of(
            "email", email,
            "senha", senha
        ));
        MvcResult loginResult = mockMvc.perform(requisicaoPost("/api/auth/login", login))
            .andExpect(status().isNoContent())
            .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder requisicaoPost(String path, String body) {
        return post(path)
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }
}
