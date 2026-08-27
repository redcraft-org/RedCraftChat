package org.redcraft.redcraftchat.locales;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Hand-written translations for the interface's own words.
 *
 * Everything else on the network is player prose, which has to be translated
 * as it arrives. These are not: they are a fixed, small set that never
 * changes between messages, and sending them through a translator every time
 * buys nothing and costs plenty. A machine translating one line with nothing
 * around it has no idea it is labelling a button, so "Close" can come back as
 * "near" and a sentence ending in a pronoun loses what the pronoun pointed
 * at. It also costs a round trip the first time each string is seen, which on
 * a menu is a visible stall.
 *
 * Anything absent here falls through to the runtime translator exactly as
 * before, so this is a quality and latency shortcut, never a requirement.
 */
public final class UiTranslations {

    private static final Map<String, Map<String, String>> BY_LANGUAGE = build();

    private UiTranslations() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * The translation of an English interface string, or null when there is
     * none and the caller should translate it the usual way.
     *
     * @param locale a full locale code such as "fr-FR"; the region is
     * ignored, since these words do not differ between regions of a language
     */
    public static String lookup(String message, String locale) {
        if (message == null || locale == null) {
            return null;
        }
        String language = locale.toLowerCase(Locale.ROOT).split("[-_]")[0];
        Map<String, String> table = BY_LANGUAGE.get(language);
        return table == null ? null : table.get(message);
    }

    /** Visible for tests: every language with an embedded table. */
    public static java.util.Set<String> languages() {
        return BY_LANGUAGE.keySet();
    }

    /** Visible for tests: the English strings covered for a language. */
    public static java.util.Set<String> covered(String language) {
        Map<String, String> table = BY_LANGUAGE.get(language);
        return table == null ? Collections.emptySet() : table.keySet();
    }

    private static Map<String, Map<String, String>> build() {
        Map<String, Map<String, String>> all = new HashMap<>();

        Map<String, String> fr = new HashMap<>();
        fr.put(UiStrings.SELECTOR_PRIMARY_TITLE, "Quelle est votre langue principale ?");
        fr.put(UiStrings.SELECTOR_OTHERS_TITLE, "Comprenez-vous d'autres langues ?");
        fr.put(UiStrings.SELECTOR_PRIMARY_HELP, "Tout le chat sera traduit dans la langue choisie");
        fr.put(UiStrings.SELECTOR_OTHERS_HELP, "Les messages dans les langues cochées ne seront pas traduits");
        fr.put(UiStrings.SELECTOR_SUBMIT, "Valider");
        fr.put(UiStrings.CHANGE_MAIN_LANGUAGE, "Changer de langue principale");
        fr.put(UiStrings.SELECTOR_CONTINUE, "Continuer");
        fr.put(UiStrings.SELECTOR_NEXT, "Suivant");
        fr.put(UiStrings.SELECTOR_PREVIOUS, "Précédent");
        fr.put(UiStrings.SELECTOR_BACK, "Retour");
        fr.put(UiStrings.SELECTOR_CLOSE, "Fermer le menu");
        fr.put(UiStrings.SELECTOR_DONE, "Terminé");
        fr.put(UiStrings.SELECTOR_CONFIRMED, "Langue confirmée, amusez-vous bien !");
        all.put("fr", fr);

        Map<String, String> es = new HashMap<>();
        es.put(UiStrings.SELECTOR_PRIMARY_TITLE, "¿Cuál es tu idioma principal?");
        es.put(UiStrings.SELECTOR_OTHERS_TITLE, "¿Entiendes otros idiomas?");
        es.put(UiStrings.SELECTOR_PRIMARY_HELP, "Todo el chat se traducirá al idioma que elijas");
        es.put(UiStrings.SELECTOR_OTHERS_HELP, "Los mensajes en los idiomas marcados no se traducirán");
        es.put(UiStrings.SELECTOR_SUBMIT, "Confirmar");
        es.put(UiStrings.CHANGE_MAIN_LANGUAGE, "Cambiar idioma principal");
        es.put(UiStrings.SELECTOR_CONTINUE, "Continuar");
        es.put(UiStrings.SELECTOR_NEXT, "Siguiente");
        es.put(UiStrings.SELECTOR_PREVIOUS, "Anterior");
        es.put(UiStrings.SELECTOR_BACK, "Volver");
        es.put(UiStrings.SELECTOR_CLOSE, "Cerrar menú");
        es.put(UiStrings.SELECTOR_DONE, "Listo");
        es.put(UiStrings.SELECTOR_CONFIRMED, "Idioma confirmado, ¡diviértete!");
        all.put("es", es);

        Map<String, String> de = new HashMap<>();
        de.put(UiStrings.SELECTOR_PRIMARY_TITLE, "Was ist deine Hauptsprache?");
        de.put(UiStrings.SELECTOR_OTHERS_TITLE, "Verstehst du andere Sprachen?");
        de.put(UiStrings.SELECTOR_PRIMARY_HELP, "Der gesamte Chat wird in die gewählte Sprache übersetzt");
        de.put(UiStrings.SELECTOR_OTHERS_HELP, "Nachrichten in den markierten Sprachen werden nicht übersetzt");
        de.put(UiStrings.SELECTOR_SUBMIT, "Bestätigen");
        de.put(UiStrings.CHANGE_MAIN_LANGUAGE, "Hauptsprache ändern");
        de.put(UiStrings.SELECTOR_CONTINUE, "Weiter");
        de.put(UiStrings.SELECTOR_NEXT, "Weiter");
        de.put(UiStrings.SELECTOR_PREVIOUS, "Zurück");
        de.put(UiStrings.SELECTOR_BACK, "Zurück");
        de.put(UiStrings.SELECTOR_CLOSE, "Menü schließen");
        de.put(UiStrings.SELECTOR_DONE, "Fertig");
        de.put(UiStrings.SELECTOR_CONFIRMED, "Sprache bestätigt, viel Spaß!");
        all.put("de", de);

        Map<String, String> it = new HashMap<>();
        it.put(UiStrings.SELECTOR_PRIMARY_TITLE, "Qual è la tua lingua principale?");
        it.put(UiStrings.SELECTOR_OTHERS_TITLE, "Capisci altre lingue?");
        it.put(UiStrings.SELECTOR_PRIMARY_HELP, "Tutta la chat sarà tradotta nella lingua scelta");
        it.put(UiStrings.SELECTOR_OTHERS_HELP, "I messaggi nelle lingue selezionate non saranno tradotti");
        it.put(UiStrings.SELECTOR_SUBMIT, "Conferma");
        it.put(UiStrings.CHANGE_MAIN_LANGUAGE, "Cambia lingua principale");
        it.put(UiStrings.SELECTOR_CONTINUE, "Continua");
        it.put(UiStrings.SELECTOR_NEXT, "Avanti");
        it.put(UiStrings.SELECTOR_PREVIOUS, "Precedente");
        it.put(UiStrings.SELECTOR_BACK, "Indietro");
        it.put(UiStrings.SELECTOR_CLOSE, "Chiudi menu");
        it.put(UiStrings.SELECTOR_DONE, "Fatto");
        it.put(UiStrings.SELECTOR_CONFIRMED, "Lingua confermata, buon divertimento!");
        all.put("it", it);

        Map<String, String> ru = new HashMap<>();
        ru.put(UiStrings.SELECTOR_PRIMARY_TITLE, "Какой у вас основной язык?");
        ru.put(UiStrings.SELECTOR_OTHERS_TITLE, "Вы понимаете другие языки?");
        ru.put(UiStrings.SELECTOR_PRIMARY_HELP, "Весь чат будет переведён на выбранный язык");
        ru.put(UiStrings.SELECTOR_OTHERS_HELP, "Сообщения на отмеченных языках не будут переводиться");
        ru.put(UiStrings.SELECTOR_SUBMIT, "Подтвердить");
        ru.put(UiStrings.CHANGE_MAIN_LANGUAGE, "Сменить основной язык");
        ru.put(UiStrings.SELECTOR_CONTINUE, "Продолжить");
        ru.put(UiStrings.SELECTOR_NEXT, "Далее");
        ru.put(UiStrings.SELECTOR_PREVIOUS, "Назад");
        ru.put(UiStrings.SELECTOR_BACK, "Назад");
        ru.put(UiStrings.SELECTOR_CLOSE, "Закрыть меню");
        ru.put(UiStrings.SELECTOR_DONE, "Готово");
        ru.put(UiStrings.SELECTOR_CONFIRMED, "Язык подтверждён, приятной игры!");
        all.put("ru", ru);

        return all;
    }
}
