package iped.utils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {

    private static final String BUNDLE_NAME = "iped-utils-messages"; //$NON-NLS-1$

    private static ResourceBundle RESOURCE_BUNDLE;

    private Messages() {
    }

    public static String getString(String key) {
        if (RESOURCE_BUNDLE == null) {
            Locale locale = Locale.forLanguageTag(System.getProperty(iped.localization.Messages.LOCALE_SYS_PROP)); // $NON-NLS-1$
            RESOURCE_BUNDLE = iped.localization.Messages.getExternalBundle(BUNDLE_NAME, locale);
        }
        try {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
