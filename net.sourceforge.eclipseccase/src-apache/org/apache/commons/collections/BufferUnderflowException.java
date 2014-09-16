/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2004 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.collections;

import java.util.NoSuchElementException;

/**
 * The BufferUnderflowException is used when the buffer is already empty.
 * <p>
 * NOTE: From version 3.0, this exception extends NoSuchElementException.
 * 
 * @since Commons Collections 2.1
 * @version $Revision$ $Date$
 * 
 * @author Avalon
 * @author Berin Loritsch
 * @author Jeff Turner
 * @author Paul Jack
 * @author Stephen Colebourne
 */
public class BufferUnderflowException extends NoSuchElementException {
	private static final long serialVersionUID = 4054570024234606028L;
	/** The root cause throwable */
	private final Throwable throwable;

	/**
	 * Constructs a new <code>BufferUnderflowException</code>.
	 */
	public BufferUnderflowException() {
		super();
		throwable = null;
	}

	/**
	 * Construct a new <code>BufferUnderflowException</code>.
	 * 
	 * @param message
	 *            the detail message for this exception
	 */
	public BufferUnderflowException(String message) {
		this(message, null);
	}

	/**
	 * Construct a new <code>BufferUnderflowException</code>.
	 * 
	 * @param message
	 *            the detail message for this exception
	 * @param exception
	 *            the root cause of the exception
	 */
	public BufferUnderflowException(String message, Throwable exception) {
		super(message);
		throwable = exception;
	}

	/**
	 * Gets the root cause of the exception.
	 * 
	 * @return the root cause
	 */
	@Override
	public final Throwable getCause() {
		return throwable;
	}

}
