package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IndexedAxis extends Axis {

	private int index;

	public IndexedAxis(final String type, final String label, final String unit, final int index) {

		super(type, label, unit);
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setDefaults( final boolean data, final int idx ) {
		setDefaultIndex( idx );
		setDefaultUnit();
		setDefaultLabel( data );
		setDefaultType();
	}

	protected void setDefaultIndex( final int idx ) {
		if( index < 0 )
			index = idx;
	}
	protected void setDefaultLabel( final boolean data ) {
		if( name == null )
			if( data )
				name = String.format( "data_%d", index );
			else
				name = String.format( "dim_%d", index );
	}

	protected void setDefaultType() {
		if( type == null )
			type = AxisUtils.getDefaultType(name);
	}

	protected void setDefaultUnit() {
		if( unit == null )
			unit = "none";
	}

	public static IndexedAxis[] axesFromLabels(final String[] labels, final String[] units, final int[] indexes ) {
		final String[] types = AxisUtils.getDefaultTypes(labels);

		final int N = labels.length;
		final IndexedAxis[] axesIdx = new IndexedAxis[N];
		for (int i = 0; i < N; i++) {
			axesIdx[i] = new IndexedAxis(types[i], labels[i], units[i], indexes[i]);
		}
		return axesIdx;
	}

	public static IndexedAxis[] axesFromLabels( final String[] labels, final String unitIn ) {
		final String unit = unitIn == null ? "none" : unitIn;
		final String[] units = new String[ labels.length ];
		Arrays.fill( units, unit );
		return axesFromLabels(labels, units, IntStream.range(0, labels.length).toArray());
	}

	public static IndexedAxis[] axesFromLabels( final String[] labels, final int[] indexes, final String unitIn ) {
		final String unit = unitIn == null ? "none" : unitIn;
		final String[] units = new String[ labels.length ];
		Arrays.fill( units, unit );
		return axesFromLabels( labels, units, indexes );
	}

	public static IndexedAxis[] axesFromLabels(final String... labels) {
		return axesFromLabels(labels, "none");
	}

	public static IndexedAxis[] defaultAxes(final int N) {
		return defaultAxes(N, "none");
	}

	public static IndexedAxis[] defaultAxes(final int N, final int firstIndex) {
		return defaultAxes(N, firstIndex, "none");
	}

	public static IndexedAxis[] defaultAxes(final int N, final String unit ) {
		return defaultAxes(N, 0, unit);
	}

	public static IndexedAxis[] defaultAxes(final int N, final int firstIndex, final String unit ) {
		return IntStream.range( 0, N ).mapToObj( i -> defaultAxis(firstIndex + i, unit )).toArray( IndexedAxis[]::new );
	}

	public static IndexedAxis defaultAxis(final int i, final String unit ) {
		return new IndexedAxis("unknown", Integer.toString(i), unit, i );
	}

	public static IndexedAxis[] axesFromIndexes(final int[] indexes) {
		return Arrays.stream(indexes).mapToObj(i -> defaultAxis(i, "none")).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] axesFromIndexes(final int[] indexes, final String unit ) {
		return Arrays.stream(indexes).mapToObj(i -> defaultAxis(i, unit)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(final int[] indexes) {
		return Arrays.stream(indexes).mapToObj(i -> dataAxis(i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(final int N, final int firstIndex ) {
		return IntStream.range(0, N).mapToObj(i -> dataAxis(firstIndex + i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(final int N) {
		return dataAxes(N,0);
	}

	public static IndexedAxis[] dataAxesStartEnd(final int start, final int endInclusive) {
		return IntStream.rangeClosed(start, endInclusive).mapToObj(i -> dataAxis(i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis dataAxis(final int i) {
		return new IndexedAxis("data", String.format("data_%d", i), "none", i);
	}

}
