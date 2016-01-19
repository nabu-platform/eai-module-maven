package be.nabu.eai.module.maven;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class MavenLibraryGUIManager extends BaseJAXBGUIManager<MavenLibraryConfiguration, MavenLibraryArtifact> {

	public MavenLibraryGUIManager() {
		super("Maven Library", MavenLibraryArtifact.class, new MavenLibraryManager(), MavenLibraryConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected MavenLibraryArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new MavenLibraryArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

}
