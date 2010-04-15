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
package org.xwiki.rendering.internal.transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Calls all existing transformations (executed by priority) on an existing XDOM object to generate a new 
 * transformed XDOM.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@Component
public class DefaultTransformationManager implements TransformationManager, Initializable
{
    /**
     * Holds the list of transformations to apply, sorted by priority in {@link #initialize()}.
     */
    @Requirement(role = Transformation.class)
    private List<Transformation> transformations = new ArrayList<Transformation>();

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Sort transformations by priority.
        Collections.sort(this.transformations);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.transformation.TransformationManager#performTransformations(org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax)
     */
    public void performTransformations(XDOM dom, Syntax syntax) throws TransformationException
    {
        for (Transformation transformation : this.transformations) {
            transformation.transform(dom, syntax);
        }
    }
}