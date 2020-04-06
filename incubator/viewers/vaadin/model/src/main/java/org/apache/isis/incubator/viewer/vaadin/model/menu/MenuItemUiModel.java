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
package org.apache.isis.incubator.viewer.vaadin.model.menu;

import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.incubator.viewer.vaadin.model.action.ActionUiModel;
import org.apache.isis.incubator.viewer.vaadin.model.action.ServiceActionUiModel;
import org.apache.isis.viewer.common.model.menuitem.MenuItemUiModelAbstract;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * 
 * @since Apr 5, 2020
 * @implNote corresponds to Wicket
 * org.apache.isis.viewer.wicket.ui.components.actionmenu.serviceactions.CssMenuItem
 */
@Log4j2
public class MenuItemUiModel 
extends MenuItemUiModelAbstract<MenuItemUiModel> {

    @Getter @Setter(AccessLevel.PRIVATE) private Object actionLinkComponent;
    @Getter @Setter(AccessLevel.PRIVATE) private ServiceActionUiModel serviceActionUiModel;
    
    public static MenuItemUiModel newMenuItem(final String name) {
        return new MenuItemUiModel(name);
    }

    private MenuItemUiModel(final String name) {
        super(name);
    }
    
    private MenuItemUiModel newSubMenuItem(final String name) {
        val subMenuItem = newMenuItem(name);
        subMenuItem.setParent(this);
        return subMenuItem;
    }
    

    /**
     * Optionally creates a sub-menu item invoking an action on the provided 
     * {@link ActionUiModel}, based on visibility and usability.
     */
    public void addMenuItemFor(final ServiceActionUiModel saModel) {
        
        val serviceEntityModel = saModel.getServiceEntityUiModel();
        val objectAction = saModel.getObjectAction();
        final boolean requiresSeparator = saModel.isFirstSection();
        val actionLinkFactory = saModel.getLinkAndLabelFactory();

        val actionHolder = serviceEntityModel.getManagedObject();
        if(!super.isVisible(actionHolder, objectAction)) {
            log.info("not visible {}", objectAction.getName());
            return;
        }

        // build the link
        val linkAndLabel = actionLinkFactory.apply(objectAction);
        if (linkAndLabel == null) {
            // can only get a null if invisible, so this should not happen given the visibility guard above
            return;
        }

        val actionLabel = saModel.getActionName() != null 
                ? saModel.getActionName() 
                : linkAndLabel.getLabel();

        val menutIem = (MenuItemUiModel) newSubMenuItem(actionLabel)
                .setDisabledReason(super.getReasonWhyDisabled(actionHolder, objectAction).orElse(null))
                .setPrototyping(objectAction.isPrototype())
                .setRequiresSeparator(requiresSeparator)
                .setRequiresImmediateConfirmation(
                        ObjectAction.Util.isAreYouSureSemantics(objectAction) &&
                        ObjectAction.Util.isNoParameters(objectAction))
                .setBlobOrClob(ObjectAction.Util.returnsBlobOrClob(objectAction))
                .setDescription(super.getDescription(objectAction).orElse(null))
                .setActionIdentifier(ObjectAction.Util.actionIdentifierFor(objectAction))
                .setCssClass(ObjectAction.Util.cssClassFor(objectAction, actionHolder))
                .setCssClassFa(ObjectAction.Util.cssClassFaFor(objectAction))
                .setCssClassFaPosition(ObjectAction.Util.cssClassFaPositionFor(objectAction));
        
        menutIem.setActionLinkComponent(linkAndLabel.getLinkComponent());
        menutIem.setServiceActionUiModel(saModel);
        
    }
}