/*
 * Copyright (C) 2011 - 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.plugins.workflowstep;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.plugin.*;
import dk.i2m.converge.core.workflow.WorkflowStateTransitionException;
import dk.i2m.converge.core.workflow.WorkflowStep;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link PluginAction} for executing a worflow state transition.
 *
 * @author Allan Lykke Christensen
 */
public class ExecuteWorkflowStepAction extends PluginAction {

    public enum Parameter {
    }

    public enum PluginActionProperty {

        FROM_WORKFLOW_STATE_STEP
    }
    private static final Logger LOG = Logger.getLogger(ExecuteWorkflowStepAction.class.getName());
    private List<PluginActionPropertyDefinition> availableProperties = null;
    private Map<String, List<String>> instanceProperties = new HashMap<String, List<String>>();
    private Map<String, List<String>> parameters = new HashMap<String, List<String>>();
    private PluginContext ctx;

    /**
     * Constructs a new instance of {@link ExecuteWorkflowStepAction}.
     */
    public ExecuteWorkflowStepAction() {
        onInit();
    }

    @Override
    public void onInit() {
        setBundle("dk.i2m.converge.plugins.workflowstep.Messages");
    }

    @Override
    public void execute(PluginContext ctx, String itemType, Long itemId,
            PluginConfiguration cfg, Map<String, List<String>> parameters)
            throws PluginActionException {

        Object obj = getObjectFromClassName(itemType);

        if (!(obj instanceof ContentItem)) {
            return;
        }
        this.instanceProperties = cfg.getPropertiesMap();

        if (!this.instanceProperties.containsKey(PluginActionProperty.FROM_WORKFLOW_STATE_STEP.name())) {
            throw new PluginActionException("Missing  "
                    + PluginActionProperty.FROM_WORKFLOW_STATE_STEP.name()
                    + " property.", true);
        }

        String workflowStepId = this.instanceProperties.get(PluginActionProperty.FROM_WORKFLOW_STATE_STEP.name()).iterator().next();

        WorkflowStep workflowStep = null;
        try {
            workflowStep = ctx.findWorkflowStep(Long.valueOf(workflowStepId));
        } catch (DataNotFoundException ex) {
            throw new PluginActionException("Unknown WorkflowStep: " + workflowStepId, true);
        } catch (NumberFormatException ex) {
            throw new PluginActionException("Invalid WorkflowStep ID: " + workflowStepId, true);
        }


        ContentItem contentItem;
        try {
            contentItem = ctx.findContentItemById(itemId);
        } catch (DataNotFoundException ex) {
            LOG.log(Level.FINE, "Could not find ContentItem #{0} to be processed in JobQueue", itemId);
            throw new PluginActionException("Could not find ContentItem #" + itemId + " to be processed in JobQueue", true);
        }

        if (!workflowStep.getFromState().equals(contentItem.getCurrentState())) {
            LOG.log(Level.FINE, "WorkflowState of ContentItem ({0}) is not "
                    + "at the specified WorkflowState ({1})",
                    new Object[]{
                        contentItem.getCurrentState(),
                        workflowStep.getFromState().getId()});
            return;
        }
        try {
            LOG.log(Level.FINE, "Executing workflow transition");
            ctx.step(contentItem, workflowStep.getId(), false);
            LOG.log(Level.FINE, "Workflow transition executed successfully");
        } catch (WorkflowStateTransitionException ex) {
            throw new PluginActionException(ex.getMessage(), true);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<PluginActionPropertyDefinition> getAvailableProperties() {
        if (availableProperties == null) {
            availableProperties =
                    new ArrayList<PluginActionPropertyDefinition>();

            availableProperties.add(new PluginActionPropertyDefinition(
                    PluginActionProperty.FROM_WORKFLOW_STATE_STEP.name(),
                    "PROPERTY_FROM_WORKFLOW_STATE_STEP",
                    "PROPERTY_FROM_WORKFLOW_STATE_STEP_TOOLTIP",
                    true,
                    "workflow_state_step",
                    false, 1));
        }
        return availableProperties;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public PluginActionPropertyDefinition findPropertyDefinition(String id)
            throws PropertyDefinitionNotFoundException {
        for (PluginActionPropertyDefinition d : getAvailableProperties()) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        throw new PropertyDefinitionNotFoundException(id + " not found");
    }
}
