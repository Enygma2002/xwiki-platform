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
 */
package org.xwiki.extension.test;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.internal.DefaultLocalExtension;
import org.xwiki.extension.repository.internal.ExtensionSerializer;

public class ResourceExtensionRepository implements ExtensionRepository
{
    private ExtensionSerializer extensionSerializer;

    private ExtensionRepositoryId repositoryId;

    private ClassLoader classLoader;

    private String baseResource;

    private Map<ExtensionId, ResourceExtension> extension = new HashMap<ExtensionId, ResourceExtension>();

    public ResourceExtensionRepository(ClassLoader classLoader, String baseResource)
    {
        this.extensionSerializer = new ExtensionSerializer();

        this.repositoryId = new ExtensionRepositoryId("resources", "resources", null);

        this.classLoader = classLoader;
        this.baseResource = baseResource;
    }

    InputStream getResourceAsStream(ExtensionId extensionId, String type) throws UnsupportedEncodingException
    {
        return this.classLoader.getResourceAsStream(getEncodedPath(extensionId, type));
    }

    URL getResource(ExtensionId extensionId, String type) throws UnsupportedEncodingException
    {
        return this.classLoader.getResource(getEncodedPath(extensionId, type));
    }

    String getEncodedPath(ExtensionId extensionId, String type) throws UnsupportedEncodingException
    {
        return this.baseResource + URLEncoder.encode(getPathSuffix(extensionId, type), "UTF-8");
    }

    String getPathSuffix(ExtensionId extensionId, String type)
    {
        return extensionId.getId() + '-' + extensionId.getVersion() + '.' + type;
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        InputStream descriptor;
        try {
            descriptor = getResourceAsStream(extensionId, "xed");
        } catch (UnsupportedEncodingException e) {
            throw new ResolveException("Invalid extension id [" + extensionId + "]", e);
        }

        if (descriptor == null) {
            throw new ResolveException("Extension [" + extensionId + "] not found");
        }

        try {
            DefaultLocalExtension localExtension = this.extensionSerializer.loadDescriptor(null, descriptor);

            ResourceExtension resourceExtension = new ResourceExtension(this, localExtension);

            this.extension.put(resourceExtension.getId(), resourceExtension);

            return resourceExtension;
        } catch (Exception e) {
            throw new ResolveException("Failed to parse descriptor for extension [" + extensionId + "]", e);
        }
    }

    public boolean exists(ExtensionId extensionId)
    {
        try {
            return getResource(extensionId, "xed") != null;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }
}
