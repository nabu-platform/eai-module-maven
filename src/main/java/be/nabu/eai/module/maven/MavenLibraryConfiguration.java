package be.nabu.eai.module.maven;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "maven")
public class MavenLibraryConfiguration {
	
	private List<String> artifacts;
	private List<String> internalDomains;
	private List<String> provided;
	private List<URI> repositories;
	private boolean updateSnapshots, encapsulated;
	// you can add whitelist specific stuff in encapsulation
	private List<String> encapsulationWhitelists;

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
	public List<String> getProvided() {
		return provided;
	}
	public void setProvided(List<String> provided) {
		this.provided = provided;
	}
	public boolean isEncapsulated() {
		return encapsulated;
	}
	public void setEncapsulated(boolean encapsulated) {
		this.encapsulated = encapsulated;
	}
	public List<String> getEncapsulationWhitelists() {
		return encapsulationWhitelists;
	}
	public void setEncapsulationWhitelists(List<String> encapsulationWhitelists) {
		this.encapsulationWhitelists = encapsulationWhitelists;
	}
}
