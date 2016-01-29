package be.nabu.eai.module.maven;

import be.nabu.eai.repository.api.ResourceRepository;
import be.nabu.eai.server.Server;
import be.nabu.eai.server.api.ServerListener;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.server.HTTPServer;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.maven.MavenListener;

public class MavenLibraryListener implements ServerListener {

	@Override
	public void listen(Server server, HTTPServer httpServer) {
		if (server.getRepository() instanceof ResourceRepository) {
			httpServer.getDispatcher().subscribe(HTTPRequest.class, new MavenListener(new EAIMavenRepository((ResourceRepository) server.getRepository()), "/maven"))
				.filter(HTTPServerUtils.limitToPath("/maven"));
		}
	}

}
