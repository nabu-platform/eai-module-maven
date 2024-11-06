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
