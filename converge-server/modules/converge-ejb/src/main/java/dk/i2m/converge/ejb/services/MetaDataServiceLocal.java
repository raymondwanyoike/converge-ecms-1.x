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
package dk.i2m.converge.ejb.services;

import dk.i2m.converge.core.EnrichException;
import javax.ejb.Local;

/**
 * Local interface for {@link MetaDataServiceBean}.
 *
 * @author Allan Lykke Christensen
 */
@Local
public interface MetaDataServiceLocal {

    java.util.Map<java.lang.String, java.lang.String> extract(java.lang.String location);

    java.util.Map<java.lang.String, java.lang.String> extractFromMp3(java.lang.String location) throws dk.i2m.converge.ejb.services.CannotExtractMetaDataException;

    java.util.Map<java.lang.String, java.lang.String> extractXmp(java.lang.String location) throws dk.i2m.converge.ejb.services.CannotExtractMetaDataException;

    java.util.Map<java.lang.String, java.lang.String> extractIPTC(java.lang.String location) throws dk.i2m.converge.ejb.services.CannotExtractMetaDataException;

    java.util.Map<java.lang.String, java.lang.String> extractImageInfo(java.lang.String location) throws dk.i2m.converge.ejb.services.CannotExtractMetaDataException;

    java.util.List<dk.i2m.converge.core.metadata.Concept> enrich(java.lang.String story) throws EnrichException;

    java.lang.String extractContent(dk.i2m.converge.core.content.catalogue.MediaItemRendition mir);
}
