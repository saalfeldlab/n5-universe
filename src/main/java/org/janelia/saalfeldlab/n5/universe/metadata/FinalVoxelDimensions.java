package org.janelia.saalfeldlab.n5.universe.metadata;

public class FinalVoxelDimensions
{
	private final String unit;

	private final double[] dimensions;

	public FinalVoxelDimensions( final String unit, final double... dimensions )
	{
		this.unit = unit;
		this.dimensions = dimensions.clone();
	}

	public int numDimensions()
	{
		return dimensions.length;
	}

	public String unit()
	{
		return unit;
	}

	public void dimensions( final double[] dims )
	{
		for ( int d = 0; d < dims.length; ++d )
			dims[ d ] = this.dimensions[ d ];
	}

	public double dimension( final int d )
	{
		return dimensions[ d ];
	}

	public static boolean equals( FinalVoxelDimensions a, FinalVoxelDimensions b )
	{
		if( !a.unit().equals(b.unit()))
			return false;

		final double eps = 1e-9;
		for( int i = 0; i < a.numDimensions(); i++ ) {
			if (Math.abs( a.dimension(i) - b.dimension(i)) > eps)
				return false;
		}

		return true;
	}
}
