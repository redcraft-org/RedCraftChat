package org.redcraft.redcraftchat.locales.providers;

import java.io.IOException;
import java.util.List;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.SupportedLocaleDatabase;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;

import com.dieselpoint.norm.Database;

public class DatabaseLocaleProvider extends StaticLocaleProvider {

    Database db = DatabaseManager.getDatabase();

    @Override
    public List<SupportedLocale> getSupportedLocales() throws IOException, InterruptedException {
        return transform(db.results(SupportedLocaleDatabase.class));
    }

    private List<SupportedLocale> transform(List<SupportedLocaleDatabase> locale) {
        return locale.stream().map(l -> new SupportedLocale(l.code, l.name)).collect(java.util.stream.Collectors.toList());
    }
}
