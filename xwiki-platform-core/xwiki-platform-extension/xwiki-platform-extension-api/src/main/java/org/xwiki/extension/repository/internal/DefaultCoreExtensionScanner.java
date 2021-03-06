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
package org.xwiki.extension.repository.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.properties.ConverterManager;

import com.google.common.base.Predicates;

/**
 * Scan jars to find core extensions.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultCoreExtensionScanner implements CoreExtensionScanner
{
    /**
     * The package containing maven informations in a jar file.
     */
    private static final String MAVENPACKAGE = "META-INF.maven";

    /**
     * Unknown.
     */
    private static final String UNKNOWN = "unknown";

    /**
     * SNAPSHOT suffix in versions.
     */
    private static final String SNAPSHOTSUFFIX = "-SNAPSHOT";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to parse some custom properties.
     */
    @Inject
    private ConverterManager converter;

    /**
     * Used to find proper {@link ExtensionLicense}.
     */
    @Inject
    private ExtensionLicenseManager licenseManager;

    @Override
    public Map<String, DefaultCoreExtension> loadExtensions(DefaultCoreExtensionRepository repository)
    {
        Set<URL> mavenURLs = ClasspathHelper.forPackage(MAVENPACKAGE);

        // FIXME: remove that as soon as possible
        mavenURLs = filterURLs(mavenURLs);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setScanners(new ResourcesScanner());
        configurationBuilder.setUrls(mavenURLs);
        configurationBuilder.filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(MAVENPACKAGE)));

        Reflections reflections = new Reflections(configurationBuilder);

        Set<String> descriptors = reflections.getResources(Predicates.equalTo("pom.xml"));

        Map<String, DefaultCoreExtension> extensions = new HashMap<String, DefaultCoreExtension>(descriptors.size());

        List<Dependency> dependencies = new ArrayList<Dependency>();
        List<Object[]> coreArtefactIds = new ArrayList<Object[]>();

        for (String descriptor : descriptors) {
            URL descriptorUrl = getClass().getClassLoader().getResource(descriptor);

            InputStream descriptorStream = getClass().getClassLoader().getResourceAsStream(descriptor);
            try {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model mavenModel = reader.read(descriptorStream);

                String version = resolveVersion(mavenModel.getVersion(), mavenModel, false);
                String groupId = resolveGroupId(mavenModel.getGroupId(), mavenModel, false);

                String extensionURLStr = descriptorUrl.toString();
                extensionURLStr =
                    extensionURLStr.substring(0, descriptorUrl.toString().indexOf(MAVENPACKAGE.replace('.', '/')));
                URL extensionURL = new URL(extensionURLStr);

                DefaultCoreExtension coreExtension =
                    new DefaultCoreExtension(repository, extensionURL, new ExtensionId(groupId + ':'
                        + mavenModel.getArtifactId(), version), packagingToType(mavenModel.getPackaging()));

                coreExtension.setName(mavenModel.getName());
                coreExtension.setDescription(mavenModel.getDescription());
                for (Developer developer : mavenModel.getDevelopers()) {
                    coreExtension.addAuthor(developer.getId());
                }
                coreExtension.setWebsite(mavenModel.getUrl());

                // licenses
                for (License license : mavenModel.getLicenses()) {
                    coreExtension.addLicense(getExtensionLicense(license));
                }

                // features
                String featuresString = mavenModel.getProperties().getProperty("xwiki.extension.features");
                if (StringUtils.isNotBlank(featuresString)) {
                    coreExtension.setFeatures(this.converter.<Collection<String>> convert(List.class, featuresString));
                }

                // dependencies
                for (Dependency mavenDependency : mavenModel.getDependencies()) {
                    if (!mavenDependency.isOptional()
                        && (mavenDependency.getScope() == null || mavenDependency.getScope().equals("compile") || mavenDependency
                            .getScope().equals("runtime"))) {
                        coreExtension.addDependency(new DefaultExtensionDependency(resolveGroupId(
                            mavenDependency.getGroupId(), mavenModel, true)
                            + ':' + mavenDependency.getArtifactId(), resolveVersion(mavenDependency.getVersion(),
                            mavenModel, true)));
                    }
                }

                // custom properties
                coreExtension.putProperty("maven.groupId", groupId);
                coreExtension.putProperty("maven.artifactId", mavenModel.getArtifactId());

                extensions.put(coreExtension.getId().getId(), coreExtension);
                coreArtefactIds.add(new Object[] {mavenModel.getArtifactId(), coreExtension});

                for (Dependency dependency : mavenModel.getDependencies()) {
                    if (dependency.getGroupId().equals("${project.groupId}")) {
                        dependency.setGroupId(groupId);
                    }
                    if (dependency.getVersion() == null) {
                        dependency.setVersion(UNKNOWN);
                    } else if (dependency.getVersion().equals("${project.version}")
                        || dependency.getVersion().equals("${pom.version}")) {
                        dependency.setVersion(version);
                    }
                    dependencies.add(dependency);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to parse descriptor [" + descriptorUrl
                    + "], it will be ignored and not found in core extensions.", e);
            } finally {
                try {
                    descriptorStream.close();
                } catch (IOException e) {
                    // Should not happen
                    this.logger.error("Failed to close descriptor stream [" + descriptorUrl + "]", e);
                }
            }

            // Normalize and guess

            Map<String, Object[]> artefacts = new HashMap<String, Object[]>();
            Set<URL> urls = ClasspathHelper.forClassLoader();

            for (URL url : urls) {
                try {
                    String path = url.toURI().getPath();
                    String filename = path.substring(path.lastIndexOf('/') + 1);

                    int extIndex = filename.lastIndexOf('.');
                    if (extIndex != -1) {
                        filename = filename.substring(0, extIndex);
                    }

                    int index;
                    if (!filename.endsWith(SNAPSHOTSUFFIX)) {
                        index = filename.lastIndexOf('-');
                    } else {
                        index = filename.lastIndexOf('-', filename.length() - SNAPSHOTSUFFIX.length());
                    }

                    if (index != -1) {
                        String artefactname = filename.substring(0, index);
                        String version = filename.substring(index + 1);

                        artefacts.put(artefactname, new Object[] {version, url});
                    }
                } catch (Exception e) {
                    this.logger.warn("Failed to parse resource name [" + url + "]", e);
                }
            }

            // Try to resolve version no easy to find from the pom.xml
            try {
                for (Object[] coreArtefactId : coreArtefactIds) {
                    Object[] artefact = artefacts.get(coreArtefactId[0]);

                    DefaultCoreExtension coreExtension = (DefaultCoreExtension) coreArtefactId[1];
                    if (artefact != null && coreExtension.getId().getVersion().charAt(0) == '$') {
                        coreExtension.setId(new ExtensionId(coreExtension.getId().getId(), (String) artefact[0]));
                        coreExtension.setGuessed(true);
                    }
                }

                // Add dependencies that does not provide proper pom.xml resource and can't be found in the classpath
                for (Dependency dependency : dependencies) {
                    String dependencyId = dependency.getGroupId() + ':' + dependency.getArtifactId();

                    Object[] artefact = artefacts.get(dependency.getArtifactId());
                    if (artefact != null) {
                        DefaultCoreExtension coreExtension = extensions.get(dependencyId);
                        if (coreExtension == null) {
                            coreExtension =
                                new DefaultCoreExtension(repository, (URL) artefact[1], new ExtensionId(dependencyId,
                                    (String) artefact[0]), packagingToType(dependency.getType()));
                            coreExtension.setGuessed(true);

                            extensions.put(dependencyId, coreExtension);
                        }
                    }
                }
            } catch (Exception e) {
                this.logger.warn("Failed to guess extra information about some extensions", e);
            }
        }

        return extensions;
    }

    private String resolveVersion(String modelVersion, Model mavenModel, boolean dependency)
    {
        String version = modelVersion;

        // TODO: download parents and resolve pom.xml properties using aether ? could be pretty expensive for
        // the init
        if (version == null) {
            if (!dependency) {
                Parent parent = mavenModel.getParent();

                if (parent != null) {
                    version = parent.getVersion();
                }
            }
        } else if (version.startsWith("$")) {
            String propertyName = version.substring(2, version.length() - 1);

            if (propertyName.equals("project.version") || propertyName.equals("pom.version")
                || propertyName.equals("version")) {
                version = resolveVersion(mavenModel.getVersion(), mavenModel, false);
            } else {
                String value = mavenModel.getProperties().getProperty(propertyName);
                if (value != null) {
                    version = value;
                }
            }
        }

        if (version == null) {
            version = UNKNOWN;
        }

        return version;
    }

    private String resolveGroupId(String modelGroupId, Model mavenModel, boolean dependency)
    {
        String groupId = modelGroupId;

        // TODO: download parents and resolve pom.xml properties using aether ? could be pretty expensive for
        // the init
        if (groupId == null) {
            if (!dependency) {
                Parent parent = mavenModel.getParent();

                if (parent != null) {
                    groupId = parent.getGroupId();
                }
            }
        } else if (groupId.startsWith("$")) {
            String propertyName = groupId.substring(2, groupId.length() - 1);

            String value = mavenModel.getProperties().getProperty(propertyName);
            if (value != null) {
                groupId = value;
            }
        }

        if (groupId == null) {
            groupId = UNKNOWN;
        }

        return groupId;
    }

    // TODO: download custom licenses content
    private ExtensionLicense getExtensionLicense(License license)
    {
        if (license.getName() == null) {
            return new ExtensionLicense("noname", null);
        }

        ExtensionLicense extensionLicense = this.licenseManager.getLicense(license.getName());

        return extensionLicense != null ? extensionLicense : new ExtensionLicense(license.getName(), null);
    }

    /**
     * Very nasty hack which unescape invalid characters from the {@link URL} path because
     * {@link Reflections#Reflections(org.reflections.Configuration)} does not do it...
     * 
     * @param urls base URLs to modify
     * @return modified base URLs
     */
    // TODO: remove when http://code.google.com/p/reflections/issues/detail?id=87 is fixed
    private Set<URL> filterURLs(Set<URL> urls)
    {
        Set<URL> results = new HashSet<URL>(urls.size());
        for (URL url : urls) {
            try {
                results.add(new URL(url.getProtocol(), url.getHost(), url.getPort(), URLDecoder.decode(url.getFile(),
                    "UTF-8")));
            } catch (Exception e) {
                this.logger.error("Failed to convert url [" + url + "]", e);

                results.add(url);
            }
        }

        return results;
    }

    /**
     * Get the extension type from maven packaging.
     * 
     * @param packaging the maven packaging
     * @return the extension type
     */
    private String packagingToType(String packaging)
    {
        // support bundle packaging
        if (packaging.equals("bundle")) {
            return "jar";
        }

        return packaging;
    }
}
