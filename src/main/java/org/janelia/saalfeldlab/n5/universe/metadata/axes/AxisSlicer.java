package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

public class AxisSlicer {
	
	private final AxisMetadata axes;

	private HashMap<Integer,Integer> sliceMap;

	public AxisSlicer( AxisMetadata axes ) {
		this.axes = axes;
		sliceMap = new HashMap<Integer,Integer>();
	}

	/**
	 * Slices at position 0 for the given label.
	 * @param label the label
	 * @return this
	 */
	public AxisSlicer slice( String label ) {
		return slice( axes.indexOf(label), 0 );
	}

	public AxisSlicer slice( String label, int position ) {
		return slice( axes.indexOf(label), position );
	}

	public AxisSlicer sliceOverride( String label, int position ) {
		return slice( axes.indexOf(label), position );
	}

	/**
	 * Slices the first axis of the given type at the pr
	 * 
	 * @param type the axis type
	 * @param position position
	 * @return this slicer object
	 */
	public AxisSlicer sliceType( String type, int position ) {
		return sliceType( type, 0, position );
	}

	public AxisSlicer sliceType( String type, int typeIndex, int position ) {
		assert typeIndex >= 0;

		final int[] idxs = axes.indexesOfType(type);
		if( typeIndex < idxs.length )
			return slice( idxs[ typeIndex ], position );
		else
			return this;
	}

//	public AxisSlicer sliceSpace(DefaultSpaceMetadata space, int[] positions) {
//
////		final int[] idxs = space.getAxes().getIndexes();
//		final int[] idxs = space.getIndexes();
//		for (int i = 0; i < idxs.length; i++)
//			slice(idxs[i], positions[i]);
//
//		return this;
//	}
	
	public AxisSlicer slice( int dimension, int position ) {

		if (!sliceMap.containsKey(dimension))
			sliceMap.put(dimension, position);

		return this;
	}
	
	public AxisSlicer sliceOverride( int dimension, int position ) {

		sliceMap.put(dimension, position);
		return this;
	}

	public <T> RandomAccessibleInterval<T> apply( final RandomAccessibleInterval<T> img ) {

		// need to slice starting from last dimensions
		// since dimension indexes are relative to original dimensionality
		final TreeSet<Integer> indexes = new TreeSet<>(sliceMap.keySet());
		RandomAccessibleInterval<T> out = img;
		final Iterator<Integer> dit = indexes.descendingIterator();
		while (dit.hasNext()) {
			int d = dit.next();
			out = Views.hyperSlice(out, d, sliceMap.get(d));
		}

		return out;	
	}

}
