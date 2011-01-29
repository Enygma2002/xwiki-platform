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
package com.xpn.xwiki.pdf.api;

import java.io.OutputStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public interface PdfExport
{
    public void exportHtml(String xhtml, OutputStream out, int type, XWikiContext context) throws XWikiException;

    public void export(XWikiDocument doc, OutputStream out, int type, XWikiContext context) throws XWikiException;

    public void exportToPDF(XWikiDocument doc, OutputStream out, XWikiContext context) throws XWikiException;

    public String convertToStrictXHtml(String input);

    public String convertXHtmlToXMLFO(String input, XWikiContext context) throws XWikiException;
}
