package be.nabu.eai.module.maven;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "maven")
public class MavenLibraryConfiguration {
	
	private List<String> artifacts;
	private List<String> internalDomains;
	private List<URI> repositories;
	private boolean updateSnapshots;

	public List<String> getInternalDomains() {
		return internalDomains;
	}
	public void setInternalDomains(List<String> internalDomains) {
		this.internalDomains = internalDomains;
	}
	
	public List<URI> getRepositories() {
		return repositories;
	}
	public void setRepositories(List<URI> repositories) {
		this.repositories = repositories;
	}
	
	public boolean isUpdateSnapshots() {
		return updateSnapshots;
	}
	public void setUpdateSnapshots(boolean updateSnapshots) {
		this.updateSnapshots = updateSnapshots;
	}
	
	public List<String> getArtifacts() {
		return artifacts;
	}
	public void setArtifacts(List<String> artifacts) {
		this.artifacts = artifacts;
	}

}
