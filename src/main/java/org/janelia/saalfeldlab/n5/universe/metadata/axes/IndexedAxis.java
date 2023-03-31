package org.janelia.saalfeldlab.n5.universe.metadata.axes;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IndexedAxis {

	private String type;

	private String label;

	private String unit;

	private int index;

	public IndexedAxis( final String type, final String label, final String unit, final int index )
	{
		this.type = type;
		this.label = label;
		this.unit = unit;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setDefaults( boolean data, int idx ) {
		setDefaultIndex( idx );
		setDefaultUnit();
		setDefaultLabel( data );
		setDefaultType();
	}

	protected void setDefaultIndex( int idx ) {
		if( index < 0 )
			index = idx;
	}
	protected void setDefaultLabel( boolean data ) {
		if( label == null )
			if( data )
				label = String.format( "data_%d", index );
			else
				label = String.format( "dim_%d", index );
	}

	protected void setDefaultType() {
		if( type == null )
			type = AxisUtils.getDefaultType(label);
	}

	protected void setDefaultUnit() {
		if( unit == null )
			unit = "none";
	}

	public String getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public String getUnit() {
		return unit;
	}

	public static IndexedAxis[] axesFromLabels(String[] labels, String[] units, int[] indexes ) {
		final String[] types = AxisUtils.getDefaultTypes(labels);

		int N = labels.length;
		IndexedAxis[] axesIdx = new IndexedAxis[N];
		for (int i = 0; i < N; i++) {
			axesIdx[i] = new IndexedAxis(types[i], labels[i], units[i], indexes[i]);
		}
		return axesIdx;
	}

	public static IndexedAxis[] axesFromLabels( String[] labels, String unitIn ) {
		String unit = unitIn == null ? "none" : unitIn;
		String[] units = new String[ labels.length ];
		Arrays.fill( units, unit );
		return axesFromLabels(labels, units, IntStream.range(0, labels.length).toArray());
	}

	public static IndexedAxis[] axesFromLabels( String[] labels, int[] indexes, String unitIn ) {
		String unit = unitIn == null ? "none" : unitIn;
		String[] units = new String[ labels.length ];
		Arrays.fill( units, unit );
		return axesFromLabels( labels, units, indexes );
	}

	public static IndexedAxis[] axesFromLabels(String... labels) {
		return axesFromLabels(labels, "none");
	}

	public static IndexedAxis[] defaultAxes(int N) {
		return defaultAxes(N, "none");
	}

	public static IndexedAxis[] defaultAxes(int N, int firstIndex) {
		return defaultAxes(N, firstIndex, "none");
	}

	public static IndexedAxis[] defaultAxes(int N, String unit ) {
		return defaultAxes(N, 0, unit);
	}

	public static IndexedAxis[] defaultAxes(int N, int firstIndex, String unit ) {
		return IntStream.range( 0, N ).mapToObj( i -> defaultAxis(firstIndex + i, unit )).toArray( IndexedAxis[]::new );
	}

	public static IndexedAxis defaultAxis(int i, String unit ) {
		return new IndexedAxis("unknown", Integer.toString(i), unit, i );
	}

	public static IndexedAxis[] axesFromIndexes(int[] indexes) {
		return Arrays.stream(indexes).mapToObj(i -> defaultAxis(i, "none")).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] axesFromIndexes(int[] indexes, String unit ) {
		return Arrays.stream(indexes).mapToObj(i -> defaultAxis(i, unit)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(int[] indexes) {
		return Arrays.stream(indexes).mapToObj(i -> dataAxis(i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(int N, int firstIndex ) {
		return IntStream.range(0, N).mapToObj(i -> dataAxis(firstIndex + i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis[] dataAxes(int N) {
		return dataAxes(N,0);
	}

	public static IndexedAxis[] dataAxesStartEnd(int start, int endInclusive) {
		return IntStream.rangeClosed(start, endInclusive).mapToObj(i -> dataAxis(i)).toArray(IndexedAxis[]::new);
	}

	public static IndexedAxis dataAxis(int i) {
		return new IndexedAxis("data", String.format("data_%d", i), "none", i);
	}
	
}
