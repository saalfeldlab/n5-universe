package org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v05.transformations;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

	public int[] inputAxesLabels()
	{
		return transforms.stream().flatMapToInt(t -> Arrays.stream(t.getInputAxes())).toArray();
	}

	public int[] outputAxesLabels()
	{
		return transforms.stream().flatMapToInt(t -> Arrays.stream(t.getOutputAxes())).toArray();
	}

	public RealTransform buildTransform()
	{
		final RealTransform[] arr = transforms.stream()
				.map( x -> (RealTransform)x.getTransform() )
				.toArray( RealTransform[]::new );

		final StackedRealTransform stackedTransform = new StackedRealTransform(arr);

		if( spaces != null )
		{
			final int nIn = spaces.getSpace(getInput()).numDimensions();
			final int[] tformInputAxes = inputAxesLabels();

			final int nOut = spaces.getSpace(getOutput()).numDimensions();
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
