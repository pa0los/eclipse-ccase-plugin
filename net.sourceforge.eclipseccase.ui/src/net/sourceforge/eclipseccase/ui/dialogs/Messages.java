/*
 * Created on Jun 3, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sourceforge.eclipseccase.ui.dialogs;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author eyopd
 * 
 *         To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Generation - Code and Comments
 */
public class Messages {
	private static final String BUNDLE_NAME = "net.sourceforge.eclipseccase.ui.dialogs.messages";//$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Messages() {
		// hidden
	}

	/**
	 * Returns the string for the specified key.
	 * 
	 * @param key
	 * @return the string for the specified key
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}