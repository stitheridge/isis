/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.metamodel.facets.objectvalue.choices;

import org.apache.isis.metamodel.adapter.ObjectAdapter;
import org.apache.isis.metamodel.consent.InteractionInitiatedBy;
import org.apache.isis.metamodel.facetapi.Facet;
import org.apache.isis.metamodel.spec.ObjectSpecification;

public interface ChoicesFacet extends Facet {

    /**
     * Gets a set of choices for this object.
     */
    public Object[] getChoices(
            final ObjectAdapter adapter,
            final InteractionInitiatedBy interactionInitiatedBy);



    public static class Util {

        private Util() {
        }

        public static boolean hasChoices(final ObjectSpecification specification) {
            return specification.getFacet(ChoicesFacet.class) != null;
        }

    }

}