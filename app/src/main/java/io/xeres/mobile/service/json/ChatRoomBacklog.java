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

public class ChatRoomBacklog
{
	private Instant created;
	private GxsId gxsId;
	private String nickname;
	private String message;

	public ChatRoomBacklog()
	{
	}

	public ChatRoomBacklog(String nickname, GxsId gxsId, String message)
	{
		this.created = Instant.now();
		this.nickname = nickname;
		this.gxsId = gxsId;
		this.message = message;
	}

	public Instant getCreated()
	{
		return created;
	}

	public void setCreated(Instant created)
	{
		this.created = created;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public String getNickname()
	{
		return nickname;
	}

	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
}