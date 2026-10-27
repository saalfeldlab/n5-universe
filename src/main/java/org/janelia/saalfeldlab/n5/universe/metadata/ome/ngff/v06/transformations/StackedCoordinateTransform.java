package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.transformations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v06.graph.CoordinateSystems;

import net.imglib2.realtransform.RealComponentMappingTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.StackedRealTransform;

public class StackedCoordinateTransform extends AbstractCoordinateTransform<RealTransform> {

	public static final String TYPE = "stacked";

	private final List<SubAxisTransformation> transforms;

	private transient CoordinateSystems spaces;

	private transient RealTransform totalTransform;

	private StackedCoordinateTransform( final Options options )
	{
		super( TYPE, options );
		this.transforms = options.transforms == null ? new ArrayList<>() : new ArrayList<>( options.transforms );
	}

	public static class Options extends TransformOptions<Options>
	{
		private List<SubAxisTransformation> transforms;

		public Options withTransforms( final List<SubAxisTransformation> transforms )
		{
			this.transforms = transforms;
			return this;
		}

		public Options withTransforms( final SubAxisTransformation... transforms )
		{
			return withTransforms( transforms == null ? null : Arrays.asList( transforms ) );
		}

		public StackedCoordinateTransform build()
		{
			return new StackedCoordinateTransform( this );
		}
	}

	public static Options options()
	{
		return new Options();
	}

	protected StackedCoordinateTransform( final StackedCoordinateTransform other, final TransformOptions<?> copyParams )
	{
		super( other, copyParams );
		this.transforms = other.transforms == null ? null
				: other.transforms.stream().map( SubAxisTransformation::copy ).collect( Collectors.toList() );
		this.spaces = other.spaces;
	}

	@Override
	public StackedCoordinateTransform copy()
	{
		return copy( new BasicOptions() );
	}

	@Override
	public StackedCoordinateTransform copy( final TransformOptions<?> copyParams )
	{
		return new StackedCoordinateTransform( this, copyParams );
	}

	public void setSpaces( final CoordinateSystems spaces )
	{
		this.spaces = spaces;
	}

	public int[] inputAxesLabels()
	{
		return transforms.stream().flatMapToInt(e -> Arrays.stream(e.getInputAxes())).toArray();
	}

	public int[] outputAxesLabels()
	{
		return transforms.stream().flatMapToInt(e -> Arrays.stream(e.getOutputAxes())).toArray();
	}

	public RealTransform buildTransform()
	{
		final RealTransform[] arr = transforms.stream()
				.map( e -> (RealTransform)e.getTransformation().getTransform() )
				.toArray( RealTransform[]::new );

		final StackedRealTransform stackedTransform = new StackedRealTransform(arr);

		if( spaces != null )
		{
			final int nIn = spaces.getSpace(getInput().getName()).numDimensions();
			final int[] tformInputAxes = inputAxesLabels();

			final int nOut = spaces.getSpace(getOutput().getName()).numDimensions();
			final int[] tformOutputAxes = outputAxesLabels();

			RealTransform pre = null;
			for (int i = 0; i < nIn; i++) {
				if (tformInputAxes[i] != i) {
					pre = new RealComponentMappingTransform(nIn, tformInputAxes);
					break;
				}
			}

			final int[] invPerm = new int[nOut];
			boolean isNatural = true;
			for (int i = 0; i < nOut; i++) {
				invPerm[tformOutputAxes[i]] = i;
				if (tformOutputAxes[i] != i) isNatural = false;
			}
			final RealTransform post = isNatural ? null : new RealComponentMappingTransform(nOut, invPerm);

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
