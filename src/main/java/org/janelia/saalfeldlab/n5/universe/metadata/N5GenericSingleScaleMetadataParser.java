package org.janelia.saalfeldlab.n5.universe.metadata;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5LabelMultisets;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * A parser for {@link N5SingleScaleMetadata} with whose keys
 * can be specified.
 *
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class N5GenericSingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata> {

  public static final String DEFAULT_MIN = "min";
  public static final String DEFAULT_MAX = "max";
  public static final String DEFAULT_RESOLUTION = "resolution";
  public static final String DEFAULT_OFFSET = "offset";
  public static final String DEFAULT_UNIT = "unit";
  public static final String DEFAULT_DOWNSAMPLING_FACTORS = "downsamplingFactors";

  public final String minKey;
  public final String maxKey;
  public final String resolutionKey;
  public final String offsetKey;
  public final String unitKey;
  public final String downsamplingFactorsKey;

  public boolean minKeyStrict = false;
  public boolean maxKeyStrict = false;
  public boolean resolutionKeyStrict = false;
  public boolean offsetKeyStrict = false;
  public boolean unitKeyStrict = false;
  public boolean downsamplingFactorsKeyStrict = false;

  public N5GenericSingleScaleMetadataParser() {
	  minKey = DEFAULT_MIN;
	  maxKey = DEFAULT_MAX;
	  resolutionKey = DEFAULT_RESOLUTION;
	  offsetKey = DEFAULT_OFFSET;
	  unitKey = DEFAULT_UNIT;
	  downsamplingFactorsKey = DEFAULT_DOWNSAMPLING_FACTORS;
  }

  public N5GenericSingleScaleMetadataParser(final String minKey, final String maxKey,
		  final String resolutionKey, final String offsetKey, final String unitKey,
		  final String downsamplingFactorsKey ) {

	this.minKey = minKey;
	this.maxKey = maxKey;
	this.resolutionKey = resolutionKey;
	this.offsetKey = offsetKey;
	this.unitKey = unitKey;
	this.downsamplingFactorsKey = downsamplingFactorsKey;
  }

  public static Builder builder() {

	return new Builder();
  }

  public static Builder builder( final boolean useDefaults ) {

	final Builder builder = new Builder();
	if( useDefaults )
	{
		builder.minKey = DEFAULT_MIN;
		builder.maxKey = DEFAULT_MAX;
		builder.resolutionKey = DEFAULT_RESOLUTION;
		builder.offsetKey = DEFAULT_OFFSET;
		builder.downsamplingFactorsKey = DEFAULT_DOWNSAMPLING_FACTORS;
		builder.unitKey = DEFAULT_UNIT;
	}
	return builder;
  }

  public static class Builder {

	private String minKey = "";
	private String maxKey = "";
	private String resolutionKey = "";
	private String offsetKey = "";
	private String downsamplingFactorsKey = "";
	private String unitKey = "";

	private boolean minStrict = false;
	private boolean maxStrict = false;
	private boolean resolutionStrict = false;
	private boolean offsetStrict = false;
	private boolean unitStrict = false;
	private boolean downsamplingFactorsStrict = false;

	public Builder min(final String key) {

	  this.minKey = key;
	  return this;
	}

	public Builder max(final String key) {

	  this.maxKey = key;
	  return this;
	}

	public Builder resolution(final String key) {

	  this.resolutionKey = key;
	  return this;
	}

	public Builder offset(final String key) {

	  this.offsetKey = key;
	  return this;
	}

	public Builder unit(final String key) {

		  this.unitKey = key;
		  return this;
	}

	public Builder downsamplingFactors(final String key) {

	  this.downsamplingFactorsKey = key;
	  return this;
	}

	public Builder resolutionStrict() {
	  this.resolutionStrict = true;
	  return this;
	}

	public Builder downsamplingFactorsStrict() {
	  this.downsamplingFactorsStrict = true;
	  return this;
	}

	public Builder offsetStrict() {
	  this.offsetStrict = true;
	  return this;
	}

	public Builder minStrict() {
	  this.minStrict = true;
	  return this;
	}

	public Builder maxStrict() {
	  this.maxStrict = true;
	  return this;
	}

	public Builder unitStrict() {
	  this.unitStrict = true;
	  return this;
	}

	public N5GenericSingleScaleMetadataParser build() {

	  final N5GenericSingleScaleMetadataParser p = new N5GenericSingleScaleMetadataParser(minKey, maxKey, resolutionKey, offsetKey, unitKey, downsamplingFactorsKey);
	  p.resolutionKeyStrict = resolutionStrict;
	  p.downsamplingFactorsKeyStrict = downsamplingFactorsStrict;
	  p.offsetKeyStrict = offsetStrict;
	  p.unitKeyStrict = unitStrict;
	  p.minKeyStrict = minStrict;
	  p.maxKeyStrict = maxStrict;
	  return p;
	}
  }

	@Override
	public Optional<N5SingleScaleMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		try {
			final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());
			if (attributes == null)
				return Optional.empty();

			final int nd = attributes.getNumDimensions();
			final String path = node.getPath();

			final double[] resolution = getAttribute(n5, path, resolutionKey, double[].class, resolutionKeyStrict,
					x -> x.length == nd,
					() -> DoubleStream.generate(() -> 1.0).limit(nd).toArray());

			final double[] downsamplingFactors = getAttribute(n5, path, downsamplingFactorsKey, double[].class,
					downsamplingFactorsKeyStrict,
					x -> x.length == nd,
					() -> DoubleStream.generate(() -> 1.0).limit(nd).toArray());

			final String unit = getAttribute(n5, path, unitKey, String.class, unitKeyStrict,
					x -> true, () -> "pixel");

			final double min = getAttribute(n5, path, minKey, double.class, minKeyStrict,
					x -> true, () -> 0.0);

			final double max = getAttribute(n5, path, maxKey, double.class, maxKeyStrict,
					x -> true,
					() -> IntensityMetadata.maxForDataType(attributes.getDataType()));

			final Boolean isLabelMultiset = N5LabelMultisets.isLabelMultisetType(n5, node.getPath());
			final AffineTransform3D transform = N5SingleScaleMetadataParser.buildTransform(downsamplingFactors, resolution, Optional.empty());

			final double[] offset = getAttribute(n5, path, offsetKey, double[].class, offsetKeyStrict,
					x -> x.length == nd,
					() -> DoubleStream.generate(() -> Double.NaN).limit(nd).toArray());

			final int N = nd > 3 ? 3 : nd;
			if (Double.isNaN(offset[0])) {
				for (int i = 0; i < N; i++)
					offset[i] = transform.get(i, 3);
			} else {
				for (int i = 0; i < N; i++)
					transform.set(offset[i], i, 3);
			}

			final N5SingleScaleMetadata metadata = new N5SingleScaleMetadata(path, transform, downsamplingFactors,
					resolution, offset, unit, attributes, min, max, isLabelMultiset);

			return Optional.of(metadata);
		} catch (final N5Exception e) {
			return Optional.empty();
		}
	}

	private static <T> Optional<T> getAttributeOptional(final N5Reader n5, final String path, final String key, final Class<T> clazz) {

		try {
			return Optional.ofNullable(n5.getAttribute(path, key, clazz));
		} catch (final N5Exception e) {
			return Optional.empty();
		}
	}

	private static <T> T getAttribute(final N5Reader n5, final String path, final String key, final Class<T> clazz,
			final boolean strict, final Predicate<T> filter,
			final Supplier<T> defaultValue) {

		final Optional<T> optAttr = getAttributeOptional( n5, path, key, clazz ).filter(filter);
		if (strict)
			return optAttr.orElseThrow(() -> new N5Exception("Missing or invalid attribute for key: " + key ));
		else
			return optAttr.orElseGet( defaultValue );
	}
}
