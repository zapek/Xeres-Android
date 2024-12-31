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

import java.util.Objects;

import io.xeres.mobile.R;

public final class ColorGenerator
{
	private ColorGenerator()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private enum ColorSpec
	{
		COLOR_00(R.color.color_00),
		COLOR_01(R.color.color_01),
		COLOR_02(R.color.color_02),
		COLOR_03(R.color.color_03),
		COLOR_04(R.color.color_04),
		COLOR_05(R.color.color_05),
		COLOR_06(R.color.color_06),
		COLOR_07(R.color.color_07),
		COLOR_08(R.color.color_08),
		COLOR_09(R.color.color_09),
		COLOR_10(R.color.color_10),
		COLOR_11(R.color.color_11),
		COLOR_12(R.color.color_12),
		COLOR_13(R.color.color_13),
		COLOR_14(R.color.color_14),
		COLOR_15(R.color.color_15);

		private final int colorResource;

		ColorSpec(int colorResource)
		{
			this.colorResource = colorResource;
		}

		public int getColorResource()
		{
			return colorResource;
		}
	}

	public static int generateColor(String s)
	{
		Objects.requireNonNull(s);
		return ColorSpec.values()[Math.floorMod(s.hashCode(), ColorSpec.values().length)].getColorResource();
	}
}
