/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package eu.kruzer.wgextender.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Transform {

	public static <T, O> List<T> toList(Iterable<O> list, Function<O, T> transform) {
		List<T> transformedList = new ArrayList<>();
		for (O element : list) {
			transformedList.add(transform.apply(element));
		}
		return transformedList;
	}

	public static <T, O> List<T> toList(O[] array, Function<O, T> transform) {
		return toList(Arrays.asList(array), transform);
	}

}
