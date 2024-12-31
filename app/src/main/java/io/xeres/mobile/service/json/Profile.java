/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres-Android.
 *
 * Xeres-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres-Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.mobile.service.json;

import java.time.Instant;
import java.util.List;

public class Profile
{
	private long id;
	private String name;
	private String pgpIdentifier;
	private Instant created;
	private boolean accepted;
	private Trust trust;
	private List<Location> locations;

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getPgpIdentifier()
	{
		return pgpIdentifier;
	}

	public void setPgpIdentifier(String pgpIdentifier)
	{
		this.pgpIdentifier = pgpIdentifier;
	}

	public Instant getCreated()
	{
		return created;
	}

	public void setCreated(Instant created)
	{
		this.created = created;
	}

	public boolean isAccepted()
	{
		return accepted;
	}

	public void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}

	public Trust getTrust()
	{
		return trust;
	}

	public void setTrust(Trust trust)
	{
		this.trust = trust;
	}

	public List<Location> getLocations()
	{
		return locations;
	}

	public void setLocations(List<Location> locations)
	{
		this.locations = locations;
	}
}
