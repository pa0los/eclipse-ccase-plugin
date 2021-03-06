/*******************************************************************************
 * Copyright (c) 2012 eclipse-ccase.sourceforge.net.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     derekhunter4 - inital API and implementation
 *     IBM Corporation - concepts and ideas from Eclipse
 *******************************************************************************/
package net.sourceforge.eclipseccase.autocomplete;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;

public abstract class AutocompleteWidget {
	
	protected AutocompleteContentProposalProvider provider = null;
	protected ContentProposalAdapter adapter = null;

	/**
	 * Return a character array representing the keyboard input triggers
	 * used for firing the ContentProposalAdapter
	 * 
	 * @return
	 * 		character array of trigger chars
	 */
	protected char[] getAutoactivationChars() {
		String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz";
		String uppercaseLetters = lowercaseLetters.toUpperCase();
		String numbers = "0123456789";
		//String delete = new String(new char[] {SWT.DEL});
		// the event in {@link ContentProposalAdapter#addControlListener(Control control)}
		// holds onto a character and when the DEL key is pressed that char
		// value is 8 so the line below catches the DEL keypress
		String delete = new String(new char[] {8}); 
		String allChars = lowercaseLetters + uppercaseLetters + numbers + delete;
		return allChars.toCharArray();
	}

	/**
	 * Returns KeyStroke object which when pressed will fire the
	 * ContentProposalAdapter
	 * 
	 * @return
	 * 		the activation keystroke
	 */
	protected KeyStroke getActivationKeystroke() {
		//keyStroke = KeyStroke.getInstance("Ctrl+Space");
		// Activate on <ctrl><space>
		return KeyStroke.getInstance(new Integer(SWT.CTRL).intValue(), new Integer(' ').intValue());
		//return null;
	}

	protected abstract AutocompleteContentProposalProvider getContentProposalProvider(String[] proposals);

}
