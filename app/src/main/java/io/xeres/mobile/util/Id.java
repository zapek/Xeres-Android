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

package io.xeres.mobile.util;

public final class Id
{
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	public static String toString(byte[] id)
	{
		var sb = new StringBuilder(id.length * 2);

		for (var b : id)
		{
			sb.append(HEX[(b & 0xf0) >> 4])
					.append(HEX[b & 0x0f]);
		}
		return sb.toString();
	}
}
