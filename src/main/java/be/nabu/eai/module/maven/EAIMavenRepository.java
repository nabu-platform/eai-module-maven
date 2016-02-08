package be.nabu.eai.module.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableNodeEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.libs.maven.BaseRepository;
import be.nabu.libs.maven.ResourceArtifact;
import be.nabu.libs.maven.api.Artifact;
import be.nabu.libs.maven.api.WritableRepository;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class EAIMavenRepository extends BaseRepository implements WritableRepository {

	private ResourceRepository repository;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private List<Artifact> artifacts = new ArrayList<Artifact>(); 
	
	public EAIMavenRepository(ResourceRepository repository) {
		this.repository = repository;
	}
	@Override
	public Artifact create(String groupId, String artifactId, String version, String packaging, InputStream input, boolean isTest) throws IOException {
		String fileName = artifactId + "-" + version + "." + packaging;
		for (MavenLibraryArtifact artifact : repository.getArtifacts(MavenLibraryArtifact.class)) {
			List<String> artifacts = artifact.getConfiguration().getArtifacts();
			if (artifacts != null && !artifacts.isEmpty()) {
				for (String id : artifacts) {
					// see if it includes a group
					int index = id.indexOf(':');
					String listedGroup = index < 0 ? null : id.substring(0, index);
					String listedName = index < 0 ? id : id.substring(index + 1);
					if ((listedGroup == null || listedGroup.equals(groupId)) && listedName.equals(artifactId)) {
						Entry entry = repository.getEntry(artifact.getId());
						if (entry instanceof ResourceEntry) {
							Resource child = ((ResourceEntry) entry).getContainer().getChild(fileName);
							if (child == null) {
								child = ((ManageableContainer<?>) ((ResourceEntry) entry).getContainer()).create(fileName, URLConnection.guessContentTypeFromName(fileName));
							}
							WritableContainer<ByteBuffer> target = ((WritableResource) child).getWritable();
							try {
								IOUtils.copyBytes(IOUtils.wrap(input), target);
							}
							finally {
								target.close();
							}
							repository.reload(artifact.getId());
							ResourceArtifact resourceArtifact = new ResourceArtifact((ReadableResource) child);
							this.artifacts.add(resourceArtifact);
							if (entry instanceof ModifiableNodeEntry) {
								// TODO: calculate the references to other modules!
								((ModifiableNodeEntry) entry).updateNode(new ArrayList<String>());
							}
							return resourceArtifact;
						}
						else {
							throw new IOException("The target maven library '" + artifact.getId() + "' is not a resource-based entry");
						}
					}
				}
			}
		}
		throw new IOException("Could not find appropriate maven library for: " + groupId + "-" + artifactId + "-" + version);
	}
	@Override
	public void scan() throws IOException {
		for (MavenLibraryArtifact artifact : repository.getArtifacts(MavenLibraryArtifact.class)) {
			List<String> artifacts = artifact.getConfiguration().getArtifacts();
			if (artifacts != null && !artifacts.isEmpty()) {
				for (String id : artifacts) {
					// see if it includes a group
					int index = id.indexOf(':');
					String artifactName = index < 0 ? id : id.substring(index + 1);
					Entry entry = repository.getEntry(artifact.getId());
					if (!(entry instanceof ResourceEntry)) {
						logger.error("Could not load maven artifact '" + id + "' in library '" + artifact.getId() + "' because it is not a resource-based entry");
					}
					else {
						for (Resource resource : ((ResourceEntry) entry).getContainer()) {
							if (resource.getName().startsWith(artifactName + "-")) {
								this.artifacts.add(new ResourceArtifact((ReadableResource) resource));
							}
						}
					}
				}
			}
			else {
				logger.warn("The maven library '" + artifact.getId() + "' has no configured artifacts");
			}
		}
	}
	@Override
	protected List<? extends Artifact> getArtifacts() {
		return artifacts;
	}
}
