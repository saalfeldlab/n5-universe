package org.janelia.saalfeldlab.n5.universe.metadata.canonical;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Optional;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.transforms.CalibratedSpatialTransform;


/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataTemplateCanonical extends AbstractMetadataTemplateParser< SpatialMetadataCanonical >{

	public SpatialMetadataTemplateCanonical( final Gson gson, final String translation )
	{
		super( gson, translation );
	}

	public Optional<SpatialMetadataCanonical> parse( final Gson gson, JsonElement elem ){

		try { 
			final String path = GsonUtils.readAttribute(elem, "path", String.class, gson);
			final CalibratedSpatialTransform transform = GsonUtils.readAttribute(elem, "spatialTransform", CalibratedSpatialTransform.class, gson);
			final Axis[] axes = GsonUtils.readAttribute(elem, "axes", Axis[].class, gson);
			final Optional<DatasetAttributes> attributes = AbstractMetadataTemplateParser.datasetAttributes(gson, elem.getAsJsonObject());

			if( attributes.isPresent())
				return Optional.of( new SpatialMetadataCanonical( path, transform, axes ));
			else
				return Optional.empty();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return Optional.empty();
	}

	@Override
	public Optional<SpatialMetadataCanonical> parseFromMap( final Gson gson, final HashMap<String, JsonElement> attributeMap ) {

		return AbstractMetadataTemplateParser.parseFromMap(gson, attributeMap, (g,e) -> {
			return this.parse(gson, e);
		});
	}

}
