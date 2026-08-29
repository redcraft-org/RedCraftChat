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
        fr.put(UiStrings.MAIL_INBOX_HEADER, "BOÎTE DE RÉCEPTION");
        fr.put(UiStrings.MAIL_NO_MAILS, "Vous n'avez aucun message.");
        fr.put(UiStrings.MAIL_SEND_TITLE, "Écrire un mail");
        fr.put(UiStrings.MAIL_MESSAGE, "Message");
        fr.put(UiStrings.MAIL_REPLY, "Répondre");
        fr.put(UiStrings.MAIL_CLICK_TO_REPLY, "Cliquez pour répondre");
        fr.put(UiStrings.MAIL_MARK_AS_READ, "Marquer comme lu");
        fr.put(UiStrings.MAIL_ALREADY_READ, "Déjà marqué comme lu");
        fr.put(UiStrings.MAIL_NEXT_PAGE, "Page suivante");
        fr.put(UiStrings.MAIL_PREVIOUS_PAGE, "Page précédente");
        fr.put(UiStrings.MAIL_NEXT_PAGE_HOVER, "Cliquez pour aller à la page suivante");
        fr.put(UiStrings.MAIL_PREVIOUS_PAGE_HOVER, "Cliquez pour aller à la page précédente");
        fr.put(UiStrings.MAIL_UNKNOWN_SLOT, "Ce message n'est pas sur la page affichée");
        fr.put(UiStrings.MAIL_REPLY_USAGE, "Utilisez /mail reply <numéro> <message>");
        fr.put(UiStrings.MAIL_HOVER_TIP, "Tapez /mail show <numéro> pour lire un message en entier");
        fr.put(UiStrings.MAIL_TITLE, "Courrier");
        fr.put(UiStrings.MAIL_OPEN_INBOX, "Boîte de réception");
        fr.put(UiStrings.MAIL_UNREAD_COUNT, "%count% non lus");
        fr.put(UiStrings.MAIL_ALL_READ, "Tout est lu");
        fr.put(UiStrings.MAIL_FROM, "de");
        fr.put(UiStrings.MAIL_SENT_TO, "Mail envoyé à %player%");
        fr.put(UiStrings.MAIL_NO_RECIPIENT, "Aucun destinataire indiqué");
        fr.put(UiStrings.MAIL_PLAYER_NOT_FOUND, "Ce joueur est introuvable");
        fr.put(UiStrings.MAIL_RECIPIENT_NAME, "Nom du joueur");
        fr.put(UiStrings.SERVERS_TITLE, "Serveurs");
        fr.put(UiStrings.SERVERS_HELP, "Choisissez où vous voulez aller");
        fr.put(UiStrings.SERVERS_RETURN, "Retour sur %server%");
        fr.put(UiStrings.SERVERS_YOU_ARE_HERE, "Vous êtes ici");
        fr.put(UiStrings.SERVERS_NONE, "Il n'y a nulle part où aller pour le moment");
        fr.put(UiStrings.SERVERS_GONE, "Ce serveur n'est pas disponible");
        fr.put(UiStrings.SERVERS_DEFAULT_TITLE, "Serveur par défaut à la connexion");
        fr.put(UiStrings.SERVERS_DEFAULT_HELP, "Où vous placer quand vous vous connectez à %network%");
        fr.put(UiStrings.SERVERS_DEFAULT_BUTTON, "Connexion : %server%");
        fr.put(UiStrings.SERVERS_DEFAULT_LAST, "Dernier serveur");
        fr.put(UiStrings.SERVERS_DEFAULT_SAVED, "Vous arriverez sur %server% désormais");
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
        es.put(UiStrings.MAIL_INBOX_HEADER, "BANDEJA DE ENTRADA");
        es.put(UiStrings.MAIL_NO_MAILS, "No tienes mensajes.");
        es.put(UiStrings.MAIL_SEND_TITLE, "Escribir un mensaje");
        es.put(UiStrings.MAIL_MESSAGE, "Mensaje");
        es.put(UiStrings.MAIL_REPLY, "Responder");
        es.put(UiStrings.MAIL_CLICK_TO_REPLY, "Haz clic para responder");
        es.put(UiStrings.MAIL_MARK_AS_READ, "Marcar como leído");
        es.put(UiStrings.MAIL_ALREADY_READ, "Ya marcado como leído");
        es.put(UiStrings.MAIL_NEXT_PAGE, "Página siguiente");
        es.put(UiStrings.MAIL_PREVIOUS_PAGE, "Página anterior");
        es.put(UiStrings.MAIL_NEXT_PAGE_HOVER, "Haz clic para ir a la página siguiente");
        es.put(UiStrings.MAIL_PREVIOUS_PAGE_HOVER, "Haz clic para ir a la página anterior");
        es.put(UiStrings.MAIL_UNKNOWN_SLOT, "Ese mensaje no está en la página que estás viendo");
        es.put(UiStrings.MAIL_REPLY_USAGE, "Usa /mail reply <número> <mensaje>");
        es.put(UiStrings.MAIL_HOVER_TIP, "Escribe /mail show <número> para leer un mensaje entero");
        es.put(UiStrings.MAIL_TITLE, "Correo");
        es.put(UiStrings.MAIL_OPEN_INBOX, "Bandeja de entrada");
        es.put(UiStrings.MAIL_UNREAD_COUNT, "%count% sin leer");
        es.put(UiStrings.MAIL_ALL_READ, "Todo leído");
        es.put(UiStrings.MAIL_FROM, "de");
        es.put(UiStrings.MAIL_SENT_TO, "Mensaje enviado a %player%");
        es.put(UiStrings.MAIL_NO_RECIPIENT, "No has indicado un destinatario");
        es.put(UiStrings.MAIL_PLAYER_NOT_FOUND, "No se encontró a ese jugador");
        es.put(UiStrings.MAIL_RECIPIENT_NAME, "Nombre del jugador");
        es.put(UiStrings.SERVERS_TITLE, "Servidores");
        es.put(UiStrings.SERVERS_HELP, "Elige a dónde quieres ir");
        es.put(UiStrings.SERVERS_RETURN, "Volver a %server%");
        es.put(UiStrings.SERVERS_YOU_ARE_HERE, "Estás aquí");
        es.put(UiStrings.SERVERS_NONE, "No hay ningún otro sitio al que ir ahora mismo");
        es.put(UiStrings.SERVERS_GONE, "Ese servidor no está disponible");
        es.put(UiStrings.SERVERS_DEFAULT_TITLE, "Servidor por defecto al entrar");
        es.put(UiStrings.SERVERS_DEFAULT_HELP, "Dónde ponerte cuando te conectas a %network%");
        es.put(UiStrings.SERVERS_DEFAULT_BUTTON, "Entrada: %server%");
        es.put(UiStrings.SERVERS_DEFAULT_LAST, "Último servidor");
        es.put(UiStrings.SERVERS_DEFAULT_SAVED, "A partir de ahora entrarás en %server%");
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
        de.put(UiStrings.MAIL_INBOX_HEADER, "POSTEINGANG");
        de.put(UiStrings.MAIL_NO_MAILS, "Du hast keine Nachrichten.");
        de.put(UiStrings.MAIL_SEND_TITLE, "Nachricht schreiben");
        de.put(UiStrings.MAIL_MESSAGE, "Nachricht");
        de.put(UiStrings.MAIL_REPLY, "Antworten");
        de.put(UiStrings.MAIL_CLICK_TO_REPLY, "Zum Antworten klicken");
        de.put(UiStrings.MAIL_MARK_AS_READ, "Als gelesen markieren");
        de.put(UiStrings.MAIL_ALREADY_READ, "Bereits als gelesen markiert");
        de.put(UiStrings.MAIL_NEXT_PAGE, "Nächste Seite");
        de.put(UiStrings.MAIL_PREVIOUS_PAGE, "Vorherige Seite");
        de.put(UiStrings.MAIL_NEXT_PAGE_HOVER, "Klicken für die nächste Seite");
        de.put(UiStrings.MAIL_PREVIOUS_PAGE_HOVER, "Klicken für die vorherige Seite");
        de.put(UiStrings.MAIL_UNKNOWN_SLOT, "Diese Nachricht ist nicht auf der angezeigten Seite");
        de.put(UiStrings.MAIL_REPLY_USAGE, "Nutze /mail reply <Nummer> <Nachricht>");
        de.put(UiStrings.MAIL_HOVER_TIP, "Tippe /mail show <Nummer>, um eine Nachricht ganz zu lesen");
        de.put(UiStrings.MAIL_TITLE, "Post");
        de.put(UiStrings.MAIL_OPEN_INBOX, "Posteingang");
        de.put(UiStrings.MAIL_UNREAD_COUNT, "%count% ungelesen");
        de.put(UiStrings.MAIL_ALL_READ, "Alles gelesen");
        de.put(UiStrings.MAIL_FROM, "von");
        de.put(UiStrings.MAIL_SENT_TO, "Nachricht an %player% gesendet");
        de.put(UiStrings.MAIL_NO_RECIPIENT, "Kein Empfänger angegeben");
        de.put(UiStrings.MAIL_PLAYER_NOT_FOUND, "Dieser Spieler wurde nicht gefunden");
        de.put(UiStrings.MAIL_RECIPIENT_NAME, "Spielername");
        de.put(UiStrings.SERVERS_TITLE, "Server");
        de.put(UiStrings.SERVERS_HELP, "Wähle, wohin du möchtest");
        de.put(UiStrings.SERVERS_RETURN, "Zurück zu %server%");
        de.put(UiStrings.SERVERS_YOU_ARE_HERE, "Du bist hier");
        de.put(UiStrings.SERVERS_NONE, "Im Moment gibt es keinen anderen Ort");
        de.put(UiStrings.SERVERS_GONE, "Dieser Server ist nicht verfügbar");
        de.put(UiStrings.SERVERS_DEFAULT_TITLE, "Standardserver beim Anmelden");
        de.put(UiStrings.SERVERS_DEFAULT_HELP, "Wohin du kommst, wenn du dich mit %network% verbindest");
        de.put(UiStrings.SERVERS_DEFAULT_BUTTON, "Anmeldung: %server%");
        de.put(UiStrings.SERVERS_DEFAULT_LAST, "Letzter Server");
        de.put(UiStrings.SERVERS_DEFAULT_SAVED, "Du landest ab jetzt auf %server%");
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
        it.put(UiStrings.MAIL_INBOX_HEADER, "POSTA IN ARRIVO");
        it.put(UiStrings.MAIL_NO_MAILS, "Non hai messaggi.");
        it.put(UiStrings.MAIL_SEND_TITLE, "Scrivi un messaggio");
        it.put(UiStrings.MAIL_MESSAGE, "Messaggio");
        it.put(UiStrings.MAIL_REPLY, "Rispondi");
        it.put(UiStrings.MAIL_CLICK_TO_REPLY, "Clicca per rispondere");
        it.put(UiStrings.MAIL_MARK_AS_READ, "Segna come letto");
        it.put(UiStrings.MAIL_ALREADY_READ, "Già segnato come letto");
        it.put(UiStrings.MAIL_NEXT_PAGE, "Pagina successiva");
        it.put(UiStrings.MAIL_PREVIOUS_PAGE, "Pagina precedente");
        it.put(UiStrings.MAIL_NEXT_PAGE_HOVER, "Clicca per la pagina successiva");
        it.put(UiStrings.MAIL_PREVIOUS_PAGE_HOVER, "Clicca per la pagina precedente");
        it.put(UiStrings.MAIL_UNKNOWN_SLOT, "Quel messaggio non è nella pagina che stai guardando");
        it.put(UiStrings.MAIL_REPLY_USAGE, "Usa /mail reply <numero> <messaggio>");
        it.put(UiStrings.MAIL_HOVER_TIP, "Scrivi /mail show <numero> per leggere un messaggio intero");
        it.put(UiStrings.MAIL_TITLE, "Posta");
        it.put(UiStrings.MAIL_OPEN_INBOX, "Posta in arrivo");
        it.put(UiStrings.MAIL_UNREAD_COUNT, "%count% da leggere");
        it.put(UiStrings.MAIL_ALL_READ, "Tutto letto");
        it.put(UiStrings.MAIL_FROM, "da");
        it.put(UiStrings.MAIL_SENT_TO, "Messaggio inviato a %player%");
        it.put(UiStrings.MAIL_NO_RECIPIENT, "Nessun destinatario indicato");
        it.put(UiStrings.MAIL_PLAYER_NOT_FOUND, "Giocatore non trovato");
        it.put(UiStrings.MAIL_RECIPIENT_NAME, "Nome del giocatore");
        it.put(UiStrings.SERVERS_TITLE, "Server");
        it.put(UiStrings.SERVERS_HELP, "Scegli dove vuoi andare");
        it.put(UiStrings.SERVERS_RETURN, "Torna su %server%");
        it.put(UiStrings.SERVERS_YOU_ARE_HERE, "Sei qui");
        it.put(UiStrings.SERVERS_NONE, "Non c'è nessun altro posto dove andare al momento");
        it.put(UiStrings.SERVERS_GONE, "Quel server non è disponibile");
        it.put(UiStrings.SERVERS_DEFAULT_TITLE, "Server predefinito all'accesso");
        it.put(UiStrings.SERVERS_DEFAULT_HELP, "Dove metterti quando ti colleghi a %network%");
        it.put(UiStrings.SERVERS_DEFAULT_BUTTON, "Accesso: %server%");
        it.put(UiStrings.SERVERS_DEFAULT_LAST, "Ultimo server");
        it.put(UiStrings.SERVERS_DEFAULT_SAVED, "D'ora in poi arriverai su %server%");
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
        ru.put(UiStrings.MAIL_INBOX_HEADER, "ВХОДЯЩИЕ");
        ru.put(UiStrings.MAIL_NO_MAILS, "У вас нет сообщений.");
        ru.put(UiStrings.MAIL_SEND_TITLE, "Написать сообщение");
        ru.put(UiStrings.MAIL_MESSAGE, "Сообщение");
        ru.put(UiStrings.MAIL_REPLY, "Ответить");
        ru.put(UiStrings.MAIL_CLICK_TO_REPLY, "Нажмите, чтобы ответить");
        ru.put(UiStrings.MAIL_MARK_AS_READ, "Отметить прочитанным");
        ru.put(UiStrings.MAIL_ALREADY_READ, "Уже отмечено прочитанным");
        ru.put(UiStrings.MAIL_NEXT_PAGE, "Следующая страница");
        ru.put(UiStrings.MAIL_PREVIOUS_PAGE, "Предыдущая страница");
        ru.put(UiStrings.MAIL_NEXT_PAGE_HOVER, "Нажмите для следующей страницы");
        ru.put(UiStrings.MAIL_PREVIOUS_PAGE_HOVER, "Нажмите для предыдущей страницы");
        ru.put(UiStrings.MAIL_UNKNOWN_SLOT, "Этого сообщения нет на открытой странице");
        ru.put(UiStrings.MAIL_REPLY_USAGE, "Используйте /mail reply <номер> <сообщение>");
        ru.put(UiStrings.MAIL_HOVER_TIP, "Введите /mail show <номер>, чтобы прочитать сообщение целиком");
        ru.put(UiStrings.MAIL_TITLE, "Почта");
        ru.put(UiStrings.MAIL_OPEN_INBOX, "Входящие");
        ru.put(UiStrings.MAIL_UNREAD_COUNT, "%count% непрочитанных");
        ru.put(UiStrings.MAIL_ALL_READ, "Всё прочитано");
        ru.put(UiStrings.MAIL_FROM, "от");
        ru.put(UiStrings.MAIL_SENT_TO, "Сообщение отправлено игроку %player%");
        ru.put(UiStrings.MAIL_NO_RECIPIENT, "Получатель не указан");
        ru.put(UiStrings.MAIL_PLAYER_NOT_FOUND, "Игрок не найден");
        ru.put(UiStrings.MAIL_RECIPIENT_NAME, "Имя игрока");
        ru.put(UiStrings.SERVERS_TITLE, "Серверы");
        ru.put(UiStrings.SERVERS_HELP, "Выберите, куда хотите отправиться");
        ru.put(UiStrings.SERVERS_RETURN, "Назад на %server%");
        ru.put(UiStrings.SERVERS_YOU_ARE_HERE, "Вы здесь");
        ru.put(UiStrings.SERVERS_NONE, "Сейчас больше некуда пойти");
        ru.put(UiStrings.SERVERS_GONE, "Этот сервер недоступен");
        ru.put(UiStrings.SERVERS_DEFAULT_TITLE, "Сервер по умолчанию при входе");
        ru.put(UiStrings.SERVERS_DEFAULT_HELP, "Куда вас отправить при подключении к %network%");
        ru.put(UiStrings.SERVERS_DEFAULT_BUTTON, "Вход: %server%");
        ru.put(UiStrings.SERVERS_DEFAULT_LAST, "Последний сервер");
        ru.put(UiStrings.SERVERS_DEFAULT_SAVED, "Теперь вы будете попадать на %server%");
        all.put("ru", ru);

        return all;
    }
}
