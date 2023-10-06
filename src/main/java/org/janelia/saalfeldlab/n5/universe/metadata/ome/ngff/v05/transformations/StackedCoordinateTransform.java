package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.graph.CoordinateSystems;

import net.imglib2.realtransform.RealComponentMappingTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.StackedRealTransform;

public class StackedCoordinateTransform extends AbstractCoordinateTransform<RealTransform> {

	private final List<CoordinateTransform<?>> transforms;

	private transient CoordinateSystems spaces;

	private transient RealTransform totalTransform;

	public StackedCoordinateTransform(
			final String name,
			final String inputSpace, final String outputSpace,
			final List<CoordinateTransform<?>> transforms )
	{
		super( "stacked", name, inputSpace, outputSpace );
		this.transforms = transforms;
	}

	public StackedCoordinateTransform(
			final String name,
			final String inputSpace, final String outputSpace,
			final CoordinateTransform<?>[] transforms )
	{
		this( name, inputSpace, outputSpace, Arrays.stream( transforms ).collect( Collectors.toList() ));
	}

	public void setSpaces( final CoordinateSystems spaces )
	{
		this.spaces = spaces;
	}

	public String[] inputAxesLabels()
	{
		return  transforms.stream().flatMap( t -> Arrays.stream(t.getInputAxes())).toArray( String[]::new );
	}

	public String[] outputAxesLabels()
	{
		return  transforms.stream().flatMap( t -> Arrays.stream(t.getOutputAxes())).toArray( String[]::new );
	}

	public RealTransform buildTransform()
	{
		final RealTransform[] arr = transforms.stream()
				.map( x -> (RealTransform)x.getTransform() )
				.toArray( RealTransform[]::new );

		final StackedRealTransform stackedTransform = new StackedRealTransform(arr);

		if( spaces != null )
		{
			final String[] inputAxisLabels = spaces.getSpace(getInput()).getAxisNames();
			final String[] tformInputAxisLabels = inputAxesLabels();

			final String[] outputAxisLabels = spaces.getSpace(getOutput()).getAxisNames();
			final String[] tformOutputAxisLabels = outputAxesLabels();

			RealTransform pre = null;
			if( !Arrays.equals(inputAxisLabels, tformInputAxisLabels))
			{
				// go from the input axis order to the transform's input axis order
				final int[] inPermParams = AxisUtils.findPermutation(inputAxisLabels, tformInputAxisLabels);
				pre = new RealComponentMappingTransform( inPermParams.length, inPermParams);
			}

			RealTransform post = null;
			if( !Arrays.equals(outputAxisLabels, tformOutputAxisLabels))
			{
				// go from the transforms output to the output axis order
				final int[] outPermParams = AxisUtils.findPermutation(tformOutputAxisLabels, outputAxisLabels );
				post = new RealComponentMappingTransform( outPermParams.length, outPermParams);
			}

			if( pre == null && post == null )
			{
				totalTransform = stackedTransform;
				return totalTransform;
			}
			else
			{
				final RealTransformSequence seq = new RealTransformSequence();
				if( pre != null )
					seq.add( pre );

				seq.add(stackedTransform);

				if( post != null )
					seq.add( post );

				totalTransform = seq;
				return totalTransform;
			}
		}
		else
		{
			totalTransform = stackedTransform;
			return totalTransform;
		}
	}

	@Override
	public RealTransform getTransform() {
		if( totalTransform == null )
			buildTransform();

		return totalTransform;
	}

}
