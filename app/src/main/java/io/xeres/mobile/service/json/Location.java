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

public class Location
{
	private long id;
	private String name;
	private byte[] locationIdentifier;
	private boolean connected;
	private Instant lastConnected;
	private Availability availability;
	private String version;

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

	public boolean isConnected()
	{
		return connected;
	}

	public void setConnected(boolean connected)
	{
		this.connected = connected;
	}

	public Instant getLastConnected()
	{
		return lastConnected;
	}

	public void setLastConnected(Instant lastConnected)
	{
		this.lastConnected = lastConnected;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public Availability getAvailability()
	{
		return availability;
	}

	public void setAvailability(Availability availability)
	{
		this.availability = availability;
	}

	public byte[] getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(byte[] locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
	}
}
