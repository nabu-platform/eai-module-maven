/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.validator.api.Validation;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class MavenLibraryManager extends JAXBArtifactManager<MavenLibraryConfiguration, MavenLibraryArtifact> implements ArtifactRepositoryManager<MavenLibraryArtifact> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public MavenLibraryManager() {
		super(MavenLibraryArtifact.class);
	}

	@Override
	protected MavenLibraryArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new MavenLibraryArtifact(id, container, repository);
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, MavenLibraryArtifact artifact) throws IOException {
		Resource child = entry.getContainer().getChild("node.properties");
		if (child == null) {
			child = ((ManageableContainer<?>) entry.getContainer()).create("node.properties", "text/plain");
		}
		WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
		try {
			Properties properties = new Properties();
			properties.put("stage", "preload");
			properties.store(IOUtils.toOutputStream(writable), "Auto-generated by " + System.getProperty("user.name", "anonymous"));
		}
		finally {
			writable.close();
		}
		return super.save(entry, artifact);
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, MavenLibraryArtifact artifact) throws IOException {
		List<Entry> children = new ArrayList<Entry>();
		if (artifact.getInternalArtifacts() == null) {
			artifact.forceLoad();
		}
		if (artifact.getInternalArtifacts() != null) {
			for (MavenArtifact child : artifact.getInternalArtifacts()) {
				try {
					List<Entry> attachChildren = MavenManager.attachChildren((ModifiableEntry) parent.getRepository().getRoot(), child, artifact.getId());
					if (attachChildren != null) {
						children.addAll(attachChildren);
					}
				}
				catch (IOException e) {
					logger.error("Could not attach maven artifact: " + artifact, e);
				}
			}
		}
		return children;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, MavenLibraryArtifact artifact) throws IOException {
		List<Entry> children = new ArrayList<Entry>();
		if (artifact.getInternalArtifacts() != null) {
			for (MavenArtifact child : artifact.getInternalArtifacts()) {
				try {
					List<Entry> detachChildren = MavenManager.detachChildren((ModifiableEntry) parent.getRepository().getRoot(), child);
					if (detachChildren != null) {
						children.addAll(detachChildren);
					}
				}
				catch (IOException e) {
					logger.error("Could not detach maven artifact: " + artifact, e);
				}
			}
		}
		return children;
	}
	

}
