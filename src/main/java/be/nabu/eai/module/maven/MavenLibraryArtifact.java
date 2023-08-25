package be.nabu.eai.module.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.RepositoryTypeResolver;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.libs.artifacts.LocalClassLoader;
import be.nabu.libs.artifacts.api.ClassProvidingArtifact;
import be.nabu.libs.artifacts.api.LazyArtifact;
import be.nabu.libs.maven.api.Artifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;
import be.nabu.libs.services.maven.MavenArtifact;
import be.nabu.libs.services.pojo.POJOInterfaceResolver;
import be.nabu.libs.types.DefinedSimpleTypeResolver;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.SPIDefinedTypeResolver;
import be.nabu.libs.types.SimpleTypeWrapperFactory;

public class MavenLibraryArtifact extends JAXBArtifact<MavenLibraryConfiguration> implements LazyArtifact, ClassProvidingArtifact, DefinedServiceInterfaceResolver {

	private be.nabu.libs.maven.ResourceRepository domainRepository;
	private List<MavenArtifact> artifacts, internalArtifacts;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public MavenLibraryArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "maven.xml", MavenLibraryConfiguration.class);
		// reattach maven artifacts on load/reload, because they are not necessarily part of their own artifact folder, they can be wiped by the reload of any part of the repository
//		getRepository().getEventDispatcher().subscribe(RepositoryEvent.class, new EventHandler<RepositoryEvent, Void>() {
//			@Override
//			public Void handle(RepositoryEvent event) {
//				if ((event.getState() == RepositoryState.LOAD || event.getState() == RepositoryState.RELOAD) && event.isDone()) {
//					for (MavenArtifact artifact : artifacts) {
//						try {
//							MavenManager.attachChildren((ModifiableEntry) getRepository().getRoot(), artifact, getId());
//						}
//						catch (IOException e) {
//							logger.error("Could not reattach maven artifact: " + artifact, e);
//						}
//					}
//				}
//				return null;
//			}
//		});
	}

	@Override
	public void forceLoad() {
		super.forceLoad();
		domainRepository = new be.nabu.libs.maven.ResourceRepository(getDirectory(), getRepository().getEventDispatcher());
		try {
			domainRepository.getDomains().addAll(getConfiguration().getInternalDomains() == null ? new ArrayList<String>() : getConfiguration().getInternalDomains());
			domainRepository.scan(false);
			DefinedTypeResolverFactory factory = new DefinedTypeResolverFactory();
			factory.addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
			factory.addResolver(new RepositoryTypeResolver(getRepository()));
			factory.addResolver(new SPIDefinedTypeResolver());
			MavenManager mavenManager = new MavenManager(domainRepository, factory.getResolver());
			artifacts = new ArrayList<MavenArtifact>();
			internalArtifacts = new ArrayList<MavenArtifact>();
			Set<Artifact> internalArtifacts = domainRepository.getInternalArtifacts();
			// first we load the internal artifacts
			// internal artifacts are exposed as services/beans etc
			for (be.nabu.libs.maven.api.Artifact internal : internalArtifacts) {
				logger.info("Loading internal maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
				MavenArtifact artifact = loadArtifact(mavenManager, internal);
				this.artifacts.add(artifact);
				this.internalArtifacts.add(artifact);
			}
			// if you have explicitly configured artifacts, we load them as well
			// non-internal artifacts are available to the classloader but not loaded as services/beans/...
			if (getConfig().getArtifacts() != null) {
				for (String artifact : getConfig().getArtifacts()) {
					int indexOf = artifact.indexOf(':');
					if (indexOf > 0) {
						String groupId = artifact.substring(0, indexOf);
						String artifactId = artifact.substring(indexOf + 1);
						SortedSet<String> versions = domainRepository.getVersions(groupId, artifactId);
						boolean isLoaded = false;
						Artifact mavenArtifact = null;
						for (String version : versions) {
							Artifact tmp = domainRepository.getArtifact(groupId, artifactId, version, false);
							if (tmp != null) {
								// make sure we keep the last one (highest version)
								mavenArtifact = tmp;
								// if we already loaded any version of it, we can ignore it
								isLoaded |= internalArtifacts.contains(tmp);
							}
						}
						if (!isLoaded && mavenArtifact != null) {
							logger.info("Loading external maven artifact " + mavenArtifact.getGroupId() + " > " + mavenArtifact.getArtifactId());
							this.artifacts.add(loadArtifact(mavenManager, mavenArtifact));
						}
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to force load " + getId(), e);
		}
	}

	private MavenArtifact loadArtifact(MavenManager mavenManager, be.nabu.libs.maven.api.Artifact mavenArtifact) throws IOException {
		MavenArtifact artifact = mavenManager.load(getRepository(), mavenArtifact, getConfiguration().isUpdateSnapshots(), getConfiguration().getRepositories() == null ? new URI[0] : getConfiguration().getRepositories().toArray(new URI[0]));
		if (getConfiguration().getProvided() != null) {
			for (String provided : getConfiguration().getProvided()) {
				String [] split = provided.split(":");
				if (split.length != 2) {
					throw new RuntimeException("Could not correctly set provided library: " + provided);
				}
				artifact.getClassLoader().addProvided(split[0], split[1]);
			}
		}
		// add a whitelist if we encapsulated it
		if (getConfig().isEncapsulated()) {
			for (LocalClassLoader loader : artifact.getClassLoaders()) {
				// the properties files are for language bundles
				List<String> whitelist = new ArrayList<String>(Arrays.asList("META-INF/services/be\\.nabu\\..*", ".*\\.png", ".*\\.properties", ".*\\.jpg", ".*\\.svg", ".*\\.jpeg"));
				if (getConfig().getEncapsulationWhitelists() != null) {
					whitelist.addAll(getConfig().getEncapsulationWhitelists());
				}
				loader.setResourceWhitelist(whitelist);
			}
		}
		return artifact;
	}

	@Override
	public List<Class<?>> getImplementationsFor(Class<?> clazz) throws IOException {
		List<Class<?>> classes = new ArrayList<Class<?>>();
		if (artifacts != null) {
			for (MavenArtifact artifact : artifacts) {
				List<Class<?>> implementationsFor = artifact.getImplementationsFor(clazz);
				if (implementationsFor != null) {
					classes.addAll(implementationsFor);
				}
			}
		}
		return classes;
	}

	@Override
	public Class<?> loadClass(String id) {
		if (artifacts != null) {
			for (MavenArtifact artifact : artifacts) {
				try {
					Class<?> loadClass = artifact.loadClass(id);
					if (loadClass != null) {
						return loadClass;
					}
				}
				catch (ClassNotFoundException e) {
					// do nothing
				}
			}
		}
		return null;
	}

	@Override
	public InputStream loadResource(String id) {
		if (artifacts != null) {
			boolean encapsulated = false;
			// if it is encapsulated, we only return nabu-based service files and images
			if (getConfig().isEncapsulated() && !id.matches("META-INF/services/be\\.nabu\\..*") && !id.matches(".*\\.png")) {
				encapsulated = true;
				if (getConfig().getEncapsulationWhitelists() != null) {
					for (String regex : getConfig().getEncapsulationWhitelists()) {
						if (id.matches(regex)) {
							encapsulated = false;
							break;
						}
					}
				}
			}
			if (encapsulated) {
				return null;
			}
			for (MavenArtifact artifact : artifacts) {
				InputStream loadResource = artifact.loadResource(id);
				if (loadResource != null) {
					return loadResource;
				}
			}
		}
		return null;
	}

	@Override
	public DefinedServiceInterface resolve(String id) {
		if (artifacts != null) {
			for (MavenArtifact artifact : artifacts) {
				DefinedServiceInterface resolved = new POJOInterfaceResolver(artifact.getClassLoader()).resolve(id);
				if (resolved != null) {
					return resolved;
				}
			}
		}
		return null;
	}

	public List<MavenArtifact> getArtifacts() {
		return artifacts;
	}
	
	public List<MavenArtifact> getInternalArtifacts() {
		return internalArtifacts;
	}

	@Override
	public Collection<LocalClassLoader> getClassLoaders() {
		List<LocalClassLoader> classLoaders = new ArrayList<LocalClassLoader>();
		if (artifacts != null) {
			for (MavenArtifact artifact : artifacts) {
				classLoaders.add(artifact.getClassLoader());
			}
		}
		return classLoaders;
	}
}
