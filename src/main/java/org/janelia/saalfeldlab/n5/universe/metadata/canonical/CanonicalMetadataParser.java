package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.ColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.IntColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.RGBAColorMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata.IntensityLimits;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.ParametrizedTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SequenceSpatialTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.SpatialTransform;

/**
 * A parser for the "canonical" metadata dialect.
 *
 * @author John Bogovic
 */
public class CanonicalMetadataParser implements N5MetadataParser<CanonicalMetadata> {

	protected Gson gson;

	protected Predicate<CanonicalMetadata> filter;

	public CanonicalMetadataParser() {
		this(x -> true);
	}

	public CanonicalMetadataParser(final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	public void setFilter(final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	public Gson getGson() {
		return gson;
	}

	public void setGson(Gson gson) {
		this.gson = gson;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		final String path = node.getPath();

		DatasetAttributes attrs = null;
		SpatialMetadataCanonical spatial = null;
		MultiResolutionSpatialMetadataCanonical multiscale = null;
		MultiChannelMetadataCanonical multichannel = null;
		IntensityLimits intensityLimits = null;
		ColorMetadata color = null;
		try {
			attrs = n5.getDatasetAttributes( path );
			spatial = n5.getAttribute(path, "spatialTransform", SpatialMetadataCanonical.class);
			multiscale = n5.getAttribute(path, "multiscales", MultiResolutionSpatialMetadataCanonical.class);
			multichannel = n5.getAttribute(path, "multichannel", MultiChannelMetadataCanonical.class);
			intensityLimits = n5.getAttribute(path, "intensityLimits", IntensityLimits.class);

			color = n5.getAttribute(path, "color", IntColorMetadata.class);
			if( color == null )
				color = n5.getAttribute(path, "color", RGBAColorMetadata.class);

		} catch (N5Exception e) {
		}

		if (spatial != null) {
			SpatialTransform transform = spatial.transform();
			if (transform instanceof ParametrizedTransform) {
				@SuppressWarnings("rawtypes")
				ParametrizedTransform pt = (ParametrizedTransform) transform;
				if (pt.getParameterPath() != null) {
					pt.buildTransform(pt.getParameters(n5));
				}
			} else if (transform instanceof SequenceSpatialTransform) {
				SequenceSpatialTransform seq = (SequenceSpatialTransform) transform;
				for (SpatialTransform t : seq.getTransformations()) {
					if (t instanceof ParametrizedTransform) {
						@SuppressWarnings("rawtypes")
						ParametrizedTransform pt = (ParametrizedTransform) t;
						if (pt.getParameterPath() != null) {
							pt.buildTransform(pt.getParameters(n5));
						}
					}
				}
			}
		}

		if (spatial == null && multichannel == null && multiscale == null && color == null && intensityLimits == null) {
			return Optional.empty();
		}

		if (attrs != null) {
			if (spatial != null)
				return Optional.of(new CanonicalSpatialDatasetMetadata(path, spatial, attrs, intensityLimits, color));
			else
				return Optional.of(new CanonicalDatasetMetadata(path, attrs, intensityLimits, color));
		} else if (spatial != null) {
			return Optional.of(new CanonicalSpatialMetadata(path, spatial, intensityLimits));
		} else if (multiscale != null && multiscale.getChildrenMetadata() != null) {
			return Optional.of(new CanonicalMultiscaleMetadata(path, multiscale));
		} else if (multichannel != null && multichannel.getPaths() != null) {
			return Optional.of(new CanonicalMultichannelMetadata(path, multichannel));
		} else {
			// if lots of things are present
			return Optional.empty();
		}
	}

	public CanonicalMetadata canonicalMetadata(final HashMap<String, JsonElement> attrMap) {
		return gson.fromJson(gson.toJson(attrMap), CanonicalMetadata.class);
	}

}
