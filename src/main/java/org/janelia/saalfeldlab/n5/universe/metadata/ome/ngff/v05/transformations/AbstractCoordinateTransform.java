package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.ArrayList;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractCoordinateTransform<T extends RealTransform> implements CoordinateTransform<T> {

	protected String type;

	protected String name;

	protected String input;

	protected String output;

	// implement
	protected transient String[] inputAxes;

	protected transient String[] outputAxes;

	protected transient CoordinateSystem inputSpaceObj;

	protected transient CoordinateSystem outputSpaceObj;

	@Override
	public abstract T getTransform();

	public AbstractCoordinateTransform( final String type,
			final String name,
			final String inputSpace, final String outputSpace ) {
		this.type = type;
		this.name = name;
		this.input = inputSpace;
		this.output = outputSpace;
	}

	public AbstractCoordinateTransform( final String type,
			final String name,
			final String[] inputAxes, final String[] outputAxes ) {
		this.type = type;
		this.name = name;
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
	}

	public AbstractCoordinateTransform( final String type, final String name ) {
		this.type = type;
		this.name = name;
	}

	public AbstractCoordinateTransform( final String type ) {
		this.type = type;
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.output = other.getOutput();
		this.inputAxes = other.getInputAxes();
		this.outputAxes = other.getOutputAxes();
	}

	public AbstractCoordinateTransform( CoordinateTransform<T> other, String[] inputAxes, String[] outputAxes )
	{
		this.name = other.getName();
		this.type = other.getType();
		this.input = other.getInput();
		this.output = other.getOutput();
		this.inputAxes = inputAxes;
		this.outputAxes = outputAxes;
	}

	/**
	 *
	 * If this object does not have input_space or output_space defined,
	 * attempts to infer the space name, given * input_axes or output_axes, if they are defined.
	 *
	 *
	 * @param spaces the spaces object
	 * @return true if input_space and output_space are defined.
	 */
	public boolean inferSpacesFromAxes( final CoordinateSystems spaces )
	{
		if( input == null && outputAxes != null )
			input = spaceNameFromAxesLabels( spaces, inputAxes );

		if( output == null && outputAxes != null )
			output = spaceNameFromAxesLabels( spaces, outputAxes );

		if( input != null && output != null )
			return true;
		else
			return false;
	}

	@Override
	public String[] getInputAxes()
	{
		if( inputAxes != null )
			return inputAxes;
		else
			return inputSpaceObj.getAxisNames();
	}

	@Override
	public String[] getOutputAxes()
	{
		if( outputAxes != null )
			return outputAxes;
		else
			return outputSpaceObj.getAxisNames();
	}

	private static String spaceNameFromAxesLabels( final CoordinateSystems spaces, final String[] axes )
	{
		final ArrayList<CoordinateSystem> candidateSpaces = spaces.getSpacesFromAxes(axes);
		if( candidateSpaces.size() == 1 )
			return candidateSpaces.get(0).getName();
		else
			return null;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getInput() {
		return input;
	}

	@Override
	public String getOutput() {
		return output;
	}

	public void setInput( final CoordinateSystem inputSpace ) {
		this.inputSpaceObj = inputSpace;
	}

	public void setOutput( final CoordinateSystem outputSpace ) {
		this.outputSpaceObj = outputSpace;
	}

	public CoordinateSystem getInputSpaceObj()
	{
		return inputSpaceObj;
	}

	public CoordinateSystem getOutputSpaceObj()
	{
		return outputSpaceObj;
	}

	public AbstractCoordinateTransform<T> setNameSpaces( final String name, final String in, final String out )
	{
		this.name = name;
		this.input = in;
		this.output = out;
		return this;
	}

	@Override
	public String toString()
	{
		return String.format("%s:(%s > %s)", name, input, output);
	}

//	@Override
//	public RealCoordinate apply( final RealCoordinate src, final RealCoordinate dst ) {
//
//		final T t = getTransform();
//
////		RealCoordinate out = new RealCoordinate(t.numTargetDimensions());
//		// this needs to work on subspaces correctly
//
//		// the simple case
//		if( src.getSpace().axesEquals( getInputSpaceObj() ))
//		{
//			t.apply(src, dst);
//			dst.setSpace(getOutputSpaceObj());
//			return dst;
//		}
//
////		if( !src.getSpace().isSubspaceOf( getInputSpaceObj() ))
//		if( ! getInputSpaceObj().isSubspaceOf( src.getSpace() ))
//		{
//			System.err.println("WARNING: input point's space does not match transforms space.\n" );
//		}
//
//		final int[] inPermParams = AxisUtils.findPermutation(
//				src.getSpace().getAxisLabels(), getInputSpaceObj().getAxisLabels() );
//
//		final int nd = src.numDimensions(); // should this be a max over src, outputDims ?
//		final int[] perm = AxisUtils.fillPermutation(inPermParams, nd );
//
//		final RealTransformSequence totalTransform = new RealTransformSequence();
//		final RealComponentMappingTransform pre = new RealComponentMappingTransform( perm.length, perm );
//		totalTransform.add(pre);
//		totalTransform.add(t);
//		totalTransform.apply(src, dst);
//
//		// copy coordinate values from src for unaffected dimensions
//		int j = t.numSourceDimensions();
//		for( int i = t.numTargetDimensions(); i < dst.numDimensions() && j < src.numDimensions(); i++ )
//			dst.setPosition( src.getDoublePosition(perm[j++]), i);
//
////		Space srcRem = src.getSpace().diff("", getInputSpaceObj());
////		System.out.println( "srcRem : " + Arrays.toString( srcRem.getAxisLabels() ));
//
//		dst.setSpace(
//				getOutputSpaceObj().union("",
//						src.getSpace().diff("", getInputSpaceObj())));
//
//		return dst;
//	}

//	public RealCoordinate applyAppend( final RealCoordinate src ) {
//
//		final T t = getTransform();
//
//		final RealCoordinate dst = new RealCoordinate( t.numTargetDimensions() );
//		if( src.getSpace().axesEquals( getInputSpaceObj() ))
//		{
//			t.apply(src, dst);
//			dst.setSpace(getOutputSpaceObj());
//			return src.append(dst);
//		}
//
//		if( ! getInputSpaceObj().isSubspaceOf( src.getSpace() ))
//		{
//			System.err.println("WARNING: input point's space does not match transforms space.\n" );
//		}
//
//		final int[] inPermParams = AxisUtils.findPermutation(
//				src.getSpace().getAxisLabels(), getInputSpaceObj().getAxisLabels() );
//
//		final int nd = src.numDimensions(); // should this be a max over src, outputDims ?
//		final int[] perm = AxisUtils.fillPermutation(inPermParams, nd );
//
//		final RealTransformSequence totalTransform = new RealTransformSequence();
//		final RealComponentMappingTransform pre = new RealComponentMappingTransform( perm.length, perm );
//		totalTransform.add(pre);
//		totalTransform.add(t);
//		totalTransform.apply(src, dst);
//		dst.setSpace(getOutputSpaceObj());
//		return src.append(dst);
//
////		// copy coordinate values from src for unaffected dimensions
////		int j = t.numSourceDimensions();
////		for( int i = t.numTargetDimensions(); i < dst.numDimensions() && j < src.numDimensions(); i++ )
////			dst.setPosition( src.getDoublePosition(perm[j++]), i);
////
//////		Space srcRem = src.getSpace().diff("", getInputSpaceObj());
//////		System.out.println( "srcRem : " + Arrays.toString( srcRem.getAxisLabels() ));
////
////		dst.setSpace(
////				getOutputSpaceObj().union("",
////						src.getSpace().diff("", getInputSpaceObj())));
////
////		return dst;
//	}

//	public AxisPoint applyAxes( final AxisPoint src ) {
//
//		final T t = getTransform();
//
//		// check if this transform's input axes are a subspace
//		// of the source point
//		final double[] in = new double[ t.numSourceDimensions() ];  // TODO optimize
//		final double[] out = new double[ t.numTargetDimensions() ];
//
//		if( getInputSpaceObj().isSubspaceOf( src.axisOrder() ))
//		{
//			src.localize( in, getInputAxes() );
//		}
//		else if( src.numDimensions() >= t.numSourceDimensions() )
//		{
//			System.err.println("WARNING: using first N dimensions of source point" );
//			// if not, default to using the first N dimensions
//			src.localize( in );
//		}
//		else
//		{
//			return null;
//		}
//
//		final AxisPoint dst = src;
//		t.apply( in, out );
//		dst.setPositions( out, getOutputAxes() );
//		return dst;
//
//	}

}
