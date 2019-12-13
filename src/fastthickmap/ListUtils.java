package fastthickmap;

import java.util.ArrayList;

public class ListUtils {

	/**
	 * Merges two sorted arrays.
	 * 
	 * @param arr1   First array.
	 * @param arr2   Second array.
	 * @param target Target array that will contain elements of arr1 and arr2.
	 */
	public static <T extends Comparable<T>> void merge(ArrayList<T> arr1, ArrayList<T> arr2, ArrayList<T> target) {
		int i = 0;
		int j = 0;

		target.clear();
		target.ensureCapacity(arr1.size() + arr2.size());

		while (i < arr1.size() && j < arr2.size()) {
			T arr1i = arr1.get(i);
			T arr2j = arr2.get(j);
			if (arr1i.compareTo(arr2j) < 0) {
				target.add(arr1i);
				i++;
			} else {
				target.add(arr2j);
				j++;
			}
		}

		while (i < arr1.size())
			target.add(arr1.get(i++));

		while (j < arr2.size())
			target.add(arr2.get(j++));
	}

}
