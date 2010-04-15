/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.url;

import java.util.Locale;

import org.xwiki.model.reference.DocumentReference;

/**
 * Represents a XWiki URL pointing to a Document Entity.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiDocumentURL extends AbstractXWikiURL
{
    private String action;

    private DocumentReference documentReference;

    private Locale locale;
    
    private String revision;

    public XWikiDocumentURL(DocumentReference documentReference)
    {
        super(XWikiURLType.DOCUMENT);
        setDocumentReference(documentReference);
    }

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    public void setDocumentReference(DocumentReference documentReference)
    {
        this.documentReference = documentReference;
    }
    
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }
    
    public Locale getLocale()
    {
        return this.locale;
    }
    
    public void setRevision(String revision)
    {
        this.revision = revision;
    }
    
    public String getRevision()
    {
        return this.revision;
    }
}