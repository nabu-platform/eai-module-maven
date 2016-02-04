package be.nabu.eai.module.maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.repository.RepositoryTypeResolver;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.managers.MavenManager;
import be.nabu.libs.artifacts.LocalClassLoader;
import be.nabu.libs.artifacts.api.ClassProvidingArtifact;
import be.nabu.libs.artifacts.api.LazyArtifact;
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
	private List<MavenArtifact> artifacts;
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
			if (getConfiguration().getInternalDomains() != null) {
				domainRepository.getDomains().addAll(getConfiguration().getInternalDomains());
				domainRepository.scan(false);
				DefinedTypeResolverFactory factory = new DefinedTypeResolverFactory();
				factory.addResolver(new DefinedSimpleTypeResolver(SimpleTypeWrapperFactory.getInstance().getWrapper()));
				factory.addResolver(new RepositoryTypeResolver(getRepository()));
				factory.addResolver(new SPIDefinedTypeResolver());
				MavenManager mavenManager = new MavenManager(domainRepository, factory.getResolver());
				artifacts = new ArrayList<MavenArtifact>();
				for (be.nabu.libs.maven.api.Artifact internal : domainRepository.getInternalArtifacts()) {
					logger.info("Loading maven artifact " + internal.getGroupId() + " > " + internal.getArtifactId());
					MavenArtifact artifact = mavenManager.load(getRepository(), internal, getConfiguration().isUpdateSnapshots(), getConfiguration().getRepositories() == null ? new URI[0] : getConfiguration().getRepositories().toArray(new URI[0]));
					if (getConfiguration().getProvided() != null) {
						for (String provided : getConfiguration().getProvided()) {
							String [] split = provided.split(":");
							if (split.length != 2) {
								throw new RuntimeException("Could not correctly set provided library: " + provided);
							}
							artifact.getClassLoader().addProvided(split[0], split[1]);
						}
					}
					artifacts.add(artifact);
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to force load " + getId(), e);
		}
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
