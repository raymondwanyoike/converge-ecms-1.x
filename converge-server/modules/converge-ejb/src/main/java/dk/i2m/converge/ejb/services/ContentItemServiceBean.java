/*
 * Copyright (C) 2012 Interactive Media Management
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.DataNotFoundException;
import dk.i2m.converge.core.content.ContentItem;
import dk.i2m.converge.core.content.ContentItemActor;
import dk.i2m.converge.core.content.NewsItem;
import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.content.catalogue.MediaItem;
import dk.i2m.converge.core.plugin.WorkflowAction;
import dk.i2m.converge.core.plugin.WorkflowValidator;
import dk.i2m.converge.core.plugin.WorkflowValidatorException;
import dk.i2m.converge.core.security.UserAccount;
import dk.i2m.converge.core.security.UserRole;
import dk.i2m.converge.core.workflow.*;
import dk.i2m.converge.ejb.facades.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.*;

/**
 * Implementation of the {@link ContenItemServiceBean} providing access to
 * working with {@link ContentItem}s.
 *
 * @author Allan Lykke Christensen
 */
@Stateless
public class ContentItemServiceBean implements ContentItemServiceLocal {

    private static final Logger LOG =
            Logger.getLogger(ContentItemServiceBean.class.getName());

    @EJB private DaoServiceLocal daoService;

    @EJB private UserServiceLocal userService;

    @EJB private PluginContextBeanLocal pluginContext;

    @EJB private NewsItemFacadeLocal newsItemFacade;
    @EJB private CatalogueFacadeLocal catalogueFacade;

    @Resource private SessionContext ctx;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ContentItem start(ContentItem contentItem) throws
            WorkflowStateTransitionException {

        if (contentItem == null) {
            throw new WorkflowStateTransitionException(
                    "ContentItem not available");
        }

        if (contentItem.getId() != null) {
            throw new DuplicateExecutionException("ContentItem #"
                    + contentItem.getId() + " already started");
        }

        Workflow workflow = null;
        if (contentItem instanceof NewsItem) {
            if (((NewsItem) contentItem).getOutlet() == null) {
                throw new WorkflowStateTransitionException(
                        "Outlet must be set for NewsItem");
            } else {
                workflow = ((NewsItem) contentItem).getOutlet().getWorkflow();
            }
        }

        if (contentItem instanceof MediaItem) {
            if (((MediaItem) contentItem).getCatalogue() == null) {
                throw new WorkflowStateTransitionException(
                        "Catalogue must be set for MediaItem");
            } else {
                workflow =
                        ((MediaItem) contentItem).getCatalogue().getWorkflow();
            }
        }

        if (workflow == null) {
            throw new WorkflowStateTransitionException(
                    "Workflow definition could not be determined");
        }

        WorkflowState startState = workflow.getStartState();
        boolean actorSet = false;
        UserRole requiredRole = startState.getActorRole();
        for (ContentItemActor actor : contentItem.getActors()) {
            if (actor.getRole().equals(requiredRole)) {
                actorSet = true;
                break;
            }
        }

        if (!actorSet) {
            throw new MissingActorException("Actor with role " + requiredRole.
                    getName() + " is missing", requiredRole);
        }

        String uid = ctx.getCallerPrincipal().getName();
        UserAccount ua = null;
        try {
            ua = userService.findById(uid);
        } catch (Exception ex) {
            throw new WorkflowStateTransitionException(
                    "Could not resolve transition initator", ex);
        }

        updateThumbnail(contentItem);

        try {
            Calendar now = Calendar.getInstance();

            WorkflowStateTransition transition = new WorkflowStateTransition(
                    contentItem, now.getTime(), startState, ua);
            transition.setStoryVersion("");
            transition.setComment("");

            contentItem.getHistory().add(transition);
            contentItem.setCurrentState(startState);
            contentItem.setUpdated(now.getTime());
            contentItem.setCreated(now.getTime());
            contentItem.setPrecalculatedCurrentActor(
                    contentItem.getCurrentActor());

            contentItem = daoService.create(contentItem);

            List<UserAccount> usersToNotify = new ArrayList<UserAccount>();
            WorkflowState state = contentItem.getCurrentState();

            switch (state.getPermission()) {
                case USER:
                    for (ContentItemActor actor : contentItem.getActors()) {
                        if (actor.getRole().equals(state.getActorRole())) {
                            usersToNotify.add(actor.getUser());
                        }
                    }
                    break;
                case GROUP:
                    for (UserAccount actor : userService.findAll()) {
                        if (actor.getUserRoles().contains(state.getActorRole())) {
                            usersToNotify.add(actor);
                        }
                    }
                    break;
            }

//            for (UserAccount userToNotify : usersToNotify) {
//                if (!userToNotify.equals(ua)) {
//                    String msgPattern = cfgService.getMessage(
//                            "notification_MSG_STORY_ASSIGNED");
//                    SimpleDateFormat sdf = new SimpleDateFormat(cfgService.
//                            getMessage("FORMAT_SHORT_DATE_AND_TIME"));
//                    sdf.setTimeZone(userToNotify.getTimeZone());
//
//                    String date =
//                            sdf.format(contentItem.getDeadline().getTime());
//
//                    Object[] args = new Object[]{contentItem.getTitle(), date,
//                        ua.getFullName()};
//                    String msg = MessageFormat.format(msgPattern, args);
//
//                    notificationService.create(userToNotify, msg);
//                }
//            }
            return contentItem;

        } catch (Exception ex) {
            throw new WorkflowStateTransitionException(ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ContentItem step(ContentItem contentItem, Long step) throws
            WorkflowStateTransitionException {

        // Get current user
        String uid = ctx.getCallerPrincipal().getName();
        UserAccount ua = null;
        try {
            ua = userService.findById(uid);
            pluginContext.setCurrentUserAccount(ua);
        } catch (Exception ex) {
            throw new WorkflowStateTransitionException(
                    "Could not resolve transition initator", ex);
        }

        Calendar now = Calendar.getInstance();

        WorkflowStep transitionStep;
        try {
            transitionStep = daoService.findById(WorkflowStep.class, step);
        } catch (DataNotFoundException ex) {
            throw new WorkflowStateTransitionException("Transition (WorkflowStep) #"
                    + step + " does not exist", ex);
        }

        WorkflowState nextState = transitionStep.getToState();

        // Checking validity of step
        WorkflowState state = contentItem.getCurrentState();
        boolean legalStep = false;
        for (WorkflowStep nextWorkflowStep : state.getNextStates()) {

            if (nextWorkflowStep.getToState().equals(nextState)) {

                boolean isInRole = !Collections.disjoint(nextWorkflowStep.
                        getValidFor(), ua.getUserRoles());

                if (nextWorkflowStep.isValidForAll() || isInRole) {
                    legalStep = true;
                }
                break;
            }
        }

        if (!legalStep) {
            throw new WorkflowStateTransitionException("Illegal transition from "
                    + state.getId() + " to " + nextState.getId());
        }

        // Checking that the necessary roles were added
        boolean isRoleValidated = false;
        UserRole requiredRole = transitionStep.getToState().getActorRole();
        boolean isUserRole = transitionStep.getToState().isUserPermission();

        if (isUserRole) {
            for (ContentItemActor actor : contentItem.getActors()) {
                if (actor.getRole().equals(requiredRole)) {
                    // User role was already added
                    isRoleValidated = true;
                    break;
                }
            }
        } else {
            isRoleValidated = true;
        }

        if (!isRoleValidated) {
            throw new WorkflowStateTransitionValidationException(
                    "VALIDATE_MISSING_ROLE",
                    new Object[]{requiredRole.getName()});
        }

        // Checking the result of workflow step validators
        for (WorkflowStepValidator validator : transitionStep.getValidators()) {
            try {
                WorkflowValidator workflowValidator = validator.getValidator();
                workflowValidator.execute(contentItem, transitionStep, validator);
            } catch (WorkflowValidatorException ex) {
                throw new WorkflowStateTransitionValidationException(ex.
                        getMessage());
            }
        }
        
        UserRole stateRole = state.getActorRole();
        boolean skipAddCurrentUserToActors = false;
        if (state.isGroupPermission()) {
            for (ContentItemActor a : contentItem.getActors()) {
                if (a.getRole().equals(stateRole) && a.getUser().equals(ua)) {
                    skipAddCurrentUserToActors = true;
                }
            }
        } else {
            skipAddCurrentUserToActors = true;
        }
        
        if (!skipAddCurrentUserToActors) {
            ContentItemActor cia = new ContentItemActor(ua, stateRole, contentItem);
            contentItem.getActors().add(cia);
        }

        WorkflowStateTransition transition = new WorkflowStateTransition(
                contentItem, now.getTime(), nextState, ua);
        if (contentItem instanceof NewsItem) {
            NewsItem newsItem = (NewsItem) contentItem;
            transition.setStoryVersion(newsItem.getStory());
            transition.setHeadlineVersion(newsItem.getTitle());
            transition.setBriefVersion(newsItem.getBrief());
            // Strip unwanted characters
            newsItem.setStory(newsItem.getStory().replaceAll("\\p{Cntrl}", " "));
            newsItem.setPrecalculatedWordCount(newsItem.getWordCount());
        }

        transition.setSubmitted(transitionStep.isTreatAsSubmitted());
        contentItem.setCurrentState(nextState);
        contentItem.getHistory().add(transition);
        contentItem.setUpdated(now.getTime());
        contentItem.setPrecalculatedCurrentActor(contentItem.getCurrentActor());

        
        
        
        if (contentItem instanceof NewsItem) {
            try {
                contentItem = newsItemFacade.checkin((NewsItem) contentItem);
            } catch (LockingException ex) {
                throw new WorkflowStateTransitionException(ex);
            }

            // Actions
            for (WorkflowStepAction action : transitionStep.getActions()) {
                try {
                    WorkflowAction act = action.getAction();
                    act.execute(pluginContext, (NewsItem) contentItem, action,
                            ua);
                } catch (WorkflowActionException ex) {
                    LOG.log(Level.SEVERE, "Could not execute action {0}",
                            action.getLabel());
                }
            }
        } else if (contentItem instanceof MediaItem) {
            contentItem = catalogueFacade.update((MediaItem)contentItem);
        }

        return contentItem;
    }

    /** {@inheritDoc } */
    @Override
    public void generateThumbnailLinks() {
        final int BATCH_SIZE = 50;
        Number numberOfItems = daoService.count(ContentItem.class, "id");
        if (numberOfItems.intValue() < 1) {
            return;
        }

        int batches = numberOfItems.intValue() / BATCH_SIZE;

        LOG.log(Level.INFO, "Updating thumbnail links in {0} batches", batches);
        int start = 0;
        for (int batch = 0; batch < batches; batch++) {
            List<ContentItem> items =
                    daoService.findAll(ContentItem.class, start, BATCH_SIZE);

            for (ContentItem ci : items) {
                updateThumbnail(ci);
            }
            start = start + BATCH_SIZE;
            LOG.log(Level.INFO, "Batch #{0} complete. Another {1} to go",
                    new Object[]{(batch + 1), batches - batch});
        }
    }

    @Override
    public void updateThumbnail(ContentItem ci) {
        if (ci instanceof MediaItem) {
            ci.setThumbnailLink(getThumbnailLinkFromMediaItem((MediaItem) ci));
        } else if (ci instanceof NewsItem) {
            ci.setThumbnailLink(getThumbnailLinkFromNewsItem((NewsItem) ci));
        }
    }

    /**
     * Gets a default <em>MediaItem</em> icon, or if a preview exist.
     * 
     * @return default <em>NewsItem</em> icon, or if a preview exist
     */
    private String getThumbnailLinkFromMediaItem(MediaItem mediaItem) {
        String link = "/images/type-media-item.png";

        if (mediaItem.isPreviewAvailable()) {
            return mediaItem.getPreview().getAbsoluteFilename();
        }
        if (mediaItem.isOriginalAvailable()) {
            if (mediaItem.getOriginal().isAudio()) {
                return "/images/type-media-item-audio.png";
            } else if (mediaItem.getOriginal().isImage()) {
                return "/images/type-media-item-image.png";
            } else if (mediaItem.getOriginal().isVideo()) {
                return "/images/type-media-item-video.png";
            } else if (mediaItem.getOriginal().isDocument()) {
                return "/images/type-media-item-document.png";
            }
        }
        return link;
    }

    /**
     * Gets a default <em>NewsItem</em> icon, or if a preview exist for one of
     * the media attachments.
     * 
     * @return default <em>NewsItem</em> icon, or if a preview exist for one of
     *         the media attachments.
     */
    private String getThumbnailLinkFromNewsItem(NewsItem newsItem) {
        String defaultLink = "/images/type-news-item.png";
        if (newsItem.getMediaAttachments().isEmpty()) {
            return defaultLink;
        }

        for (NewsItemMediaAttachment mia : newsItem.getMediaAttachments()) {
            MediaItem mi = mia.getMediaItem();
            if (mi != null && mi.isPreviewAvailable()) {
                return mi.getPreview().getAbsoluteFilename();
            }
        }
        return defaultLink;
    }
}
