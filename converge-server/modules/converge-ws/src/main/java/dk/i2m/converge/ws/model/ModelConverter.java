/*
 * Copyright (C) 2011 Interactive Media Management
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
package dk.i2m.converge.ws.model;

import dk.i2m.converge.core.content.NewsItemMediaAttachment;
import dk.i2m.converge.core.content.NewsItemPlacement;
import java.text.SimpleDateFormat;

/**
 * Static utility class for converting model objects for transmitting
 * via web service.
 *
 * @author Allan Lykke Christensen
 */
public class ModelConverter {

    private static SimpleDateFormat DATE_LINE_FORMAT = new SimpleDateFormat("d MMMM yyyy");

    public static NewsItemActor toNewsItemActor(dk.i2m.converge.core.content.NewsItemActor actor) {
        NewsItemActor nia = new NewsItemActor();
        nia.setUsername(actor.getUser().getUsername());
        nia.setName(actor.getUser().getFullName());
        nia.setRole(actor.getRole().getName());
        nia.setRoleId(actor.getRole().getId());
        return nia;
    }

    public static Section toSection(dk.i2m.converge.core.workflow.Section section) {
        Section s = new Section();
        s.setId(section.getId());
        s.setTitle(section.getFullName());
        s.setDisplayOrder(1);
        return s;
    }

    public static MediaItem toMediaItem(NewsItemMediaAttachment attachment) throws AttachmentNotAvailableException {
        if (attachment.getMediaItem() != null) {
            MediaItem mediaItem = new MediaItem();
            mediaItem.setCaption(attachment.getCaption());

            mediaItem.setId(attachment.getMediaItem().getId());

            if (attachment.getMediaItem().isOriginalAvailable()) {
                mediaItem.setRendition(attachment.getMediaItem().getOriginal().getRendition().getName());
                mediaItem.setContentType(attachment.getMediaItem().getOriginal().getContentType());
                mediaItem.setUrl(attachment.getMediaItem().getOriginal().getAbsoluteFilename());
            } else {
                mediaItem.setRendition("");
                mediaItem.setContentType("application/unknown");
                mediaItem.setUrl("");
            }

            mediaItem.setTitle(attachment.getMediaItem().getTitle());

            return mediaItem;
        } else {
            throw new AttachmentNotAvailableException();
        }

    }

    public static NewsItem toNewsItem(NewsItemPlacement nip) {
        NewsItem item = new NewsItem();
        item.setId(nip.getNewsItem().getId());
        item.setTitle(nip.getNewsItem().getTitle());
        item.setBrief(nip.getNewsItem().getBrief());
        item.setByLine(nip.getNewsItem().getByLine());
        item.setDateLine(DATE_LINE_FORMAT.format(nip.getEdition().getPublicationDate().getTime()));
        item.setStory(nip.getNewsItem().getStory());
        item.setDisplayOrder(nip.getPosition());
        item.setStart(nip.getStart());

        // Convert Media Attachments
        for (NewsItemMediaAttachment attachment : nip.getNewsItem().getMediaAttachments()) {
            try {
                item.getMedia().add(toMediaItem(attachment));
            } catch (AttachmentNotAvailableException ex) {
                System.err.println("Couldn't fetch attachment, skipping");
            }
        }

        // Convert Section
        Section s = toSection(nip.getSection());
        item.setSection(s);

        // Convert Actors
        for (dk.i2m.converge.core.content.NewsItemActor actor : nip.getNewsItem().getActors()) {
            item.getActors().add(toNewsItemActor(actor));
        }

        for (dk.i2m.converge.core.workflow.WorkflowStep step : nip.getNewsItem().getCurrentState().getNextStates()) {
            item.getWorkflowOptions().add(toWorkflowOption(step));
        }

        return item;
    }

    public static NewsItem toNewsItem(dk.i2m.converge.core.content.NewsItem ni) {
        NewsItem item = new NewsItem();
        item.setId(ni.getId());
        item.setTitle(ni.getTitle());
        item.setByLine(ni.getByLine());
        item.setDateLine("");
        item.setStory(ni.getStory());
        item.setDisplayOrder(0);
        item.setStart(0);
        item.setBrief(ni.getBrief());

        // Convert Media Attachments
        for (NewsItemMediaAttachment attachment : ni.getMediaAttachments()) {
            try {
                item.getMedia().add(toMediaItem(attachment));
            } catch (AttachmentNotAvailableException ex) {
                System.err.println("Couldn't fetch attachment, skipping");
            }
        }

        item.setSection(null);

        // Convert Actors
        for (dk.i2m.converge.core.content.NewsItemActor actor : ni.getActors()) {
            item.getActors().add(toNewsItemActor(actor));
        }

        for (dk.i2m.converge.core.workflow.WorkflowStep step : ni.getCurrentState().getNextStates()) {
            item.getWorkflowOptions().add(toWorkflowOption(step));
        }

        return item;
    }

    private static WorkflowOption toWorkflowOption(dk.i2m.converge.core.workflow.WorkflowStep step) {
        WorkflowOption option = new WorkflowOption();
        option.setOptionId(step.getId());
        option.setLabel(step.getName());
        option.setDescription(step.getDescription());
        //TODO: Fix - Introduce display order in WorkflowStep
        option.setDisplayOrder(step.getId().intValue());
        return option;
    }

    public static Outlet toOutlet(dk.i2m.converge.core.workflow.Outlet convergeOutlet) {
        Outlet outlet = new Outlet();

        outlet.setId(convergeOutlet.getId());
        outlet.setTitle(convergeOutlet.getTitle());

        for (dk.i2m.converge.core.workflow.Section s : convergeOutlet.getActiveSections()) {
            outlet.getSections().add(toSection(s));
        }
        return outlet;
    }
}
