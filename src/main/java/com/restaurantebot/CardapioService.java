package com.restaurantebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo do restaurante. Centraliza tudo o que é "mockado/configurável":
 *   - imagem do cardápio (Google Drive ou fallback local);
 *   - tamanhos e preços base das marmitas;
 *   - opções de cada categoria da marmita e seus valores extras;
 *   - bebidas disponíveis.
 *
 * Para alterar valores/itens basta editar os métodos abaixo — nenhum outro
 * arquivo precisa ser tocado. Quando houver uma API/Drive externos, é aqui
 * que a integração entra.
 */
public class CardapioService {

    private static final Logger log = LoggerFactory.getLogger(CardapioService.class);

    // ── Imagem do cardápio ────────────────────────────────────────────────────
    /**
     * Retorna um {@link InputFile} pronto para envio ao Telegram com a imagem
     * do cardápio. Se {@link Config#DRIVE_CARDAPIO_FILE_ID} estiver preenchido,
     * baixa do Google Drive via URL pública; caso contrário, usa o arquivo
     * local definido em {@link Config#CARDAPIO_LOCAL_PATH}.
     */
    public InputFile buscarImagemCardapio() {
        String fileId = Config.DRIVE_CARDAPIO_FILE_ID;

        if (fileId != null && !fileId.isBlank()) {
            try {
                String url = montarUrlDownloadDrive(fileId);
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                    InputStream stream = new BufferedInputStream(conn.getInputStream());
                    return new InputFile(stream, "cardapio.jpg");
                } else {
                    log.warn("Falha ao baixar cardapio do Drive (HTTP {}). Usando fallback local.",
                            conn.getResponseCode());
                }
            } catch (Exception e) {
                log.warn("Erro ao baixar cardapio do Drive ({}). Usando fallback local.",
                        e.getMessage());
            }
        }

        File imagem = new File(Config.CARDAPIO_LOCAL_PATH);
        return new InputFile(imagem);
    }

    // ── Tamanhos da marmita ───────────────────────────────────────────────────
    /** Tamanhos disponíveis. Edite aqui para mudar preços ou adicionar tamanhos. */
    public List<ItemCardapio> buscarTamanhosMarmita() {
        return List.of(
                new ItemCardapio("Marmita pequena", 18.00),
                new ItemCardapio("Marmita média",   22.00),
                new ItemCardapio("Marmita grande",  28.00)
        );
    }

    // ── Opções por categoria ──────────────────────────────────────────────────
    private static final Map<Categoria, List<OpcaoCategoria>> OPCOES =
            new EnumMap<>(Categoria.class);

    static {
        OPCOES.put(Categoria.ARROZ, List.of(
                new OpcaoCategoria("Arroz branco",    0.00),
                new OpcaoCategoria("Arroz temperado", 0.00)
        ));
        OPCOES.put(Categoria.FEIJAO, List.of(
                new OpcaoCategoria("Feijão tropeiro", 0.00),
                new OpcaoCategoria("Feijão de caldo", 0.00),
                new OpcaoCategoria("Baião",           0.00)
        ));
        OPCOES.put(Categoria.MACARRAO, List.of(
                new OpcaoCategoria("Alho e óleo", 0.00)
        ));
        OPCOES.put(Categoria.LEGUMES, List.of(
                new OpcaoCategoria("Repolho com cenoura", 0.00),
                new OpcaoCategoria("Salada crua",         0.00),
                new OpcaoCategoria("Beterraba",           0.00),
                new OpcaoCategoria("Batata doce",         0.00),
                new OpcaoCategoria("Macaxeira",           0.00)
        ));
        OPCOES.put(Categoria.CARNES, List.of(
                new OpcaoCategoria("Frango assado",        0.00),
                new OpcaoCategoria("Linguiça assada",      0.00),
                new OpcaoCategoria("Cupim assado",         0.00),
                new OpcaoCategoria("Galinha cozida",       0.00),
                new OpcaoCategoria("Frango à milanesa",    0.00),
                new OpcaoCategoria("Frango à parmegiana",  2.00)
        ));
        OPCOES.put(Categoria.ADICIONAIS, List.of(
                new OpcaoCategoria("Farofa",          0.00),
                new OpcaoCategoria("Bolinho de arroz", 0.00),
                new OpcaoCategoria("Batata frita",     5.00),
                new OpcaoCategoria("Ovo frito",        2.00)
        ));
    }

    /** Opções disponíveis para a categoria informada. Edite o map estático acima para alterar. */
    public List<OpcaoCategoria> buscarOpcoesCategoria(Categoria categoria) {
        return OPCOES.get(categoria);
    }

    // ── Bebidas ───────────────────────────────────────────────────────────────
    /** Bebidas disponíveis. Edite aqui para mudar preços ou catálogo. */
    public List<ItemCardapio> buscarBebidas() {
        return List.of(
                new ItemCardapio("Refrigerante lata",  6.00),
                new ItemCardapio("Refrigerante 1L",   10.00),
                new ItemCardapio("Suco natural",       8.00),
                new ItemCardapio("Água mineral",       3.00)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String montarUrlDownloadDrive(String fileId) {
        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }
}
