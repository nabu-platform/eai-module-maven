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
