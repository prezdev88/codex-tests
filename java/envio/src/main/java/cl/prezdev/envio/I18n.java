package cl.prezdev.envio;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class I18n {

    private static final Map<Language, AppTexts> APP_TEXTS = new EnumMap<>(Language.class);
    private static final Map<Language, PanelTexts> PANEL_TEXTS = new EnumMap<>(Language.class);

    static {
        APP_TEXTS.put(Language.ES, new AppTexts(
                "Visor de Cliente HTTP",
                "Ver",
                "Normal",
                "Mediano",
                "Grande",
                "Configuración",
                "Editar ajustes...",
                "Idioma",
                "Español",
                "Inglés"
        ));
        APP_TEXTS.put(Language.EN, new AppTexts(
                "HTTP Client Viewer",
                "View",
                "Normal",
                "Medium",
                "Large",
                "Settings",
                "Edit preferences...",
                "Language",
                "Spanish",
                "English"
        ));

        PANEL_TEXTS.put(Language.ES, new PanelTexts(
                "Método",
                "URL",
                "Cuerpo (JSON opcional)",
                "Enviar",
                "Listo",
                "Llamando al endpoint...",
                "La URL es obligatoria",
                "Operación completada",
                detalle -> "Error ejecutando la petición: " + detalle,
                "Árbol JSON",
                "JSON formateado",
                "Request crudo",
                "Response crudo",
                "Sin datos",
                "No es JSON válido",
                "Objeto",
                "Arreglo"
        ));

        PANEL_TEXTS.put(Language.EN, new PanelTexts(
                "Method",
                "URL",
                "Body (optional JSON)",
                "Send",
                "Ready",
                "Calling endpoint...",
                "URL is required",
                "Operation completed",
                detail -> "Error executing request: " + detail,
                "JSON tree",
                "Formatted JSON",
                "Raw request",
                "Raw response",
                "No data",
                "Invalid JSON",
                "Object",
                "Array"
        ));
    }

    private I18n() {
    }

    public static AppTexts app(Language language) {
        return APP_TEXTS.getOrDefault(language, APP_TEXTS.get(Language.ES));
    }

    public static PanelTexts panel(Language language) {
        return PANEL_TEXTS.getOrDefault(language, PANEL_TEXTS.get(Language.ES));
    }

    public record AppTexts(
            String windowTitle,
            String viewMenu,
            String viewScaleNormal,
            String viewScaleMedium,
            String viewScaleLarge,
            String settingsMenu,
            String editSettings,
            String languageMenu,
            String languageSpanish,
            String languageEnglish
    ) {
    }

    public record PanelTexts(
            String methodLabel,
            String urlLabel,
            String bodyLabel,
            String sendButton,
            String statusReady,
            String statusCalling,
            String statusUrlRequired,
            String statusCompleted,
            java.util.function.Function<String, String> statusErrorWithDetail,
            String tabJsonTree,
            String tabJsonFormatted,
            String tabRawRequest,
            String tabRawResponse,
            String jsonTreeNoData,
            String jsonTreeInvalid,
            String jsonTreeObject,
            String jsonTreeArray
    ) {
        public String statusErrorWithDetail(String detail) {
            return statusErrorWithDetail.apply(detail);
        }
    }
}
