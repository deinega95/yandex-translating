package dise.yandextranslate.db;


//класс для представления одного перевода

public class ElementHistoryOrFavorite {

    private String text;
    private String translatingText;
    private String codeTranslating;
    private Integer id;

    public ElementHistoryOrFavorite(Integer id, String text, String translatingText, String codeTranslating) {
        this.id = id;
        this.text = text;
        this.translatingText = translatingText;
        this.codeTranslating = codeTranslating;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTranslatingText() {
        return translatingText;
    }

    public void setTranslatingText(String translatingText) {
        this.translatingText = translatingText;
    }

    public String getCodeTranslating() {
        return codeTranslating;
    }

    public void setCodeTranslating(String codeTranslating) {
        this.codeTranslating = codeTranslating;
    }
}